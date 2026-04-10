/**
 * FEP 业务处理层（P2a 同步模式）。
 *
 * <p>职责：XSD 结构校验、报文级状态机、同步流水线编排。
 * 持久化通过 {@link com.puchain.fep.processor.state.MessageProcessStore} 端口解耦，
 * 默认 {@code InMemory} 实现用于单元测试；生产由 fep-web 提供 JPA 适配器。</p>
 *
 * <p>不包含：业务规则引擎、异步报文（P2b）、对账引擎（P2c）、安全加解密（P5）。</p>
 */
package com.puchain.fep.processor;
