package com.puchain.fep.web.integration.dirmap;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * fep-web Adapter 层 JPA Repository — DIR-MAP 主配置。
 *
 * <p>v1g P0-B 修复：production-side DirMapConfigStore Port 实现的 DAO 依赖。
 * {@code findAll() / findById(DirMapConfigKey) / count() / save(...)} 均由 Spring Data 派生。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DirMapConfigJpaRepository
        extends JpaRepository<DirMapConfigEntity, DirMapConfigKey> {
}
