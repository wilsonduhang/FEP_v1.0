package com.puchain.fep.web.integration.dirmap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * fep-web Adapter 层 JPA Repository for DIR-MAP audit history.
 *
 * <p>v1f D7 Hexagonal — Repository 不在 fep-processor，由 fep-web 端独占。
 * fep-processor 端通过 DirMapConfigStore Port 接口的 update() 方法
 * 间接触发 history 写入（v1i P0-B6：history 由 fep-web Service 层
 * {@code DirMapConfigAdminService.update} 单一来源负责，Adapter 不写）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DirMapConfigHistoryRepository
        extends JpaRepository<DirMapConfigHistoryEntity, String> {

    /**
     * 按 (messageType, accessRole) 倒序查询变更历史（最新条在前）。
     *
     * @param messageType 报文类型字符串（4 位 HNDEMP 报文号），非 null
     * @param accessRole  接入角色枚举名，非 null
     * @return 变更历史条目，按 changedAt 降序；无匹配返回空 list
     */
    List<DirMapConfigHistoryEntity> findByMessageTypeAndAccessRoleOrderByChangedAtDesc(
            String messageType, String accessRole);
}
