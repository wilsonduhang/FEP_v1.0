package com.puchain.fep.web.sysmgmt.config.alert.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NotifyMethodSetConverter} 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class NotifyMethodSetConverterTest {

    private final NotifyMethodSetConverter conv = new NotifyMethodSetConverter();

    @Test
    void toDatabaseColumn_shouldJoinSortedByEnumName() {
        assertThat(conv.convertToDatabaseColumn(Set.of(NotifyMethod.IN_APP, NotifyMethod.EMAIL)))
                .isEqualTo("EMAIL,IN_APP");
    }

    @Test
    void toEntityAttribute_shouldParseAllMethods() {
        assertThat(conv.convertToEntityAttribute("IN_APP,EMAIL"))
                .containsExactlyInAnyOrder(NotifyMethod.IN_APP, NotifyMethod.EMAIL);
    }

    @Test
    void toEntityAttribute_shouldReturnEmptySetForNullOrBlank() {
        assertThat(conv.convertToEntityAttribute(null)).isEmpty();
        assertThat(conv.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    void toDatabaseColumn_shouldReturnEmptyStringForEmptySet() {
        assertThat(conv.convertToDatabaseColumn(Set.of())).isEmpty();
    }
}
