package com.puchain.fep.web.bizdata.definition.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.bizdata.definition.domain.BizMessageDefinition;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionCreateRequest;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionResponse;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionUpdateRequest;
import com.puchain.fep.web.bizdata.definition.repository.BizMessageDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for business message definition management.
 *
 * <p>Provides CRUD, status toggle, and paginated search for message definitions.
 * See PRD v1.3 section 5.3.1 + section 5.3.2 (FR-WEB-BIZ-LIST, FR-WEB-BIZ-DICT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class BizMessageDefinitionService {

    private static final Logger log =
            LoggerFactory.getLogger(BizMessageDefinitionService.class);

    private final BizMessageDefinitionRepository definitionRepository;

    /**
     * Construct BizMessageDefinitionService.
     *
     * @param definitionRepository message definition repository
     */
    public BizMessageDefinitionService(
            final BizMessageDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    /**
     * Search message definitions with keyword (paginated).
     *
     * <p>Keyword matches messageCode or messageName; null returns all.</p>
     *
     * @param keyword  search keyword (may be null)
     * @param pageNum  page number (1-based)
     * @param pageSize page size
     * @return paginated results
     */
    @Transactional(readOnly = true)
    public PageResult<DefinitionResponse> search(final String keyword,
                                                  final int pageNum,
                                                  final int pageSize) {
        Page<BizMessageDefinition> page = definitionRepository.search(
                keyword,
                PageRequest.of(pageNum - 1, pageSize,
                        Sort.by("sortOrder").ascending()
                                .and(Sort.by("createTime").descending())));
        return new PageResult<>(
                page.getContent().stream().map(DefinitionResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * Get a message definition by ID.
     *
     * @param definitionId definition ID
     * @return definition response
     * @throws FepBusinessException definition not found (BIZ_5012)
     */
    @Transactional(readOnly = true)
    public DefinitionResponse getById(final String definitionId) {
        BizMessageDefinition entity = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5012,
                        "报文类型不存在: " + definitionId));
        return DefinitionResponse.from(entity);
    }

    /**
     * Create a new message definition.
     *
     * @param request creation request
     * @return created definition response
     * @throws FepBusinessException message code already exists (BIZ_5011)
     */
    @Transactional
    public DefinitionResponse create(final DefinitionCreateRequest request) {
        if (definitionRepository.existsByMessageCode(request.getMessageCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5011,
                    "报文类型编码已存在: " + request.getMessageCode());
        }

        BizMessageDefinition entity = new BizMessageDefinition();
        entity.setDefinitionId(IdGenerator.uuid32());
        entity.setMessageCode(request.getMessageCode());
        entity.setMessageName(request.getMessageName());
        entity.setDirection(request.getDirection());
        entity.setBusinessTypeId(request.getBusinessTypeId());
        entity.setFieldCount(request.getFieldCount());
        entity.setFieldSummary(request.getFieldSummary());
        entity.setSampleXml(request.getSampleXml());
        entity.setSortOrder(request.getSortOrder());
        entity.setDefinitionStatus(EnableDisableStatus.ENABLED);

        BizMessageDefinition saved = definitionRepository.save(entity);
        log.info("Created message definition: id={}, code={}",
                saved.getDefinitionId(),
                LogSanitizer.sanitize(saved.getMessageCode()));
        return DefinitionResponse.from(saved);
    }

    /**
     * Update a message definition.
     *
     * @param definitionId definition ID
     * @param request      update request
     * @return updated definition response
     * @throws FepBusinessException definition not found (BIZ_5012)
     *                              or code conflict (BIZ_5011)
     */
    @Transactional
    public DefinitionResponse update(final String definitionId,
                                      final DefinitionUpdateRequest request) {
        BizMessageDefinition entity = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5012,
                        "报文类型不存在: " + definitionId));

        if (request.getMessageCode() != null
                && definitionRepository.existsByMessageCodeAndDefinitionIdNot(
                        request.getMessageCode(), definitionId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5011,
                    "报文类型编码已存在: " + request.getMessageCode());
        }

        if (request.getMessageCode() != null) {
            entity.setMessageCode(request.getMessageCode());
        }
        if (request.getMessageName() != null) {
            entity.setMessageName(request.getMessageName());
        }
        if (request.getDirection() != null) {
            entity.setDirection(request.getDirection());
        }
        if (request.getBusinessTypeId() != null) {
            entity.setBusinessTypeId(request.getBusinessTypeId());
        }
        if (request.getFieldCount() != null) {
            entity.setFieldCount(request.getFieldCount());
        }
        if (request.getFieldSummary() != null) {
            entity.setFieldSummary(request.getFieldSummary());
        }
        if (request.getSampleXml() != null) {
            entity.setSampleXml(request.getSampleXml());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }

        BizMessageDefinition saved = definitionRepository.save(entity);
        log.info("Updated message definition: id={}", saved.getDefinitionId());
        return DefinitionResponse.from(saved);
    }

    /**
     * Toggle definition status (ENABLED to DISABLED or vice versa).
     *
     * @param definitionId definition ID
     * @return updated definition response
     * @throws FepBusinessException definition not found (BIZ_5012)
     */
    @Transactional
    public DefinitionResponse toggleStatus(final String definitionId) {
        BizMessageDefinition entity = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5012,
                        "报文类型不存在: " + definitionId));
        EnableDisableStatus newStatus =
                entity.getDefinitionStatus() == EnableDisableStatus.ENABLED
                        ? EnableDisableStatus.DISABLED : EnableDisableStatus.ENABLED;
        entity.setDefinitionStatus(newStatus);
        BizMessageDefinition saved = definitionRepository.save(entity);
        log.info("Toggled message definition status: id={}, newStatus={}",
                definitionId, newStatus);
        return DefinitionResponse.from(saved);
    }

    /**
     * Delete a message definition.
     *
     * @param definitionId definition ID
     * @throws FepBusinessException definition not found (BIZ_5012)
     */
    @Transactional
    public void delete(final String definitionId) {
        if (!definitionRepository.existsById(definitionId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5012,
                    "报文类型不存在: " + definitionId);
        }
        definitionRepository.deleteById(definitionId);
        log.info("Deleted message definition: id={}", definitionId);
    }
}
