package com.puchain.fep.web.tlq.node.repository;

import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * TLQ 节点配置 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface TlqNodeRepository extends JpaRepository<TlqNode, String> {

    /**
     * 按节点名称判断是否存在。
     *
     * @param nodeName 节点名称
     * @return 是否存在
     */
    boolean existsByNodeName(String nodeName);

    /**
     * 按主机 IP 和端口判断是否存在。
     *
     * @param hostIp 主机 IP
     * @param port   端口
     * @return 是否存在
     */
    boolean existsByHostIpAndPort(String hostIp, int port);

    /**
     * 按条件过滤查询节点列表（角色/状态均可选），分页返回。
     *
     * @param role     节点角色（可为 null 表示不过滤）
     * @param status   节点状态（可为 null 表示不过滤）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT n FROM TlqNode n "
            + "WHERE (:role IS NULL OR n.nodeRole = :role) "
            + "AND (:status IS NULL OR n.nodeStatus = :status)")
    Page<TlqNode> findByFilters(@Param("role") TlqNodeRole role,
                                @Param("status") TlqNodeStatus status,
                                Pageable pageable);
}
