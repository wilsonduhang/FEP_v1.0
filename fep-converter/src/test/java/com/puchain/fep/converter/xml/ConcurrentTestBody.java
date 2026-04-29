package com.puchain.fep.converter.xml;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Test fixture exclusively for {@link JaxbContextCacheTest}.
 * Do NOT reuse in other test classes — would pollute the shared static cache.
 *
 * <p>Used by the concurrent build-once test to occupy a dedicated cache key
 * unaffected by other tests' cache hits.
 *
 * <p>Promoted from inner class to top-level package-private class (v1b 修订 P0-#5)
 * to eliminate JAXB reflective access fragility on the enclosing test class.
 * Annotated with {@link XmlRootElement} (v1c P1 C-2) to remove the implicit
 * propOrder discovery edge case when registered into a JAXBContext.
 */
@XmlRootElement
class ConcurrentTestBody { }
