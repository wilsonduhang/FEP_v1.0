package com.puchain.fep.web.callback.http;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * 凭证感知连通性探测编排：按 interfaceId 加载 {@link SubOutputInterface}，委托
 * {@link CallbackHttpClient#probe} 执行凭证感知 HEAD 探测。
 *
 * <p>归属 callback 模块（callback 已依赖 submission/outputinterface，反向会成环）。
 * 参见 PRD v1.3 §5.5.2 测试连通性 + §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackConnectivityProbe {

    private final SubOutputInterfaceRepository interfaceRepo;
    private final CallbackHttpClient httpClient;

    /**
     * 构造连通性探测编排组件。
     *
     * @param interfaceRepo 输出接口仓储，不可为 null
     * @param httpClient    回调 HTTP 客户端（含凭证感知 probe），不可为 null
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackConnectivityProbe(final SubOutputInterfaceRepository interfaceRepo,
                                     final CallbackHttpClient httpClient) {
        this.interfaceRepo = interfaceRepo;
        this.httpClient = httpClient;
    }

    /**
     * 对指定输出接口执行凭证感知连通性探测。
     *
     * @param interfaceId 输出接口 ID
     * @return 探测结果（不含任何凭证明文/密文）
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    public CallbackProbeResult probe(final String interfaceId) {
        final SubOutputInterface target = interfaceRepo.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "output interface not found, interfaceId=" + interfaceId));
        return httpClient.probe(target);
    }
}
