package com.puchain.fep.web.submission.datasource.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.datasource.domain.SubDataSource;
import com.puchain.fep.web.submission.datasource.dto.DataSourceCreateRequest;
import com.puchain.fep.web.submission.datasource.dto.DataSourceResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubDataSourceService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SubDataSourceServiceTest {

    @Mock
    private SubDataSourceRepository dataSourceRepository;

    @InjectMocks
    private SubDataSourceService service;

    private DataSourceCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new DataSourceCreateRequest();
        validRequest.setSourceName("测试数据源");
        validRequest.setContactAddress("长沙市岳麓区");
        validRequest.setContactPhone("07318888888");
        validRequest.setPushEnabled(false);
    }

    @Test
    void create_shouldSucceed_whenNameIsUnique() {
        when(dataSourceRepository.existsBySourceName("测试数据源")).thenReturn(false);
        when(dataSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataSourceResponse resp = service.create(validRequest);

        assertThat(resp.getSourceName()).isEqualTo("测试数据源");
        assertThat(resp.getContactAddress()).isEqualTo("长沙市岳麓区");
        assertThat(resp.getContactPhone()).isEqualTo("07318888888");
        assertThat(resp.getSourceStatus()).isEqualTo(EnableDisableStatus.ENABLED);
        assertThat(resp.isPushEnabled()).isFalse();
        verify(dataSourceRepository).save(any());
    }

    @Test
    void create_shouldThrow_whenNameDuplicate() {
        when(dataSourceRepository.existsBySourceName("测试数据源")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(dataSourceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }

    @Test
    void delete_shouldSucceed_whenExists() {
        when(dataSourceRepository.existsById("id1")).thenReturn(true);

        service.delete("id1");

        verify(dataSourceRepository).deleteById("id1");
    }

    @Test
    void update_shouldThrow_whenNameConflict() {
        SubDataSource entity = new SubDataSource();
        entity.setSourceId("id1");
        entity.setSourceName("旧名称");
        entity.setContactAddress("旧地址");
        entity.setContactPhone("12345678901");
        entity.setSourceStatus(EnableDisableStatus.ENABLED);
        when(dataSourceRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(dataSourceRepository.existsBySourceNameAndIdNot("测试数据源", "id1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update("id1", validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }
}
