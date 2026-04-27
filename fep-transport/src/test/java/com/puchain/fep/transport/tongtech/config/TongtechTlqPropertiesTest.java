package com.puchain.fep.transport.tongtech.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Unit tests for {@link TongtechTlqProperties}.
 *
 * <p>Asserts default values match {@code docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md
 * §5.5} and verifies Spring Boot Binder behavior for full and partial property binding.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechTlqPropertiesTest {

    @Test
    @DisplayName("default values align with sdk-validation §5.5")
    void defaultValues_shouldMatchSdkValidationSection5_5() {
        TongtechTlqProperties props = new TongtechTlqProperties();

        assertThat(props.getBrokerHost()).isEqualTo("127.0.0.1");
        assertThat(props.getBrokerPort()).isEqualTo(10024);
        assertThat(props.getBrokerId()).isEqualTo(1);
        assertThat(props.getUserName()).isEmpty();
        assertThat(props.getPassword()).isEmpty();
        assertThat(props.getQcuName()).isEqualTo("QCU1");
        assertThat(props.getConnTimeSec()).isEqualTo(30);
        assertThat(props.getReplyTmoutSec()).isEqualTo(30);
        assertThat(props.getSecExitFlag()).isZero();
        assertThat(props.getConsumerPollIntervalMs()).isEqualTo(100L);
        assertThat(props.getAdminHost()).isEqualTo("127.0.0.1");
        assertThat(props.getAdminPort()).isEqualTo(9999);
    }

    @Test
    @DisplayName("Binder overrides every property via fep.transport.tongtech.* keys")
    void bindingFromYaml_shouldOverrideDefaults() {
        Map<String, Object> map = new HashMap<>();
        map.put("fep.transport.tongtech.broker-host", "10.0.0.1");
        map.put("fep.transport.tongtech.broker-port", "20024");
        map.put("fep.transport.tongtech.broker-id", "5");
        map.put("fep.transport.tongtech.user-name", "fep_user");
        map.put("fep.transport.tongtech.password", "secret");
        map.put("fep.transport.tongtech.qcu-name", "QCU2");
        map.put("fep.transport.tongtech.conn-time-sec", "60");
        map.put("fep.transport.tongtech.reply-tmout-sec", "120");
        map.put("fep.transport.tongtech.sec-exit-flag", "1");
        map.put("fep.transport.tongtech.consumer-poll-interval-ms", "250");
        map.put("fep.transport.tongtech.admin-host", "10.0.0.2");
        map.put("fep.transport.tongtech.admin-port", "9998");

        TongtechTlqProperties bound = bind(map);

        assertThat(bound.getBrokerHost()).isEqualTo("10.0.0.1");
        assertThat(bound.getBrokerPort()).isEqualTo(20024);
        assertThat(bound.getBrokerId()).isEqualTo(5);
        assertThat(bound.getUserName()).isEqualTo("fep_user");
        assertThat(bound.getPassword()).isEqualTo("secret");
        assertThat(bound.getQcuName()).isEqualTo("QCU2");
        assertThat(bound.getConnTimeSec()).isEqualTo(60);
        assertThat(bound.getReplyTmoutSec()).isEqualTo(120);
        assertThat(bound.getSecExitFlag()).isEqualTo(1);
        assertThat(bound.getConsumerPollIntervalMs()).isEqualTo(250L);
        assertThat(bound.getAdminHost()).isEqualTo("10.0.0.2");
        assertThat(bound.getAdminPort()).isEqualTo(9998);
    }

    @Test
    @DisplayName("partial binding keeps default values for unset properties")
    void partialBinding_shouldKeepDefaultsForUnsetFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("fep.transport.tongtech.broker-host", "192.168.10.5");

        TongtechTlqProperties bound = bind(map);

        assertThat(bound.getBrokerHost()).isEqualTo("192.168.10.5");
        // remaining fields keep defaults
        assertThat(bound.getBrokerPort()).isEqualTo(10024);
        assertThat(bound.getBrokerId()).isEqualTo(1);
        assertThat(bound.getUserName()).isEmpty();
        assertThat(bound.getPassword()).isEmpty();
        assertThat(bound.getQcuName()).isEqualTo("QCU1");
        assertThat(bound.getConnTimeSec()).isEqualTo(30);
        assertThat(bound.getReplyTmoutSec()).isEqualTo(30);
        assertThat(bound.getSecExitFlag()).isZero();
        assertThat(bound.getConsumerPollIntervalMs()).isEqualTo(100L);
        assertThat(bound.getAdminHost()).isEqualTo("127.0.0.1");
        assertThat(bound.getAdminPort()).isEqualTo(9999);
    }

    private static TongtechTlqProperties bind(final Map<String, Object> map) {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
        Binder binder = new Binder(source);
        return binder.bind("fep.transport.tongtech", Bindable.of(TongtechTlqProperties.class))
                .orElseGet(TongtechTlqProperties::new);
    }
}
