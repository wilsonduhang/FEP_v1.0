package com.puchain.fep.common.domain;

/**
 * FEP 统一错误码枚举。
 *
 * <p>错误码格式: {前缀}_{四位数字}（前缀 PARAM/AUTH/BIZ/SYS/DEP）</p>
 *
 * <p>参见 PRD v1.3 §9.3 错误码体系。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum FepErrorCode {

    // 成功
    SUCCESS("200", "成功"),

    // 参数错误 4000-4999
    PARAM_4001("PARAM_4001", "请求参数不能为空"),
    PARAM_4002("PARAM_4002", "请求参数格式错误"),
    PARAM_4003("PARAM_4003", "请求参数超出范围"),

    // 认证错误 0401
    AUTH_0401("AUTH_0401", "令牌无效或过期"),
    AUTH_0402("AUTH_0402", "账号或密码错误"),
    AUTH_0403("AUTH_0403", "无权访问该资源"),
    AUTH_0404("AUTH_0404", "验证码错误或已失效"),
    AUTH_0405("AUTH_0405", "账号已被锁定"),
    AUTH_0406("AUTH_0406", "账号已被禁用"),

    // 业务错误 5000-5999
    BIZ_5001("BIZ_5001", "资源不存在"),
    BIZ_5002("BIZ_5002", "资源已存在"),
    BIZ_5003("BIZ_5003", "业务状态不允许此操作"),
    BIZ_5004("BIZ_5004", "数据关联校验失败"),
    BIZ_5005("BIZ_5005", "消息接收者类型与 ID 不匹配"),
    BIZ_5006("BIZ_5006", "文件已过期或不存在"),
    BIZ_5007("BIZ_5007", "配置项不存在"),
    BIZ_5008("BIZ_5008", "统一社会信用代码已存在"),
    BIZ_5009("BIZ_5009", "关联业务类型不存在"),
    BIZ_5010("BIZ_5010", "文件格式不支持"),
    BIZ_5011("BIZ_5011", "报文类型编码已存在"),
    BIZ_5012("BIZ_5012", "报文类型不存在"),
    BIZ_5013("BIZ_5013", "TLQ 节点名称已存在"),
    BIZ_5014("BIZ_5014", "TLQ 队列名称已存在"),
    BIZ_5015("BIZ_5015", "TLQ 节点不存在"),

    // 通信层错误 7000-7999
    TRANS_7001("TRANS_7001", "报文大小超过 TLQ 最大限制 24KB"),
    TRANS_7002("TRANS_7002", "TLQ 连接失败"),
    TRANS_7003("TRANS_7003", "TLQ 消息发送失败"),
    TRANS_7004("TRANS_7004", "节点状态不允许此操作"),

    // 系统错误 0500
    SYS_0500("SYS_0500", "系统内部错误"),

    // 依赖服务错误 6000-6999
    DEP_6001("DEP_6001", "依赖服务暂时不可用"),

    // 报文转换层错误 8000-8999
    CONV_8001("CONV_8001", "报文 XML 序列化失败"),
    CONV_8002("CONV_8002", "报文 XML 反序列化失败"),
    CONV_8003("CONV_8003", "报文类型未注册"),
    CONV_8004("CONV_8004", "报文签名范围提取失败"),
    CONV_8005("CONV_8005", "报文压缩/解压失败"),
    CONV_8006("CONV_8006", "报文加密/解密失败"),
    CONV_8007("CONV_8007", "报文 payload 超过 TLQ 24KB 上限"),

    // 业务处理层错误 8500-8599（P2 业务处理层）
    PROC_8501("PROC_8501", "报文 XSD 结构校验失败"),
    PROC_8502("PROC_8502", "报文处理状态非法转移"),
    PROC_8503("PROC_8503", "XSD 资源加载失败"),
    PROC_8504("PROC_8504", "不支持的报文类型"),
    PROC_8505("PROC_8505", "同步处理流水线执行失败"),
    PROC_8506("PROC_8506", "处理记录存储不可用"),

    // 对账引擎错误 8600-8699（P2e 对账引擎）
    RECON_DAILY_LIMIT_EXCEEDED("RECON_8607", "当日对账记录数超过上限 999"),
    RECON_DIR_MAP_MISS("RECON_8608", "MessageDirectionMap 未注册该报文与角色组合");

    private final String code;
    private final String defaultMessage;

    FepErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
