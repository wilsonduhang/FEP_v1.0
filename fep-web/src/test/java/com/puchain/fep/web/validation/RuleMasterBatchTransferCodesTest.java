package com.puchain.fep.web.validation;

import com.puchain.fep.processor.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批量传输码 + 查询/备案结果母本（Plan Task 9，11 条注册）对生产 application.yml 的求值测试。
 *
 * <p>值集源：报文规范表 5.1.4-1 传输周期 / 5.1.4-2 传输类型 / 5.1.4-3 报送状态（p200）+
 * 表 5.1.6-1 查询结果 / 5.1.6-2 备案结果（p201；5.1.6-1 中 10002 规范重复笔误已去重）。</p>
 */
class RuleMasterBatchTransferCodesTest {

    private static String envelope(String msgNo, String body, String field, String value) {
        return "<CFX><HEAD><MsgNo>" + msgNo + "</MsgNo></HEAD><MSG><" + body + ">"
                + "<" + field + ">" + value + "</" + field + ">"
                + "</" + body + "></MSG></CFX>";
    }

    private static void assertRule(String msgNo, String body, String field,
                                   String legal, String illegal) throws IOException {
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelope(msgNo, body, field, legal)).valid())
                .as("%s %s=%s legal", msgNo, field, legal).isTrue();
        ValidationResult bad = RuleMasterTestSupport.validate(msgNo,
                envelope(msgNo, body, field, illegal));
        assertThat(bad.valid()).as("%s %s=%s illegal", msgNo, field, illegal).isFalse();
        assertThat(String.join(";", bad.errors())).contains(field);
    }

    @ParameterizedTest
    @CsvSource({
            "1101, DataTransfer1101",
            "2101, DataTransfer2101",
            "1102, DataTransferCheckRequest1102",
            "2102, DataTransferCheckResponse2102"})
    void period_table5141_shouldEnforce(String msgNo, String body) throws IOException {
        // 表 5.1.4-1：1-7 + 99；8 不在表内
        assertRule(msgNo, body, "Period", "99", "8");
    }

    @ParameterizedTest
    @CsvSource({"1101, DataTransfer1101", "2101, DataTransfer2101"})
    void type_table5142_shouldEnforce(String msgNo, String body) throws IOException {
        // 表 5.1.4-2：1/2/3；4 不在表内
        assertRule(msgNo, body, "Type", "3", "4");
    }

    @ParameterizedTest
    @CsvSource({"1102, DataTransferCheckRequest1102", "2102, DataTransferCheckResponse2102"})
    void status_table5143_shouldEnforce(String msgNo, String body) throws IOException {
        // 表 5.1.4-3：1-6；7 不在表内
        assertRule(msgNo, body, "Status", "6", "7");
    }

    @Test
    void queryResult_table5161_shouldEnforce() throws IOException {
        // 表 5.1.6-1：90000/10001-10008/19999；20000 不在表内
        assertRule("2103", "CompanyInfoBatchResponse2103", "QueryResult", "10008", "20000");
        assertRule("2103", "CompanyInfoBatchResponse2103", "QueryResult", "90000", "00000");
    }

    @ParameterizedTest
    @CsvSource({"2004, CompanyAuthFileResponse2004", "2104, CompanyAuthFileBatchResponse2104"})
    void recordResult_table5162_shouldEnforce(String msgNo, String body) throws IOException {
        // 表 5.1.6-2：90000/91000/10001-10004/11001-11005/19999；12000 不在表内
        assertRule(msgNo, body, "RecordResult", "11005", "12000");
    }

    @Test
    void fieldAbsent_shouldPass() throws IOException {
        // 缺失通过（可选语义）
        assertThat(RuleMasterTestSupport.validate("1101",
                "<CFX><HEAD><MsgNo>1101</MsgNo></HEAD><MSG><DataTransfer1101>"
                        + "<MainClass>GYL</MainClass><SecondClass>HX01</SecondClass>"
                        + "</DataTransfer1101></MSG></CFX>").valid()).isTrue();
    }
}
