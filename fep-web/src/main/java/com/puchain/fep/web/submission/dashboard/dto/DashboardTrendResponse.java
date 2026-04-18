package com.puchain.fep.web.submission.dashboard.dto;

import java.util.List;

/**
 * 报送管理推送趋势响应 DTO。
 *
 * <p>按日期粒度返回 pushed / pending 双系列计数，用于 Dashboard 趋势图。
 * 参见 PRD v1.3 §5.5.1 + §5.9.1。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DashboardTrendResponse {

    private List<String> dates;
    private List<Long> pushedCounts;
    private List<Long> pendingCounts;

    /** 默认构造。 */
    public DashboardTrendResponse() {
        /* default */
    }

    /**
     * 获取日期序列（yyyy-MM-dd）。
     *
     * @return 日期序列
     */
    public List<String> getDates() {
        return dates;
    }

    /**
     * 设置日期序列。
     *
     * @param dates 日期序列
     */
    public void setDates(final List<String> dates) {
        this.dates = dates;
    }

    /**
     * 获取已推送计数序列（与 dates 等长，push_status=PUSHED）。
     *
     * @return 已推送计数
     */
    public List<Long> getPushedCounts() {
        return pushedCounts;
    }

    /**
     * 设置已推送计数。
     *
     * @param pushedCounts 已推送计数
     */
    public void setPushedCounts(final List<Long> pushedCounts) {
        this.pushedCounts = pushedCounts;
    }

    /**
     * 获取待推送计数序列（仅统计 push_status=PENDING；PUSHING/FAILED 不计入）。
     *
     * @return 待推送计数
     */
    public List<Long> getPendingCounts() {
        return pendingCounts;
    }

    /**
     * 设置待推送计数。
     *
     * @param pendingCounts 待推送计数
     */
    public void setPendingCounts(final List<Long> pendingCounts) {
        this.pendingCounts = pendingCounts;
    }
}
