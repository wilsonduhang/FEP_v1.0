package com.puchain.fep.web.submission.scene.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.submission.scene.domain.ScenePushMethod;
import com.puchain.fep.web.submission.scene.domain.SubBusinessScene;
import com.puchain.fep.web.submission.scene.dto.SceneCreateRequest;
import com.puchain.fep.web.submission.scene.dto.SceneResponse;
import com.puchain.fep.web.submission.scene.repository.SubBusinessSceneRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务场景管理 Service。
 *
 * <p>提供业务场景 CRUD、状态切换功能，
 * 并在创建/更新时校验 businessTypeId 外键有效性及
 * MANUAL 推送模式下 importTemplatePath 必填规则。
 * 参见 PRD v1.3 §5.5.4 业务场景管理（FR-WEB-SUB-SCENE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SubBusinessSceneService {

    private static final Logger log = LoggerFactory.getLogger(SubBusinessSceneService.class);

    private final SubBusinessSceneRepository sceneRepository;
    private final SysBusinessTypeRepository businessTypeRepository;

    /**
     * 构造 SubBusinessSceneService。
     *
     * @param sceneRepository        业务场景 Repository
     * @param businessTypeRepository 业务类型 Repository（用于 FK 校验）
     */
    public SubBusinessSceneService(final SubBusinessSceneRepository sceneRepository,
                                   final SysBusinessTypeRepository businessTypeRepository) {
        this.sceneRepository = sceneRepository;
        this.businessTypeRepository = businessTypeRepository;
    }

    /**
     * 搜索业务场景列表（分页）。
     *
     * <p>关键字匹配场景名称；可按业务类型 ID 过滤；按 sortOrder ASC 排序。</p>
     *
     * @param keyword        关键字（可为空/null 表示全量查询）
     * @param businessTypeId 业务类型 ID（可为 null 表示不过滤）
     * @param pageNum        页码（1-based）
     * @param pageSize       每页大小
     * @return 分页结果
     */
    public PageResult<SceneResponse> search(final String keyword,
                                            final String businessTypeId,
                                            final int pageNum,
                                            final int pageSize) {
        Page<SubBusinessScene> page = sceneRepository.search(
                keyword,
                businessTypeId,
                PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder")));
        return new PageResult<>(
                page.getContent().stream().map(SceneResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 获取业务场景详情。
     *
     * @param sceneId 场景 ID
     * @return 业务场景响应
     * @throws FepBusinessException 场景不存在（BIZ_5001）
     */
    public SceneResponse getById(final String sceneId) {
        SubBusinessScene entity = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务场景不存在: " + sceneId));
        return SceneResponse.from(entity);
    }

    /**
     * 新增业务场景。
     *
     * @param request 创建请求
     * @return 业务场景响应
     * @throws FepBusinessException 名称已存在（BIZ_5002）、关联业务类型不存在（BIZ_5009）
     *                              或 MANUAL 模式未提供模板路径（PARAM_4001）
     */
    @Transactional
    public SceneResponse create(final SceneCreateRequest request) {
        if (sceneRepository.existsBySceneName(request.getSceneName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "场景名称已存在: " + request.getSceneName());
        }
        validateBusinessType(request.getBusinessTypeId());
        validateManualTemplate(request);

        SubBusinessScene entity = new SubBusinessScene();
        entity.setSceneId(IdGenerator.uuid32());
        entity.setSceneName(request.getSceneName());
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setPushMethod(request.getPushMethod());
        entity.setImportTemplatePath(request.getImportTemplatePath());
        entity.setRequestUrl(request.getRequestUrl());
        entity.setSortOrder(request.getSortOrder());
        entity.setSceneStatus(EnableDisableStatus.ENABLED);

        SubBusinessScene saved = sceneRepository.save(entity);
        log.info("Created business scene: id={}, name={}",
                saved.getSceneId(), LogSanitizer.sanitize(saved.getSceneName()));
        return SceneResponse.from(saved);
    }

    /**
     * 编辑业务场景。
     *
     * @param sceneId 场景 ID
     * @param request 更新请求
     * @return 更新后的业务场景响应
     * @throws FepBusinessException 场景不存在（BIZ_5001）、名称冲突（BIZ_5002）、
     *                              关联业务类型不存在（BIZ_5009）
     *                              或 MANUAL 模式未提供模板路径（PARAM_4001）
     */
    @Transactional
    public SceneResponse update(final String sceneId,
                                final SceneCreateRequest request) {
        SubBusinessScene entity = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务场景不存在: " + sceneId));
        if (sceneRepository.existsBySceneNameAndIdNot(request.getSceneName(), sceneId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "场景名称已存在: " + request.getSceneName());
        }
        validateBusinessType(request.getBusinessTypeId());
        validateManualTemplate(request);

        entity.setSceneName(request.getSceneName());
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setPushMethod(request.getPushMethod());
        entity.setImportTemplatePath(request.getImportTemplatePath());
        entity.setRequestUrl(request.getRequestUrl());
        entity.setSortOrder(request.getSortOrder());

        SubBusinessScene saved = sceneRepository.save(entity);
        log.info("Updated business scene: id={}", saved.getSceneId());
        return SceneResponse.from(saved);
    }

    /**
     * 切换场景状态（ENABLED↔DISABLED）。
     *
     * @param sceneId 场景 ID
     * @return 更新后的业务场景响应
     * @throws FepBusinessException 场景不存在（BIZ_5001）
     */
    @Transactional
    public SceneResponse toggleStatus(final String sceneId) {
        SubBusinessScene entity = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "业务场景不存在: " + sceneId));
        EnableDisableStatus newStatus =
                entity.getSceneStatus() == EnableDisableStatus.ENABLED
                        ? EnableDisableStatus.DISABLED : EnableDisableStatus.ENABLED;
        entity.setSceneStatus(newStatus);
        SubBusinessScene saved = sceneRepository.save(entity);
        log.info("Toggled business scene status: id={}, newStatus={}",
                sceneId, newStatus);
        return SceneResponse.from(saved);
    }

    /**
     * 删除业务场景。
     *
     * @param sceneId 场景 ID
     * @throws FepBusinessException 场景不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String sceneId) {
        if (!sceneRepository.existsById(sceneId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "业务场景不存在: " + sceneId);
        }
        sceneRepository.deleteById(sceneId);
        log.info("Deleted business scene: id={}", sceneId);
    }

    /**
     * 校验 businessTypeId 外键有效性。
     *
     * @param businessTypeId 业务类型 ID
     * @throws FepBusinessException 关联业务类型不存在（BIZ_5009）
     */
    private void validateBusinessType(final String businessTypeId) {
        if (!businessTypeRepository.existsById(businessTypeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5009,
                    "关联业务类型不存在: " + businessTypeId);
        }
    }

    /**
     * 校验 MANUAL 推送模式下 importTemplatePath 必填。
     *
     * @param request 创建/更新请求
     * @throws FepBusinessException MANUAL 模式未提供模板路径（PARAM_4001）
     */
    private void validateManualTemplate(final SceneCreateRequest request) {
        if (request.getPushMethod() == ScenePushMethod.MANUAL
                && (request.getImportTemplatePath() == null
                    || request.getImportTemplatePath().isBlank())) {
            throw new FepBusinessException(FepErrorCode.PARAM_4001,
                    "手动上传模式必须提供导入模板文件路径");
        }
    }
}
