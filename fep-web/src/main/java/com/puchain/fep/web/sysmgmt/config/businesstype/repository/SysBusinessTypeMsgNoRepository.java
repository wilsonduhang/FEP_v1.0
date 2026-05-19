package com.puchain.fep.web.sysmgmt.config.businesstype.repository;

import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessTypeMsgNo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * {@link SysBusinessTypeMsgNo} 仓储。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SysBusinessTypeMsgNoRepository
        extends JpaRepository<SysBusinessTypeMsgNo, String> {

    /**
     * 查指定 msgNo 关联的全部 businessType id（不过滤状态，状态过滤在
     * {@code CallbackTargetResolver} join SysBusinessType.typeStatus 完成）。
     *
     * @param msgNo inbound 报文号，非空
     * @return businessType id 列表，可能为空
     */
    @Query("select m.typeId from SysBusinessTypeMsgNo m where m.msgNo = :msgNo")
    List<String> findBusinessTypeIdsByMsgNo(@Param("msgNo") String msgNo);

    /**
     * 删除指定业务类型的所有 msgNo 成员（重建时先删后插）。
     *
     * @param typeId 业务类型 ID，非空
     */
    void deleteByTypeId(String typeId);
}
