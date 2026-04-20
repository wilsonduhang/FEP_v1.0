/**
 * Shared test-util barrel for admin-ui unit tests.
 *
 * <p>Consumed by component and page tests under {@code src/features/** /
 * __tests__}. Keep this layer framework-only (vitest + @vue/test-utils +
 * element-plus) — no feature-specific helpers.</p>
 */
export { findButtonByText, setupElMessageSpies } from './dom-helpers';
