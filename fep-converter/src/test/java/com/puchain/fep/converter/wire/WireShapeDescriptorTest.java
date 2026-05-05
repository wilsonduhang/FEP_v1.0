package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link WireShapeDescriptor} 单元测试（P5 T3）。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>反射构造 {@link RequestBusinessHead} / {@link ResponseBusinessHead} 成功路径</li>
 *   <li>compact constructor 拒绝 null 参数（boundary）</li>
 *   <li>反射失败 → FepBusinessException + OUTBOUND_5101（boundary）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("WireShapeDescriptor")
class WireShapeDescriptorTest {

    @Test
    @DisplayName("newHeadInstance creates RequestBusinessHead for 3102 descriptor")
    void newHeadInstance_should_create_RequestBusinessHead_for_3102() {
        WireShapeDescriptor descriptor = new WireShapeDescriptor(
                "BatchHead3102", RequestBusinessHead.class, false);
        assertThat(descriptor.newHeadInstance()).isInstanceOf(RequestBusinessHead.class);
    }

    @Test
    @DisplayName("newHeadInstance creates ResponseBusinessHead for 3101 descriptor")
    void newHeadInstance_should_create_ResponseBusinessHead_for_3101() {
        WireShapeDescriptor descriptor = new WireShapeDescriptor(
                "BatchHead3101", ResponseBusinessHead.class, true);
        assertThat(descriptor.newHeadInstance()).isInstanceOf(ResponseBusinessHead.class);
    }

    @Test
    @DisplayName("compact constructor rejects null headElementName")
    void compactConstructor_should_reject_null_headElementName() {
        assertThatThrownBy(() -> new WireShapeDescriptor(
                null, RequestBusinessHead.class, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("headElementName");
    }

    @Test
    @DisplayName("compact constructor rejects null headClass")
    void compactConstructor_should_reject_null_headClass() {
        assertThatThrownBy(() -> new WireShapeDescriptor(
                "RealHead3009", null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("headClass");
    }

    @Test
    @DisplayName("newHeadInstance wraps reflective failure as OUTBOUND_5101")
    void newHeadInstance_should_wrap_reflection_failure_as_5101() {
        // FailingHead has a public no-arg ctor that throws — triggers
        // ReflectiveOperationException (InvocationTargetException) inside newHeadInstance.
        WireShapeDescriptor descriptor = new WireShapeDescriptor(
                "RealHeadX", FailingHead.class, false);
        assertThatThrownBy(descriptor::newHeadInstance)
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE)
                .hasMessageContaining("无法实例化 head 类");
    }

    /**
     * 反射构造必失败的 head 类（无参 ctor 抛 RuntimeException）。
     */
    public static class FailingHead extends RequestBusinessHead {
        public FailingHead() {
            throw new RuntimeException("ctor intentionally fails for boundary test");
        }
    }
}
