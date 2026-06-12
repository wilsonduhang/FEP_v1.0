package com.puchain.fep.web.validation;

import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GROUP_COOCCURRENCE 母本（Plan Task 6，G1-G4）对生产 application.yml 的绑定与求值测试。
 *
 * <p>分组源：报文规范 §2.1.3.1（全规范仅 4 组）——G1 表 2.2.2.2-1 p8（HEAD 文件三字段，
 * scope=HEAD 全报文通配）；G2 表 3.3.8.2-3 p84（3004 RiskRate+edUpdateDateTime）；
 * G3 表 3.3.13.2-10 p111（pzInfo SignElement+klzrfSign，注册 3000/3004，3105 因
 * SignInfo.SignElement required 冲突禁注册）；G4 表 3.3.20.2-3 p149（3115
 * SignElement+qsfqSign）。</p>
 */
class RuleMasterGroupCooccurrenceTest {

    @Test
    void headFileGroup_partialUse_shouldViolate_onAnyMessage() throws IOException {
        // 验收 1：任意报文（9005 样本）HEAD 含 FileName+FileSize 缺 FileContentHash → 违规
        String xml = "<CFX><HEAD><MsgNo>9005</MsgNo><FileName>a.zip</FileName>"
                + "<FileSize>10</FileSize></HEAD><MSG><RealHead9005>"
                + "<TransitionNo>20260612</TransitionNo></RealHead9005></MSG></CFX>";
        ValidationResult r = RuleMasterTestSupport.validate("9005", xml);
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().get(0)).contains("FileContentHash");
    }

    @Test
    void group3004_riskRateWithoutEdUpdateDateTime_shouldViolate() throws IOException {
        // 验收 2：RiskRate 为 XML 容器（无直接文本），元素存在即"使用"
        String partial = "<CFX><MSG><pzInfoReturn3004><RiskRate><orgCode>X1</orgCode></RiskRate>"
                + "</pzInfoReturn3004></MSG></CFX>";
        ValidationResult r = RuleMasterTestSupport.validate("3004", partial);
        assertThat(r.valid()).isFalse();
        assertThat(String.join(";", r.errors())).contains("edUpdateDateTime");

        String both = "<CFX><MSG><pzInfoReturn3004><RiskRate><orgCode>X1</orgCode></RiskRate>"
                + "<edUpdateDateTime>20260612103000</edUpdateDateTime></pzInfoReturn3004></MSG></CFX>";
        assertThat(RuleMasterTestSupport.validate("3004", both).valid()).isTrue();
    }

    @Test
    void group3000And3004_signElementWithoutKlzrfSign_shouldViolate() throws IOException {
        // 验收 3：pzInfo 凭证签名组（共享结构）在 3000 与 3004 均注册
        String xml3000 = "<CFX><MSG><dzpzInfo3000><pzInfo><SignElement>hxqyName|pzNo</SignElement>"
                + "</pzInfo></dzpzInfo3000></MSG></CFX>";
        ValidationResult r3000 = RuleMasterTestSupport.validate("3000", xml3000);
        assertThat(r3000.valid()).isFalse();
        assertThat(String.join(";", r3000.errors())).contains("klzrfSign");

        String xml3004 = "<CFX><MSG><pzInfoReturn3004><pzInfo><SignElement>hxqyName|pzNo</SignElement>"
                + "</pzInfo></pzInfoReturn3004></MSG></CFX>";
        assertThat(RuleMasterTestSupport.validate("3004", xml3004).valid()).isFalse();
    }

    @Test
    void group3115_signElementWithoutQsfqSign_shouldViolate() throws IOException {
        // 验收 4：3115 顶层清算指令签名组
        String xml = "<CFX><MSG><PlatPay3115><SignElement>fkrAccName|Amt</SignElement>"
                + "</PlatPay3115></MSG></CFX>";
        ValidationResult r = RuleMasterTestSupport.validate("3115", xml);
        assertThat(r.valid()).isFalse();
        assertThat(String.join(";", r.errors())).contains("qsfqSign");
    }

    @Test
    void msg3105_signInfoSignElementAlone_shouldPass_groupNotRegistered() throws IOException {
        // 验收 5（负向）：3105 SignInfo.SignElement 为 required，分组未注册 → 不得误拒
        String xml = "<CFX><MSG><rzApplyInfo3105><SignInfo>"
                + "<SignElement>hxqyName|rzqyName|rzAmt</SignElement>"
                + "</SignInfo></rzApplyInfo3105></MSG></CFX>";
        assertThat(RuleMasterTestSupport.validate("3105", xml).valid()).isTrue();
    }

    @Test
    void productionYaml_groupRuleEntryCounts_shouldMatchPlan() throws IOException {
        // 验收 6：G 组注册条目数实算（"*" 1 + 3000 1 + 3004 2 + 3115 1 = 5）
        RuleDefinitionProperties props = RuleMasterTestSupport.bindProductionRules();
        assertThat(countGroupRules(props, "*")).isEqualTo(1);
        assertThat(countGroupRules(props, "3000")).isEqualTo(1);
        assertThat(countGroupRules(props, "3004")).isEqualTo(2);
        assertThat(countGroupRules(props, "3115")).isEqualTo(1);
        assertThat(props.getRules().get("*").get(0).getScope()).isEqualTo("HEAD");
    }

    @Test
    void msg1102_bodyFileNameWithoutHeadFileFields_shouldPass_headScope() throws IOException {
        // 验收 7（决策 5 防误伤回归）：1102 核对项 body FileName（历史已报送文件名）不触发 HEAD 组
        String xml = "<CFX><HEAD><MsgNo>1102</MsgNo></HEAD><MSG><BatchHead1102>"
                + "<TransitionNo>20260612</TransitionNo></BatchHead1102><DataTransferCheckRequest1102>"
                + "<Item><FileName>history-20260601.csv</FileName></Item>"
                + "</DataTransferCheckRequest1102></MSG></CFX>";
        assertThat(RuleMasterTestSupport.validate("1102", xml).valid()).isTrue();
    }

    private static long countGroupRules(RuleDefinitionProperties props, String key) {
        return props.getRules().getOrDefault(key, List.of()).stream()
                .filter(d -> "GROUP_COOCCURRENCE".equals(d.getType())).count();
    }
}
