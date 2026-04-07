package com.puchain.fep.web.sysmgmt.config.alert.repository;

import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 接口预警规则 Repository。
 *
 * <p>t_sys_alert_rule 为单条配置记录，无需自定义查询方法。
 * 参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SysAlertRuleRepository extends JpaRepository<SysAlertRule, String> {
}
