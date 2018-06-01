package java.lang.invoke;

/**
 * An entity that has a field or method type descriptor, as per JVMS 4.3.2 or 4.3.3.
 * @jvms 4.3.2 Field Descriptors
 * @jvms 4.3.3 Method Descriptors
 */
public interface TypeDescriptor {
    /**
     * Return the type descriptor for this instance, which may be a field or method type descriptor.
     * @return the type descriptor
     */
    String descriptorString();
}
