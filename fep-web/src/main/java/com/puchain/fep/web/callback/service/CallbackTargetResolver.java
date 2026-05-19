package com.puchain.fep.web.callback.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
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
    private final SysBusinessTypeRepository businessTypeRepo;
    private final SubOutputInterfaceRepository interfaceRepo;

    /**
     * @param msgNoRepo        业务类型 msgNo 成员仓储，非空
     * @param businessTypeRepo 业务类型仓储（BT 状态过滤），非空
     * @param interfaceRepo    输出接口仓储，非空
     */
    public CallbackTargetResolver(final SysBusinessTypeMsgNoRepository msgNoRepo,
                                  final SysBusinessTypeRepository businessTypeRepo,
                                  final SubOutputInterfaceRepository interfaceRepo) {
        this.msgNoRepo = msgNoRepo;
        this.businessTypeRepo = businessTypeRepo;
        this.interfaceRepo = interfaceRepo;
    }

    /**
     * 解析 msgNo 命中的全部 enabled 输出接口（fan-out）。
     *
     * <p>解析链三段过滤：msgNo → {@code SysBusinessTypeMsgNo} 成员 typeId →
     * 仅保留 ENABLED 的 {@code SysBusinessType}（DISABLED 业务类型不解析）→
     * 其下 ENABLED 的 {@code SubOutputInterface}。</p>
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
        final List<String> enabledBtIds = businessTypeRepo
                .findTypeIdsByTypeIdInAndTypeStatus(btIds, EnableDisableStatus.ENABLED);
        if (enabledBtIds.isEmpty()) {
            return List.of();
        }
        return interfaceRepo.findByBusinessTypeIdInAndInterfaceStatus(
                enabledBtIds, EnableDisableStatus.ENABLED);
    }
}
