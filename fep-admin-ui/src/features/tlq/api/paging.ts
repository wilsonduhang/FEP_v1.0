/**
 * TLQ paging adapter.
 *
 * <p>TLQ backend (TlqNodeController / TlqConnectivityController) uses
 * {@code page} (0-based) + {@code size} in REQUEST query params, while the
 * rest of FEP admin-ui uses {@code pageNum} (1-based) + {@code pageSize}.
 * The response {@code PageResult.pageNum} is always 1-based per
 * {@code PageResult} contract, so conversion is required on the REQUEST
 * side only.</p>
 */

/**
 * Convert UI {@code pageNum} (1-based) to backend {@code page} (0-based).
 *
 * @param pageNum 1-based page number from UI state
 * @returns 0-based page number for backend query
 * @throws Error when {@code pageNum < 1}
 */
export function toZeroBasedPage(pageNum: number): number {
  if (pageNum < 1) {
    throw new Error(`toZeroBasedPage expects pageNum >= 1, got ${pageNum}`);
  }
  return pageNum - 1;
}
