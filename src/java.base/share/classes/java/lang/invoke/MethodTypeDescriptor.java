package java.lang.invoke;

import java.util.List;

/**
 * Represents a method type descriptor, as per JVMS 4.3.3
 *
 * @param <F> the type representing field type descriptors
 * @param <M> the class implementing {@linkplain MethodTypeDescriptor}
 */
public interface MethodTypeDescriptor<F extends FieldTypeDescriptor<F>, M extends MethodTypeDescriptor<F, M>>
        extends TypeDescriptor {

    /**
     * Return the number of parameters in the method type
     * @return the number of parameters
     */
    int parameterCount();

    /**
     * Return a field descriptor describing the requested parameter of the method type
     * described by this descriptor
     * @param i the index of the parameter
     * @return a field descriptor for the requested parameter type
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount)}
     */
    F parameterType(int i);

    /**
     * Return a field descriptor describing the return type of the method type described
     * by this descriptor
     * @return a field descriptor for the return type
     */
    F returnType();

    /**
     * Return an array of field descriptors for the parameter types of the method type
     * described by this descriptor
     * @return field descriptors for the parameter types
     */
    F[] parameterArray();

    /**
     * Return a list of field descriptors for the parameter types of the method type
     * described by this descriptor
     * @return field descriptors for the parameter types
     */
    List<F> parameterList();

    /**
     * Return a method descriptor that is identical to this one, except that the return
     * type has been changed to the specified type
     *
     * @param newReturn a field descriptor for the new return type
     * @throws NullPointerException if any argument is {@code null}
     * @return the new method descriptor
     */
    M changeReturnType(F newReturn);

    /**
     * Return a method descriptor that is identical to this one,
     * except that a single parameter type has been changed to the specified type.
     *
     * @param index the index of the parameter to change
     * @param paramType a field descriptor describing the new parameter type
     * @return the new method descriptor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount)}
     */
    M changeParameterType(int index, F paramType);

    /**
     * Return a method descriptor that is identical to this one,
     * except that a range of parameter types have been removed.
     *
     * @param start the index of the first parameter to remove
     * @param end the index after the last parameter to remove
     * @return the new method descriptor
     * @throws IndexOutOfBoundsException if {@code start} is outside the half-open
     * range {[0, parameterCount)}, or {@code end} is outside the closed range
     * {@code [0, parameterCount]}
     */
    M dropParameterTypes(int start, int end);

    /**
     * Return a method descriptor that is identical to this one,
     * except that a range of additional parameter types have been inserted.
     *
     * @param pos the index at which to insert the first inserted parameter
     * @param paramTypes field descriptors describing the new parameter types
     *                   to insert
     * @return the new method descriptor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IndexOutOfBoundsException if {@code pos} is outside the closed
     * range {[0, parameterCount]}
     */
    @SuppressWarnings("unchecked")
    M insertParameterTypes(int pos, F... paramTypes);
}
