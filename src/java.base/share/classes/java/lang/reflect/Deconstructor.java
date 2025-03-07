package java.lang.reflect;

import java.util.ArrayList;

/**
 * {@code Deconstructor} provides information about, and access to, a single
 * deconstructor for a class.
 *
 * @param <T> the class in which the deconstructor is declared
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
public final class Deconstructor<T> extends MemberPattern<T> {
    /**
     * TODO make private again
     * Package-private member pattern used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass x
     * @param modifiers    x
     * @param patternFlags x
     * @param slot         x
     * @param patternBindings    x
     * @param signature    x
     * @param annotations  x
     */
    public Deconstructor(Class<T> declaringClass,
                         int modifiers,
                         int patternFlags,
                         int slot,
                         ArrayList<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations) {
        super(declaringClass,
                null,
                null,
                modifiers,
                patternFlags,
                slot,
                patternBindings,
                signature,
                annotations,
                null);
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
     * Package-private routine (exposed to java.lang.Class via
     * ReflectAccess) which returns a copy of this Deconstructor. The copy's
     * "root" field points to this Deconstructor.
     */
    Deconstructor<T> copy() {
        if (this.root != null)
            throw new IllegalArgumentException("Can not copy a non-root MemberPattern");

        Deconstructor<T> res = new Deconstructor<>(this.clazz,
                this.modifiers,
                this.patternFlags,
                this.slot,
                this.patternBindings,
                this.signature,
                this.annotations);
        res.root = this;
        return res;
    }
}
