package com.puchain.fep.collector.support;

import java.util.Optional;

/**
 * 采集水位存储抽象。
 *
 * <p>水位（watermark）记录每个适配器最后一次成功推进到的位置，由 adapter 实现
 * 自定义编码（如 {@code Instant} ISO-8601 字符串、JDBC 主键值、MQ offset）。
 * 调度器在 {@link CollectorAdapter#acknowledge} 成功后调用 {@link #put} 推进。
 *
 * <p><b>实现约定：</b>
 * <ul>
 *   <li>{@link #get} 必须线程安全（多个 adapter 调度可能并发访问）</li>
 *   <li>{@link #put} 必须原子覆盖（不存在 read-modify-write 竞态）</li>
 *   <li>未知 adapterId 调用 {@link #get} 必须返回 {@link Optional#empty}（首次运行语义）</li>
 * </ul>
 *
 * <p>生产实现：{@code com.puchain.fep.web.collector.JpaWatermarkStore}（T8 引入；
 * T8-fix 由 JDBC MERGE 迁移到 JPA Repository 以支持 MySQL 8 dialect）。单测实现：
 * {@link InMemoryWatermarkStore}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface WatermarkStore {

    /**
     * 读取适配器水位。
     *
     * @param adapterId 适配器 ID（非 null）
     * @return 水位字符串（{@link Optional}，未知 adapterId 返回 {@link Optional#empty}）
     */
    Optional<String> get(String adapterId);

    /**
     * 写入 / 覆盖适配器水位。
     *
     * @param adapterId 适配器 ID（非 null）
     * @param watermark 新水位字符串（非 null；编码格式由 adapter 自定义）
     */
    void put(String adapterId, String watermark);
}
