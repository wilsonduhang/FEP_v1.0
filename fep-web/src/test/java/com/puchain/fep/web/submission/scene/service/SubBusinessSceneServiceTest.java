package com.puchain.fep.web.submission.scene.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.scene.domain.ScenePushMethod;
import com.puchain.fep.web.submission.scene.dto.SceneCreateRequest;
import com.puchain.fep.web.submission.scene.dto.SceneResponse;
import com.puchain.fep.web.submission.scene.repository.SubBusinessSceneRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubBusinessSceneService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SubBusinessSceneServiceTest {

    @Mock
    private SubBusinessSceneRepository sceneRepository;

    @Mock
    private SysBusinessTypeRepository businessTypeRepository;

    @InjectMocks
    private SubBusinessSceneService service;

    private SceneCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new SceneCreateRequest();
        validRequest.setSceneName("流水贷场景");
        validRequest.setBusinessTypeId("biz-type-001");
        validRequest.setPushMethod(ScenePushMethod.AUTO);
        validRequest.setRequestUrl("https://example.com/scene/data");
        validRequest.setSortOrder(1);
    }

    @Test
    void create_shouldSucceed_whenNameIsUnique() {
        when(sceneRepository.existsBySceneName("流水贷场景")).thenReturn(false);
        when(businessTypeRepository.existsById("biz-type-001")).thenReturn(true);
        when(sceneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SceneResponse resp = service.create(validRequest);

        assertThat(resp.getSceneName()).isEqualTo("流水贷场景");
        assertThat(resp.getBusinessTypeId()).isEqualTo("biz-type-001");
        assertThat(resp.getPushMethod()).isEqualTo(ScenePushMethod.AUTO);
        assertThat(resp.getSceneStatus()).isEqualTo(EnableDisableStatus.ENABLED);
        assertThat(resp.getRequestUrl()).isEqualTo("https://example.com/scene/data");
        verify(sceneRepository).save(any());
    }

    @Test
    void create_shouldThrow_whenBusinessTypeNotExists() {
        when(sceneRepository.existsBySceneName("流水贷场景")).thenReturn(false);
        when(businessTypeRepository.existsById("biz-type-001")).thenReturn(false);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5009));
    }

    @Test
    void create_manualWithoutTemplate_shouldThrow() {
        validRequest.setPushMethod(ScenePushMethod.MANUAL);
        // importTemplatePath is null by default
        when(sceneRepository.existsBySceneName("流水贷场景")).thenReturn(false);
        when(businessTypeRepository.existsById("biz-type-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.PARAM_4001))
                .hasMessageContaining("手动上传模式必须提供导入模板文件路径");
    }

    @Test
    void create_manualWithTemplate_shouldSucceed() {
        validRequest.setPushMethod(ScenePushMethod.MANUAL);
        validRequest.setImportTemplatePath("/templates/loan-template.xlsx");
        when(sceneRepository.existsBySceneName("流水贷场景")).thenReturn(false);
        when(businessTypeRepository.existsById("biz-type-001")).thenReturn(true);
        when(sceneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SceneResponse resp = service.create(validRequest);

        assertThat(resp.getPushMethod()).isEqualTo(ScenePushMethod.MANUAL);
        assertThat(resp.getImportTemplatePath()).isEqualTo("/templates/loan-template.xlsx");
        verify(sceneRepository).save(any());
    }
}
