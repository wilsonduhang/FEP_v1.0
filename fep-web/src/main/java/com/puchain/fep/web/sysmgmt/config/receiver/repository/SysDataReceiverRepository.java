package com.puchain.fep.web.sysmgmt.config.receiver.repository;

import com.puchain.fep.web.sysmgmt.config.receiver.domain.SysDataReceiver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 数据接收方 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysDataReceiverRepository extends JpaRepository<SysDataReceiver, String> {

    /**
     * 判断接收方名称是否已存在。
     *
     * @param receiverName 接收方名称
     * @return true 已存在
     */
    boolean existsByReceiverName(String receiverName);

    /**
     * 判断名称是否被其他记录使用（排除指定 ID）。
     *
     * @param receiverName 接收方名称
     * @param receiverId   要排除的接收方 ID
     * @return true 已被其他记录使用
     */
    boolean existsByReceiverNameAndReceiverIdNot(String receiverName, String receiverId);

    /**
     * 按接收方名称模糊搜索（分页）。
     *
     * @param keyword  名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysDataReceiver> findByReceiverNameContaining(String keyword, Pageable pageable);
}
