package com.puchain.fep.web.validation;

import com.puchain.fep.processor.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Q-F1 回归守护：证明历史 fixture 出母本占位值（MainA01/SubA0101 等）修复为母本合法对
 * {@code COINFO/I1001} 后，绑定生产 {@code application.yml} 的业务规则母本接受该报文，
 * 而占位 {@code MainA01} 被拒——锁死"绑生产 yml 流水线 IT 引爆"回归面。
 *
 * <p>源：§5.8 规则母本 R1/R2（报文 1001 MainClass ENUM + SecondClass DEPENDENT_ENUM）；
 * 母本合法集 §5.1.3 表 5.1.3-1（p199）。复用同包 {@link RuleMasterTestSupport} 的生产配置
 * 绑定 + 全量注册 + 静态缓存 pattern（与 {@code RuleMasterMainClass1001Test} 同源）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RuleMasterFixtureComplianceGuardTest {

    /** 1001 envelope，MainClass/SecondClass 取入参（RuleContext 路径无关，本地名字段视图）。 */
    private static String envelope1001(final String mainClass, final String secondClass) {
        return "<CFX><HEAD><MsgNo>1001</MsgNo></HEAD><MSG><RealHead1001>"
                + "<MainClass>" + mainClass + "</MainClass>"
                + "<SecondClass>" + secondClass + "</SecondClass>"
                + "</RealHead1001></MSG></CFX>";
    }

    @Test
    void fixedFixtureValue_COINFO_I1001_shouldPassProductionRuleMaster() throws IOException {
        ValidationResult r = RuleMasterTestSupport.validate("1001", envelope1001("COINFO", "I1001"));
        assertThat(r.valid())
                .as("修复后 fixture 值 COINFO/I1001 必须通过生产母本（成员资格 + 依赖枚举自洽）")
                .isTrue();
    }

    @Test
    void legacyPlaceholderValue_MainA01_shouldFailProductionRuleMaster() throws IOException {
        ValidationResult r = RuleMasterTestSupport.validate("1001", envelope1001("MainA01", "SubA0101"));
        assertThat(r.valid())
                .as("臆造占位 MainA01 不在母本合法集，必须被拒（证明缺陷真实存在）")
                .isFalse();
        assertThat(r.errors())
                .as("拒绝原因须含 MainClass 字段")
                .anySatisfy(e -> assertThat(e).contains("MainClass"));
    }
}
