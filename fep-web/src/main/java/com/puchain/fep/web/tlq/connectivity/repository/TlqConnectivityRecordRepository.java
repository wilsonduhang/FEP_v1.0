package com.puchain.fep.web.tlq.connectivity.repository;

import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;
import com.puchain.fep.web.tlq.connectivity.domain.TlqConnectivityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * TLQ 连通性测试记录 Repository。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface TlqConnectivityRecordRepository extends JpaRepository<TlqConnectivityRecord, String> {

    /**
     * 按节点 ID 分页查询连通性记录（测试时间倒序）。
     *
     * @param nodeId   节点 ID
     * @param pageable 分页参数
     * @return 分页记录列表
     */
    Page<TlqConnectivityRecord> findByNodeIdOrderByTestTimeDesc(String nodeId, Pageable pageable);

    /**
     * 查询指定节点最近一条连通性记录。
     *
     * @param nodeId 节点 ID
     * @return 最近记录（可能为空）
     */
    Optional<TlqConnectivityRecord> findFirstByNodeIdOrderByTestTimeDesc(String nodeId);

    /**
     * 统计指定节点的测试总次数。
     *
     * @param nodeId 节点 ID
     * @return 测试总次数
     */
    long countByNodeId(String nodeId);

    /**
     * 统计指定节点特定结果的测试次数。
     *
     * @param nodeId     节点 ID
     * @param testResult 测试结果
     * @return 匹配次数
     */
    long countByNodeIdAndTestResult(String nodeId, ConnectivityTestResult testResult);
}
