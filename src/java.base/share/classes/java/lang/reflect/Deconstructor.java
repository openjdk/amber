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

import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ExecutableRepository;
import sun.reflect.generics.repository.GenericDeclRepository;
import sun.reflect.generics.scope.DeconstructorScope;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.runtime.Carriers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.runtime.PatternBytecodeName.mangle;

/**
 * {@code Deconstructor} provides information about, and access to, a single
 * deconstructor for a class.
 *
 * @param <T> the class in which the constructor is declared
 *
 * @see Member
 * @see Class
 * @see Class#getDeconstructors()
 * @see Class#getDeconstructor(Class[])
 * @see Class#getDeclaredDeconstructors()
 * @see Class#getDeclaredDeconstructor(Class[])
 *
 * @since 24
 */
public final class Deconstructor<T> extends Executable {
    private final Class<T>            clazz;
    private final int                 slot;
    private ArrayList<PatternBinding> patternBindings;

    private final int                 modifiers;
    private final int                 patternFlags;
    // Generics and annotations support
    private final transient String    signature;
    // generic info repository; lazily initialized
    private transient volatile ExecutableRepository genericInfo;
    private final byte[]              annotations;

    // Generics infrastructure
    // Accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, DeconstructorScope.make(this));
    }

    private Deconstructor<T> root;

    @Override
    Deconstructor<T> getRoot() {
        return root;
    }

    /**
     * TODO make private again
     * Package-private deconstructor used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass x
     * @param modifiers x
     * @param patternFlags x
     * @param slot x
     * @param patternBindings x
     * @param signature x
     * @param annotations x
     *
     */
    public Deconstructor(Class<T> declaringClass,
                         int modifiers,
                         int patternFlags,
                         int slot,
                         ArrayList<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations) {
        this.clazz = declaringClass;
        this.modifiers = modifiers;
        this.patternFlags = patternFlags;
        this.slot = slot;
        this.patternBindings = patternBindings;
        this.signature = signature;
        this.annotations = annotations;
    }

    /**
     * Package-private routine (exposed to java.lang.Class via
     * ReflectAccess) which returns a copy of this Constructor. The copy's
     * "root" field points to this Deconstructor.
     */
    Deconstructor<T> copy() {
        // This routine enables sharing of ConstructorAccessor objects
        // among Deconstructor objects which refer to the same underlying
        // method in the VM. (All of this contortion is only necessary
        // because of the "accessibility" bit in AccessibleObject,
        // which implicitly requires that new java.lang.reflect
        // objects be fabricated for each reflective call on Class
        // objects.)
        if (this.root != null)
            throw new IllegalArgumentException("Can not copy a non-root Constructor");

        Deconstructor<T> res = new Deconstructor<>(clazz,
                                               modifiers,
                                               patternFlags,
                                               slot,
                                               patternBindings,
                                               signature,
                                               annotations);
        res.root = this;
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * <p> A {@code SecurityException} is also thrown if this object is a
     * {@code Deconstructor} object for the class {@code Class} and {@code flag}
     * is true. </p>
     *
     * @param flag {@inheritDoc}
     *
     * @throws InaccessibleObjectException {@inheritDoc}
     * @throws SecurityException if the request is denied by the security manager
     *         or this is a constructor for {@code java.lang.Class}
     *
     */
    @Override
    @CallerSensitive
    public void setAccessible(boolean flag) {
        AccessibleObject.checkPermission();
        if (flag) {
            checkCanSetAccessible(Reflection.getCallerClass());
        }
        setAccessible0(flag);
    }

    @Override
    void checkCanSetAccessible(Class<?> caller) {
        checkCanSetAccessible(caller, clazz);
        if (clazz == Class.class) {
            // can we change this to InaccessibleObjectException?
            throw new SecurityException("Cannot make a java.lang.Class"
                                        + " constructor accessible");
        }
    }

    /**
     * Returns the {@code Class} object representing the class that
     * declares the constructor represented by this object.
     */
    @Override
    public Class<T> getDeclaringClass() {
        return clazz;
    }

    /**
     * Returns the name of this constructor, as a string.  This is
     * the binary name of the constructor's declaring class.
     */
    @Override
    public String getName() {
        return getDeclaringClass().getName();
    }

    /**
     * {@inheritDoc}
     * @jls 8.8.3 Constructor Modifiers
     */
    @Override
    public int getModifiers() {
        return modifiers;
    }

    /**
     * {@inheritDoc}
     * @jls X.X.X Deconstructor Modifiers
     */
    @Override
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return new Annotation[0][];
    }

    /**
     * Returns an array of arrays of {@code Annotation}s that
     * represent the annotations on the bindings, in
     * declaration order, of the {@code Deconstructor} represented by
     * this object.
     *
     * @return an array of arrays that represent the annotations on
     *    the bindings, in declaration order, of
     *    the deconstructor represented by this object
     */
    public Annotation[][] getBindingAnnotations() {
        return patternBindings.stream().map(pb -> pb.getAnnotations()).toArray(Annotation[][]::new);
    }

    @Override
    boolean handleParameterNumberMismatch(int resultLength, Class<?>[] parameterTypes) {
        return false;
    }

    /**
     * {@inheritDoc}
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @since 1.5
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TypeVariable<Deconstructor<T>>[] getTypeParameters() {
      if (getSignature() != null) {
        return (TypeVariable<Deconstructor<T>>[])getGenericInfo().getTypeParameters();
      } else
          return (TypeVariable<Deconstructor<T>>[])GenericDeclRepository.EMPTY_TYPE_VARS;
    }

    @Override
    Class<?>[] getSharedParameterTypes() {
        return new Class<?>[0];
    }

    @Override
    Class<?>[] getSharedExceptionTypes() {
        return new Class<?>[0];
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return new Class<?>[0];
    }

    @Override
    public int getParameterCount() {
        return 0;
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return new Class<?>[0];
    }

    /**
     * Returns a string describing this {@code Deconstructor},
     * including type parameters.  The string is formatted as the
     * constructor access modifiers, if any, followed by an
     * angle-bracketed comma separated list of the constructor's type
     * parameters, if any, including  informative bounds of the
     * type parameters, if any, followed by the fully-qualified name of the
     * declaring class, followed by a parenthesized, comma-separated
     * list of the constructor's generic formal parameter types.
     *
     * If this constructor was declared to take a variable number of
     * arguments, instead of denoting the last parameter as
     * "<code><i>Type</i>[]</code>", it is denoted as
     * "<code><i>Type</i>...</code>".
     *
     * A space is used to separate access modifiers from one another
     * and from the type parameters or class name.  If there are no
     * type parameters, the type parameter list is elided; if the type
     * parameter list is present, a space separates the list from the
     * class name.  If the constructor is declared to throw
     * exceptions, the parameter list is followed by a space, followed
     * by the word "{@code throws}" followed by a
     * comma-separated list of the generic thrown exception types.
     *
     * <p>The only possible modifiers for constructors are the access
     * modifiers {@code public}, {@code protected} or
     * {@code private}.  Only one of these may appear, or none if the
     * constructor has default (package) access.
     *
     * @return a string describing this {@code Constructor},
     * include type parameters
     *
     * @since 1.5
     * @jls 8.8.3 Constructor Modifiers
     * @jls 8.9.2 Enum Body Declarations
     */
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
     * Returns the pattern bindings of this deconstructor.
     *
     * @return pattern bindings
     */
    public PatternBinding[] getPatternBindings() {
        return patternBindings.toArray(PatternBinding[]::new);
    }

    /**
     * Returns the pattern flags of this deconstructor.
     *
     * @return pattern bindings
     */
    public int getPatternFlags() {
        return patternFlags;
    }

    /**
     * Initiate pattern matching of this deconstructor on the designated matchCandidate.
     *
     * @param matchCandidate the match candidate to perform pattern matching over.
     *
     * @return an array object created as a result of pattern matching
     *
     * @throws    IllegalAccessException    if this {@code Constructor} object
     *              is enforcing Java language access control and the underlying
     *              constructor is inaccessible.
     * @throws    MatchException if the pattern matching provoked
     *              by this deconstructor fails.
     */
    public Object[] invoke(Object matchCandidate)
        throws IllegalAccessException, MatchException
    {
        String underlyingName = mangle(this.getDeclaringClass(), Arrays.stream(getPatternBindings()).map(pb -> pb.getType()).toArray(Class[]::new));

        try {
            Method method = this.getDeclaringClass().getDeclaredMethod(underlyingName, matchCandidate.getClass());
            method.setAccessible(override);
            Object carrier = method.invoke(matchCandidate, matchCandidate);

            Class<?>[] bindingClasses = Arrays.stream(this.getPatternBindings()).map(d -> d.getType()).toArray(Class[]::new);
            MethodType methodType = MethodType.methodType(Object.class, bindingClasses);

            ArrayList<Object> ret = new ArrayList<>();
            for (int i = 0; i < bindingClasses.length; i++) {
                ret.add(Carriers.component(methodType, i).invoke(carrier));
            }

            return ret.stream().toArray();
        } catch (Throwable e) {
            throw new MatchException(e.getMessage(), e);
        }
    }

    byte[] getRawAnnotations() {
        return annotations;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException  {@inheritDoc}
     * @since 1.5
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return super.getAnnotation(annotationClass);
    }

    /**
     * {@inheritDoc}
     * @since 1.5
     */
    @Override
    public Annotation[] getDeclaredAnnotations()  {
        return super.getDeclaredAnnotations();
    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return null;
    }
}
