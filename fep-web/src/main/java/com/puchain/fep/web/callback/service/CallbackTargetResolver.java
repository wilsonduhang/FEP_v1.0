package com.puchain.fep.web.callback.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 配置驱动解析：inbound msgNo → SysBusinessType(含 msgNo) →
 * enabled {@link SubOutputInterface}。无匹配返回空列表（静默跳过，
 * 数仓模式机构天然无输出接口配置，非异常）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackTargetResolver {

    private final SysBusinessTypeMsgNoRepository msgNoRepo;
    private final SubOutputInterfaceRepository interfaceRepo;

    /**
     * @param msgNoRepo     业务类型 msgNo 成员仓储，非空
     * @param interfaceRepo 输出接口仓储，非空
     */
    public CallbackTargetResolver(final SysBusinessTypeMsgNoRepository msgNoRepo,
                                  final SubOutputInterfaceRepository interfaceRepo) {
        this.msgNoRepo = msgNoRepo;
        this.interfaceRepo = interfaceRepo;
    }

    /**
     * 解析 msgNo 命中的全部 enabled 输出接口（fan-out）。
     *
     * @param msgNo inbound 报文号，非空
     * @return 命中的 enabled 输出接口列表，无匹配为空（静默跳过语义）
     */
    public List<SubOutputInterface> resolve(final String msgNo) {
        Objects.requireNonNull(msgNo, "msgNo");
        final List<String> btIds = msgNoRepo.findBusinessTypeIdsByMsgNo(msgNo);
        if (btIds.isEmpty()) {
            return List.of();
        }
        return interfaceRepo.findByBusinessTypeIdInAndInterfaceStatus(
                btIds, EnableDisableStatus.ENABLED);
    }
}
