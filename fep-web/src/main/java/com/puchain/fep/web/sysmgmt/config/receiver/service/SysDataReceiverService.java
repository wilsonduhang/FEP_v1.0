package com.puchain.fep.web.sysmgmt.config.receiver.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.receiver.domain.SysDataReceiver;
import com.puchain.fep.web.sysmgmt.config.receiver.dto.DataReceiverCreateRequest;
import com.puchain.fep.web.sysmgmt.config.receiver.dto.DataReceiverResponse;
import com.puchain.fep.web.sysmgmt.config.receiver.repository.SysDataReceiverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据接收方管理服务。
 *
 * <p>提供数据接收方 CRUD、关键字搜索功能。
 * 参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysDataReceiverService {

    private static final Logger log = LoggerFactory.getLogger(SysDataReceiverService.class);

    private final SysDataReceiverRepository receiverRepository;

    /**
     * 构造 SysDataReceiverService。
     *
     * @param receiverRepository 数据接收方 Repository
     */
    public SysDataReceiverService(final SysDataReceiverRepository receiverRepository) {
        this.receiverRepository = receiverRepository;
    }

    /**
     * 按关键字搜索数据接收方（分页）。
     *
     * <p>关键字匹配接收方名称；关键字为空时返回全部。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<DataReceiverResponse> search(final String keyword,
                                                   final int pageNum,
                                                   final int pageSize) {
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize,
                Sort.by("createTime").descending());

        Page<SysDataReceiver> page;
        if (keyword == null || keyword.isBlank()) {
            page = receiverRepository.findAll(pageable);
        } else {
            page = receiverRepository.findByReceiverNameContaining(keyword, pageable);
        }

        return PageResult.from(page, pageNum, pageSize, DataReceiverResponse::from);
    }

    /**
     * 创建数据接收方。
     *
     * @param request 创建请求
     * @return 数据接收方响应
     * @throws FepBusinessException 名称已存在
     */
    @Transactional
    public DataReceiverResponse create(final DataReceiverCreateRequest request) {
        if (receiverRepository.existsByReceiverName(request.getReceiverName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据接收方名称已存在: " + request.getReceiverName());
        }

        SysDataReceiver entity = new SysDataReceiver();
        entity.setReceiverId(IdGenerator.uuid32());
        entity.setReceiverName(request.getReceiverName());
        entity.setReceiverMethod(request.getReceiverMethod());
        entity.setReceiverAddress(request.getReceiverAddress());
        entity.setReceiverStatus(EnableDisableStatus.ENABLED);

        SysDataReceiver saved = receiverRepository.save(entity);
        log.info("DataReceiver created: name={}", saved.getReceiverName());
        return DataReceiverResponse.from(saved);
    }

    /**
     * 更新数据接收方信息。
     *
     * @param receiverId 接收方 ID
     * @param request    更新请求
     * @return 更新后的数据接收方响应
     * @throws FepBusinessException 接收方不存在或名称冲突
     */
    @Transactional
    public DataReceiverResponse update(final String receiverId,
                                       final DataReceiverCreateRequest request) {
        SysDataReceiver entity = receiverRepository.findById(receiverId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据接收方不存在: " + receiverId));

        if (receiverRepository.existsByReceiverNameAndReceiverIdNot(
                request.getReceiverName(), receiverId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "数据接收方名称已存在: " + request.getReceiverName());
        }

        entity.setReceiverName(request.getReceiverName());
        entity.setReceiverMethod(request.getReceiverMethod());
        entity.setReceiverAddress(request.getReceiverAddress());
        if (request.getReceiverStatus() != null) {
            entity.setReceiverStatus(request.getReceiverStatus());
        }

        SysDataReceiver saved = receiverRepository.save(entity);
        log.info("DataReceiver updated: name={}", saved.getReceiverName());
        return DataReceiverResponse.from(saved);
    }

    /**
     * 删除数据接收方。
     *
     * @param receiverId 接收方 ID
     * @throws FepBusinessException 接收方不存在
     */
    @Transactional
    public void delete(final String receiverId) {
        SysDataReceiver entity = receiverRepository.findById(receiverId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "数据接收方不存在: " + receiverId));

        receiverRepository.delete(entity);
        log.info("DataReceiver deleted: name={}", entity.getReceiverName());
    }
}
