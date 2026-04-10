package com.puchain.fep.converter.type;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 按 msgNo 查找 {@link MessageType} 的注册表。
 *
 * <p>线程安全：内部使用不可变 Map，构造后无状态变更。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageTypeRegistry {

    private final Map<String, MessageType> byMsgNo;

    /**
     * 默认构造：从 {@link MessageType#values()} 构建不可变索引。
     */
    public MessageTypeRegistry() {
        this.byMsgNo = Arrays.stream(MessageType.values())
                .collect(Collectors.toUnmodifiableMap(MessageType::msgNo, t -> t));
    }

    /**
     * 按 msgNo 查找报文类型。
     *
     * @param msgNo 4 位报文编号
     * @return 匹配的 {@link MessageType}
     * @throws MessageConverterException 当 msgNo 未注册时抛出 {@link FepErrorCode#CONV_8003}
     */
    public MessageType lookup(final String msgNo) {
        MessageType type = byMsgNo.get(msgNo);
        if (type == null) {
            throw new MessageConverterException(FepErrorCode.CONV_8003, "msgNo=" + msgNo);
        }
        return type;
    }

    /**
     * 判断 msgNo 是否已注册。
     *
     * @param msgNo 4 位报文编号
     * @return 已注册返回 true
     */
    public boolean isRegistered(final String msgNo) {
        return byMsgNo.containsKey(msgNo);
    }

    /**
     * 返回注册表条目数量。
     *
     * @return 已注册报文类型数量（恒为 44）
     */
    public int size() {
        return byMsgNo.size();
    }
}
