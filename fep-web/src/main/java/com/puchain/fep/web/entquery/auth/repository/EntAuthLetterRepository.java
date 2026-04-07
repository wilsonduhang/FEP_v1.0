package com.puchain.fep.web.entquery.auth.repository;

import com.puchain.fep.web.entquery.auth.domain.EntAuthLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 授权书 Repository。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface EntAuthLetterRepository extends JpaRepository<EntAuthLetter, String> {

    /**
     * 按授权书类型、状态、关键字分页搜索授权书。
     *
     * <p>关键字匹配被授权企业 USCI 或名称；authType/letterStatus 为 null 时不过滤。</p>
     *
     * @param authType     授权书类型字符串（可为 null）
     * @param letterStatus 授权书状态字符串（可为 null）
     * @param keyword      关键字（可为 null）
     * @param pageable     分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM EntAuthLetter l WHERE "
            + "(:authType IS NULL OR CAST(l.authType AS string) = :authType) "
            + "AND (:letterStatus IS NULL OR CAST(l.letterStatus AS string) = :letterStatus) "
            + "AND (:keyword IS NULL OR l.authorizedUsci LIKE CONCAT('%', :keyword, '%') "
            + "OR l.authorizedName LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY l.createTime DESC")
    Page<EntAuthLetter> search(@Param("authType") String authType,
                               @Param("letterStatus") String letterStatus,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}
