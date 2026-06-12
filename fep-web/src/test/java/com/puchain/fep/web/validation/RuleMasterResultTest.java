package com.puchain.fep.web.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 母本（Plan Task 7，19 报文统一全 52 码表）对生产 application.yml 的求值测试。
 *
 * <p>值域源：报文规范表 5.1.2-3 处理结果代码表（p197-199）；统一取值域依据 Plan 决策 4
 * （脚注"的'业务处理结果'"为语义描述非封闭域，回执合法携带 95007/95001 等系统码）。</p>
 */
class RuleMasterResultTest {

    /** msgNo → 业务头元素名（逐报文 grep XSD 实测）。 */
    private static final Map<String, String> HEAD_ELEMENT = Map.ofEntries(
            Map.entry("2001", "RealHead2001"), Map.entry("2004", "RealHead2004"),
            Map.entry("2102", "BatchHead2102"), Map.entry("2103", "BatchHead2103"),
            Map.entry("2104", "BatchHead2104"), Map.entry("3002", "RealHead3002"),
            Map.entry("3004", "RealHead3004"), Map.entry("3006", "RealHead3006"),
            Map.entry("3008", "RealHead3008"), Map.entry("3101", "BatchHead3101"),
            Map.entry("3103", "BatchHead3103"), Map.entry("3108", "BatchHead3108"),
            Map.entry("3113", "BatchHead3113"), Map.entry("3115", "BatchHead3115"),
            Map.entry("3020", "RealHead3020"), Map.entry("9007", "RealHead9007"),
            Map.entry("9009", "RealHead9009"), Map.entry("9020", "RealHead9020"),
            Map.entry("9120", "BatchHead9120"));

    private static Stream<String> resultMsgNos() {
        return HEAD_ELEMENT.keySet().stream().sorted();
    }

    private static String envelopeWithResult(String msgNo, String result) {
        String head = HEAD_ELEMENT.get(msgNo);
        return "<CFX><HEAD><MsgNo>" + msgNo + "</MsgNo></HEAD><MSG><" + head + ">"
                + "<TransitionNo>20260612</TransitionNo><Result>" + result + "</Result>"
                + "</" + head + "></MSG></CFX>";
    }

    @ParameterizedTest
    @MethodSource("resultMsgNos")
    void anyRegisteredMessage_fullTableCode_shouldPass_garbageShouldViolate(String msgNo)
            throws IOException {
        // 验收 1/3：全表码（90000 业务档 + 21001 控制类系统码）通过，表外垃圾值违规
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelopeWithResult(msgNo, "90000")).valid()).as("%s 90000", msgNo).isTrue();
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelopeWithResult(msgNo, "21001")).valid()).as("%s 21001", msgNo).isTrue();
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelopeWithResult(msgNo, "12345")).valid()).as("%s 12345", msgNo).isFalse();
    }

    @Test
    void msg3004_systemCode95007_specNamedScenario_shouldPass() throws IOException {
        // 验收 2：规范行 10907——查询不存在的凭证，3004 回执处理结果代码 95007（决策 4 防误拒回归）
        assertThat(RuleMasterTestSupport.validate("3004",
                envelopeWithResult("3004", "95007")).valid()).isTrue();
    }

    @Test
    void resultAbsent_requestDirection_shouldPass() throws IOException {
        // 验收 4：Result 缺失（请求方向）→ 通过
        String xml = "<CFX><HEAD><MsgNo>3115</MsgNo></HEAD><MSG><BatchHead3115>"
                + "<TransitionNo>20260612</TransitionNo></BatchHead3115></MSG></CFX>";
        assertThat(RuleMasterTestSupport.validate("3115", xml).valid()).isTrue();
    }
}
