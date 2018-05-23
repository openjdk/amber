package java.lang.invoke;

/**
 * Represents a field type descriptor, as per JVMS 4.3.2.
 *
 * @param <F> the class implementing {@linkplain FieldTypeDescriptor}
 */
public interface FieldTypeDescriptor<F extends FieldTypeDescriptor<F>> extends TypeDescriptor {
    /**
     * Does this field descriptor describe an array type?
     * @return whether this field descriptor describes an array type
     */
    boolean isArray();

    /**
     * Does this field descriptor describe a primitive type?
     * @return whether this field descriptor describes a primitive type
     */
    boolean isPrimitive();

    /**
     * If this field descriptor describes an array type, return
     * a descriptor for its component type.
     * @return the component type
     * @throws IllegalStateException if this descriptor does not describe an array type
     */
    F componentType();

    /**
     * Return a descriptor for the array type whose component type is described by this
     * descriptor
     * @return the descriptor for the array type
     */
    F arrayType();
}
