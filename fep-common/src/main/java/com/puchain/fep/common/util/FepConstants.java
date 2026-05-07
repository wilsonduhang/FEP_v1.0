package com.puchain.fep.common.util;

/**
 * FEP 全局常量。
 *
 * <p>R-2 (2026-05-07): 引入 {@link #HNDEMP_NODE_CODE} 替换 codebase 中
 * 109+ 处硬编码字面量 {@code "A1000143000104"}。后续新增 CommonHead 装配 /
 * 测试样本必须引用此常量，禁止内联字符串。</p>
 *
 * <p>决策依据: {@code docs/plans/2026-05-06-p5-v2-cleanup-r1-r2-b1-umbrella.md} §3 T2。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class FepConstants {

    /**
     * HNDEMP 中心节点代码（湖南省金融大数据中心数据交换管理平台）。
     *
     * <p>固定 14 字符，源自 PRD v1.3。所有 CommonHead.SrcNodeCode /
     * DestNodeCode 装配 + 测试样本必须引用此常量。</p>
     */
    public static final String HNDEMP_NODE_CODE = "A1000143000104";

    private FepConstants() {
        throw new UnsupportedOperationException("FepConstants is a utility class and cannot be instantiated");
    }
}
