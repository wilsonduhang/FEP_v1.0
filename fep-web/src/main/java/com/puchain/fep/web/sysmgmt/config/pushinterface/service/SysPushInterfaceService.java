package com.puchain.fep.web.sysmgmt.config.pushinterface.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.AuthType;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.SysPushInterface;
import com.puchain.fep.web.sysmgmt.config.pushinterface.dto.PushInterfaceCreateRequest;
import com.puchain.fep.web.sysmgmt.config.pushinterface.dto.PushInterfaceResponse;
import com.puchain.fep.web.sysmgmt.config.pushinterface.repository.SysPushInterfaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推送接口管理服务。
 *
 * <p>提供推送接口 CRUD、状态切换、关键字搜索功能，
 * 并在创建/更新时校验 businessTypeId 外键有效性。
 * 参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysPushInterfaceService {

    private static final Logger log = LoggerFactory.getLogger(SysPushInterfaceService.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_RETRY_COUNT = 3;

    private final SysPushInterfaceRepository pushInterfaceRepository;
    private final SysBusinessTypeRepository businessTypeRepository;

    /**
     * 构造 SysPushInterfaceService。
     *
     * @param pushInterfaceRepository 推送接口 Repository
     * @param businessTypeRepository  业务类型 Repository（用于 FK 校验）
     */
    public SysPushInterfaceService(final SysPushInterfaceRepository pushInterfaceRepository,
                                   final SysBusinessTypeRepository businessTypeRepository) {
        this.pushInterfaceRepository = pushInterfaceRepository;
        this.businessTypeRepository = businessTypeRepository;
    }

    /**
     * 按关键字搜索推送接口（分页）。
     *
     * <p>关键字匹配接口名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<PushInterfaceResponse> search(final String keyword,
                                                    final int pageNum,
                                                    final int pageSize) {
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize,
                Sort.by("createTime").descending());

        Page<SysPushInterface> page;
        if (keyword == null || keyword.isBlank()) {
            page = pushInterfaceRepository.findAll(pageable);
        } else {
            page = pushInterfaceRepository.findByInterfaceNameContaining(keyword, pageable);
        }

        List<String> typeIds = page.getContent().stream()
                .map(SysPushInterface::getBusinessTypeId)
                .filter(id -> id != null && !id.isBlank())
                .distinct().toList();
        Map<String, String> typeNames = businessTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(SysBusinessType::getTypeId, SysBusinessType::getTypeName));

        return PageResult.from(page, pageNum, pageSize,
                e -> PushInterfaceResponse.from(e,
                        typeNames.getOrDefault(e.getBusinessTypeId(), null)));
    }

    /**
     * 创建推送接口。
     *
     * @param request 创建请求
     * @return 推送接口响应
     * @throws FepBusinessException 名称已存在（BIZ_5002）或关联业务类型不存在（BIZ_5004）
     */
    @Transactional
    public PushInterfaceResponse create(final PushInterfaceCreateRequest request) {
        if (pushInterfaceRepository.existsByInterfaceName(request.getInterfaceName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "推送接口名称已存在: " + request.getInterfaceName());
        }

        validateBusinessTypeId(request.getBusinessTypeId());

        SysPushInterface entity = new SysPushInterface();
        entity.setInterfaceId(IdGenerator.uuid32());
        applyRequestToEntity(entity, request);
        entity.setInterfaceStatus(EnableDisableStatus.ENABLED);

        SysPushInterface saved = pushInterfaceRepository.save(entity);
        log.info("PushInterface created: name={}", saved.getInterfaceName());
        return toResponse(saved);
    }

    /**
     * 更新推送接口信息。
     *
     * @param interfaceId 接口 ID
     * @param request     更新请求
     * @return 更新后的推送接口响应
     * @throws FepBusinessException 接口不存在（BIZ_5001）、名称冲突（BIZ_5002）
     *                              或关联业务类型不存在（BIZ_5004）
     */
    @Transactional
    public PushInterfaceResponse update(final String interfaceId,
                                        final PushInterfaceCreateRequest request) {
        SysPushInterface entity = pushInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "推送接口不存在: " + interfaceId));

        if (pushInterfaceRepository.existsByInterfaceNameAndInterfaceIdNot(
                request.getInterfaceName(), interfaceId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "推送接口名称已存在: " + request.getInterfaceName());
        }

        validateBusinessTypeId(request.getBusinessTypeId());
        applyRequestToEntity(entity, request);

        SysPushInterface saved = pushInterfaceRepository.save(entity);
        log.info("PushInterface updated: name={}", saved.getInterfaceName());
        return toResponse(saved);
    }

    /**
     * 删除推送接口。
     *
     * @param interfaceId 接口 ID
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String interfaceId) {
        SysPushInterface entity = pushInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "推送接口不存在: " + interfaceId));

        pushInterfaceRepository.delete(entity);
        log.info("PushInterface deleted: name={}", entity.getInterfaceName());
    }

    /**
     * 切换推送接口状态（启用/禁用）。
     *
     * @param interfaceId 接口 ID
     * @param status      目标状态
     * @return 更新后的推送接口响应
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    @Transactional
    public PushInterfaceResponse toggleStatus(final String interfaceId,
                                              final EnableDisableStatus status) {
        SysPushInterface entity = pushInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "推送接口不存在: " + interfaceId));

        entity.setInterfaceStatus(status);
        SysPushInterface saved = pushInterfaceRepository.save(entity);
        log.info("PushInterface status changed: name={}, status={}", saved.getInterfaceName(), status);
        return toResponse(saved);
    }

    /**
     * 校验 businessTypeId 外键有效性。
     *
     * <p>若 businessTypeId 非空，则验证该 ID 在业务类型表中是否存在。</p>
     *
     * @param businessTypeId 业务类型 ID（可为 null）
     * @throws FepBusinessException 关联业务类型不存在（BIZ_5004）
     */
    private void validateBusinessTypeId(final String businessTypeId) {
        if (businessTypeId != null && !businessTypeId.isBlank()) {
            if (!businessTypeRepository.existsById(businessTypeId)) {
                throw new FepBusinessException(FepErrorCode.BIZ_5004,
                        "关联的业务类型不存在: " + businessTypeId);
            }
        }
    }

    /**
     * 将请求字段应用到 Entity（创建和更新公用）。
     *
     * @param entity  目标 Entity
     * @param request 请求 DTO
     */
    private void applyRequestToEntity(final SysPushInterface entity,
                                      final PushInterfaceCreateRequest request) {
        entity.setInterfaceName(request.getInterfaceName());
        entity.setInterfaceUrl(request.getInterfaceUrl());
        entity.setPushMethod(request.getPushMethod());
        entity.setAuthType(request.getAuthType() != null ? request.getAuthType() : AuthType.NONE);
        entity.setTimeoutSeconds(request.getTimeoutSeconds() != null
                ? request.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS);
        entity.setRetryCount(request.getRetryCount() != null
                ? request.getRetryCount() : DEFAULT_RETRY_COUNT);
        entity.setBusinessTypeId(request.getBusinessTypeId());
    }

    /**
     * 将 Entity 转换为响应 DTO（含业务类型名称 lookup）。
     *
     * @param entity 推送接口 Entity
     * @return 响应 DTO
     */
    private PushInterfaceResponse toResponse(final SysPushInterface entity) {
        String businessTypeName = null;
        if (entity.getBusinessTypeId() != null && !entity.getBusinessTypeId().isBlank()) {
            businessTypeName = businessTypeRepository.findById(entity.getBusinessTypeId())
                    .map(SysBusinessType::getTypeName)
                    .orElse(null);
        }
        return PushInterfaceResponse.from(entity, businessTypeName);
    }
}
