-- V20__seed_dir_map_config.sql
-- 88 rows = 44 MessageType × 2 AccessRole.
-- Source: MessageDirectionMap.java static block, lines 43-273.
-- Each (message_type, access_role) row corresponds to ONE t.put(...) call.

INSERT INTO t_dir_map_config (message_type, access_role, direction, requires_fep, processing_mode, updated_by) VALUES
-- PRD §4.6 row 1: 3000 电子凭证信息登记 (MessageDirectionMap.java L43-46)
('3000', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
('3000', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- PRD §4.6 row 2: 3001 业务进展实时查询请求 (L48-51)
('3001', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('3001', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 3: 3002 业务进展查询回执 (L53-56)
('3002', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('3002', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 4: 3003 融资状态查询请求 (L58-61)
('3003', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('3003', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 5: 3004 融资状态查询回执 (L63-66)
('3004', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('3004', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 6: 3005 对公账户查询请求 (L68-71)
('3005', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('3005', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 7: 3006 对公账户查询回执 (L73-76)
('3006', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('3006', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 8: 3007 发票核验请求 (L78-81)
('3007', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('3007', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 9: 3008 发票核验回执 (L83-86)
('3008', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('3008', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 10: 3009 融资结果登记 (L88-91)
('3009', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('3009', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
-- PRD §4.6 row 11: 3101 电子合同信息流转 (L93-96)
('3101', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('3101', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- PRD §4.6 row 12: 3102 融资企业开户建档申请 (L98-101)
('3102', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  FALSE, 'MODE_2', 'system'),
('3102', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
-- PRD §4.6 row 13: 3103 融资企业开户建档回执 (L103-106)
('3103', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
('3103', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
-- PRD §4.6 row 14: 3105 电子凭证融资申请 (L108-111)
('3105', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
('3105', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
-- PRD §4.6 row 15: 3107 平台凭证对账申请 (L113-116)
('3107', 'ACCEPTING_ORG',    'NOT_APPLICABLE',   FALSE, 'MODE_2', 'system'),
('3107', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
-- PRD §4.6 row 16: 3108 平台凭证对账回执 (L118-121)
('3108', 'ACCEPTING_ORG',    'NOT_APPLICABLE',   FALSE, 'MODE_2', 'system'),
('3108', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
-- PRD §4.6 row 17: 3109 企业信息登记 (L123-126)
('3109', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('3109', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- PRD §4.6 row 18: 3112 核心企业授信查询请求 (L128-131)
('3112', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_5', 'system'),
('3112', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_5', 'system'),
-- PRD §4.6 row 19: 3113 核心企业授信查询回执 (L133-136)
('3113', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_5', 'system'),
('3113', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_5', 'system'),
-- PRD §4.6 row 20: 3115 资金清算信息指令及回执 (L138-141)
('3115', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_5', 'system'),
('3115', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_5', 'system'),
-- PRD §4.6 row 21: 3116 银行资金日对账 (L143-146)
('3116', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('3116', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
-- PRD §4.6 row 22: 3020 供应链实时业务通用转发 (L148-151)
('3020', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('3020', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- PRD §4.6 row 23: 3120 供应链非实时业务通用转发 (L153-156)
('3120', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('3120', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- ============== P2c/P2d 扩展 21 报文 × 2 = 42 条（非 PRD §4.6 直接条文，工程决策延续）==============
-- §4.2 REALTIME row 1: 1001 企业信息实时查询请求 (L165-168)
('1001', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('1001', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- §4.2 row 2: 2001 企业信息实时查询回执 (L170-173)
('2001', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('2001', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- §4.2 row 3: 1004 企业信息查询授权书发送 (L175-178)
('1004', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
('1004', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
-- §4.2 row 4: 2004 企业信息查询授权书回执 (L180-183)
('2004', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_1', 'system'),
('2004', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_1', 'system'),
-- §4.3 BATCH row 1: 1101 外联机构数据报送 (L187-190)
('1101', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('1101', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
-- §4.3 row 2: 2101 数据推送 (L192-195)
('2101', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
('2101', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- §4.3 row 3: 1102 数据报送核对请求 (L197-200)
('1102', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
('1102', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
-- §4.3 row 4: 2102 数据报送核对回执 (L202-205)
('2102', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
('2102', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
-- §4.3 row 5: 1103 企业信息批量查询请求 (L207-210)
('1103', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
('1103', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
-- §4.3 row 6: 2103 企业信息批量查询回执 (L212-215)
('2103', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_3', 'system'),
('2103', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_3', 'system'),
-- §4.3 row 7: 1104 授权书批量发送 (L217-220)
('1104', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
('1104', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
-- §4.3 row 8: 2104 授权书批量回执 (L222-225)
('2104', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  TRUE,  'MODE_2', 'system'),
('2104', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  TRUE,  'MODE_2', 'system'),
-- §4.5 COMMON: 9000 实时业务通用转发 (L230-233)
('9000', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
('9000', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
-- §4.5 9005 连通性测试 (L235-238)
('9005', 'ACCEPTING_ORG',    'NOT_APPLICABLE',   FALSE, 'MODE_3', 'system'),
('9005', 'INFO_SERVICE_ORG', 'NOT_APPLICABLE',   FALSE, 'MODE_3', 'system'),
-- §4.5 9006 节点登录请求 (L240-243)
('9006', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_1', 'system'),
('9006', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  FALSE, 'MODE_1', 'system'),
-- §4.5 9007 节点登录回执 (L245-248)
('9007', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  FALSE, 'MODE_1', 'system'),
('9007', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  FALSE, 'MODE_1', 'system'),
-- §4.5 9008 节点登出请求 (L250-253)
('9008', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_1', 'system'),
('9008', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  FALSE, 'MODE_1', 'system'),
-- §4.5 9009 节点登出回执 (L255-258)
('9009', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  FALSE, 'MODE_1', 'system'),
('9009', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  FALSE, 'MODE_1', 'system'),
-- §4.5 9020 实时业务通用应答 (L260-263)
('9020', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
('9020', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
-- §4.5 9100 非实时业务通用转发 (L265-268)
('9100', 'ACCEPTING_ORG',    'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
('9100', 'INFO_SERVICE_ORG', 'OUTBOUND_ACTIVE',  FALSE, 'MODE_3', 'system'),
-- §4.5 9120 非实时业务通用应答 (L270-273)
('9120', 'ACCEPTING_ORG',    'INBOUND_PASSIVE',  FALSE, 'MODE_3', 'system'),
('9120', 'INFO_SERVICE_ORG', 'INBOUND_PASSIVE',  FALSE, 'MODE_3', 'system');
-- ============== TOTAL: 23 §4.6 × 2 + 21 扩展 × 2 = 88 rows ==============
