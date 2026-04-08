package com.puchain.fep.web.submission.scene.repository;

import com.puchain.fep.web.submission.scene.domain.SubBusinessScene;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 业务场景 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SubBusinessSceneRepository extends JpaRepository<SubBusinessScene, String> {

    /**
     * 按场景名称判断是否存在。
     *
     * @param sceneName 场景名称
     * @return 是否存在
     */
    boolean existsBySceneName(String sceneName);

    /**
     * 按场景名称判断是否存在（排除指定 ID）。
     *
     * @param sceneName 场景名称
     * @param sceneId   排除的场景 ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(s) > 0 FROM SubBusinessScene s "
            + "WHERE s.sceneName = :name AND s.sceneId <> :id")
    boolean existsBySceneNameAndIdNot(@Param("name") String sceneName,
                                      @Param("id") String sceneId);

    /**
     * 按关键字和业务类型 ID 搜索（场景名称模糊匹配），分页返回。
     *
     * @param keyword        关键字（可为 null）
     * @param businessTypeId 业务类型 ID（可为 null）
     * @param pageable       分页参数
     * @return 分页结果
     */
    @Query("SELECT s FROM SubBusinessScene s "
            + "WHERE (:keyword IS NULL OR s.sceneName LIKE %:keyword%) "
            + "AND (:bizTypeId IS NULL OR s.businessTypeId = :bizTypeId)")
    Page<SubBusinessScene> search(@Param("keyword") String keyword,
                                  @Param("bizTypeId") String businessTypeId,
                                  Pageable pageable);
}
