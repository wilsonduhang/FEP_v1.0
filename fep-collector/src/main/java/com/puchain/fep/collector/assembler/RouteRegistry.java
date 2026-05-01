package com.puchain.fep.collector.assembler;

import com.puchain.fep.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 报文组装路由注册表（Plan §T7b §3）。
 *
 * <p>启动期由所有 {@link RouteContributor} bean 注入合并 routes，存为不可变 Map；
 * {@link DefaultPayloadAssembler#assemble} 据此 O(1) 反查。
 *
 * <p><b>校验：</b>构造期检测重复 payloadDataType key 与重复 messageType（早失败）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RouteRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RouteRegistry.class);

    /** payloadDataType → AssemblerRoute（不可变）。 */
    private final Map<String, AssemblerRoute> routes;

    /**
     * 构造路由注册表。
     *
     * @param contributors 所有 {@link RouteContributor} bean（非 null；不得贡献零路由）
     * @throws IllegalStateException 检测到 payloadDataType 或 messageType 重复，
     *                               或合并后路由数为 0（未注册任何 RouteContributor / 全部 contribute 返回空）
     */
    public RouteRegistry(final List<RouteContributor> contributors) {
        Objects.requireNonNull(contributors, "contributors");
        final Map<String, AssemblerRoute> merged = new HashMap<>();
        final Set<String> messageTypes = new HashSet<>();
        for (RouteContributor contributor : contributors) {
            for (Map.Entry<String, AssemblerRoute> entry : contributor.contribute().entrySet()) {
                final String key = entry.getKey();
                final AssemblerRoute route = entry.getValue();
                if (merged.containsKey(key)) {
                    throw new IllegalStateException(
                            "duplicate payloadDataType: " + LogSanitizer.sanitize(key));
                }
                if (!messageTypes.add(route.messageType())) {
                    throw new IllegalStateException(
                            "duplicate messageType: "
                                    + LogSanitizer.sanitize(route.messageType()));
                }
                merged.put(key, route);
            }
        }
        if (merged.isEmpty()) {
            // Fail-fast at startup vs runtime COLLECT_ASSEMBLE_FAILURE per request.
            // Catches misconfig (no RouteContributor bean / all contribute() returned empty).
            throw new IllegalStateException(
                    "no AssemblerRoute registered — at least one RouteContributor bean must contribute");
        }
        this.routes = Collections.unmodifiableMap(merged);
        LOG.info("RouteRegistry initialized: {} routes", merged.size());
    }

    /**
     * 反查路由。
     *
     * @param payloadDataType 业务侧载荷类型
     * @return 匹配 {@link AssemblerRoute}，未注册返回 {@code null}
     */
    public AssemblerRoute lookup(final String payloadDataType) {
        if (payloadDataType == null) {
            return null;
        }
        return routes.get(payloadDataType);
    }

    /**
     * @return 已注册路由总数
     */
    public int size() {
        return routes.size();
    }

    /**
     * @return 已注册路由的不可变快照（{@code Map<payloadDataType, AssemblerRoute>}）
     */
    public Map<String, AssemblerRoute> routes() {
        // Defensive copy via Map.copyOf — both internal field and return are immutable.
        // This satisfies SpotBugs EI_EXPOSE_REP without requiring an exclude entry.
        return Map.copyOf(routes);
    }
}
