package com.puchain.fep.web.sysmgmt.config.outputtype.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.config.outputtype.domain.OutputTypeStatus;
import com.puchain.fep.web.sysmgmt.config.outputtype.domain.SysOutputType;
import com.puchain.fep.web.sysmgmt.config.outputtype.dto.OutputTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.outputtype.dto.OutputTypeResponse;
import com.puchain.fep.web.sysmgmt.config.outputtype.repository.SysOutputTypeRepository;
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
 * 输出类型管理服务。
 *
 * <p>提供输出类型 CRUD、关键字搜索功能。
 * 参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysOutputTypeService {

    private static final Logger log = LoggerFactory.getLogger(SysOutputTypeService.class);

    private final SysOutputTypeRepository outputTypeRepository;

    /**
     * 构造 SysOutputTypeService。
     *
     * @param outputTypeRepository 输出类型 Repository
     */
    public SysOutputTypeService(final SysOutputTypeRepository outputTypeRepository) {
        this.outputTypeRepository = outputTypeRepository;
    }

    /**
     * 按关键字搜索输出类型（分页）。
     *
     * <p>关键字匹配类型名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<OutputTypeResponse> search(final String keyword,
                                                 final int pageNum,
                                                 final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("createTime").descending());

        Page<SysOutputType> page;
        if (keyword == null || keyword.isBlank()) {
            page = outputTypeRepository.findAll(pageable);
        } else {
            page = outputTypeRepository.findByTypeNameContaining(keyword, pageable);
        }

        List<OutputTypeResponse> records = page.getContent().stream()
                .map(OutputTypeResponse::from)
                .toList();

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 创建输出类型。
     *
     * @param request 创建请求
     * @return 输出类型响应
     * @throws FepBusinessException 编码已存在
     */
    @Transactional
    public OutputTypeResponse create(final OutputTypeCreateRequest request) {
        if (outputTypeRepository.existsByTypeCode(request.getTypeCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "输出类型编码已存在: " + request.getTypeCode());
        }

        SysOutputType entity = new SysOutputType();
        entity.setOutputTypeId(IdGenerator.uuid32());
        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());
        entity.setTypeStatus(OutputTypeStatus.ENABLED);

        SysOutputType saved = outputTypeRepository.save(entity);
        log.info("OutputType created: code={}", saved.getTypeCode());
        return OutputTypeResponse.from(saved);
    }

    /**
     * 更新输出类型信息。
     *
     * @param outputTypeId 输出类型 ID
     * @param request      更新请求
     * @return 更新后的输出类型响应
     * @throws FepBusinessException 输出类型不存在或编码冲突
     */
    @Transactional
    public OutputTypeResponse update(final String outputTypeId,
                                     final OutputTypeCreateRequest request) {
        SysOutputType entity = outputTypeRepository.findById(outputTypeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出类型不存在: " + outputTypeId));

        if (outputTypeRepository.existsByTypeCodeAndOutputTypeIdNot(
                request.getTypeCode(), outputTypeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "输出类型编码已存在: " + request.getTypeCode());
        }

        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());

        SysOutputType saved = outputTypeRepository.save(entity);
        log.info("OutputType updated: code={}", saved.getTypeCode());
        return OutputTypeResponse.from(saved);
    }

    /**
     * 删除输出类型。
     *
     * @param outputTypeId 输出类型 ID
     * @throws FepBusinessException 输出类型不存在
     */
    @Transactional
    public void delete(final String outputTypeId) {
        SysOutputType entity = outputTypeRepository.findById(outputTypeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "输出类型不存在: " + outputTypeId));

        outputTypeRepository.delete(entity);
        log.info("OutputType deleted: code={}", entity.getTypeCode());
    }
}
