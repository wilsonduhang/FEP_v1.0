package com.puchain.fep.web.sysmgmt.config.enterprise.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.AuditStatus;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterprise;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseBiz;
import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseQueryConfig;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseBizInfoRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseBizInfoResponse;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseQueryConfigRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseQueryConfigResponse;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseResponse;
import com.puchain.fep.web.sysmgmt.config.enterprise.repository.SysEnterpriseBizRepository;
import com.puchain.fep.web.sysmgmt.config.enterprise.repository.SysEnterpriseQueryConfigRepository;
import com.puchain.fep.web.sysmgmt.config.enterprise.repository.SysEnterpriseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /** Number of trailing characters to show when masking USCI in logs. */
    private static final int USCI_MASK_SUFFIX_LEN = 4;

    private final SysEnterpriseRepository enterpriseRepository;
    private final SysEnterpriseBizRepository bizRepository;
    private final SysEnterpriseQueryConfigRepository queryConfigRepository;
    private final SysBusinessTypeRepository businessTypeRepository;

    /**
     * 构造 SysEnterpriseService。
     *
     * @param enterpriseRepository  企业主体 Repository
     * @param bizRepository         企业业务信息关联 Repository
     * @param queryConfigRepository 企业精准查询配置 Repository
     * @param businessTypeRepository 业务类型 Repository
     */
    public SysEnterpriseService(final SysEnterpriseRepository enterpriseRepository,
                                final SysEnterpriseBizRepository bizRepository,
                                final SysEnterpriseQueryConfigRepository queryConfigRepository,
                                final SysBusinessTypeRepository businessTypeRepository) {
        this.enterpriseRepository = enterpriseRepository;
        this.bizRepository = bizRepository;
        this.queryConfigRepository = queryConfigRepository;
        this.businessTypeRepository = businessTypeRepository;
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

        return PageResult.from(page, pageNum, pageSize, EnterpriseResponse::from);
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
                    "统一社会信用代码已存在");
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
        log.info("Enterprise created: name={}, usci=****{}", saved.getEnterpriseName(),
                saved.getUsci().substring(saved.getUsci().length() - USCI_MASK_SUFFIX_LEN));
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

        if (request.getUsci() != null && !entity.getUsci().equals(request.getUsci())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003, "USCI 创建后不可变更");
        }

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

        bizRepository.deleteAll(bizRepository.findByEnterpriseId(enterpriseId));
        queryConfigRepository.findByEnterpriseId(enterpriseId).ifPresent(queryConfigRepository::delete);

        enterpriseRepository.delete(entity);
        log.info("Enterprise deleted: id={}, name={}", entity.getEnterpriseId(), entity.getEnterpriseName());
    }

    /**
     * 查询企业关联的业务信息列表。
     *
     * @param enterpriseId 企业主体 ID
     * @return 业务信息关联列表
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    public List<EnterpriseBizInfoResponse> listBizInfo(final String enterpriseId) {
        enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));
        return bizRepository.findByEnterpriseId(enterpriseId).stream()
                .map(EnterpriseBizInfoResponse::from)
                .toList();
    }

    /**
     * 添加企业业务信息关联。
     *
     * @param enterpriseId 企业主体 ID
     * @param request      关联创建请求
     * @return 创建后的业务信息关联响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）或业务类型不存在（BIZ_5004）
     */
    @Transactional
    public EnterpriseBizInfoResponse addBizInfo(final String enterpriseId,
                                                final EnterpriseBizInfoRequest request) {
        enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));

        if (!businessTypeRepository.existsById(request.getBusinessTypeId())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5004,
                    "业务类型不存在: " + request.getBusinessTypeId());
        }

        SysEnterpriseBiz entity = new SysEnterpriseBiz();
        entity.setId(IdGenerator.uuid32());
        entity.setEnterpriseId(enterpriseId);
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setConfigJson(request.getConfigJson());
        entity.setStatus("ACTIVE");
        entity.setCreateTime(LocalDateTime.now());

        SysEnterpriseBiz saved = bizRepository.save(entity);
        log.info("EnterpriseBiz added: enterpriseId={}, businessTypeId={}", enterpriseId,
                request.getBusinessTypeId());
        return EnterpriseBizInfoResponse.from(saved);
    }

    /**
     * 删除企业业务信息关联。
     *
     * @param enterpriseId 企业主体 ID
     * @param bizInfoId    业务信息关联 ID
     * @throws FepBusinessException 记录不存在或不属于该企业（BIZ_5001）
     */
    @Transactional
    public void removeBizInfo(final String enterpriseId, final String bizInfoId) {
        SysEnterpriseBiz entity = bizRepository.findById(bizInfoId)
                .filter(e -> enterpriseId.equals(e.getEnterpriseId()))
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务信息关联不存在: " + bizInfoId));

        bizRepository.delete(entity);
        log.info("EnterpriseBiz removed: id={}, enterpriseId={}", bizInfoId, enterpriseId);
    }

    /**
     * 获取企业精准查询配置（未配置时返回 null）。
     *
     * @param enterpriseId 企业主体 ID
     * @return 查询配置响应（可为 null）
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    public EnterpriseQueryConfigResponse getQueryConfig(final String enterpriseId) {
        enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));
        return queryConfigRepository.findByEnterpriseId(enterpriseId)
                .map(EnterpriseQueryConfigResponse::from)
                .orElse(null);
    }

    /**
     * 更新（upsert）企业精准查询配置。
     *
     * @param enterpriseId 企业主体 ID
     * @param request      查询配置更新请求
     * @return 更新后的查询配置响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    @Transactional
    public EnterpriseQueryConfigResponse updateQueryConfig(final String enterpriseId,
                                                          final EnterpriseQueryConfigRequest request) {
        enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + enterpriseId));

        LocalDateTime now = LocalDateTime.now();
        SysEnterpriseQueryConfig entity = queryConfigRepository.findByEnterpriseId(enterpriseId)
                .orElseGet(() -> {
                    SysEnterpriseQueryConfig newCfg = new SysEnterpriseQueryConfig();
                    newCfg.setId(IdGenerator.uuid32());
                    newCfg.setEnterpriseId(enterpriseId);
                    newCfg.setStatus("ACTIVE");
                    newCfg.setCreateTime(now);
                    return newCfg;
                });

        entity.setQueryType(request.getQueryType());
        entity.setQueryParams(request.getQueryParams());
        entity.setUpdateTime(now);

        SysEnterpriseQueryConfig saved = queryConfigRepository.save(entity);
        log.info("EnterpriseQueryConfig upserted: enterpriseId={}, queryType={}", enterpriseId,
                request.getQueryType());
        return EnterpriseQueryConfigResponse.from(saved);
    }
}
