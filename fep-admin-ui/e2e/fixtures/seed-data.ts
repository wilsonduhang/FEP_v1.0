/**
 * E2E 测试 seed 数据常量 —— 与后端 {@code E2eSeedRunner} 保持同步。
 *
 * <p>启动 {@code --spring.profiles.active=dev,dev-e2e} 时,
 * {@code E2eSeedRunner} 会向 {@code t_sys_enterprise} upsert 以下两行
 * ({@code audit_status='APPROVED'}), 以便 Playwright 场景 4-7 能通过
 * {@code EntQueryTaskService.create()} 的 APPROVED 企业校验。</p>
 *
 * @since P7.2b
 */
export const E2E_SEED_ENTERPRISES = [
  { usci: '91110000MA01A00001', enterpriseName: 'E2E 测试企业 A' },
  { usci: '91110000MA01A00002', enterpriseName: 'E2E 测试企业 B' },
] as const;
