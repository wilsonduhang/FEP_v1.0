package com.puchain.fep.web.submission.dashboard.dto;

/**
 * 报送管理分布统计单项。
 *
 * <p>用于 Dashboard 饼图，按 messageType 或 businessType 分组聚合。
 * 参见 PRD v1.3 §5.5.1 + §5.9.1。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DashboardDistributionItem {

    private String name;
    private long value;

    /** 默认构造。 */
    public DashboardDistributionItem() {
        /* default */
    }

    /**
     * 获取分组维度值。
     *
     * @return 维度值
     */
    public String getName() {
        return name;
    }

    /**
     * 设置维度值。
     *
     * @param name 维度值
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * 获取聚合计数。
     *
     * @return 计数
     */
    public long getValue() {
        return value;
    }

    /**
     * 设置聚合计数。
     *
     * @param value 计数
     */
    public void setValue(final long value) {
        this.value = value;
    }
}
