package com.puchain.fep.converter.xml;

import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.RequestBusinessHead;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide {@link JAXBContext} cache keyed by the unordered set of registered classes.
 *
 * <p>JAXBContext construction reflects on the registered classes (50-300ms typical for
 * a 3-class set with annotations), but the resulting context is thread-safe and
 * intended for indefinite reuse. This cache eliminates duplicate construction across
 * the two production marshal sites
 * ({@code BatchMessageProcessorService} batch outbound,
 * {@code InboundMessageDispatcher} inbound parse) and prepares for a planned
 * third site in P4.
 *
 * <p>Cache key uses {@link Set} of classes, so {@code getForClasses(A, B)} and
 * {@code getForClasses(B, A)} share the same context — matching JAXBContext's
 * order-insensitive registration semantics.
 *
 * <p>Failure semantics: a first-build failure is wrapped in
 * {@link IllegalStateException} and the failing key is <strong>not</strong>
 * inserted into the cache, so a subsequent call retries from scratch.
 *
 * <p>Lifetime: contexts live for the JVM lifetime. Body classes in this project are
 * a closed set of &le;44 message types &times; 2 access roles, so unbounded growth is
 * not a concern.
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class JaxbContextCache {

    private static final ConcurrentHashMap<Set<Class<?>>, JAXBContext> CACHE =
            new ConcurrentHashMap<>();

    private JaxbContextCache() {
        // utility class
    }

    /**
     * Convenience accessor for the default outbound/inbound CFX marshal class set
     * (CfxMessage + RequestBusinessHead + body).
     *
     * @param bodyClass non-null body POJO class
     * @return cached JAXBContext registering CfxMessage, RequestBusinessHead, and bodyClass
     * @throws NullPointerException  if {@code bodyClass} is null
     * @throws IllegalStateException if first JAXBContext construction fails
     */
    public static JAXBContext getForBody(final Class<?> bodyClass) {
        Objects.requireNonNull(bodyClass, "bodyClass must not be null");
        return getForClasses(CfxMessage.class, RequestBusinessHead.class, bodyClass);
    }

    /**
     * Flexible accessor for arbitrary class registration sets. Use this when the
     * default {@link #getForBody(Class)} class triple is not appropriate.
     *
     * @param classes one or more JAXB-annotated classes; null entries are rejected
     * @return cached JAXBContext for the unordered set of classes
     * @throws IllegalArgumentException if {@code classes} is empty or contains null
     * @throws IllegalStateException    if first JAXBContext construction fails
     */
    public static JAXBContext getForClasses(final Class<?>... classes) {
        if (classes == null || classes.length == 0) {
            throw new IllegalArgumentException("at least one class is required");
        }
        for (Class<?> c : classes) {
            if (c == null) {
                throw new IllegalArgumentException("class array contains null");
            }
        }
        final Set<Class<?>> key = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(classes)));
        return CACHE.computeIfAbsent(key, JaxbContextCache::build);
    }

    /**
     * Build a new JAXBContext for the given class set. Wraps {@link JAXBException}
     * in {@link IllegalStateException} so callers don't need to declare a checked
     * exception. The map's {@code computeIfAbsent} semantics ensure that if this
     * throws, the failing key is not inserted into the cache.
     */
    private static JAXBContext build(final Set<Class<?>> classes) {
        try {
            return JAXBContext.newInstance(classes.toArray(new Class<?>[0]));
        } catch (JAXBException e) {
            throw new IllegalStateException(
                    "Failed to build JAXBContext for classes=" + classes, e);
        }
    }
}
