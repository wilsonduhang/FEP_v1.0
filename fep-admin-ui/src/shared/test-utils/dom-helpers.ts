import { vi } from 'vitest';
import { ElMessage } from 'element-plus';

/**
 * Find a DOM button by exact text-content match, scoped to document.body
 * (handles Element Plus teleported dialogs). Returns undefined if not found.
 *
 * <p>Used by dialog unit tests to drive submit/cancel via real DOM click
 * rather than {@code vm.method()} bypass (Plan global test red line #1).
 * Matches the peer pattern used in {@code TlqNodeEditDialog.test.ts} /
 * {@code DefinitionEditDialog.test.ts} — text is trimmed before comparison
 * so whitespace in the slot content does not break lookups.</p>
 */
export function findButtonByText(text: string): HTMLButtonElement | undefined {
  return Array.from(document.body.querySelectorAll('button')).find(
    (btn) => btn.textContent?.trim() === text,
  ) as HTMLButtonElement | undefined;
}

/**
 * Install {@link vi.spyOn} mocks on {@code ElMessage.success}, {@code info},
 * {@code warning}, and {@code error}. Call inside {@code beforeEach()} of
 * tests that assert toast dispatch — it replaces the repeated per-test
 * {@code vi.spyOn(ElMessage, ...)} boilerplate.
 *
 * <p>Only applicable to test files that import {@link ElMessage} directly
 * (dialog component tests). Page tests that replace the whole
 * {@code element-plus} module via {@code vi.mock('element-plus', ...)}
 * configure their own {@code ElMessage} mock and should not call this
 * helper.</p>
 */
export function setupElMessageSpies(): void {
  const noop = () => ({}) as ReturnType<typeof ElMessage.success>;
  vi.spyOn(ElMessage, 'success').mockImplementation(noop);
  vi.spyOn(ElMessage, 'info').mockImplementation(noop);
  vi.spyOn(ElMessage, 'warning').mockImplementation(noop);
  vi.spyOn(ElMessage, 'error').mockImplementation(noop);
}
