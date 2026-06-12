package com.puchain.fep.web.validation;

import com.puchain.fep.processor.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 供应链枚举 + 节点状态母本（Plan Task 10，23 条注册）对生产 application.yml 的求值测试。
 *
 * <p>值集源：报文规范 §5.1.7 供应链通用代码规范（p201-205，逐字段脚注行号见 Plan §母本表）+
 * 表 5.1.5-1 节点状态（p200）+ 表 5.1.2-3 处理结果（3108/3113 RetCode）+
 * 3115 qsReturnCode 脚注（行 22480，"1-失败成功"为"成功"笔误，签字确认 {0,1}）。</p>
 */
class RuleMasterSupplyChainCodesTest {

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
            // msgNo, body, field, legal, illegal — 值集源见类 Javadoc 与 yaml 注释
            "3102, ArchiveInfo3102, ApplyMode, 3, 4",          // 表 5.1.7-6 {1,2,3}
            "3102, ArchiveInfo3102, qyType, 6, 7",             // 表 5.1.7-19 {1..6}
            "3102, ArchiveInfo3102, qySize, SC03, SC04",       // 表 5.1.7-20 {SC00..SC03}
            "3000, dzpzInfo3000, ApplyMode, 2, 3",             // 表 5.1.7-22 {1,2}
            "3103, ArchiveReturnInfo3103, CreationRetCode, 99, 10", // 表 5.1.7-3 开户建档段（无 10）
            "3001, ProgressQuery3001, QueryType, 2, 3",        // 表 5.1.7-18 {1,2}
            "3002, ProgressQueryReturn3002, QueryType, 2, 3",
            "3006, qyAccQueryReturn3006, AccReturnCode, 4, 5", // 表 5.1.7-2 {0,1,2,3,4,9}（errata 2026-06-13 补 4-冻结）
            "3105, rzApplyInfo3105, ApplyMode, 3, 4",          // 表 5.1.7-6 {1,2,3}
            "3105, rzApplyInfo3105, StdBizMode, 31, 22",       // 表 5.1.7-4 {11,12,21,31}
            "3105, rzApplyInfo3105, fxMode, 3, 4",             // 表 5.1.7-7 {1,2,3}
            "3004, pzInfoReturn3004, rzPhaseCode, 10, 25",     // 表 5.1.7-3 凭证融资段
            "3009, rzReturnInfo3009, rzPhaseCode, 99, 25",
            "3108, pzCheckQueryReturn3108, RetCode, 95007, 12345", // 表 5.1.2-3 全 52 码
            "3113, hxqyCreditAmt3113, RetCode, 90000, 12345",
            "3109, qyRegister3109, qyFlag, 3, 4",              // 表 5.1.7-9 {1,2,3}
            "3109, qyRegister3109, PlatState, 2, 3",           // 表 5.1.7-10 {1,2}
            "3109, qyRegister3109, PlatType, 4, 5",            // 表 5.1.7-13 {1..4}
            "3109, qyRegister3109, PlatServiceObject, 4, 5",   // 表 5.1.7-14 {1..4}
            "3109, qyRegister3109, PlatDevelopmentMethod, 3, 4", // 表 5.1.7-15 {1,2,3}
            "3115, PlatPay3115, qsReturnCode, 1, 2",           // 脚注行 22480 {0,1}
            "9007, LoginResponse9007, Status, 99, 4",          // 表 5.1.5-1 {1,2,3,99}
            "9009, LogoutResponse9009, Status, 99, 4"})
    void supplyChainAndNodeStatusCodes_shouldEnforce(
            String msgNo, String body, String field, String legal, String illegal)
            throws IOException {
        assertRule(msgNo, body, field, legal, illegal);
    }

    @Test
    void fieldAbsent_shouldPass() throws IOException {
        // 验收"缺失"态：无任何枚举字段的最小 3109 报文 → 通过
        assertThat(RuleMasterTestSupport.validate("3109",
                "<CFX><HEAD><MsgNo>3109</MsgNo></HEAD><MSG><qyRegister3109>"
                        + "<SerialNo>1</SerialNo></qyRegister3109></MSG></CFX>").valid()).isTrue();
    }

    @Test
    void msg9007_statusAndResultRules_coexistAndAggregate() throws IOException {
        // 验收"9007 Status 与 Result 规则并存聚合"：双字段同时非法 → 两条违规聚合
        ValidationResult r = RuleMasterTestSupport.validate("9007",
                "<CFX><HEAD><MsgNo>9007</MsgNo></HEAD><MSG><RealHead9007>"
                        + "<Result>12345</Result></RealHead9007>"
                        + "<LoginResponse9007><Status>4</Status></LoginResponse9007></MSG></CFX>");
        assertThat(r.valid()).isFalse();
        assertThat(r.errors()).hasSize(2);
        assertThat(String.join(";", r.errors())).contains("Result").contains("Status");
    }

    @Test
    void creationRetCode_archiveSegmentExcludes10_rzPhaseIncludes10() throws IOException {
        // 表 5.1.7-3 两段差异：开户建档段无 "10"（0-未申请仅凭证融资段有），凭证融资段含 "10"
        assertThat(RuleMasterTestSupport.validate("3103",
                envelope("3103", "ArchiveReturnInfo3103", "CreationRetCode", "10")).valid())
                .as("3103 CreationRetCode=10 须违规").isFalse();
        assertThat(RuleMasterTestSupport.validate("3004",
                envelope("3004", "pzInfoReturn3004", "rzPhaseCode", "10")).valid())
                .as("3004 rzPhaseCode=10 须合法").isTrue();
    }
}
