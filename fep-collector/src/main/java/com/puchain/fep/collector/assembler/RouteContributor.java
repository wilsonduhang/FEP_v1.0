package com.puchain.fep.collector.assembler;

import java.util.Map;

/**
 * 路由贡献者 SPI（Plan §T7b §3）。
 *
 * <p>{@code Mode2Routes} / {@code Mode3Routes} 等 {@code @Configuration} 类实现本接口，
 * 返回 {@code Map<payloadDataType, AssemblerRoute>}；{@link RouteRegistry} 在启动期
 * 通过构造函数注入收集所有 contributor 并合并为单一 immutable map。
 *
 * <p><b>分组目的：</b>按 PRD 模式分类（模式 2 接口模式 4 报文 / 模式 3 数仓模式 4 报文），
 * 让 routes 注册更易审阅与扩展。当前 8 条路由分两组各 4 条；P5+ 新增报文按所属模式追加。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface RouteContributor {

    /**
     * 返回本 contributor 贡献的路由集（payloadDataType → AssemblerRoute）。
     *
     * @return 路由 Map（非 null；可空但通常不应为空）
     */
    Map<String, AssemblerRoute> contribute();
}
