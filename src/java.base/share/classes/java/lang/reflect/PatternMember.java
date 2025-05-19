/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import java.util.List;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ExecutableRepository;
import sun.reflect.generics.repository.GenericDeclRepository;
import sun.reflect.generics.scope.PatternMemberScope;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.runtime.PatternBytecodeName.mangle;

/**
 * The reflection view of a single deconstructor or pattern method. These both accept a <b>match
 * candidate</b>, determine whether a match is found, and if so, produce <b>extracted values</b>.
 *
 * <p>Like other {@link Executable}s (methods and constructors), it includes parameters. However,
 * it uses these parameters in a different way: here they are <b>out-parameters</b>, conveying
 * extracted values outward to the client.
 *
 * @param <T> the type of match candidate this pattern member accepts
 *
 * @since 26
 */
public abstract sealed class PatternMember<T> extends Executable permits Deconstructor {
    final Class<?>                          declaringClass;
    final Class<T>                          candidateType;
    final List<Parameter>                   outParameters;
    final List<PatternBinding>              patternBindings;

    final int                               modifiers;
    final int                               patternFlags;
    // Generics and annotations support
    final transient String                  signature;
    // generic info repository; lazily initialized
    transient volatile ExecutableRepository genericInfo;
    final byte[]                            annotations;

    private final byte[]                    parameterAnnotations;

    // Generics infrastructure
    // Accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, PatternMemberScope.make(this));
    }

    PatternMember<T> root;

    @Override
    PatternMember<T> getRoot() {
        return root;
    }

    /**
     * TODO make private again
     * Package-private member pattern used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang package via
     * jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass       x
     * @param candidateType        x
     * @param modifiers            x
     * @param patternFlags         x
     * @param outParameters        x
     * @param patternBindings      x
     * @param signature            x
     * @param annotations          x
     * @param parameterAnnotations x
     */
    public PatternMember(Class<?> declaringClass,
                         Class<T> candidateType,
                         int modifiers,
                         int patternFlags,
                         List<Parameter> outParameters,
                         List<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations,
                         byte[] parameterAnnotations) {
        this.declaringClass = declaringClass;
        this.candidateType = candidateType;
        this.modifiers = modifiers;
        this.patternFlags = patternFlags;
        this.outParameters = outParameters;
        this.patternBindings = patternBindings;
        this.signature = signature;
        this.annotations = annotations;
        this.parameterAnnotations = parameterAnnotations;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    /**
     * Returns the (erased) type of match candidates accepted by this pattern member. In the case
     * of a deconstructor it is the same as the declaring class.
     *
     * @return type of match candidate
     */
    public Class<T> getCandidateType() { return candidateType; }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    // Skip the usual way that executables find their Parameters
    // at least for now, we just had them passed in instead
    // Note that if this is a deconstructor of an inner member class the first eleemnt of the
    // returned array represents the owner/outer instance.
    @Override
    public Parameter[] getParameters() {
        return outParameters.toArray(Parameter[]::new);
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return sharedGetParameterAnnotations(getParameterTypes(), parameterAnnotations);
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return outParameters.stream().map(p -> p.getType()).toArray(Class<?>[]::new);
    }

    @Override
    public int getParameterCount() { return outParameters.size(); }

    // Probably not technically necessary to override
    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    @Override
    public final Class<?>[] getExceptionTypes() {
        return new Class<?>[0];
    }

    // Probably not technically necessary to override
    @Override
    public final Type[] getGenericExceptionTypes() {
        return new Type[0];
    }

    /**
     * Returns an array of arrays of {@code Annotation}s that
     * represent the annotations on the bindings, in
     * declaration order, of the {@code PatternMember} represented by
     * this object.
     *
     * @return an array of arrays that represent the annotations on
     *    the bindings, in declaration order, of
     *    the member pattern represented by this object
     */
    public Annotation[][] getBindingAnnotations() {
        return patternBindings.stream().map(pb -> pb.getAnnotations()).toArray(Annotation[][]::new);
    }

    @Override
    boolean handleParameterNumberMismatch(int resultLength, Class<?>[] parameterTypes) {
        return false;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TypeVariable<PatternMember<T>>[] getTypeParameters() {
        if (getSignature() != null) {
            return (TypeVariable<PatternMember<T>>[])getGenericInfo().getTypeParameters();
        } else
            return (TypeVariable<PatternMember<T>>[])GenericDeclRepository.EMPTY_TYPE_VARS;
    }

    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.constructorModifiers(), false);
    }

    String getSignature() {
        return signature;
    }

    @Override
    byte[] getAnnotationBytes() {
        return annotations;
    }

    @Override
    boolean hasGenericInformation() {
        return false;
    }

    ExecutableRepository getGenericInfo() {
        var genericInfo = this.genericInfo;
        // lazily initialize repository if necessary
        if (genericInfo == null) {
            // create and cache generic info repository
            genericInfo =
                    ExecutableRepository.make(getSignature(),
                            getFactory());
            this.genericInfo = genericInfo;
        }
        return genericInfo; //return cached repository
    }

    @Override
    void specificToStringHeader(StringBuilder sb) {
        sb.append(getDeclaringClass().getTypeName());
    }

    @Override
    void specificToGenericStringHeader(StringBuilder sb) {
        specificToStringHeader(sb);
    }

    String sharedToGenericString(int modifierMask, boolean isDefault) {
        try {
            StringBuilder sb = new StringBuilder();

            printModifiersIfNonzero(sb, modifierMask, isDefault);

            sb.append("pattern ");
            TypeVariable<?>[] typeparms = getTypeParameters();
            if (typeparms.length > 0) {
                sb.append(Arrays.stream(typeparms)
                        .map(Executable::typeVarBounds)
                        .collect(Collectors.joining(",", "<", "> ")));
            }

            specificToGenericStringHeader(sb);

            sb.append('(');
            StringJoiner sj = new StringJoiner(",");
            Type[] params = Arrays.stream(getPatternBindings()).map(pb -> pb.getGenericType()).toArray(Type[]::new);
            for (int j = 0; j < params.length; j++) {
                String param = params[j].getTypeName();
                if (isVarArgs() && (j == params.length - 1)) // replace T[] with T...
                    param = param.replaceFirst("\\[\\]$", "...");
                sj.add(param);
            }
            sb.append(sj.toString());
            sb.append(')');

            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    /**
     * Returns the pattern bindings of {@code PatternMember}.
     *
     * @return pattern bindings
     */
    public PatternBinding[] getPatternBindings() {
        return patternBindings.toArray(PatternBinding[]::new);
    }

    /**
     * Returns the pattern flags of {@code PatternMember}.
     *
     * @return pattern bindings
     */
    public int getPatternFlags() {
        return patternFlags;
    }

    byte[] getRawAnnotations() {
        return annotations;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return super.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations()  {
        return super.getDeclaredAnnotations();
    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return null;
    }

    String getMangledName() {
        return mangle(this.getDeclaringClass(), Arrays.stream(getPatternBindings()).map(pb -> pb.getType()).toArray(Class[]::new));
    }
}
