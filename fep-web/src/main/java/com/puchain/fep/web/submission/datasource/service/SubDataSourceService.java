package com.puchain.fep.web.submission.datasource.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.submission.datasource.domain.SubDataSource;
import com.puchain.fep.web.submission.datasource.dto.DataSourceCreateRequest;
import com.puchain.fep.web.submission.datasource.dto.DataSourceResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据源管理 Service。
 *
 * <p>提供数据源 CRUD 功能，并在创建/更新时校验名称唯一性。
 * 参见 PRD v1.3 §5.5.3 数据源管理（FR-WEB-SUB-SRC）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SubDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(SubDataSourceService.class);

    private final SubDataSourceRepository dataSourceRepository;

    /**
     * 构造 SubDataSourceService。
     *
     * @param dataSourceRepository 数据源 Repository
     */
    public SubDataSourceService(final SubDataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * 搜索数据源列表（分页）。
     *
     * <p>关键字匹配数据源名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<DataSourceResponse> search(final String keyword,
                                                  final int pageNum,
                                                  final int pageSize) {
        Page<SubDataSource> page = dataSourceRepository.search(
                keyword,
                PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream().map(DataSourceResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 获取数据源详情。
     *
     * @param sourceId 数据源 ID
     * @return 数据源响应
     * @throws FepBusinessException 数据源不存在（BIZ_5001）
     */
    public DataSourceResponse getById(final String sourceId) {
        SubDataSource entity = dataSourceRepository.findById(sourceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据源不存在: " + sourceId));
        return DataSourceResponse.from(entity);
    }

    /**
     * 新增数据源。
     *
     * @param request 创建请求
     * @return 数据源响应
     * @throws FepBusinessException 名称已存在（BIZ_5002）
     */
    @Transactional
    public DataSourceResponse create(final DataSourceCreateRequest request) {
        if (dataSourceRepository.existsBySourceName(request.getSourceName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据源名称已存在: " + request.getSourceName());
        }

        SubDataSource entity = new SubDataSource();
        entity.setSourceId(IdGenerator.uuid32());
        entity.setSourceName(request.getSourceName());
        entity.setLogoPath(request.getLogoPath());
        entity.setContactAddress(request.getContactAddress());
        entity.setContactPhone(request.getContactPhone());
        entity.setPushEnabled(request.isPushEnabled());
        entity.setContentType(request.getContentType());
        entity.setClientId(request.getClientId());
        entity.setSourceStatus(EnableDisableStatus.ENABLED);

        SubDataSource saved = dataSourceRepository.save(entity);
        log.info("Created data source: id={}, name={}",
                saved.getSourceId(), saved.getSourceName());
        return DataSourceResponse.from(saved);
    }

    /**
     * 编辑数据源。
     *
     * @param sourceId 数据源 ID
     * @param request  更新请求
     * @return 更新后的数据源响应
     * @throws FepBusinessException 数据源不存在（BIZ_5001）或名称冲突（BIZ_5002）
     */
    @Transactional
    public DataSourceResponse update(final String sourceId,
                                      final DataSourceCreateRequest request) {
        SubDataSource entity = dataSourceRepository.findById(sourceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据源不存在: " + sourceId));
        if (dataSourceRepository.existsBySourceNameAndIdNot(
                request.getSourceName(), sourceId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据源名称已存在: " + request.getSourceName());
        }

        entity.setSourceName(request.getSourceName());
        entity.setLogoPath(request.getLogoPath());
        entity.setContactAddress(request.getContactAddress());
        entity.setContactPhone(request.getContactPhone());
        entity.setPushEnabled(request.isPushEnabled());
        entity.setContentType(request.getContentType());
        entity.setClientId(request.getClientId());

        SubDataSource saved = dataSourceRepository.save(entity);
        log.info("Updated data source: id={}", saved.getSourceId());
        return DataSourceResponse.from(saved);
    }

    /**
     * 删除数据源。
     *
     * @param sourceId 数据源 ID
     * @throws FepBusinessException 数据源不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String sourceId) {
        if (!dataSourceRepository.existsById(sourceId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "数据源不存在: " + sourceId);
        }
        dataSourceRepository.deleteById(sourceId);
        log.info("Deleted data source: id={}", sourceId);
    }
}
