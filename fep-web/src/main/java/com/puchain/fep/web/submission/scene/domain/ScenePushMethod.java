package com.puchain.fep.web.submission.scene.domain;

/**
 * 业务场景推送方式枚举。
 *
 * <p>参见 PRD v1.3 §5.5.4 业务场景管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ScenePushMethod {

    /** 自动推送（系统定时或事件触发）。 */
    AUTO,

    /** 手动上传（需提供导入模板文件路径）。 */
    MANUAL
}
