package java.lang.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.runtime.Carriers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The reflection view of a single deconstructor, providing relevant information and enabling
 * execution with {@link #tryMatch(Object)}.
 *
 * @param <T> the class declaring this deconstructor; also the deconstructor's match candidate type
 * @since 26
 */
public final class Deconstructor<T> extends PatternMember<T> {
    /**
     * TODO make private again
     * Package-private member pattern used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via jdk.internal.access.JavaLangReflectAccess.
     *
     * @param declaringClass  x
     * @param modifiers       x
     * @param patternFlags    x
     * @param outParameters   x
     * @param patternBindings x
     * @param signature       x
     * @param annotations     x
     */
    public Deconstructor(Class<T> declaringClass,
                         int modifiers,
                         int patternFlags,
                         List<Parameter> outParameters,
                         List<PatternBinding> patternBindings,
                         String signature,
                         byte[] annotations) {
        super(declaringClass,
                declaringClass,
                modifiers,
                patternFlags,
                outParameters,
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
        return patternBindings.stream().map(p -> p.getType()).toArray(Class<?>[]::new);
    }

    @Override
    Class<?>[] getSharedExceptionTypes() {
        return new Class<?>[0];
    }

    /**
     * Reflectively invoke this pattern member to determine whether {@code candidate} is a match,
     * and if so, extract values.
     *
     * @param candidate the match candidate to perform pattern matching over.
     *
     * @return if the pattern matches, an array containing the extracted values; if not,
     *              {@code null} is returned instead. If this deconstructor belongs to an inner
     *              class, the first element of the returned array is the owning (outer) instance.
     * @throws    IllegalArgumentException  if {@code candidate} is not of this deconstructor's
     *              accepted match candidate type.
     * @throws    IllegalAccessException    if this {@code PatternMember} object
     *              is enforcing Java language access control and the underlying
     *              constructor is inaccessible.
     * @throws    MatchException if the pattern matching provoked
     *              by this {@code PatternMember} fails.
     */
    public Object[] tryMatch(T candidate) throws IllegalAccessException, MatchException {
        String underlyingName = getMangledName();

        try {
            Method method = getDeclaringClass().getDeclaredMethod(underlyingName, candidate.getClass(), MethodHandle.class);
            method.setAccessible(override);
            MethodType bindingMT = MethodType.methodType(
                Object.class,
                Arrays.stream(this.getPatternBindings())
                    .map(PatternBinding::getType)
                    .toArray(Class[]::new)
            );
            MethodHandle initializingConstructor = Carriers.initializingConstructor(bindingMT);

            return (Object[])Carriers.boxedComponentValueArray(bindingMT).invoke(method.invoke(candidate, candidate, initializingConstructor));
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
                this.outParameters,
                this.patternBindings,
                this.signature,
                this.annotations);
        res.root = this;
        return res;
    }

    private static final String DINIT = "\\^dinit\\_";
    String getMangledName() {
        return DINIT + ":" + super.getMangledName();
    }
}
