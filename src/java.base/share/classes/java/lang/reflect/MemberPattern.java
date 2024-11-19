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
import sun.reflect.generics.scope.MemberPatternScope;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.runtime.Carriers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.runtime.PatternBytecodeName.mangle;

/**
 * {@code MemberPattern} provides information about, and access to, a single
 * member pattern for a class.
 *
 * @param <T> the class in which the member pattern is declared
 *
 * @see Executable
 * @see Class
 *
 * @since 24
 */
public abstract sealed class MemberPattern<T> extends Executable permits Deconstructor {
    final Class<T>                          clazz;
    final int                               slot;
    final Class<?>[]                        parameterTypes;
    final Class<?>[]                        exceptionTypes;
    final ArrayList<PatternBinding>         patternBindings;

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
        return CoreReflectionFactory.make(this, MemberPatternScope.make(this));
    }

    MemberPattern<T> root;

    @Override
    MemberPattern<T> getRoot() {
        return root;
    }

    /**
     * TODO make private again
     * Package-private member pattern used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass x
     * @param parameterTypes x
     * @param checkedExceptions x
     * @param modifiers x
     * @param patternFlags x
     * @param slot x
     * @param patternBindings x
     * @param signature x
     * @param annotations x
     * @param parameterAnnotations x
     *
     */
    public MemberPattern(Class<T> declaringClass,
                         Class<?>[] parameterTypes,
                         Class<?>[] checkedExceptions,
                         int modifiers,
                         int patternFlags,
                         int slot,
                         ArrayList<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations,
                         byte[] parameterAnnotations) {
        this.clazz = declaringClass;
        this.parameterTypes = parameterTypes;
        this.exceptionTypes = checkedExceptions;
        this.modifiers = modifiers;
        this.patternFlags = patternFlags;
        this.slot = slot;
        this.patternBindings = patternBindings;
        this.signature = signature;
        this.annotations = annotations;
        this.parameterAnnotations = parameterAnnotations;
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
     * @jls X.X.X MemberPattern Modifiers
     */
    @Override
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return sharedGetParameterAnnotations(parameterTypes, parameterAnnotations);
    }

    byte[] getRawParameterAnnotations() {
        return parameterAnnotations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes.length == 0 ? parameterTypes : parameterTypes.clone();
    }

    /**
     * {@inheritDoc}
     * @since 1.8
     */
    public int getParameterCount() { return parameterTypes.length; }

    /**
     * {@inheritDoc}
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @throws TypeNotPresentException {@inheritDoc}
     * @throws MalformedParameterizedTypeException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getExceptionTypes() {
        return exceptionTypes.length == 0 ? exceptionTypes : exceptionTypes.clone();
    }

    /**
     * {@inheritDoc}
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @throws TypeNotPresentException {@inheritDoc}
     * @throws MalformedParameterizedTypeException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }

    /**
     * Returns an array of arrays of {@code Annotation}s that
     * represent the annotations on the bindings, in
     * declaration order, of the {@code MemberPattern} represented by
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

    /**
     * {@inheritDoc}
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @since 1.5
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TypeVariable<MemberPattern<T>>[] getTypeParameters() {
      if (getSignature() != null) {
        return (TypeVariable<MemberPattern<T>>[])getGenericInfo().getTypeParameters();
      } else
          return (TypeVariable<MemberPattern<T>>[])GenericDeclRepository.EMPTY_TYPE_VARS;
    }



    /**
     * Returns a string describing this {@code MemberPattern},
     * including type parameters.  The string is formatted as the
     * constructor access modifiers, if any, followed by an
     * angle-bracketed comma separated list of the constructor's type
     * parameters, if any, including  informative bounds of the
     * type parameters, if any, followed by the fully-qualified name of the
     * declaring class, followed by a parenthesized, comma-separated
     * list of the {@code MemberPattern}'s generic formal parameter types.
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
     * @return a string describing this {@code MemberPattern},
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
     * Returns the pattern bindings of {@code MemberPattern}.
     *
     * @return pattern bindings
     */
    public PatternBinding[] getPatternBindings() {
        return patternBindings.toArray(PatternBinding[]::new);
    }

    /**
     * Returns the pattern flags of {@code MemberPattern}.
     *
     * @return pattern bindings
     */
    public int getPatternFlags() {
        return patternFlags;
    }

    /**
     * Initiate pattern matching of this {@code MemberPattern} on the designated {@code matchCandidate}.
     *
     * @param matchCandidate the match candidate to perform pattern matching over.
     *
     * @return an array object created as a result of pattern matching
     *
     * @throws    IllegalAccessException    if this {@code MemberPattern} object
     *              is enforcing Java language access control and the underlying
     *              constructor is inaccessible.
     * @throws    MatchException if the pattern matching provoked
     *              by this {@code MemberPattern} fails.
     */
    public Object[] invoke(Object matchCandidate)
        throws IllegalAccessException, MatchException
    {
        String underlyingName = getMangledName();

        try {
            Method method = getDeclaringClass().getDeclaredMethod(underlyingName, matchCandidate.getClass());
            method.setAccessible(override);
            return (Object[])Carriers.boxedComponentValueArray(
                MethodType.methodType(
                    Object.class,
                    Arrays.stream(this.getPatternBindings())
                          .map(PatternBinding::getType)
                          .toArray(Class[]::new)
                )
            ).invoke(
                method.invoke(matchCandidate, matchCandidate)
            );
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

    String getMangledName() {
        return mangle(this.getDeclaringClass(), Arrays.stream(getPatternBindings()).map(pb -> pb.getType()).toArray(Class[]::new));
    }
}
