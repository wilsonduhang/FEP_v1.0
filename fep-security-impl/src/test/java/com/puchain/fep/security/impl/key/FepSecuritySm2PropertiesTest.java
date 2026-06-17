package com.puchain.fep.security.impl.key;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** FepSecuritySm2Properties 三段 setter 防御拷贝 + null-guard 归一基线（REUSE-2）。 */
class FepSecuritySm2PropertiesTest {

    @Test
    void setLoginKeys_null_becomesEmptyMutableMap() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        p.setLoginKeys(null);
        assertThat(p.getLoginKeys()).isEmpty();
        // 仍可 relaxed-binding mutate（live 引用）
        p.getLoginKeys().put("k", new FepSecuritySm2Properties.LoginKeyPair());
        assertThat(p.getLoginKeys()).containsKey("k");
    }

    @Test
    void setAuditKeys_null_becomesEmptyMutableMap() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        p.setAuditKeys(null);
        assertThat(p.getAuditKeys()).isEmpty();
        p.getAuditKeys().put("k", new FepSecuritySm2Properties.LoginKeyPair());
        assertThat(p.getAuditKeys()).containsKey("k");
    }

    @Test
    void setMsgSignKeys_isDefensiveCopy() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        final Map<String, FepSecuritySm2Properties.LoginKeyPair> src = new LinkedHashMap<>();
        src.put("k", new FepSecuritySm2Properties.LoginKeyPair());
        p.setMsgSignKeys(src);
        src.clear();                       // 源清空不应影响已 set 的副本
        assertThat(p.getMsgSignKeys()).containsKey("k");
    }
}
