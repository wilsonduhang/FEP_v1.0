package com.puchain.fep.web.sysmgmt.config.businesstype.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeResponse;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessTypeMsgNo;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
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

    /** msgNo 合法格式：恰好 4 位数字。 */
    private static final String MSG_NO_PATTERN = "\\d{4}";

    private final SysBusinessTypeRepository businessTypeRepository;
    private final SysBusinessTypeMsgNoRepository msgNoRepository;

    /**
     * 构造 SysBusinessTypeService。
     *
     * @param businessTypeRepository 业务类型 Repository
     * @param msgNoRepository        业务类型 msgNo 成员 Repository
     */
    public SysBusinessTypeService(final SysBusinessTypeRepository businessTypeRepository,
                                  final SysBusinessTypeMsgNoRepository msgNoRepository) {
        this.businessTypeRepository = businessTypeRepository;
        this.msgNoRepository = msgNoRepository;
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
        saveMsgNos(saved.getTypeId(), request.getMsgNos());
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
        rebuildMsgNos(typeId, request.getMsgNos());
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

        msgNoRepository.deleteByTypeId(typeId);
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

    /**
     * 验证并保存 msgNo 成员列表（create 路径，先验证再批量插入）。
     *
     * @param typeId 业务类型 ID
     * @param msgNos inbound 报文号列表，可为 null 或空
     * @throws FepBusinessException msgNo 格式非 4 位数字
     */
    private void saveMsgNos(final String typeId, final List<String> msgNos) {
        if (msgNos == null || msgNos.isEmpty()) {
            return;
        }
        validateMsgNos(msgNos);
        final List<SysBusinessTypeMsgNo> entities = msgNos.stream()
                .map(m -> new SysBusinessTypeMsgNo(typeId, m))
                .toList();
        msgNoRepository.saveAll(entities);
    }

    /**
     * 重建 msgNo 成员（update 路径：删旧 insert 新）。
     *
     * @param typeId 业务类型 ID
     * @param msgNos inbound 报文号列表，可为 null（表示不修改，保留原有成员）
     * @throws FepBusinessException msgNo 格式非 4 位数字
     */
    private void rebuildMsgNos(final String typeId, final List<String> msgNos) {
        if (msgNos == null) {
            return;
        }
        validateMsgNos(msgNos);
        msgNoRepository.deleteByTypeId(typeId);
        if (!msgNos.isEmpty()) {
            final List<SysBusinessTypeMsgNo> entities = msgNos.stream()
                    .map(m -> new SysBusinessTypeMsgNo(typeId, m))
                    .toList();
            msgNoRepository.saveAll(entities);
        }
    }

    /**
     * 验证 msgNo 列表：每项必须为 4 位数字，且不含重复项。
     *
     * <p>去重检查前置可避免重复项进入 {@code saveAll} 后触发
     * {@code uk_sbtm_type_msg} 唯一约束 → {@code DataIntegrityViolationException}
     * （会暴露为 5xx 而非业务参数错误）。</p>
     *
     * @param msgNos 报文号列表，非空
     * @throws FepBusinessException 任一项格式不合规或存在重复项
     */
    private void validateMsgNos(final List<String> msgNos) {
        for (final String msgNo : msgNos) {
            if (msgNo == null || !msgNo.matches(MSG_NO_PATTERN)) {
                throw new FepBusinessException(FepErrorCode.PARAM_4002,
                        "msgNo 必须为 4 位数字: " + msgNo);
            }
        }
        if (msgNos.stream().distinct().count() != msgNos.size()) {
            throw new FepBusinessException(FepErrorCode.PARAM_4002,
                    "msgNos 包含重复报文号: " + msgNos);
        }
    }
}
