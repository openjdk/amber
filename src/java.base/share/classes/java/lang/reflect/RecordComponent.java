package java.lang.reflect;

import jdk.internal.access.SharedSecrets;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

/**
 * A {@code RecordComponent} provides information about, and dynamic access to, a
 * record component in a record class.
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
    private byte[] annotations;

    /**
     * Returns the name of the record component represented by this {@code RecordComponent} object.
     *
     * @return the name of the record component represented by this {@code RecordComponent} object.
     */
    public String getName() {
        return name;
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
        return null;
    }

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a type to specify
     * the annotated type of the record component represented by this
     * {@code RecordComponent}.
     *
     * @return an object representing the declared type of the record component
     * represented by this {@code RecordComponent}
     */
    public AnnotatedType getAnnotatedType() { return null; }

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
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) { return null; }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    /**
     * Returns {@code true} if this {@code RecordComponent} was declared with
     * variable arity; returns {@code false} otherwise.
     *
     * @return {@code true} if an only if this {@code RecordComponent} was declared
     * with a variable arity.
     */
    public boolean isVarArgs()  {
        /*
          once we have the new Class::getRecordComponents, we can use it to retrieve the canonical constructor, if it is
          varargs and this record component is the last in the record components array, then voila
         */
        return false;
    }

    /**
     * Returns a string describing this {@code RecordComponent}, including
     * its generic type.  The format is the access modifiers for the
     * record component, always {@code private} and {@code final}, followed
     * by the generic record component type, followed by a space, followed by
     * the fully-qualified name of the class declaring the record component,
     * followed by a period, followed by the name of the record component.
     *
     * @return a string describing this {@code RecordComponent}, including
     * its generic type
     */
    public String toGenericString() {
        return null;
    }
}
