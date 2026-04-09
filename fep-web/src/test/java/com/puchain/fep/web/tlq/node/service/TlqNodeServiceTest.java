package com.puchain.fep.web.tlq.node.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import com.puchain.fep.web.tlq.node.dto.TlqNodeCreateRequest;
import com.puchain.fep.web.tlq.node.dto.TlqNodeResponse;
import com.puchain.fep.web.tlq.node.dto.TlqNodeUpdateRequest;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import com.puchain.fep.web.tlq.queue.repository.TlqQueueConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TlqNodeService 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TlqNodeServiceTest {

    @Mock
    private TlqNodeRepository nodeRepository;

    @Mock
    private TlqQueueConfigRepository queueConfigRepository;

    @InjectMocks
    private TlqNodeService tlqNodeService;

    // ─── helpers ────────────────────────────────────────────────────────────

    private TlqNodeCreateRequest buildCreateRequest() {
        TlqNodeCreateRequest req = new TlqNodeCreateRequest();
        req.setNodeName("Node-A");
        req.setNodeRole(TlqNodeRole.MASTER_PRODUCER);
        req.setHostIp("192.168.1.10");
        req.setPort(9100);
        return req;
    }

    private TlqNode buildPersistedNode(final String id) {
        TlqNode node = new TlqNode();
        node.setNodeId(id);
        node.setNodeName("Node-A");
        node.setNodeRole(TlqNodeRole.MASTER_PRODUCER);
        node.setHostIp("192.168.1.10");
        node.setPort(9100);
        node.setProtocol("TCP");
        node.setNodeStatus(TlqNodeStatus.UNKNOWN);
        return node;
    }

    // ─── createNode ─────────────────────────────────────────────────────────

    @Test
    void createNode_validInput_returnsNodeWithUuidAndUnknownStatus() {
        TlqNodeCreateRequest req = buildCreateRequest();
        when(nodeRepository.existsByNodeName("Node-A")).thenReturn(false);
        when(nodeRepository.existsByHostIpAndPort("192.168.1.10", 9100)).thenReturn(false);
        when(nodeRepository.save(any(TlqNode.class))).thenAnswer(inv -> inv.getArgument(0));

        TlqNodeResponse resp = tlqNodeService.createNode(req);

        assertThat(resp.getNodeId()).isNotBlank();
        assertThat(resp.getNodeId()).hasSize(32);
        assertThat(resp.getNodeStatus()).isEqualTo(TlqNodeStatus.UNKNOWN);
        assertThat(resp.getProtocol()).isEqualTo("TCP");
    }

    @Test
    void createNode_duplicateName_throwsBiz5013() {
        TlqNodeCreateRequest req = buildCreateRequest();
        when(nodeRepository.existsByNodeName("Node-A")).thenReturn(true);

        assertThatThrownBy(() -> tlqNodeService.createNode(req))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5013));
        verify(nodeRepository, never()).save(any());
    }

    @Test
    void createNode_duplicateHostPort_throwsBiz5002() {
        TlqNodeCreateRequest req = buildCreateRequest();
        when(nodeRepository.existsByNodeName("Node-A")).thenReturn(false);
        when(nodeRepository.existsByHostIpAndPort("192.168.1.10", 9100)).thenReturn(true);

        assertThatThrownBy(() -> tlqNodeService.createNode(req))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
        verify(nodeRepository, never()).save(any());
    }

    // ─── getNode ────────────────────────────────────────────────────────────

    @Test
    void getNode_existingId_returnsResponse() {
        TlqNode node = buildPersistedNode("abc123");
        when(nodeRepository.findById("abc123")).thenReturn(Optional.of(node));

        TlqNodeResponse resp = tlqNodeService.getNode("abc123");

        assertThat(resp.getNodeId()).isEqualTo("abc123");
        assertThat(resp.getNodeName()).isEqualTo("Node-A");
    }

    @Test
    void getNode_nonExistingId_throwsBiz5015() {
        when(nodeRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tlqNodeService.getNode("not-found"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5015));
    }

    // ─── deleteNode ─────────────────────────────────────────────────────────

    @Test
    void deleteNode_nodeWithQueues_throwsBiz5004() {
        TlqNode node = buildPersistedNode("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(queueConfigRepository.existsByNodeId("node-1")).thenReturn(true);

        assertThatThrownBy(() -> tlqNodeService.deleteNode("node-1"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5004));
        verify(nodeRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteNode_nodeWithoutQueues_deletesSuccessfully() {
        TlqNode node = buildPersistedNode("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(queueConfigRepository.existsByNodeId("node-1")).thenReturn(false);

        tlqNodeService.deleteNode("node-1");

        verify(nodeRepository).deleteById("node-1");
    }

    // ─── changeStatus ───────────────────────────────────────────────────────

    @Test
    void changeStatus_validTransition_updatesStatus() {
        TlqNode node = buildPersistedNode("node-1");
        // node starts as UNKNOWN, UNKNOWN→ONLINE is valid
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(TlqNode.class))).thenAnswer(inv -> inv.getArgument(0));

        TlqNodeResponse resp = tlqNodeService.changeStatus("node-1", TlqNodeStatus.ONLINE);

        assertThat(resp.getNodeStatus()).isEqualTo(TlqNodeStatus.ONLINE);
    }

    @Test
    void changeStatus_invalidTransition_throwsBiz5003() {
        TlqNode node = buildPersistedNode("node-1");
        // UNKNOWN→OFFLINE is not allowed
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));

        assertThatThrownBy(() -> tlqNodeService.changeStatus("node-1", TlqNodeStatus.OFFLINE))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5003));
        verify(nodeRepository, never()).save(any());
    }

    // ─── updateNode ─────────────────────────────────────────────────────────

    @Test
    void updateNode_validInput_updatesFieldsButNotRole() {
        TlqNode node = buildPersistedNode("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.existsByNodeName("Node-B")).thenReturn(false);
        when(nodeRepository.existsByHostIpAndPort(anyString(), anyInt())).thenReturn(false);
        when(nodeRepository.save(any(TlqNode.class))).thenAnswer(inv -> inv.getArgument(0));

        TlqNodeUpdateRequest req = new TlqNodeUpdateRequest();
        req.setNodeName("Node-B");
        req.setHostIp("10.0.0.1");
        req.setPort(9200);
        req.setDescription("updated");

        TlqNodeResponse resp = tlqNodeService.updateNode("node-1", req);

        assertThat(resp.getNodeName()).isEqualTo("Node-B");
        assertThat(resp.getHostIp()).isEqualTo("10.0.0.1");
        assertThat(resp.getPort()).isEqualTo(9200);
        assertThat(resp.getDescription()).isEqualTo("updated");
        // role must remain unchanged
        assertThat(resp.getNodeRole()).isEqualTo(TlqNodeRole.MASTER_PRODUCER);
    }
}
