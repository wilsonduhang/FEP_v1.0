package com.puchain.fep.web.messageinbound.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.messageinbound.dto.InboundMessageRequest;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Objects;

/**
 * 入站报文 REST Controller（PRD §5.3.2.13 + §1991）。
 *
 * <p>P3 Task 2 — 暴露 1 个端点：</p>
 * <ul>
 *   <li>{@code POST /api/v1/messages/inbound} — 接收 dev/test 环境的入站报文，
 *       Base64 解码后委派 {@link InboundMessageDispatcher#dispatch} 进入同步流水线。</li>
 * </ul>
 *
 * <h3>架构边界</h3>
 * <p>生产环境入站走 TLQ 通道（{@code TlqInboundListener}，P3 Task 3）；
 * 本端点仅用于 dev/test 环境联调和 Task 5 集成测试。Controller thin —
 * 只做 Base64 解码 + 委派，业务逻辑在 dispatcher 与下游 listener。</p>
 *
 * <h3>异常路径</h3>
 * <ul>
 *   <li>Base64 格式错误 → 抛 {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_DECODE_FAILURE})
 *       由 {@code GlobalExceptionHandler} 映射 HTTP 400。</li>
 *   <li>未注册 messageType → dispatcher 抛 {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_INVALID_TYPE})
 *       同样 HTTP 400。</li>
 *   <li>Bean Validation 校验失败 → 默认 handler 映射 HTTP 400。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "报文入站", description = "PRD §5.3.2.13 入站报文同步处理端点（dev/test）")
public class MessageInboundController {

    private static final Logger LOG = LoggerFactory.getLogger(MessageInboundController.class);

    private final InboundMessageDispatcher dispatcher;

    /**
     * Constructs the controller.
     *
     * @param dispatcher inbound dispatcher service, non-null
     */
    public MessageInboundController(final InboundMessageDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    /**
     * 接收入站报文，Base64 解码后委派 dispatcher。
     *
     * @param request inbound message request DTO（messageType + transitionNo + xmlBase64）
     * @return ApiResult 携带 InboundMessageResponse（recordId / status / eventPublished）
     */
    @PostMapping("/inbound")
    @OperationLog(module = "报文入站", type = OperationType.CREATE,
            description = "接收入站报文")
    @Operation(summary = "接收入站报文",
            description = "Base64 解码 XML 后委派 InboundMessageDispatcher")
    @ApiResponse(responseCode = "200", description = "处理成功（status=COMPLETED 或 FAILED）")
    @ApiResponse(responseCode = "400",
            description = "参数校验失败 / xmlBase64 解码失败 / messageType 未注册")
    public ApiResult<InboundMessageResponse> handleInbound(
            @Valid @RequestBody final InboundMessageRequest request) {
        final byte[] xml = decodeBase64(request.getXmlBase64());
        LOG.info("inbound REST received messageType={} transitionNo={} xmlBytes={}",
                request.getMessageType(),
                LogSanitizer.sanitize(request.getTransitionNo()),
                xml.length);
        final InboundMessageResponse response = dispatcher.dispatch(
                request.getMessageType(), request.getTransitionNo(), xml);
        return ApiResult.success(response);
    }

    /**
     * Base64 解码 helper。把 {@link IllegalArgumentException} 包装成
     * {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_DECODE_FAILURE})
     * 让 {@code GlobalExceptionHandler} 统一映射 HTTP 400 + 错误码 MSG_8702。
     *
     * @param base64 Base64 编码字符串，非空
     * @return 解码后的字节数组
     * @throws FepBusinessException MSG_8702 if Base64 格式非法
     */
    private static byte[] decodeBase64(final String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new FepBusinessException(
                    FepErrorCode.MSG_INBOUND_DECODE_FAILURE,
                    "xmlBase64 decode failed: " + LogSanitizer.sanitize(e.getMessage()), e);
        }
    }
}
