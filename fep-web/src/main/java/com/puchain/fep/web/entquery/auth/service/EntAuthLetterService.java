package com.puchain.fep.web.entquery.auth.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.entquery.auth.domain.AuthType;
import com.puchain.fep.web.entquery.auth.domain.EntAuthLetter;
import com.puchain.fep.web.entquery.auth.domain.LetterStatus;
import com.puchain.fep.web.entquery.auth.dto.AuthLetterCreateRequest;
import com.puchain.fep.web.entquery.auth.dto.AuthLetterResponse;
import com.puchain.fep.web.entquery.auth.repository.EntAuthLetterRepository;
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

/**
 * 授权书管理服务。
 *
 * <p>提供授权书 CRUD 及提交功能。
 * 参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class EntAuthLetterService {

    private static final Logger log = LoggerFactory.getLogger(EntAuthLetterService.class);

    private final EntAuthLetterRepository letterRepository;
    private final SysEnterpriseRepository enterpriseRepository;

    /**
     * 构造 EntAuthLetterService。
     *
     * @param letterRepository     授权书 Repository
     * @param enterpriseRepository 企业主体 Repository
     */
    public EntAuthLetterService(final EntAuthLetterRepository letterRepository,
                                final SysEnterpriseRepository enterpriseRepository) {
        this.letterRepository = letterRepository;
        this.enterpriseRepository = enterpriseRepository;
    }

    /**
     * 创建授权书。
     *
     * @param request 创建请求
     * @return 授权书响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    @Transactional
    public AuthLetterResponse create(final AuthLetterCreateRequest request) {
        enterpriseRepository.findById(request.getEnterpriseId())
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + request.getEnterpriseId()));

        LocalDateTime now = LocalDateTime.now();

        EntAuthLetter entity = new EntAuthLetter();
        entity.setLetterId(IdGenerator.uuid32());
        entity.setEnterpriseId(request.getEnterpriseId());
        entity.setAuthType(AuthType.valueOf(request.getAuthType()));
        entity.setAuthScope(request.getAuthScope());
        entity.setAuthorizedUsci(request.getAuthorizedUsci());
        entity.setAuthorizedName(request.getAuthorizedName());
        entity.setFilePath(request.getFilePath());
        entity.setLetterStatus(LetterStatus.DRAFT);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        EntAuthLetter saved = letterRepository.save(entity);
        log.info("Auth letter created: letterId={}, authType={}, authorizedUsci={}",
                saved.getLetterId(), saved.getAuthType(),
                LogSanitizer.maskUsci(saved.getAuthorizedUsci()));
        return AuthLetterResponse.from(saved);
    }

    /**
     * 更新授权书（仅 DRAFT 状态可更新，不更新 enterpriseId）。
     *
     * @param letterId 授权书 ID
     * @param request  更新请求
     * @return 更新后的授权书响应
     * @throws FepBusinessException 授权书不存在（BIZ_5001）或非 DRAFT 状态（BIZ_5003）
     */
    @Transactional
    public AuthLetterResponse update(final String letterId,
                                     final AuthLetterCreateRequest request) {
        EntAuthLetter entity = letterRepository.findById(letterId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "授权书不存在: " + letterId));

        if (entity.getLetterStatus() != LetterStatus.DRAFT) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 DRAFT 状态的授权书可编辑，当前状态: " + entity.getLetterStatus());
        }

        entity.setAuthType(AuthType.valueOf(request.getAuthType()));
        entity.setAuthScope(request.getAuthScope());
        entity.setAuthorizedUsci(request.getAuthorizedUsci());
        entity.setAuthorizedName(request.getAuthorizedName());
        entity.setFilePath(request.getFilePath());
        entity.setUpdateTime(LocalDateTime.now());

        EntAuthLetter saved = letterRepository.save(entity);
        log.info("Auth letter updated: letterId={}", saved.getLetterId());
        return AuthLetterResponse.from(saved);
    }

    /**
     * 提交授权书（将状态从 DRAFT 变更为 SUBMITTED）。
     *
     * <p>实际 1004/1104 报文发送待 P1 阶段实现。</p>
     *
     * @param letterId 授权书 ID
     * @return 更新后的授权书响应
     * @throws FepBusinessException 授权书不存在（BIZ_5001）或非 DRAFT 状态（BIZ_5003）
     */
    @Transactional
    public AuthLetterResponse submit(final String letterId) {
        EntAuthLetter entity = letterRepository.findById(letterId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "授权书不存在: " + letterId));

        if (entity.getLetterStatus() != LetterStatus.DRAFT) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 DRAFT 状态的授权书可提交，当前状态: " + entity.getLetterStatus());
        }

        entity.setLetterStatus(LetterStatus.SUBMITTED);
        entity.setSubmitTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        EntAuthLetter saved = letterRepository.save(entity);
        log.info("Auth letter submitted: letterId={}, status=SUBMITTED", saved.getLetterId());
        return AuthLetterResponse.from(saved);
    }

    /**
     * 删除授权书（仅 DRAFT 状态可删除）。
     *
     * @param letterId 授权书 ID
     * @throws FepBusinessException 授权书不存在（BIZ_5001）或非 DRAFT 状态（BIZ_5003）
     */
    @Transactional
    public void delete(final String letterId) {
        EntAuthLetter entity = letterRepository.findById(letterId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "授权书不存在: " + letterId));

        if (entity.getLetterStatus() != LetterStatus.DRAFT) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 DRAFT 状态的授权书可删除，当前状态: " + entity.getLetterStatus());
        }

        letterRepository.delete(entity);
        log.info("Auth letter deleted: letterId={}", letterId);
    }

    /**
     * 按 ID 查询授权书详情。
     *
     * @param letterId 授权书 ID
     * @return 授权书响应
     * @throws FepBusinessException 授权书不存在（BIZ_5001）
     */
    public AuthLetterResponse getById(final String letterId) {
        EntAuthLetter entity = letterRepository.findById(letterId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "授权书不存在: " + letterId));
        return AuthLetterResponse.from(entity);
    }

    /**
     * 搜索授权书（分页）。
     *
     * @param authType     授权书类型（可为 null）
     * @param letterStatus 授权书状态（可为 null）
     * @param keyword      关键字（可为 null，匹配被授权企业 USCI 或名称）
     * @param pageNum      页码（1-based）
     * @param pageSize     每页大小
     * @return 分页结果
     */
    public PageResult<AuthLetterResponse> search(final String authType,
                                                 final String letterStatus,
                                                 final String keyword,
                                                 final int pageNum,
                                                 final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("createTime").descending());

        String at = (authType == null || authType.isBlank()) ? null : authType;
        String ls = (letterStatus == null || letterStatus.isBlank()) ? null : letterStatus;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;

        Page<EntAuthLetter> page = letterRepository.search(at, ls, kw, pageable);

        return PageResult.from(page, pageNum, pageSize, AuthLetterResponse::from);
    }
}
