package com.puchain.fep.web.sysmgmt.config.datatypeconfig.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.datatypeconfig.domain.SysDataTypeConfig;
import com.puchain.fep.web.sysmgmt.config.datatypeconfig.dto.DataTypeConfigCreateRequest;
import com.puchain.fep.web.sysmgmt.config.datatypeconfig.dto.DataTypeConfigResponse;
import com.puchain.fep.web.sysmgmt.config.datatypeconfig.repository.SysDataTypeConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据类型管理服务。
 *
 * <p>提供数据类型 CRUD、关键字搜索功能。
 * 参见 PRD v1.3 §5.10.7.2f 数据类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysDataTypeConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysDataTypeConfigService.class);

    private final SysDataTypeConfigRepository dataTypeConfigRepository;

    /**
     * 构造 SysDataTypeConfigService。
     *
     * @param dataTypeConfigRepository 数据类型 Repository
     */
    public SysDataTypeConfigService(final SysDataTypeConfigRepository dataTypeConfigRepository) {
        this.dataTypeConfigRepository = dataTypeConfigRepository;
    }

    /**
     * 按关键字搜索数据类型（分页）。
     *
     * <p>关键字匹配类型名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<DataTypeConfigResponse> search(final String keyword,
                                                     final int pageNum,
                                                     final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("createTime").descending());

        Page<SysDataTypeConfig> page;
        if (keyword == null || keyword.isBlank()) {
            page = dataTypeConfigRepository.findAll(pageable);
        } else {
            page = dataTypeConfigRepository.findByTypeNameContaining(keyword, pageable);
        }

        return PageResult.from(page, pageNum, pageSize, DataTypeConfigResponse::from);
    }

    /**
     * 创建数据类型。
     *
     * @param request 创建请求
     * @return 数据类型响应
     * @throws FepBusinessException 编码已存在
     */
    @Transactional
    public DataTypeConfigResponse create(final DataTypeConfigCreateRequest request) {
        if (dataTypeConfigRepository.existsByTypeCode(request.getTypeCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据类型编码已存在: " + request.getTypeCode());
        }

        SysDataTypeConfig entity = new SysDataTypeConfig();
        entity.setDataTypeId(IdGenerator.uuid32());
        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());
        entity.setTypeStatus(EnableDisableStatus.ENABLED);

        SysDataTypeConfig saved = dataTypeConfigRepository.save(entity);
        log.info("DataTypeConfig created: code={}", saved.getTypeCode());
        return DataTypeConfigResponse.from(saved);
    }

    /**
     * 更新数据类型信息。
     *
     * @param dataTypeId 数据类型 ID
     * @param request    更新请求
     * @return 更新后的数据类型响应
     * @throws FepBusinessException 数据类型不存在或编码冲突
     */
    @Transactional
    public DataTypeConfigResponse update(final String dataTypeId,
                                         final DataTypeConfigCreateRequest request) {
        SysDataTypeConfig entity = dataTypeConfigRepository.findById(dataTypeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据类型不存在: " + dataTypeId));

        if (dataTypeConfigRepository.existsByTypeCodeAndDataTypeIdNot(
                request.getTypeCode(), dataTypeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据类型编码已存在: " + request.getTypeCode());
        }

        entity.setTypeName(request.getTypeName());
        entity.setTypeCode(request.getTypeCode());

        SysDataTypeConfig saved = dataTypeConfigRepository.save(entity);
        log.info("DataTypeConfig updated: code={}", saved.getTypeCode());
        return DataTypeConfigResponse.from(saved);
    }

    /**
     * 删除数据类型。
     *
     * @param dataTypeId 数据类型 ID
     * @throws FepBusinessException 数据类型不存在
     */
    @Transactional
    public void delete(final String dataTypeId) {
        SysDataTypeConfig entity = dataTypeConfigRepository.findById(dataTypeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据类型不存在: " + dataTypeId));

        dataTypeConfigRepository.delete(entity);
        log.info("DataTypeConfig deleted: code={}", entity.getTypeCode());
    }
}
