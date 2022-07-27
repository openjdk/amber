/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.runtime;

import java.io.FilePermission;
import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.NESTMATE;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.STRONG;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.stream.Stream;

import jdk.internal.javac.PreviewFeature;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_FINAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import jdk.internal.misc.VM;
import jdk.internal.org.objectweb.asm.Label;
import static jdk.internal.org.objectweb.asm.Opcodes.AASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_FINAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static jdk.internal.org.objectweb.asm.Opcodes.ANEWARRAY;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.InstructionAdapter;
import sun.util.logging.PlatformLogger;

/**
 * Bootstrap methods for linking {@code invokedynamic} call sites that implement
 * the selection functionality of the {@code switch} statement.  The bootstraps
 * take additional static arguments corresponding to the {@code case} labels
 * of the {@code switch}, implicitly numbered sequentially from {@code [0..N)}.
 *
 * @since 17
 */
@PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING)
public class SwitchBootstraps {

    private SwitchBootstraps() {}

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle DO_TYPE_SWITCH;
    private static final MethodHandle DO_ENUM_SWITCH;

    static {
        try {
            DO_TYPE_SWITCH = LOOKUP.findStatic(SwitchBootstraps.class, "doTypeSwitch",
                                           MethodType.methodType(int.class, Object.class, int.class, Object[].class));
            DO_ENUM_SWITCH = LOOKUP.findStatic(SwitchBootstraps.class, "doEnumSwitch",
                                           MethodType.methodType(int.class, Enum.class, int.class, Object[].class));
        }
        catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a target of a reference type.  The static
     * arguments are an array of case labels which must be non-null and of type
     * {@code String} or {@code Integer} or {@code Class}.
     * <p>
     * The type of the returned {@code CallSite}'s method handle will have
     * a return type of {@code int}.   It has two parameters: the first argument
     * will be an {@code Object} instance ({@code target}) and the second
     * will be {@code int} ({@code restart}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     * <p>
     * If the {@code target} is not {@code null}, then the method of the call site
     * returns the index of the first element in the {@code labels} array starting from
     * the {@code restart} index matching one of the following conditions:
     * <ul>
     *   <li>the element is of type {@code Class} that is assignable
     *       from the target's class; or</li>
     *   <li>the element is of type {@code String} or {@code Integer} and
     *       equals to the target.</li>
     * </ul>
     * <p>
     * If no element in the {@code labels} array matches the target, then
     * the method of the call site return the length of the {@code labels} array.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with two parameters,
     *                       a reference type, an {@code int}, and {@code int} as a return type.
     * @param labels case labels - {@code String} and {@code Integer} constants
     *               and {@code Class} instances, in any combination
     * @return a {@code CallSite} returning the first matching element as described above
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element in the labels array is null, if the
     * invocation type is not not a method type of first parameter of a reference type,
     * second parameter of type {@code int} and with {@code int} as its return type,
     * or if {@code labels} contains an element that is not of type {@code String},
     * {@code Integer} or {@code Class}.
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite typeSwitch(MethodHandles.Lookup lookup,
                                      String invocationName,
                                      MethodType invocationType,
                                      Object... labels) {
        if (invocationType.parameterCount() != 2
            || (!invocationType.returnType().equals(int.class))
            || invocationType.parameterType(0).isPrimitive()
            || !invocationType.parameterType(1).equals(int.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        requireNonNull(labels);

        labels = labels.clone();
        Stream.of(labels).forEach(SwitchBootstraps::verifyLabel);

        MethodHandle target = MethodHandles.insertArguments(DO_TYPE_SWITCH, 2, (Object) labels);
        return new ConstantCallSite(target);
    }

    private static void verifyLabel(Object label) {
        if (label == null) {
            throw new IllegalArgumentException("null label found");
        }
        Class<?> labelClass = label.getClass();
        if (labelClass != Class.class &&
            labelClass != String.class &&
            labelClass != Integer.class) {
            throw new IllegalArgumentException("label with illegal type found: " + label.getClass());
        }
    }

    private static int doTypeSwitch(Object target, int startIndex, Object[] labels) {
        if (target == null)
            return -1;

        // Dumbest possible strategy
        Class<?> targetClass = target.getClass();
        for (int i = startIndex; i < labels.length; i++) {
            Object label = labels[i];
            if (label instanceof Class<?> c) {
                if (c.isAssignableFrom(targetClass))
                    return i;
            } else if (label instanceof Integer constant) {
                if (target instanceof Number input && constant.intValue() == input.intValue()) {
                    return i;
                } else if (target instanceof Character input && constant.intValue() == input.charValue()) {
                    return i;
                }
            } else if (label.equals(target)) {
                return i;
            }
        }

        return labels.length;
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a target of a reference type.  The static
     * arguments are an array of case labels which must be non-null and of type
     * {@code String} or {@code Integer} or {@code Class}.
     * <p>
     * The type of the returned {@code CallSite}'s method handle will have
     * a return type of {@code int}.   It has two parameters: the first argument
     * will be an {@code Object} instance ({@code target}) and the second
     * will be {@code int} ({@code restart}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     * <p>
     * If the {@code target} is not {@code null}, then the method of the call site
     * returns the index of the first element in the {@code labels} array starting from
     * the {@code restart} index matching one of the following conditions:
     * <ul>
     *   <li>the element is of type {@code Class} that is assignable
     *       from the target's class; or</li>
     *   <li>the element is of type {@code String} or {@code Integer} and
     *       equals to the target.</li>
     * </ul>
     * <p>
     * If no element in the {@code labels} array matches the target, then
     * the method of the call site return the length of the {@code labels} array.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with two parameters,
     *                       a reference type, an {@code int}, and {@code int} as a return type.
     * @param labels case labels - {@code String} and {@code Integer} constants
     *               and {@code Class} instances, in any combination
     * @return a {@code CallSite} returning the first matching element as described above
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element in the labels array is null, if the
     * invocation type is not not a method type of first parameter of a reference type,
     * second parameter of type {@code int} and with {@code int} as its return type,
     * or if {@code labels} contains an element that is not of type {@code String},
     * {@code Integer} or {@code Class}.
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite typeSwitch3(MethodHandles.Lookup lookup,
                                      String invocationName,
                                      MethodType invocationType,
                                      Object... labels) {
        if (invocationType.parameterCount() != 2
            || (!invocationType.returnType().equals(int.class))
            || invocationType.parameterType(0).isPrimitive()
            || !invocationType.parameterType(1).equals(int.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        requireNonNull(labels);

        labels = labels.clone();
        Stream.of(labels).forEach(SwitchBootstraps::verifyLabel);

        boolean hasOnlyClassLabels = Stream.of(labels).allMatch(l -> l instanceof Class<?>);
        MethodHandle target;

        if (hasOnlyClassLabels) {
            try {
                target = lookup.findStatic(generateInnerClass(lookup, labels), "typeSwitch", MethodType.methodType(int.class, Object.class, int.class));
            } catch (LambdaConversionException | NoSuchMethodException | IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            target = MethodHandles.insertArguments(DO_TYPE_SWITCH, 2, (Object) labels);
        }

        return new ConstantCallSite(target);
    }

    static final int CLASSFILE_VERSION = VM.classFileVersion();
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    @SuppressWarnings("removal")
    private static Class<?> generateInnerClass(MethodHandles.Lookup caller, Object[] labels) throws LambdaConversionException {
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(CLASSFILE_VERSION, ACC_SUPER + ACC_FINAL + ACC_SYNTHETIC,
                 caller.lookupClass().getPackageName().replace('.', '/') + "/TypeSwitch", null, //XXX: default package
                 JAVA_LANG_OBJECT, new String[0]);

        InstructionAdapter mv
                = new InstructionAdapter(cw.visitMethod(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                    "typeSwitch", "(Ljava/lang/Object;I)I", null, new String[0]));

        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        Label swtch = new Label();
        mv.visitJumpInsn(IFNONNULL, swtch);
        mv.iconst(-1);
        mv.visitInsn(IRETURN);
        mv.visitLabel(swtch);
        mv.visitVarInsn(ILOAD, 1);
        Label dflt = new Label();
        record Element(Label label, Class<?> clazz) {}
        List<Element> cases = new ArrayList<Element>();
        for (Object o : labels) {
            cases.add(new Element(new Label(), (Class<?>) o));
        }
        mv.visitTableSwitchInsn(0, labels.length - 1, dflt, cases.stream().map(e -> e.label()).toArray(s -> new Label[s]));
        for (int idx = 0; idx < cases.size(); idx++) {
            Element element = cases.get(idx);
            mv.visitLabel(element.label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(INSTANCEOF, element.clazz().getName().replace('.', '/'));
            mv.visitJumpInsn(IFEQ, idx + 1 < cases.size() ? cases.get(idx + 1).label() : dflt);
            mv.iconst(idx);
            mv.visitInsn(IRETURN);
        }
        mv.visitLabel(dflt);
        mv.iconst(cases.size());
        mv.visitInsn(IRETURN);
        // Maxs computed by ClassWriter.COMPUTE_MAXS, these arguments ignored
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        cw.visitEnd();

        // Define the generated class in this VM.

        final byte[] classBytes = cw.toByteArray();
        // If requested, dump out to a file for debugging purposes
//        var dumper = ProxyClassesDumper.getInstance("/tmp/classes/classes");
//        if (dumper != null) {
//            AccessController.doPrivileged(new PrivilegedAction<>() {
//                @Override
//                public Void run() {
//                    dumper.dumpClass("foobar", classBytes);
//                    return null;
//                }
//            }, null,
//            new FilePermission("<<ALL FILES>>", "read, write"),
//            // createDirectories may need it
//            new PropertyPermission("user.dir", "read"));
//        }
        try {
            // this class is linked at the indy callsite; so define a hidden nestmate
            MethodHandles.Lookup lookup;
            lookup = caller.defineHiddenClass(classBytes, true, NESTMATE, STRONG);
            return lookup.lookupClass();
        } catch (IllegalAccessException e) {
            throw new LambdaConversionException("Exception defining lambda proxy class", e);
        } catch (Throwable t) {
            throw new InternalError(t);
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a target of an enum type. The static
     * arguments are used to encode the case labels associated to the switch
     * construct, where each label can be encoded in two ways:
     * <ul>
     *   <li>as a {@code String} value, which represents the name of
     *       the enum constant associated with the label</li>
     *   <li>as a {@code Class} value, which represents the enum type
     *       associated with a type test pattern</li>
     * </ul>
     * <p>
     * The returned {@code CallSite}'s method handle will have
     * a return type of {@code int} and accepts two parameters: the first argument
     * will be an {@code Enum} instance ({@code target}) and the second
     * will be {@code int} ({@code restart}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     * <p>
     * If the {@code target} is not {@code null}, then the method of the call site
     * returns the index of the first element in the {@code labels} array starting from
     * the {@code restart} index matching one of the following conditions:
     * <ul>
     *   <li>the element is of type {@code Class} that is assignable
     *       from the target's class; or</li>
     *   <li>the element is of type {@code String} and equals to the target
     *       enum constant's {@link Enum#name()}.</li>
     * </ul>
     * <p>
     * If no element in the {@code labels} array matches the target, then
     * the method of the call site return the length of the {@code labels} array.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller. When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with two parameters,
     *                       an enum type, an {@code int}, and {@code int} as a return type.
     * @param labels case labels - {@code String} constants and {@code Class} instances,
     *               in any combination
     * @return a {@code CallSite} returning the first matching element as described above
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element in the labels array is null, if the
     * invocation type is not a method type whose first parameter type is an enum type,
     * second parameter of type {@code int} and whose return type is {@code int},
     * or if {@code labels} contains an element that is not of type {@code String} or
     * {@code Class} of the target enum type.
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite enumSwitch(MethodHandles.Lookup lookup,
                                      String invocationName,
                                      MethodType invocationType,
                                      Object... labels) {
        if (invocationType.parameterCount() != 2
            || (!invocationType.returnType().equals(int.class))
            || invocationType.parameterType(0).isPrimitive()
            || !invocationType.parameterType(0).isEnum()
            || !invocationType.parameterType(1).equals(int.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        requireNonNull(labels);

        labels = labels.clone();

        Class<?> enumClass = invocationType.parameterType(0);
        labels = Stream.of(labels).map(l -> convertEnumConstants(lookup, enumClass, l)).toArray();

        MethodHandle target =
                MethodHandles.insertArguments(DO_ENUM_SWITCH, 2, (Object) labels);
        target = target.asType(invocationType);

        return new ConstantCallSite(target);
    }

    private static <E extends Enum<E>> Object convertEnumConstants(MethodHandles.Lookup lookup, Class<?> enumClassTemplate, Object label) {
        if (label == null) {
            throw new IllegalArgumentException("null label found");
        }
        Class<?> labelClass = label.getClass();
        if (labelClass == Class.class) {
            if (label != enumClassTemplate) {
                throw new IllegalArgumentException("the Class label: " + label +
                                                   ", expected the provided enum class: " + enumClassTemplate);
            }
            return label;
        } else if (labelClass == String.class) {
            @SuppressWarnings("unchecked")
            Class<E> enumClass = (Class<E>) enumClassTemplate;
            try {
                return ConstantBootstraps.enumConstant(lookup, (String) label, enumClass);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("label with illegal type found: " + labelClass +
                                               ", expected label of type either String or Class");
        }
    }

    private static int doEnumSwitch(Enum<?> target, int startIndex, Object[] labels) {
        if (target == null)
            return -1;

        // Dumbest possible strategy
        Class<?> targetClass = target.getClass();
        for (int i = startIndex; i < labels.length; i++) {
            Object label = labels[i];
            if (label instanceof Class<?> c) {
                if (c.isAssignableFrom(targetClass))
                    return i;
            } else if (label == target) {
                return i;
            }
        }

        return labels.length;
    }

}

final class ProxyClassesDumper {
    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final char[] BAD_CHARS = {
        '\\', ':', '*', '?', '"', '<', '>', '|'
    };
    private static final String[] REPLACEMENT = {
        "%5C", "%3A", "%2A", "%3F", "%22", "%3C", "%3E", "%7C"
    };

    private final Path dumpDir;

    @SuppressWarnings("removal")
    public static ProxyClassesDumper getInstance(String path) {
        if (null == path) {
            return null;
        }
        try {
            path = path.trim();
            final Path dir = Path.of(path.isEmpty() ? "." : path);
            AccessController.doPrivileged(new PrivilegedAction<>() {
                    @Override
                    public Void run() {
                        validateDumpDir(dir);
                        return null;
                    }
                }, null, new FilePermission("<<ALL FILES>>", "read, write"));
            return new ProxyClassesDumper(dir);
        } catch (InvalidPathException ex) {
            PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
                          .warning("Path " + path + " is not valid - dumping disabled", ex);
        } catch (IllegalArgumentException iae) {
            PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
                          .warning(iae.getMessage() + " - dumping disabled");
        }
        return null;
    }

    private ProxyClassesDumper(Path path) {
        dumpDir = Objects.requireNonNull(path);
    }

    private static void validateDumpDir(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Directory " + path + " does not exist");
        } else if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path " + path + " is not a directory");
        } else if (!Files.isWritable(path)) {
            throw new IllegalArgumentException("Directory " + path + " is not writable");
        }
    }

    public static String encodeForFilename(String className) {
        final int len = className.length();
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = className.charAt(i);
            // control characters
            if (c <= 31) {
                sb.append('%');
                sb.append(HEX[c >> 4 & 0x0F]);
                sb.append(HEX[c & 0x0F]);
            } else {
                int j = 0;
                for (; j < BAD_CHARS.length; j++) {
                    if (c == BAD_CHARS[j]) {
                        sb.append(REPLACEMENT[j]);
                        break;
                    }
                }
                if (j >= BAD_CHARS.length) {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }

    public void dumpClass(String className, final byte[] classBytes) {
        Path file;
        try {
            file = dumpDir.resolve(encodeForFilename(className) + ".class");
        } catch (InvalidPathException ex) {
            PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
                          .warning("Invalid path for class " + className);
            return;
        }

        try {
            Path dir = file.getParent();
            Files.createDirectories(dir);
            Files.write(file, classBytes);
        } catch (Exception ignore) {
            PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
                          .warning("Exception writing to path at " + file.toString());
            // simply don't care if this operation failed
        }
    }
}
