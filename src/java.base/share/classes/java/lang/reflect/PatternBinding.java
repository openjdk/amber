/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.SharedSecrets;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.FieldRepository;
import sun.reflect.generics.scope.ClassScope;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@code PatternBinding} provides information about, and dynamic access to,
 * an initialized binding of a deconstructor.
 *
 * @see Deconstructor#getPatternBindings() ()
 * @see Deconstructor
 * @since 23
 */
public final class PatternBinding implements AnnotatedElement {
    // declaring class
    private Deconstructor<?>                   declaringDeconstructor;
    private String                             name;
    private Class<?>                           type;
    private String                             signature;
    private int                                slot;
    // generic info repository; lazily initialized
    private transient volatile FieldRepository genericInfo;
    private byte[]                             annotations;
    private byte[]                             typeAnnotations;
    private PatternBinding                     root;

    /**
     * TODO make private again
     * Package-private pattern binding used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringDeconstructor x
     * @param name x
     * @param type x
     * @param signature x
     * @param slot x
     * @param annotations x
     * @param typeAnnotations x
     *
     */
    public PatternBinding(Deconstructor<?> declaringDeconstructor,
                          String name,
                          Class<?> type,
                          String signature,
                          int slot,
                          byte[] annotations,
                          byte[] typeAnnotations) {
        this.declaringDeconstructor = declaringDeconstructor;
        this.name = name;
        this.type = type;
        this.signature = signature;
        this.slot = slot;
        this.annotations = annotations;
        this.typeAnnotations = typeAnnotations;
    }

    /**
     * Returns the name of this pattern binding.
     *
     * @return the name of this pattern binding
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a {@code Class} that identifies the declared type for this
     * pattern binding.
     *
     * @return a {@code Class} identifying the declared type of the component
     * represented by this pattern binding
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Returns a {@code String} that describes the generic type signature for
     * this pattern binding.
     *
     * @return a {@code String} that describes the generic type signature for
     * this pattern binding
     *
     * @jvms 4.7.9.1 Signatures
     */
    public String getGenericSignature() {
        return signature;
    }

    /**
     * Returns a {@code Type} object that represents the declared type for
     * this pattern binding.
     *
     * <p>If the declared type of the pattern binding is a parameterized type,
     * the {@code Type} object returned reflects the actual type arguments used
     * in the source code.
     *
     * <p>If the type of the underlying pattern binding is a type variable or a
     * parameterized type, it is created. Otherwise, it is resolved.
     *
     * @return a {@code Type} object that represents the declared type for
     *         this pattern binding
     * @throws GenericSignatureFormatError if the generic pattern binding
     *         signature does not conform to the format specified in
     *         <cite>The Java Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if the generic type
     *         signature of the underlying pattern binding refers to a non-existent
     *         type declaration
     * @throws MalformedParameterizedTypeException if the generic
     *         signature of the underlying pattern binding refers to a parameterized
     *         type that cannot be instantiated for any reason
     */
    public Type getGenericType() {
        if (getGenericSignature() != null)
            return getGenericInfo().getGenericType();
        else
            return getType();
    }

    // Accessor for generic info repository
    private FieldRepository getGenericInfo() {
        var genericInfo = this.genericInfo;
        // lazily initialize repository if necessary
        if (genericInfo == null) {
            // create and cache generic info repository
            genericInfo = FieldRepository.make(getGenericSignature(), getFactory()); // TODO
            this.genericInfo = genericInfo;
        }
        return genericInfo; //return cached repository
    }

    // Accessor for factory
    private GenericsFactory getFactory() {
        Class<?> c = getDeclaringDeconstructor().getDeclaringClass();
        // create scope and factory
        return CoreReflectionFactory.make(c, ClassScope.make(c));
    }

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a type to specify
     * the declared type of this pattern binding.
     *
     * @return an object representing the declared type of this pattern binding
     */
    public AnnotatedType getAnnotatedType() {
        return TypeAnnotationParser.buildAnnotatedType(typeAnnotations,
                SharedSecrets.getJavaLangAccess().
                        getConstantPool(getDeclaringDeconstructor().getDeclaringClass()),
                this,
                getDeclaringDeconstructor().getDeclaringClass(),
                getGenericType(),
                TypeAnnotation.TypeAnnotationTarget.FIELD);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotation returned by this method is a
     * declaration annotation.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return annotationClass.cast(declaredAnnotations().get(annotationClass));
    }

    private transient volatile Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

    private Map<Class<? extends Annotation>, Annotation> declaredAnnotations() {
        Map<Class<? extends Annotation>, Annotation> declAnnos;
        if ((declAnnos = declaredAnnotations) == null) {
            synchronized (this) {
                if ((declAnnos = declaredAnnotations) == null) {
                    PatternBinding root = this.root;
                    if (root != null) {
                        declAnnos = root.declaredAnnotations();
                    } else {
                        Annotation[][] ret = AnnotationParser.parseParameterAnnotations(
                                annotations,
                                SharedSecrets.getJavaLangAccess()
                                        .getConstantPool(getDeclaringDeconstructor().getDeclaringClass()),
                                getDeclaringDeconstructor().getDeclaringClass());

                        int myIndex = slot;
                        declAnnos = Arrays.stream(ret[myIndex]).collect(Collectors.toMap(a -> a.annotationType(), a-> a));
                    }
                    declaredAnnotations = declAnnos;
                }
            }
        }
        return declAnnos;
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getDeclaredAnnotations() { return AnnotationParser.toArray(declaredAnnotations()); }

    /**
     * Returns the deconstructor which declares this pattern binding.
     *
     * @return The deconstructor declaring this pattern binding.
     */
    public Deconstructor<?> getDeclaringDeconstructor() {
        return declaringDeconstructor;
    }
}
