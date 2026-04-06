package com.puchain.fep.web.sysmgmt.log.domain;

/**
 * 操作类型枚举，用于 {@link com.puchain.fep.web.sysmgmt.log.annotation.OperationLog} 注解。
 *
 * <p>参见 PRD v1.3 §5.10.6 日志管理 / §8.3 操作审计日志全覆盖。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum OperationType {

    /** 查询操作。 */
    QUERY,

    /** 新建/创建操作。 */
    CREATE,

    /** 修改/更新操作。 */
    UPDATE,

    /** 删除操作。 */
    DELETE,

    /** 用户登录。 */
    LOGIN,

    /** 用户登出。 */
    LOGOUT,

    /** 数据导出。 */
    EXPORT,

    /** 其他操作。 */
    OTHER
}
