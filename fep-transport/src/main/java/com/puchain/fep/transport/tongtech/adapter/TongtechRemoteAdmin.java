package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.tongtech.tlq.admin.dynamic.manage.TlqDynamic;
import com.tongtech.tlq.admin.remote.api.TLQConnect;
import com.tongtech.tlq.admin.remote.api.TLQParameterException;
import com.tongtech.tlq.admin.remote.api.TLQRemoteException;
import com.tongtech.tlq.admin.remote.api.node.TLQOptNodeSystem;
import com.tongtech.tlq.admin.remote.api.qcu.TLQOptCheck;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqQCU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Tongtech (real TLQ SDK) {@link RemoteAdmin} implementation
 * (P1c T7 v1d / PRD v1.3 §3.7 + §5.7.5).
 *
 * <p><strong>4 阶段连通性探测</strong> {@link #checkConnectivity(String, int)}:</p>
 * <ol>
 *   <li>{@link TLQConnect}{@code (adminHost, adminPort)} — admin 通道
 *       ({@code props.getAdminHost()/getAdminPort()}, broker 管理端)</li>
 *   <li>{@link TLQOptCheck#checkIP(String)} — DNS/IP 可达性（探测目标 host）</li>
 *   <li>{@link TLQOptCheck#checkListenPort(int)} — 端口监听（探测目标 port）</li>
 *   <li>{@link TlqQCU#tlqTestLine(String, int)} — client 通道协议层探测
 *       （本地 QCU 必须已建链，由 {@link TongtechTlqConnectionFactory} 管理）</li>
 * </ol>
 *
 * <p>{@code admin 通道} (1-3 阶段) 与 {@code client 通道} (4 阶段) 在生产中
 * host:port 完全可能不同（broker 管理端口 vs 业务端口），不可混用。</p>
 *
 * <p><strong>Resource lifecycle</strong>: 每次探测构造一个独立 {@link TLQConnect}
 * 并在 {@code finally} 静默关闭，避免长持有 admin 通道。本地 QCU 由 connection
 * factory 维护，不在此关闭。</p>
 *
 * <p><strong>真机校准项</strong> (P1c-IT-bridge follow-up):</p>
 * <ul>
 *   <li>{@link #mapStateString(String)} 关键字 (RUNNING / STOPPED / ERROR / etc.)
 *       由真机 {@code TLQOptNodeSystem.getNodeState()} 输出校准</li>
 *   <li>{@link TLQConnect} 用户名/密码 setter 由真机 SDK 文档/调试确认；当前
 *       仅依赖底层协议默认认证</li>
 * </ul>
 *
 * <p>Active when {@code fep.transport.provider=tongtech}. Component-scanned by
 * {@link com.puchain.fep.transport.tongtech.config.TongtechTransportConfiguration}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechRemoteAdmin implements RemoteAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechRemoteAdmin.class);

    /** Successful tlqTestLine return code. */
    private static final int TLQ_TEST_LINE_OK = 0;

    /** Conversion factor for {@code System.nanoTime()} → milliseconds. */
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final TongtechTlqProperties props;
    private final TongtechTlqConnectionFactory connFactory;

    /**
     * Construct a Tongtech-backed remote admin.
     *
     * @param props       TLQ properties (provides admin host/port via
     *                    {@link TongtechTlqProperties#getAdminHost()} /
     *                    {@link TongtechTlqProperties#getAdminPort()})
     * @param connFactory local QCU connection factory (provides the client-side
     *                    {@link TlqQCU} for {@link TlqQCU#tlqTestLine(String, int)})
     */
    public TongtechRemoteAdmin(final TongtechTlqProperties props,
                               final TongtechTlqConnectionFactory connFactory) {
        this.props = props;
        this.connFactory = connFactory;
    }

    /**
     * Run the 4-stage connectivity probe.
     *
     * @param host target host (probed via {@code checkIP / checkListenPort / tlqTestLine})
     * @param port target port
     * @return probe result with reachable / rttMs / detail
     */
    @Override
    public ConnectivityProbe checkConnectivity(final String host, final int port) {
        final long start = System.nanoTime();
        TLQConnect adminConn = null;
        try {
            // Stage 1 — admin 通道直接构造（无中介），props.getAdminHost/AdminPort 是 broker 管理端
            adminConn = new TLQConnect(props.getAdminHost(), props.getAdminPort());
            adminConn.connect();

            // Stage 2 — TLQOptCheck (TLQConnect, TlqDynamic) 双参构造
            final TlqDynamic dynamic = new TlqDynamic();
            final TLQOptCheck check = new TLQOptCheck(adminConn, dynamic);
            if (!check.checkIP(host)) {
                return fail(start, "checkIP", "IP 不可达: " + host);
            }

            // Stage 3 — checkListenPort 返 boolean
            if (!check.checkListenPort(port)) {
                return fail(start, "checkListenPort", "端口未监听: " + port);
            }

            // Stage 4 — client 通道协议探测（与 admin 通道分离）
            //   本地 QCU 必须已 connect，由 TongtechTlqConnectionFactory 管理
            if (!connFactory.isConnected()) {
                connFactory.connect();
            }
            final TlqQCU qcu = connFactory.getQCU();
            final int lineRc = qcu.tlqTestLine(host, port);
            if (lineRc != TLQ_TEST_LINE_OK) {
                return fail(start, "tlqTestLine", "rc=" + lineRc);
            }

            final long rttMs = (System.nanoTime() - start) / NANOS_PER_MILLI;
            LOG.debug("Connectivity OK host={} port={} rtt={}ms", host, port, rttMs);
            return new ConnectivityProbe(true, rttMs, "OK");
        } catch (TLQParameterException pe) {
            // TLQConnect ctor / setters 参数校验失败（与 TLQRemoteException 同 SDK 包但不同语义）
            LOG.warn("Admin connect parameter invalid host={} port={} cause={}",
                    host, port, pe.getMessage());
            return fail(start, "TLQParameterException", pe.getMessage());
        } catch (TLQRemoteException re) {
            // 1-3 阶段（admin 通道）远端异常
            LOG.warn("Remote admin failed host={} port={} cause={}",
                    host, port, re.getMessage());
            return fail(start, "TLQRemoteException", re.getMessage());
        } catch (TlqException te) {
            // 4 阶段（client 通道 tlqTestLine）异常
            LOG.warn("Client probe failed host={} port={} cause={}",
                    host, port, te.getErrorCause());
            return fail(start, "TlqException", te.getErrorCause());
        } finally {
            closeQuietly(adminConn);
        }
    }

    /**
     * Read the SDK node state string and map it to the FEP {@link NodeState} enum.
     *
     * @param host advisory host (currently used only for log diagnostics —
     *             {@link TLQOptNodeSystem#getNodeState()} is no-arg, so the
     *             node identity is implicit in the {@link TLQConnect} session)
     * @param port advisory port
     * @return mapped node state; {@link NodeState#UNKNOWN} when SDK call fails
     *         or returns an unrecognised string
     */
    @Override
    public NodeState getRemoteNodeState(final String host, final int port) {
        TLQConnect adminConn = null;
        try {
            adminConn = new TLQConnect(props.getAdminHost(), props.getAdminPort());
            adminConn.connect();
            final TlqDynamic dynamic = new TlqDynamic();
            final TLQOptNodeSystem nodeSys = new TLQOptNodeSystem(adminConn, dynamic);
            final String stateStr = nodeSys.getNodeState();
            final NodeState mapped = mapStateString(stateStr);
            LOG.debug("getRemoteNodeState host={} port={} sdkState='{}' mapped={}",
                    host, port, stateStr, mapped);
            return mapped;
        } catch (TLQParameterException pe) {
            LOG.warn("getNodeState parameter invalid cause={}", pe.getMessage());
            return NodeState.UNKNOWN;
        } catch (TLQRemoteException re) {
            LOG.warn("getNodeState failed cause={}", re.getMessage());
            return NodeState.UNKNOWN;
        } finally {
            closeQuietly(adminConn);
        }
    }

    /**
     * Quietly close an admin connection — {@code finally} cleanup must not mask
     * earlier exceptions, so any close failure is logged at WARN and swallowed.
     *
     * @param conn admin connection (may be {@code null} if construction failed)
     */
    private void closeQuietly(final TLQConnect conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (TLQRemoteException ce) {
            LOG.warn("Admin conn close failed: {}", ce.getMessage());
        }
    }

    /**
     * Build a failed probe result, recording the failed stage and cause string.
     *
     * @param start probe start time in nanoseconds (from {@link System#nanoTime()})
     * @param stage failed stage label (e.g. {@code "checkIP" / "tlqTestLine"})
     * @param cause short cause string surfaced to {@code detail}
     * @return failure probe with reachable=false
     */
    private ConnectivityProbe fail(final long start, final String stage, final String cause) {
        final long rttMs = (System.nanoTime() - start) / NANOS_PER_MILLI;
        return new ConnectivityProbe(false, rttMs, stage + ": " + cause);
    }

    /**
     * Map an SDK node-state string to the FEP {@link NodeState} enum via case-insensitive
     * keyword matching (parallels {@code TongtechErrorMapper.mapCause}).
     *
     * <p>The exact SDK string set is calibrated by the P1c-IT-bridge real-machine
     * follow-up; this fallback covers the documented states.</p>
     *
     * @param s SDK state string (may be {@code null})
     * @return mapped {@link NodeState}, never {@code null}
     */
    NodeState mapStateString(final String s) {
        if (s == null) {
            return NodeState.UNKNOWN;
        }
        final String u = s.toUpperCase(java.util.Locale.ROOT);
        if (u.contains("RUNNING") || u.contains("ONLINE") || u.contains("ACTIVE")) {
            return NodeState.ONLINE;
        }
        if (u.contains("STOPPED") || u.contains("OFFLINE") || u.contains("DOWN")) {
            return NodeState.OFFLINE;
        }
        if (u.contains("ERROR") || u.contains("FAULT")) {
            return NodeState.ERROR;
        }
        return NodeState.UNKNOWN;
    }
}
