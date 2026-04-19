import { describe, expect, it } from 'vitest';
import { formatAmount, formatDateTime } from '../format';

describe('formatAmount', () => {
  it('formats BigDecimal string to 1,234.56', () => {
    expect(formatAmount('1234.56')).toBe('1,234.56');
  });
  it('formats number to 1,234.56', () => {
    expect(formatAmount(1234.56)).toBe('1,234.56');
  });
  it('renders - for null', () => {
    expect(formatAmount(null)).toBe('-');
  });
  it('renders - for undefined', () => {
    expect(formatAmount(undefined)).toBe('-');
  });
});

describe('formatDateTime', () => {
  it('formats ISO 8601 to YYYY-MM-DD HH:mm:ss', () => {
    expect(formatDateTime('2026-04-18T14:23:01')).toBe('2026-04-18 14:23:01');
  });
  it('renders - for null', () => {
    expect(formatDateTime(null)).toBe('-');
  });
});
