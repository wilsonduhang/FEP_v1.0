package com.puchain.fep.web.tlq.queue.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import com.puchain.fep.web.tlq.queue.domain.TlqChannelType;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueConfig;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueType;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueBatchGenerateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigCreateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigResponse;
import com.puchain.fep.web.tlq.queue.repository.TlqQueueConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TlqQueueConfigService 单元测试。
 *
 * <p>参见 PRD v1.3 §3.1.2 TLQ 队列管理（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TlqQueueConfigServiceTest {

    @Mock
    private TlqQueueConfigRepository queueConfigRepository;

    @Mock
    private TlqNodeRepository nodeRepository;

    @InjectMocks
    private TlqQueueConfigService queueConfigService;

    // ===== Test 1: createQueue — valid input =====

    @Test
    void createQueue_validInput_returnsQueueConfig() {
        when(nodeRepository.existsById("node1")).thenReturn(true);
        when(queueConfigRepository.existsByQueueName("QLOCAL.ORG001.REAL.1")).thenReturn(false);
        when(queueConfigRepository.save(any(TlqQueueConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TlqQueueConfigCreateRequest req = new TlqQueueConfigCreateRequest();
        req.setNodeId("node1");
        req.setQueueName("QLOCAL.ORG001.REAL.1");
        req.setChannelType(TlqChannelType.REALTIME);
        req.setQueueType(TlqQueueType.LOCAL);

        TlqQueueConfigResponse resp = queueConfigService.createQueue(req);

        assertNotNull(resp.getQueueId());
        assertEquals(32, resp.getQueueId().length(), "UUID 应为 32 位");
        assertEquals("QLOCAL.ORG001.REAL.1", resp.getQueueName());
        assertEquals(TlqChannelType.REALTIME, resp.getChannelType());
        assertEquals(EnableDisableStatus.ENABLED, resp.getQueueStatus());
    }

    // ===== Test 2: createQueue — duplicate name throws BIZ_5014 =====

    @Test
    void createQueue_duplicateName_throwsBiz5014() {
        when(nodeRepository.existsById("node1")).thenReturn(true);
        when(queueConfigRepository.existsByQueueName("QLOCAL.ORG001.REAL.1")).thenReturn(true);

        TlqQueueConfigCreateRequest req = new TlqQueueConfigCreateRequest();
        req.setNodeId("node1");
        req.setQueueName("QLOCAL.ORG001.REAL.1");
        req.setChannelType(TlqChannelType.REALTIME);
        req.setQueueType(TlqQueueType.LOCAL);

        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> queueConfigService.createQueue(req));
        assertEquals(FepErrorCode.BIZ_5014, ex.getErrorCode());
    }

    // ===== Test 3: createQueue — non-existent node throws BIZ_5015 =====

    @Test
    void createQueue_nonExistentNode_throwsBiz5015() {
        when(nodeRepository.existsById("nonexistent")).thenReturn(false);

        TlqQueueConfigCreateRequest req = new TlqQueueConfigCreateRequest();
        req.setNodeId("nonexistent");
        req.setQueueName("QLOCAL.ORG001.REAL.1");
        req.setChannelType(TlqChannelType.REALTIME);
        req.setQueueType(TlqQueueType.LOCAL);

        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> queueConfigService.createQueue(req));
        assertEquals(FepErrorCode.BIZ_5015, ex.getErrorCode());
    }

    // ===== Test 4: batchGenerate — valid org code creates 9 queues =====

    @Test
    void batchGenerate_validOrgCode_creates9Queues() {
        final String orgCode = "B1234567890123";
        when(nodeRepository.existsById("node1")).thenReturn(true);
        when(queueConfigRepository.existsByQueueName(anyString())).thenReturn(false);
        when(queueConfigRepository.save(any(TlqQueueConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TlqQueueBatchGenerateRequest req = new TlqQueueBatchGenerateRequest();
        req.setNodeId("node1");
        req.setOrganizationCode(orgCode);

        List<TlqQueueConfigResponse> result = queueConfigService.batchGenerateQueues(req);

        assertEquals(9, result.size(), "应生成 9 条队列");

        List<String> names = result.stream().map(TlqQueueConfigResponse::getQueueName).toList();
        // Per PRD §3.1.2 naming convention
        assertFalse(names.stream().noneMatch(n -> n.equals("QLOCAL." + orgCode + ".REAL.1")),
                "应包含 QLOCAL.B1234567890123.REAL.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QLOCAL." + orgCode + ".BATCH.1")),
                "应包含 QLOCAL.B1234567890123.BATCH.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QREMOTE.A1000143000104.REAL.1")),
                "应包含 QREMOTE.A1000143000104.REAL.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QREMOTE.A1000143000104.BATCH.1")),
                "应包含 QREMOTE.A1000143000104.BATCH.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QLOCAL.A1000143000104.REAL.1")),
                "应包含 QLOCAL.A1000143000104.REAL.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QLOCAL.A1000143000104.BATCH.1")),
                "应包含 QLOCAL.A1000143000104.BATCH.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QSEND.A1000143000104.REAL.1")),
                "应包含 QSEND.A1000143000104.REAL.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QSEND.A1000143000104.BATCH.1")),
                "应包含 QSEND.A1000143000104.BATCH.1");
        assertFalse(names.stream().noneMatch(n -> n.equals("QDEAD." + orgCode)),
                "应包含 QDEAD.B1234567890123");
    }

    // ===== Test 5: listByNode — returns queues for node =====

    @Test
    void listByNode_returnsQueuesForNode() {
        when(nodeRepository.existsById("node1")).thenReturn(true);
        TlqQueueConfig q1 = buildQueue("q1", "node1", "QLOCAL.ORG.REAL.1",
                TlqChannelType.REALTIME, TlqQueueType.LOCAL);
        TlqQueueConfig q2 = buildQueue("q2", "node1", "QLOCAL.ORG.BATCH.1",
                TlqChannelType.BATCH, TlqQueueType.LOCAL);
        when(queueConfigRepository.findByNodeIdOrderByChannelTypeAscQueueTypeAsc("node1"))
                .thenReturn(List.of(q1, q2));

        List<TlqQueueConfigResponse> result = queueConfigService.listByNode("node1");

        assertEquals(2, result.size());
        assertEquals("node1", result.get(0).getNodeId());
    }

    // ===== Test 6: batchGenerate — partial existing skips existing queues =====

    @Test
    void batchGenerate_partialExisting_skipsExistingQueues() {
        final String orgCode = "B1234567890123";
        when(nodeRepository.existsById("node1")).thenReturn(true);
        // Default: all new
        when(queueConfigRepository.existsByQueueName(anyString())).thenReturn(false);
        // Override: 2 queues already exist
        when(queueConfigRepository.existsByQueueName("QLOCAL." + orgCode + ".REAL.1"))
                .thenReturn(true);
        when(queueConfigRepository.existsByQueueName("QLOCAL." + orgCode + ".BATCH.1"))
                .thenReturn(true);
        when(queueConfigRepository.save(any(TlqQueueConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TlqQueueBatchGenerateRequest req = new TlqQueueBatchGenerateRequest();
        req.setNodeId("node1");
        req.setOrganizationCode(orgCode);

        List<TlqQueueConfigResponse> result = queueConfigService.batchGenerateQueues(req);

        assertEquals(7, result.size(), "应跳过 2 个已存在队列，剩余 7 条");
        result.forEach(r -> assertNotNull(r, "结果列表中不应含 null"));
    }

    // ===== Test 7: deleteQueue — existing id deletes successfully =====

    @Test
    void deleteQueue_existingId_deletesSuccessfully() {
        TlqQueueConfig q = buildQueue("q1", "node1", "QLOCAL.ORG.REAL.1",
                TlqChannelType.REALTIME, TlqQueueType.LOCAL);
        when(queueConfigRepository.findById("q1")).thenReturn(Optional.of(q));

        queueConfigService.deleteQueue("q1");

        verify(queueConfigRepository, times(1)).deleteById("q1");
    }

    // ===== helpers =====

    private TlqQueueConfig buildQueue(final String queueId, final String nodeId,
                                       final String queueName,
                                       final TlqChannelType channelType,
                                       final TlqQueueType queueType) {
        TlqQueueConfig q = new TlqQueueConfig();
        q.setQueueId(queueId);
        q.setNodeId(nodeId);
        q.setQueueName(queueName);
        q.setChannelType(channelType);
        q.setQueueType(queueType);
        q.setQueueStatus(EnableDisableStatus.ENABLED);
        return q;
    }
}
