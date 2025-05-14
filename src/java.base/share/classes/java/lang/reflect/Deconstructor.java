package java.lang.reflect;

import java.lang.invoke.MethodType;
import java.lang.runtime.Carriers;
import java.util.ArrayList;
import java.util.Arrays;

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
public final class Deconstructor<T> extends PatternMember<T> {
    /**
     * TODO make private again
     * Package-private member pattern used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass x
     * @param modifiers    x
     * @param patternFlags x
     * @param patternBindings    x
     * @param signature    x
     * @param annotations  x
     */
    public Deconstructor(Class<T> declaringClass,
                         int modifiers,
                         int patternFlags,
                         ArrayList<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations) {
        super(declaringClass,
                declaringClass,
                null,
                modifiers,
                patternFlags,
                patternBindings,
                signature,
                annotations,
                null);
    }

    @Override
    public String getName() {
        return getDeclaringClass().getName();
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

    /**
     * Initiate pattern matching of this {@code PatternMember} on the designated {@code matchCandidate}.
     *
     * @param candidate the match candidate to perform pattern matching over.
     *
     * @return an array object created as a result of pattern matching
     *
     * @throws    IllegalAccessException    if this {@code PatternMember} object
     *              is enforcing Java language access control and the underlying
     *              constructor is inaccessible.
     * @throws    MatchException if the pattern matching provoked
     *              by this {@code PatternMember} fails.
     */
    public Object[] tryMatch(Object candidate) throws IllegalAccessException, MatchException {
        String underlyingName = getMangledName();

        try {
            Method method = getDeclaringClass().getDeclaredMethod(underlyingName, candidate.getClass());
            method.setAccessible(override);
            MethodType methodType = MethodType.methodType(
                Object.class,
                Arrays.stream(getPatternBindings())
                    .map(PatternBinding::getType)
                    .toArray(Class[]::new)
            );
            return (Object[]) Carriers.boxedComponentValueArray(methodType).invoke(method.invoke(candidate, candidate));
        } catch (Throwable e) {
            throw new MatchException(e.getMessage(), e);
        }
    }

    /**
     * Package-private routine (exposed to java.lang.Class via
     * ReflectAccess) which returns a copy of this Deconstructor. The copy's
     * "root" field points to this Deconstructor.
     */
    Deconstructor<T> copy() {
        if (this.root != null)
            throw new IllegalArgumentException("Can not copy a non-root PatternMember");

        Deconstructor<T> res = new Deconstructor<>(
                this.candidateType,
                this.modifiers,
                this.patternFlags,
                this.patternBindings,
                this.signature,
                this.annotations);
        res.root = this;
        return res;
    }
}
