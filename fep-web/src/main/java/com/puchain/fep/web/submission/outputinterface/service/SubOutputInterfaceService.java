package com.puchain.fep.web.submission.outputinterface.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceCreateRequest;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceResponse;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URI;

/**
 * 输出接口管理 Service。
 *
 *
 * <p>提供输出接口 CRUD、状态切换、连通性测试功能，
 * 并在创建/更新时校验 businessTypeId 外键有效性。
 * 参见 PRD v1.3 §5.5.2 输出接口管理（FR-WEB-SUB-OUT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SubOutputInterfaceService {

    private static final Logger log = LoggerFactory.getLogger(SubOutputInterfaceService.class);

    /** 毫秒/秒转换系数。 */
    private static final int MILLIS_PER_SECOND = 1000;

    /** HTTP 成功响应码下界（含）。 */
    private static final int HTTP_OK_MIN = 200;

    /** HTTP 成功响应码上界（不含）。 */
    private static final int HTTP_OK_MAX = 400;

    /** 连通性测试最大超时秒数。 */
    private static final int MAX_TEST_TIMEOUT_SECONDS = 10;

    private final SubOutputInterfaceRepository outputInterfaceRepository;
    private final SysBusinessTypeRepository businessTypeRepository;

    /**
     * 构造 SubOutputInterfaceService。
     *
     * @param outputInterfaceRepository 输出接口 Repository
     * @param businessTypeRepository    业务类型 Repository（用于 FK 校验）
     */
    public SubOutputInterfaceService(final SubOutputInterfaceRepository outputInterfaceRepository,
                                     final SysBusinessTypeRepository businessTypeRepository) {
        this.outputInterfaceRepository = outputInterfaceRepository;
        this.businessTypeRepository = businessTypeRepository;
    }

    /**
     * 搜索输出接口列表（分页）。
     *
     * <p>关键字匹配接口名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<OutputInterfaceResponse> search(final String keyword,
                                                       final int pageNum,
                                                       final int pageSize) {
        Page<SubOutputInterface> page = outputInterfaceRepository.search(
                keyword,
                PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream().map(OutputInterfaceResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 获取输出接口详情。
     *
     * @param interfaceId 接口 ID
     * @return 输出接口响应
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    public OutputInterfaceResponse getById(final String interfaceId) {
        SubOutputInterface entity = outputInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出接口不存在: " + interfaceId));
        return OutputInterfaceResponse.from(entity);
    }

    /**
     * 新增输出接口。
     *
     * @param request 创建请求
     * @return 输出接口响应
     * @throws FepBusinessException 名称已存在（BIZ_5002）或关联业务类型不存在（BIZ_5009）
     */
    @Transactional
    public OutputInterfaceResponse create(final OutputInterfaceCreateRequest request) {
        if (outputInterfaceRepository.existsByInterfaceName(request.getInterfaceName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "接口名称已存在: " + request.getInterfaceName());
        }
        validateBusinessType(request.getBusinessTypeId());

        SubOutputInterface entity = new SubOutputInterface();
        entity.setInterfaceId(IdGenerator.uuid32());
        entity.setInterfaceName(request.getInterfaceName());
        entity.setInterfaceUrl(request.getInterfaceUrl());
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setAuthType(request.getAuthType());
        entity.setTimeoutSeconds(request.getTimeoutSeconds());
        entity.setRetryCount(request.getRetryCount());
        entity.setInterfaceStatus(EnableDisableStatus.ENABLED);
        entity.setCallCount(0L);

        SubOutputInterface saved = outputInterfaceRepository.save(entity);
        log.info("Created output interface: id={}, name={}",
                saved.getInterfaceId(), LogSanitizer.sanitize(saved.getInterfaceName()));
        return OutputInterfaceResponse.from(saved);
    }

    /**
     * 编辑输出接口。
     *
     * @param interfaceId 接口 ID
     * @param request     更新请求
     * @return 更新后的输出接口响应
     * @throws FepBusinessException 接口不存在（BIZ_5001）、名称冲突（BIZ_5002）
     *                              或关联业务类型不存在（BIZ_5009）
     */
    @Transactional
    public OutputInterfaceResponse update(final String interfaceId,
                                           final OutputInterfaceCreateRequest request) {
        SubOutputInterface entity = outputInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出接口不存在: " + interfaceId));
        if (outputInterfaceRepository.existsByInterfaceNameAndIdNot(
                request.getInterfaceName(), interfaceId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "接口名称已存在: " + request.getInterfaceName());
        }
        validateBusinessType(request.getBusinessTypeId());

        entity.setInterfaceName(request.getInterfaceName());
        entity.setInterfaceUrl(request.getInterfaceUrl());
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setAuthType(request.getAuthType());
        entity.setTimeoutSeconds(request.getTimeoutSeconds());
        entity.setRetryCount(request.getRetryCount());

        SubOutputInterface saved = outputInterfaceRepository.save(entity);
        log.info("Updated output interface: id={}", saved.getInterfaceId());
        return OutputInterfaceResponse.from(saved);
    }

    /**
     * 切换接口状态（ENABLED↔DISABLED）。
     *
     * @param interfaceId 接口 ID
     * @return 更新后的输出接口响应
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    @Transactional
    public OutputInterfaceResponse toggleStatus(final String interfaceId) {
        SubOutputInterface entity = outputInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出接口不存在: " + interfaceId));
        EnableDisableStatus newStatus =
                entity.getInterfaceStatus() == EnableDisableStatus.ENABLED
                        ? EnableDisableStatus.DISABLED : EnableDisableStatus.ENABLED;
        entity.setInterfaceStatus(newStatus);
        SubOutputInterface saved = outputInterfaceRepository.save(entity);
        log.info("Toggled output interface status: id={}, newStatus={}",
                interfaceId, newStatus);
        return OutputInterfaceResponse.from(saved);
    }

    /**
     * 删除输出接口。
     *
     * @param interfaceId 接口 ID
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String interfaceId) {
        if (!outputInterfaceRepository.existsById(interfaceId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "输出接口不存在: " + interfaceId);
        }
        outputInterfaceRepository.deleteById(interfaceId);
        log.info("Deleted output interface: id={}", interfaceId);
    }

    /**
     * 测试输出接口连通性（HTTP HEAD 探测）。
     *
     * @param interfaceId 接口 ID
     * @return true 连通，false 不通
     * @throws FepBusinessException 接口不存在（BIZ_5001）
     */
    public boolean testConnectivity(final String interfaceId) {
        SubOutputInterface entity = outputInterfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出接口不存在: " + interfaceId));
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(entity.getInterfaceUrl()).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            int effectiveTimeout = Math.min(entity.getTimeoutSeconds(),
                    MAX_TEST_TIMEOUT_SECONDS) * MILLIS_PER_SECOND;
            conn.setConnectTimeout(effectiveTimeout);
            conn.setReadTimeout(effectiveTimeout);
            int code = conn.getResponseCode();
            conn.disconnect();
            log.info("Connectivity test for interface {}: HTTP {}", interfaceId, code);
            return code >= HTTP_OK_MIN && code < HTTP_OK_MAX;
        } catch (java.io.IOException e) {
            log.warn("Connectivity test failed for interface {}: {}",
                    interfaceId, e.getMessage());
            return false;
        }
    }

    /**
     * 校验 businessTypeId 外键有效性。
     *
     * @param businessTypeId 业务类型 ID（可为 null）
     * @throws FepBusinessException 关联业务类型不存在（BIZ_5009）
     */
    private void validateBusinessType(final String businessTypeId) {
        if (businessTypeId != null && !businessTypeRepository.existsById(businessTypeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5009,
                    "关联业务类型不存在: " + businessTypeId);
        }
    }
}
