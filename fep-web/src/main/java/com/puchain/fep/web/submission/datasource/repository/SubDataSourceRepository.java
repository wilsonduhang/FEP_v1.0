package com.puchain.fep.web.submission.datasource.repository;

import com.puchain.fep.web.submission.datasource.domain.SubDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 数据源 Repository。
 *
 * <p>提供数据源的基本 CRUD 及名称唯一性检查、模糊搜索。
 * 参见 PRD v1.3 §5.5.3 数据源管理（FR-WEB-SUB-SRC）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SubDataSourceRepository extends JpaRepository<SubDataSource, String> {

    /**
     * 按数据源名称判断是否存在。
     *
     * @param sourceName 数据源名称
     * @return 是否存在
     */
    boolean existsBySourceName(String sourceName);

    /**
     * 按数据源名称判断是否存在（排除指定 ID）。
     *
     * @param sourceName 数据源名称
     * @param sourceId   排除的数据源 ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(d) > 0 FROM SubDataSource d "
            + "WHERE d.sourceName = :name AND d.sourceId <> :id")
    boolean existsBySourceNameAndIdNot(@Param("name") String sourceName,
                                       @Param("id") String sourceId);

    /**
     * 按关键字模糊搜索（数据源名称），分页返回。
     *
     * @param keyword  关键字（可为 null）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT d FROM SubDataSource d "
            + "WHERE (:keyword IS NULL OR d.sourceName LIKE %:keyword%)")
    Page<SubDataSource> search(@Param("keyword") String keyword, Pageable pageable);
}
