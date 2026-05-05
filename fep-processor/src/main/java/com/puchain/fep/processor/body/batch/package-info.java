/**
 * BATCH（非实时类）报文 Body POJO 包，对应 PRD v1.3 §4.3。
 *
 * <p>报文清单（{@code MessageCategory.BATCH}，8 个）：1101 / 2101 / 1102 / 2102 /
 * 1103 / 2103 / 1104 / 2104。当前 Plan（2026-05-05）首批落地 1103 + 2103 一对，
 * 建立 BATCH 类 Body POJO 实现模板（outer wrapper + {@code List<Item>} 嵌套结构）。</p>
 *
 * <p>命名约定：</p>
 * <ul>
 *   <li>outer wrapper 类：{@code CompanyInfoBatch{Request|Response}<msgNo>}</li>
 *   <li>item 类：{@code CompanyInfoBatchItem<msgNo>}（与 outer 同包同文件夹分文件）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
package com.puchain.fep.processor.body.batch;
