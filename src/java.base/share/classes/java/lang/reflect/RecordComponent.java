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

/**
 * A {@code RecordComponent} provides information about, and dynamic access to, a
 * record component in a record class. Record components can only be created by the VM
 * runtime, thus no public constructor is provided.
 *
 * @see AnnotatedElement
 * @see java.lang.Class
 *
 * @since 14
 */
public final
class RecordComponent implements AnnotatedElement {
    // declaring class
    private Class<?> clazz;
    private String name;
    private Class<?> type;
    private Method accessor;
    private String signature;
    // generic info repository; lazily initialized
    private transient FieldRepository genericInfo;
    private byte[] annotations;
    private byte[] typeAnnotations;
    private RecordComponent root;

    // only the JVM can create record components
    private RecordComponent() {}

    /**
     * Returns the name of the record component represented by this {@code RecordComponent} object.
     *
     * @return the name of the record component represented by this {@code RecordComponent} object.
     */
    public String getName() {
        return name;
    }

    private Class<?> getDeclaringClass() {
        return clazz;
    }

    /**
     * Returns a {@code Class} object that identifies the
     * declared type for the record component represented by this
     * {@code RecordComponent} object.
     *
     * @return a {@code Class} object identifying the declared
     * type of the record component represented by this object
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Returns a {@code String} object that identifies the
     * generic type.
     *
     * @return a {@code String} object identifying the generic declared
     * type of the record component represented by this object
     */
    public String getGenericSignature() {
        return signature;
    }

    /**
     * Returns a {@code Type} object that represents the declared type for
     * the record component represented by this {@code RecordComponent} object.
     *
     * <p>If the declared type of the record component is a parameterized type,
     * the {@code Type} object returned must accurately reflect the
     * actual type arguments used in the source code.
     *
     * <p>If the type of the underlying record component is a type variable or a
     * parameterized type, it is created. Otherwise, it is resolved.
     *
     * @return a {@code Type} object that represents the declared type for
     *     the record component represented by this {@code RecordComponent} object
     * @throws GenericSignatureFormatError if the generic record component
     *     signature does not conform to the format specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if the generic type
     *     signature of the underlying record component refers to a non-existent
     *     type declaration
     * @throws MalformedParameterizedTypeException if the generic
     *     signature of the underlying record component refers to a parameterized type
     *     that cannot be instantiated for any reason
     */
    public Type getGenericType() {
        if (getGenericSignature() != null)
            return getGenericInfo().getGenericType();
        else
            return getType();
    }

    // Accessor for generic info repository
    private FieldRepository getGenericInfo() {
        // lazily initialize repository if necessary
        if (genericInfo == null) {
            // create and cache generic info repository
            genericInfo = FieldRepository.make(getGenericSignature(),
                    getFactory());
        }
        return genericInfo; //return cached repository
    }

    // Accessor for factory
    private GenericsFactory getFactory() {
        Class<?> c = getDeclaringClass();
        // create scope and factory
        return CoreReflectionFactory.make(c, ClassScope.make(c));
    }

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a type to specify
     * the annotated type of the record component represented by this
     * {@code RecordComponent}.
     *
     * @return an object representing the declared type of the record component
     * represented by this {@code RecordComponent}
     */
    public AnnotatedType getAnnotatedType() {
        if (typeAnnotations != null) {
            // debug
            // System.out.println("length of type annotations " + typeAnnotations.length);
        }
        return TypeAnnotationParser.buildAnnotatedType(typeAnnotations,
                SharedSecrets.getJavaLangAccess().
                        getConstantPool(getDeclaringClass()),
                this,
                getDeclaringClass(),
                getGenericType(),
                TypeAnnotation.TypeAnnotationTarget.FIELD);
    }

    /**
     * Returns a {@code Method} object that represents the accessor for the
     * record component represented by this {@code RecordComponent} object.
     *
     * @return a {@code Method} object that represents the accessor for the
     * record component represented by this {@code RecordComponent} object.
     */
    public Method getAccessor() {
        return accessor;
    }

    /**
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
                    RecordComponent root = this.root;
                    if (root != null) {
                        declAnnos = root.declaredAnnotations();
                    } else {
                        declAnnos = AnnotationParser.parseAnnotations(
                                annotations,
                                SharedSecrets.getJavaLangAccess()
                                        .getConstantPool(getDeclaringClass()),
                                getDeclaringClass());
                    }
                    declaredAnnotations = declAnnos;
                }
            }
        }
        return declAnnos;
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() { return AnnotationParser.toArray(declaredAnnotations()); }

    /**
     * Returns {@code true} if this {@code RecordComponent} was declared with
     * variable arity; returns {@code false} otherwise.
     *
     * @return {@code true} if an only if this {@code RecordComponent} was declared
     * with a variable arity.
     */
    public boolean isVarArgs()  {
        RecordComponent[] recordComponents = getDeclaringClass().getRecordComponents();
        if (recordComponents == null || recordComponents.length == 0) {
            return false;
        }
        try {
            Constructor<?> canonical = getDeclaringClass()
                    .getConstructor(Arrays.stream(recordComponents).map(RecordComponent::getAccessor)
                            .map(Method::getReturnType).toArray(Class<?>[]::new));
            return canonical.isVarArgs() && this.getName().equals(recordComponents[recordComponents.length - 1].getName());
        } catch (NoSuchMethodException nsme) {
            throw new IncompatibleClassChangeError(
                    String.format("a canonical constructor couldn't be found for record %s",
                            getDeclaringClass().getCanonicalName()));
        }
    }

    /**
     * Returns a string describing this {@code RecordComponent}, including
     * its generic type.  The format is the access modifiers for the
     * record component, always {@code private} and {@code final}, in that
     * order, followed by the generic record component type, followed by a
     * space, followed by the fully-qualified name of the class declaring
     * the record component, followed by a period, followed by the name of
     * the record component.
     *
     * @return a string describing this {@code RecordComponent}, including
     * its generic type
     */
    public String toGenericString() {
        int mod = Modifier.PRIVATE | Modifier.FINAL;
        Type type = getGenericType();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
                + type.getTypeName() + " "
                + getDeclaringClass().getTypeName() + "."
                + getName());
    }
}
