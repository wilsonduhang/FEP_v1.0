package com.puchain.fep.architecture;

import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 审计日志 append-only（架构 §1219，GM S5）：生产代码对 SysOperationLogRepository
 * 仅允许白名单方法（写入 save + 链读/查询），delete 系/saveAll 等修改面禁用。
 * 测试代码不受限（DoNotIncludeTests——测试清理需 delete）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@AnalyzeClasses(packages = "com.puchain.fep",
        importOptions = ImportOption.DoNotIncludeTests.class)
class OperationLogAppendOnlyArchTest {

    /** 生产代码允许的 Repository 方法白名单。 */
    private static final Set<String> ALLOWED = Set.of(
            "save", "search", "findById",
            "findTopBySeqIsNotNullOrderBySeqDesc",
            "findBySeq", "findBySeqGreaterThanEqualOrderBySeqAsc");

    @ArchTest
    static final ArchRule operation_log_repository_is_append_only =
            noClasses().should().callMethodWhere(new DescribedPredicate<JavaCall<?>>(
                    "SysOperationLogRepository 白名单外方法（append-only）") {
                @Override
                public boolean test(final JavaCall<?> call) {
                    return call.getTargetOwner()
                            .isAssignableTo(SysOperationLogRepository.class)
                            && !ALLOWED.contains(call.getName());
                }
            }).because("审计日志不可篡改 append-only（架构 §1219）：生产代码仅白名单方法");
}
