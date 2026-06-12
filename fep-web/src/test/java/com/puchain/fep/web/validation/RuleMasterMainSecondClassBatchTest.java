package com.puchain.fep.web.validation;

import com.puchain.fep.processor.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MainClass/SecondClass 扩批母本（Plan Task 8，7 报文 ×2 规则）对生产 application.yml 的求值测试。
 *
 * <p>值集源：报文规范表 5.1.3-1 业务类别代码规范（p199，与 1001 R1/R2 同源同值）；
 * 1004/2004/1104/2104 因交付 XSD 无此字段排除（死规则纪律，Plan §排除清单）。</p>
 */
class RuleMasterMainSecondClassBatchTest {

    /** msgNo → body 根元素名（逐报文 grep XSD 实测）。 */
    private static final Map<String, String> BODY_ELEMENT = Map.of(
            "2001", "CompanyInfoResponse2001",
            "1101", "DataTransfer1101",
            "2101", "DataTransfer2101",
            "1102", "DataTransferCheckRequest1102",
            "2102", "DataTransferCheckResponse2102",
            "1103", "CompanyInfoBatchRequest1103",
            "2103", "CompanyInfoBatchResponse2103");

    private static Stream<String> msgNos() {
        return BODY_ELEMENT.keySet().stream().sorted();
    }

    private static String envelope(String msgNo, String mainClass, String secondClass) {
        String body = BODY_ELEMENT.get(msgNo);
        return "<CFX><HEAD><MsgNo>" + msgNo + "</MsgNo></HEAD><MSG><" + body + ">"
                + "<MainClass>" + mainClass + "</MainClass>"
                + "<SecondClass>" + secondClass + "</SecondClass>"
                + "</" + body + "></MSG></CFX>";
    }

    @ParameterizedTest
    @MethodSource("msgNos")
    void mainAndSecondClass_validPair_shouldPass_invalidMain_shouldViolate(String msgNo)
            throws IOException {
        // 验收 1：GYL+HX01 合法通过；MainClass=BAD 违规
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelope(msgNo, "GYL", "HX01")).valid()).as("%s GYL+HX01", msgNo).isTrue();
        ValidationResult bad = RuleMasterTestSupport.validate(msgNo, envelope(msgNo, "BAD", "HX01"));
        assertThat(bad.valid()).as("%s MainClass=BAD", msgNo).isFalse();
        assertThat(String.join(";", bad.errors())).contains("BAD");
    }

    @Test
    void repeatedItems_secondPairInvalid_shouldViolate_pairwise() throws IOException {
        // 验收 2：批量报文重复项第 2 对 (EAST, BAD) 违规（Task 3/4 全值/成对扩展生效）
        String xml = "<CFX><HEAD><MsgNo>1102</MsgNo></HEAD><MSG><DataTransferCheckRequest1102>"
                + "<DataTransferCheck><MainClass>GYL</MainClass><SecondClass>HX01</SecondClass></DataTransferCheck>"
                + "<DataTransferCheck><MainClass>EAST</MainClass><SecondClass>BAD</SecondClass></DataTransferCheck>"
                + "</DataTransferCheckRequest1102></MSG></CFX>";
        ValidationResult r = RuleMasterTestSupport.validate("1102", xml);
        assertThat(r.valid()).isFalse();
        assertThat(String.join(";", r.errors())).contains("BAD").contains("EAST");
    }

    @Test
    void generalMainClass_freeSecondClass_shouldPass() throws IOException {
        // 验收 3：GENERAL 未映射 → 小类不约束（R2 语义一致）
        assertThat(RuleMasterTestSupport.validate("2103",
                envelope("2103", "GENERAL", "FREEDEF")).valid()).isTrue();
    }
}
