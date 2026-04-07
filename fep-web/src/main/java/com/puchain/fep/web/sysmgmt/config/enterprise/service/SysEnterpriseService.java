package com.puchain.fep.web.sysmgmt.config.enterprise.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.AuditStatus;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterprise;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseResponse;
import com.puchain.fep.web.sysmgmt.config.enterprise.repository.SysEnterpriseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 企业主体管理服务。
 *
 * <p>提供企业主体 CRUD 及关键字/状态搜索功能。
 * 参见 PRD v1.3 §5.10.7.3 企业主体管理（FR-WEB-SYS-CONF-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysEnterpriseService {

    private static final Logger log = LoggerFactory.getLogger(SysEnterpriseService.class);

    private final SysEnterpriseRepository enterpriseRepository;

    /**
     * 构造 SysEnterpriseService。
     *
     * @param enterpriseRepository 企业主体 Repository
     */
    public SysEnterpriseService(final SysEnterpriseRepository enterpriseRepository) {
        this.enterpriseRepository = enterpriseRepository;
    }

    /**
     * 按关键字和审核状态搜索企业主体（分页）。
     *
     * @param keyword     关键字（可为空/null 表示全量查询）
     * @param auditStatus 审核状态字符串（可为空/null 表示不过滤）
     * @param pageNum     页码（1-based）
     * @param pageSize    每页大小
     * @return 分页结果
     */
    public PageResult<EnterpriseResponse> search(final String keyword,
                                                 final String auditStatus,
                                                 final int pageNum,
                                                 final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("createTime").descending());

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String status = (auditStatus == null || auditStatus.isBlank()) ? null : auditStatus;

        Page<SysEnterprise> page = enterpriseRepository.search(kw, status, pageable);

        List<EnterpriseResponse> records = page.getContent().stream()
                .map(EnterpriseResponse::from)
                .toList();

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 创建企业主体。
     *
     * @param request 创建请求
     * @return 企业主体响应
     * @throws FepBusinessException USCI 已存在（BIZ_5002）
     */
    @Transactional
    public EnterpriseResponse create(final EnterpriseCreateRequest request) {
        if (enterpriseRepository.findByUsci(request.getUsci()).isPresent()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "统一社会信用代码已存在: " + request.getUsci());
        }

        SysEnterprise entity = new SysEnterprise();
        entity.setEnterpriseId(IdGenerator.uuid32());
        entity.setEnterpriseName(request.getEnterpriseName());
        entity.setUsci(request.getUsci());
        entity.setContentType(request.getContentType());
        entity.setClientId(request.getClientId());
        entity.setKeyParams(request.getKeyParams());
        entity.setSignFilePath(request.getSignFilePath());
        entity.setAuditStatus(AuditStatus.PENDING);
        entity.setBizCount(0);

        SysEnterprise saved = enterpriseRepository.save(entity);
        log.info("Enterprise created: name={}, usci={}", saved.getEnterpriseName(), saved.getUsci());
        return EnterpriseResponse.from(saved);
    }

    /**
     * 更新企业主体信息（USCI 不可变）。
     *
     * @param enterpriseId 企业主体 ID
     * @param request      更新请求
     * @return 更新后的企业主体响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    @Transactional
    public EnterpriseResponse update(final String enterpriseId,
                                     final EnterpriseCreateRequest request) {
        SysEnterprise entity = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));

        entity.setEnterpriseName(request.getEnterpriseName());
        entity.setContentType(request.getContentType());
        entity.setClientId(request.getClientId());
        entity.setKeyParams(request.getKeyParams());
        entity.setSignFilePath(request.getSignFilePath());

        SysEnterprise saved = enterpriseRepository.save(entity);
        log.info("Enterprise updated: id={}, name={}", saved.getEnterpriseId(), saved.getEnterpriseName());
        return EnterpriseResponse.from(saved);
    }

    /**
     * 按 ID 查询企业主体。
     *
     * @param enterpriseId 企业主体 ID
     * @return 企业主体响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    public EnterpriseResponse getById(final String enterpriseId) {
        SysEnterprise entity = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));
        return EnterpriseResponse.from(entity);
    }

    /**
     * 删除企业主体。
     *
     * @param enterpriseId 企业主体 ID
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String enterpriseId) {
        SysEnterprise entity = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));

        enterpriseRepository.delete(entity);
        log.info("Enterprise deleted: id={}, name={}", entity.getEnterpriseId(), entity.getEnterpriseName());
    }
}
