package com.puchain.fep.web.tlq.connectivity.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;
import com.puchain.fep.web.tlq.connectivity.domain.TlqConnectivityRecord;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivitySummaryResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityTestResponse;
import com.puchain.fep.web.tlq.connectivity.repository.TlqConnectivityRecordRepository;
import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TlqConnectivityService 单元测试。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TlqConnectivityServiceTest {

    @Mock
    private TlqConnectivityRecordRepository recordRepository;

    @Mock
    private TlqNodeRepository nodeRepository;

    @Mock
    private RemoteAdmin remoteAdmin;

    @InjectMocks
    private TlqConnectivityService connectivityService;

    private static TlqNode buildNode(final String id, final String host, final int port) {
        TlqNode node = new TlqNode();
        node.setNodeId(id);
        node.setHostIp(host);
        node.setPort(port);
        return node;
    }

    // ===== Test 1: triggerTest — reachable probe creates SUCCESS record =====

    @Test
    void triggerTest_reachable_createsSuccessRecord() {
        TlqNode node = buildNode("node1", "10.0.0.1", 20001);
        when(nodeRepository.existsById("node1")).thenReturn(true);
        when(nodeRepository.findById("node1")).thenReturn(Optional.of(node));
        when(remoteAdmin.checkConnectivity(anyString(), anyInt()))
                .thenReturn(new RemoteAdmin.ConnectivityProbe(true, 12L, "OK"));
        when(recordRepository.save(any(TlqConnectivityRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConnectivityTestResponse response = connectivityService.triggerTest("node1");

        assertEquals(32, response.getRecordId().length(), "recordId 应为 32 位 UUID");
        assertEquals("node1", response.getNodeId());
        assertEquals(ConnectivityTestResult.SUCCESS, response.getResult(),
                "reachable=true 应映射为 SUCCESS");
        assertEquals(12, response.getRttMs(), "RTT 来自 probe.rttMs()");
        assertEquals("OK", response.getMessage(),
                "message 应直接来自 probe.detail()");

        ArgumentCaptor<TlqConnectivityRecord> captor =
                ArgumentCaptor.forClass(TlqConnectivityRecord.class);
        verify(recordRepository).save(captor.capture());
        TlqConnectivityRecord saved = captor.getValue();
        assertEquals(ConnectivityTestResult.SUCCESS, saved.getTestResult());
        assertEquals(12, saved.getRttMs());
        assertEquals("MANUAL", saved.getTriggeredBy());
        assertNull(saved.getErrorMessage(), "成功路径 errorMessage 应为 null");
    }

    // ===== Test 1b: triggerTest — unreachable probe creates FAILURE record =====

    @Test
    void triggerTest_unreachable_createsFailureRecordWithDetail() {
        TlqNode node = buildNode("node-down", "10.0.0.99", 20001);
        when(nodeRepository.existsById("node-down")).thenReturn(true);
        when(nodeRepository.findById("node-down")).thenReturn(Optional.of(node));
        when(remoteAdmin.checkConnectivity(anyString(), anyInt()))
                .thenReturn(new RemoteAdmin.ConnectivityProbe(false, 5L,
                        "checkIP: IP 不可达: 10.0.0.99"));
        when(recordRepository.save(any(TlqConnectivityRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConnectivityTestResponse response = connectivityService.triggerTest("node-down");

        assertEquals(ConnectivityTestResult.FAILURE, response.getResult());
        assertEquals(5, response.getRttMs());
        assertEquals("checkIP: IP 不可达: 10.0.0.99", response.getMessage());

        ArgumentCaptor<TlqConnectivityRecord> captor =
                ArgumentCaptor.forClass(TlqConnectivityRecord.class);
        verify(recordRepository).save(captor.capture());
        TlqConnectivityRecord saved = captor.getValue();
        assertEquals(ConnectivityTestResult.FAILURE, saved.getTestResult());
        assertNotNull(saved.getErrorMessage(), "失败路径 errorMessage 应被填充");
        assertEquals("checkIP: IP 不可达: 10.0.0.99", saved.getErrorMessage());
    }

    // ===== Test 2: triggerTest — non-existent node throws BIZ_5015 =====

    @Test
    void triggerTest_nonExistentNode_throwsBiz5015() {
        when(nodeRepository.existsById("ghost")).thenReturn(false);

        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> connectivityService.triggerTest("ghost"));

        assertEquals(FepErrorCode.BIZ_5015, ex.getErrorCode(),
                "节点不存在应抛 BIZ_5015");
    }

    // ===== Test 3: getSummary — with records returns correct stats =====

    @Test
    void getSummary_withRecords_returnsCorrectStats() {
        final String nodeId = "node1";
        when(nodeRepository.existsById(nodeId)).thenReturn(true);

        TlqConnectivityRecord lastRecord = new TlqConnectivityRecord();
        lastRecord.setRecordId("rec1");
        lastRecord.setNodeId(nodeId);
        lastRecord.setTestResult(ConnectivityTestResult.SUCCESS);
        lastRecord.setTestTime(LocalDateTime.of(2026, 4, 9, 10, 0, 0));

        when(recordRepository.findFirstByNodeIdOrderByTestTimeDesc(nodeId))
                .thenReturn(Optional.of(lastRecord));
        when(recordRepository.countByNodeId(nodeId)).thenReturn(10L);
        when(recordRepository.countByNodeIdAndTestResult(nodeId, ConnectivityTestResult.SUCCESS))
                .thenReturn(8L);

        ConnectivitySummaryResponse summary = connectivityService.getSummary(nodeId);

        assertEquals(nodeId, summary.getNodeId());
        assertEquals(ConnectivityTestResult.SUCCESS, summary.getLastResult());
        assertEquals(LocalDateTime.of(2026, 4, 9, 10, 0, 0), summary.getLastTestTime());
        assertEquals(10L, summary.getTotalTests());
        assertEquals(8L, summary.getSuccessCount());
        assertEquals(80.0, summary.getSuccessRate(), 0.001,
                "10 次中 8 次成功 → 成功率 80.0%");
    }

    // ===== Test 4: getSummary — no records returns zero stats =====

    @Test
    void getSummary_noRecords_returnsZeroStats() {
        final String nodeId = "node2";
        when(nodeRepository.existsById(nodeId)).thenReturn(true);
        when(recordRepository.findFirstByNodeIdOrderByTestTimeDesc(nodeId))
                .thenReturn(Optional.empty());
        when(recordRepository.countByNodeId(nodeId)).thenReturn(0L);
        when(recordRepository.countByNodeIdAndTestResult(nodeId, ConnectivityTestResult.SUCCESS))
                .thenReturn(0L);

        ConnectivitySummaryResponse summary = connectivityService.getSummary(nodeId);

        assertEquals(nodeId, summary.getNodeId());
        assertNull(summary.getLastResult(), "无记录时 lastResult 应为 null");
        assertNull(summary.getLastTestTime(), "无记录时 lastTestTime 应为 null");
        assertEquals(0L, summary.getTotalTests());
        assertEquals(0L, summary.getSuccessCount());
        assertEquals(0.0, summary.getSuccessRate(), 0.001,
                "无测试记录时成功率应为 0");
    }
}
