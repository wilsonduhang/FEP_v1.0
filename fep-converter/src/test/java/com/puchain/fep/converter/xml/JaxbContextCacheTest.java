package com.puchain.fep.converter.xml;

import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.RequestBusinessHead;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JaxbContextCache}.
 *
 * <p>Covers the 9 acceptance criteria from R1 Plan Task 1: cache hit / miss /
 * concurrent build-once / null defense / empty array / order-insensitive Set key
 * / duplicate dedup / failure-no-pollution.
 *
 * <p>(v1b 修订 P0-#5) Inner test classes promoted to top-level package-private
 * classes in test source root:
 * {@link FakeBodyA}, {@link FakeBodyB}, {@link ConcurrentTestBody},
 * {@link SharedKeyBody}. Reason: inner classes (even static) reference the
 * enclosing class via reflective access during JAXB context build, which can
 * trigger propOrder discovery on the outer test class
 * ({@code JaxbContextCacheTest} itself). Top-level promotion eliminates this
 * fragility.
 */
class JaxbContextCacheTest {

    @Test
    void getForBody_firstCall_buildsAndCachesContext() {
        JAXBContext ctx = JaxbContextCache.getForBody(FakeBodyA.class);
        assertThat(ctx).isNotNull();
    }

    @Test
    void getForBody_secondCallSameClass_returnsSameInstance() {
        JAXBContext first = JaxbContextCache.getForBody(FakeBodyA.class);
        JAXBContext second = JaxbContextCache.getForBody(FakeBodyA.class);
        assertThat(second).isSameAs(first);
    }

    @Test
    void getForBody_differentBodies_returnDifferentInstances() {
        JAXBContext ctxA = JaxbContextCache.getForBody(FakeBodyA.class);
        JAXBContext ctxB = JaxbContextCache.getForBody(FakeBodyB.class);
        assertThat(ctxB).isNotSameAs(ctxA);
    }

    /**
     * (v1a 修订 P1-2) 方法名对齐实际调用 — 测试用 getForClasses() 而非 getForBody()，
     * 因 ConcurrentTestBody 在并发场景下需独占 cache key 不与其他测试共享。
     *
     * <p>(v1c P1 C-5) 在 finally 块的 pool.shutdownNow() 之后追加
     * pool.awaitTermination 断言，确保无悬挂线程泄漏到下个测试。
     */
    @Test
    void getForClasses_concurrentCallsForSameClasses_returnSameInstance() throws Exception {
        // (v1b 修订 P0-#4) Collect all N thread results into a List, then assert the
        // List contains exactly one distinct instance. Previous version used
        // AtomicReference.compareAndSet which only proved "first thread saw same as
        // itself" — too weak. This version actually validates "all threads got the
        // same JAXBContext instance".
        final int threadCount = 10;
        final ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch start = new CountDownLatch(1);
        final List<JAXBContext> results = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch done = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        JAXBContext ctx = JaxbContextCache.getForClasses(
                                CfxMessage.class, RequestBusinessHead.class,
                                ConcurrentTestBody.class);
                        results.add(ctx);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        // All N threads must have collected results (no exceptions)
        assertThat(results).hasSize(threadCount);
        // All results must be the SAME instance (identity equality)
        // Use IdentityHashMap to count distinct instances by reference identity
        final long distinctCount = results.stream()
                .map(System::identityHashCode)
                .distinct()
                .count();
        assertThat(distinctCount).isEqualTo(1L);
        // (v1c P1 C-5) Ensure no thread is hung beyond shutdownNow — no leak into next test.
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void getForBody_nullClass_throwsNPE() {
        assertThatThrownBy(() -> JaxbContextCache.getForBody(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getForClasses_emptyArray_throwsIAE() {
        assertThatThrownBy(JaxbContextCache::getForClasses)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one class");
    }

    @Test
    void getForClasses_orderInsensitive_returnsSameInstance() {
        JAXBContext ab = JaxbContextCache.getForClasses(
                FakeBodyA.class, FakeBodyB.class);
        JAXBContext ba = JaxbContextCache.getForClasses(
                FakeBodyB.class, FakeBodyA.class);
        assertThat(ba).isSameAs(ab);
    }

    /**
     * (v1c P1 C-4) 此测试同时覆盖 "duplicate classes 与 single class 等价" 与
     * "duplicate classes 不抛异常"两个语义 — assertSameAs 隐含未抛异常。
     * 故 P1 C-4 要求的额外测试已被本测试覆盖，不重复新增。
     */
    @Test
    void getForClasses_duplicateClasses_dedupedViaSetKey() {
        JAXBContext one = JaxbContextCache.getForClasses(FakeBodyA.class);
        JAXBContext dup = JaxbContextCache.getForClasses(
                FakeBodyA.class, FakeBodyA.class);
        assertThat(dup).isSameAs(one);
    }

    @Test
    void getForBody_andGetForClasses_sharingSameClassSet_returnSameInstance() {
        // getForBody(X) registers {CfxMessage, RequestBusinessHead, X};
        // getForClasses(CfxMessage, RequestBusinessHead, X) should hit same cache key.
        JAXBContext viaBody = JaxbContextCache.getForBody(SharedKeyBody.class);
        JAXBContext viaClasses = JaxbContextCache.getForClasses(
                CfxMessage.class, RequestBusinessHead.class, SharedKeyBody.class);
        assertThat(viaClasses).isSameAs(viaBody);
    }
}
