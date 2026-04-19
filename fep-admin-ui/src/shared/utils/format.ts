/**
 * Shared formatting utilities for display layer.
 *
 * <p>Used by feature pages (submission reports, biz-data records, etc.) to
 * render BigDecimal amounts (Java {@code BigDecimal} serialized as string via
 * {@code ToStringSerializer}) and ISO 8601 timestamps in a uniform way.</p>
 */

/**
 * Formats a monetary amount (BigDecimal string or number) as a comma-grouped
 * decimal with fixed 2 fraction digits, e.g. {@code "1,234.56"}.
 *
 * <p>Null, undefined, empty string, and non-numeric values are rendered as
 * {@code "-"} so tables never display blank cells.</p>
 *
 * @param value BigDecimal string (preferred, avoids JS number precision loss
 *              for values above 2^53) or native number
 * @returns comma-grouped amount string, or {@code "-"} when the input is empty
 *          / non-numeric
 */
export function formatAmount(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) {
    return '-';
  }
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/**
 * Formats an ISO 8601 datetime (e.g. {@code 2026-04-18T14:23:01}) as
 * {@code "YYYY-MM-DD HH:mm:ss"} in the local timezone.
 *
 * <p>Null, undefined, and unparseable values render as {@code "-"}.</p>
 *
 * @param value ISO 8601 datetime string from backend (Java
 *              {@code LocalDateTime} default serialization)
 * @returns {@code "YYYY-MM-DD HH:mm:ss"} or {@code "-"} when input is empty
 *          / invalid
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const d = new Date(value);
  if (isNaN(d.getTime())) {
    return '-';
  }
  const pad = (n: number): string => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}
