package com.puchain.fep.web.audit.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 报文人工审核工作流配置（PRD v1.3 §5.8「多级审核（可配置）」）。
 *
 * <p>前缀 {@code fep.review}。MVP 为单级（{@code levels=1}）；{@code levels>1}
 * 预留多级串行审核扩展点（L1→L2→…→Ln 逐级通过才终结，任一级驳回即终结）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.review")
public class ReviewWorkflowProperties {

    /** 审核需经过的总层级数。默认 1（单级）；&gt;1 启用多级串行审核。 */
    private int levels = 1;

    /**
     * @return 审核总层级数（默认 1）
     */
    public int getLevels() {
        return levels;
    }

    /**
     * @param levels 审核总层级数；须 &ge; 1
     */
    public void setLevels(final int levels) {
        this.levels = levels;
    }
}
