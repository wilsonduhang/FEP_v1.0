package com.puchain.fep.web.sysmgmt.config.businesstype.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeResponse;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
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
 * 业务类型管理服务。
 *
 * <p>提供业务类型 CRUD、状态切换、关键字搜索功能。
 * 参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysBusinessTypeService {

    private static final Logger log = LoggerFactory.getLogger(SysBusinessTypeService.class);

    private final SysBusinessTypeRepository businessTypeRepository;

    /**
     * 构造 SysBusinessTypeService。
     *
     * @param businessTypeRepository 业务类型 Repository
     */
    public SysBusinessTypeService(final SysBusinessTypeRepository businessTypeRepository) {
        this.businessTypeRepository = businessTypeRepository;
    }

    /**
     * 按关键字搜索业务类型（分页）。
     *
     * <p>关键字匹配类型名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<BusinessTypeResponse> search(final String keyword,
                                                   final int pageNum,
                                                   final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("sortOrder").ascending().and(Sort.by("createTime").descending()));

        Page<SysBusinessType> page;
        if (keyword == null || keyword.isBlank()) {
            page = businessTypeRepository.findAll(pageable);
        } else {
            page = businessTypeRepository.findByTypeNameContaining(keyword, pageable);
        }

        List<BusinessTypeResponse> records = page.getContent().stream()
                .map(BusinessTypeResponse::from)
                .toList();

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 创建业务类型。
     *
     * @param request 创建请求
     * @return 业务类型响应
     * @throws FepBusinessException 编码已存在
     */
    @Transactional
    public BusinessTypeResponse create(final BusinessTypeCreateRequest request) {
        if (businessTypeRepository.existsByTypeCode(request.getTypeCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "业务类型编码已存在: " + request.getTypeCode());
        }

        SysBusinessType entity = new SysBusinessType();
        entity.setTypeId(IdGenerator.uuid32());
        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());
        entity.setSortOrder(request.getSortOrder());
        entity.setTypeStatus(EnableDisableStatus.ENABLED);

        SysBusinessType saved = businessTypeRepository.save(entity);
        log.info("BusinessType created: code={}", saved.getTypeCode());
        return BusinessTypeResponse.from(saved);
    }

    /**
     * 更新业务类型信息。
     *
     * @param typeId  业务类型 ID
     * @param request 更新请求
     * @return 更新后的业务类型响应
     * @throws FepBusinessException 业务类型不存在或编码冲突
     */
    @Transactional
    public BusinessTypeResponse update(final String typeId,
                                       final BusinessTypeCreateRequest request) {
        SysBusinessType entity = businessTypeRepository.findById(typeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务类型不存在: " + typeId));

        if (businessTypeRepository.existsByTypeCodeAndTypeIdNot(request.getTypeCode(), typeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "业务类型编码已存在: " + request.getTypeCode());
        }

        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());
        entity.setSortOrder(request.getSortOrder());

        SysBusinessType saved = businessTypeRepository.save(entity);
        log.info("BusinessType updated: code={}", saved.getTypeCode());
        return BusinessTypeResponse.from(saved);
    }

    /**
     * 删除业务类型。
     *
     * @param typeId 业务类型 ID
     * @throws FepBusinessException 业务类型不存在
     */
    @Transactional
    public void delete(final String typeId) {
        SysBusinessType entity = businessTypeRepository.findById(typeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务类型不存在: " + typeId));

        businessTypeRepository.delete(entity);
        log.info("BusinessType deleted: code={}", entity.getTypeCode());
    }

    /**
     * 切换业务类型状态（启用/停用）。
     *
     * @param typeId 业务类型 ID
     * @param status 目标状态
     * @return 更新后的业务类型响应
     * @throws FepBusinessException 业务类型不存在
     */
    @Transactional
    public BusinessTypeResponse toggleStatus(final String typeId,
                                             final EnableDisableStatus status) {
        SysBusinessType entity = businessTypeRepository.findById(typeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务类型不存在: " + typeId));

        entity.setTypeStatus(status);
        SysBusinessType saved = businessTypeRepository.save(entity);
        log.info("BusinessType status changed: code={}, status={}", saved.getTypeCode(), status);
        return BusinessTypeResponse.from(saved);
    }
}
