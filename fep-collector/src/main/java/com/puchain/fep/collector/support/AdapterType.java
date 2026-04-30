package com.puchain.fep.collector.support;

/**
 * 数据采集适配器类型枚举。
 *
 * <p>覆盖 PRD v1.3 §2.2.2 数仓模式四类源系统接入方式：
 * <ul>
 *   <li>{@link #JDBC} — 直连数据库（Oracle / MySQL / PostgreSQL 等）</li>
 *   <li>{@link #FILE} — 文件读取（CSV / Excel / 定长文本）</li>
 *   <li>{@link #MQ}   — 消息队列订阅（Kafka / RocketMQ / RabbitMQ 等）</li>
 *   <li>{@link #ESB}  — 企业服务总线对接（HTTP / SOAP / 自定义协议）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum AdapterType {

    /** 直连数据库类型（JDBC）。 */
    JDBC("数据库直连"),

    /** 文件读取类型（CSV / Excel / 定长文本）。 */
    FILE("文件读取"),

    /** 消息队列订阅类型（Kafka / RocketMQ / RabbitMQ 等）。 */
    MQ("消息队列订阅"),

    /** 企业服务总线对接类型（HTTP / SOAP / 自定义协议）。 */
    ESB("企业服务总线对接");

    /** 中文标签（用于日志 / 管理 UI 显示）。 */
    private final String label;

    AdapterType(final String label) {
        this.label = label;
    }

    /**
     * 返回中文标签。
     *
     * @return 中文标签（非 null）
     */
    public String label() {
        return label;
    }
}
