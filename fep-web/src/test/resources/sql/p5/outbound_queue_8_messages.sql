-- P5 T9 fixture: 8 PENDING rows covering supply-chain finance message types
-- (3009 / 3101 / 3102 / 3105 / 3107 / 3109 / 3112 / 3116) for
-- P5OutboundEndToEndIntegrationTest end-to-end verification.
--
-- 14 required columns from V22__create_outbound_message_queue.sql:
--   queue_id (UUID32 hex), message_type, transition_no (8-digit numeric per
--   PRD §3.2.3 + OutboundHeadFields constraint), idempotency_key (UNIQUE),
--   message_head_xml (<OutboundHeadFields> wrapper, OutboundHeadXmlParser
--   reads back), message_body_xml (real Body POJO @XmlRootElement names per
--   fep-processor body classes), payload_data_type, source_ref,
--   status, retry_count, next_retry_at, error_message, created_at, updated_at.
-- V25 columns sent_at / msg_id / tlq_send_result default NULL on insert.
--
-- Pre-DELETE ensures idempotent re-runs across multiple @Test methods (the
-- @Sql class-level annotation re-applies the script before each test, but
-- without per-test @Transactional rollback the prior method's updated
-- rows would survive — the DELETE clears them on the queue_id prefix
-- aaaa1111bbbb2222cccc3333dddd00 used here).
DELETE FROM outbound_message_queue
WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd00%';

INSERT INTO outbound_message_queue (
    queue_id, message_type, transition_no, idempotency_key,
    message_head_xml, message_body_xml, payload_data_type, source_ref,
    status, retry_count, next_retry_at, error_message,
    created_at, updated_at
) VALUES
('aaaa1111bbbb2222cccc3333dddd0001', '3009', '00000001', 'idem_3009_0001',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000001</transitionNo></OutboundHeadFields>',
 '<rzReturnInfo3009><SerialNo>SN3009000000000000000000000001</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><PlatApplyNo>PLAT001</PlatApplyNo><hxqyName>核心企业测试</hxqyName><rzpzNo>PZ001</rzpzNo><rzPhaseCode>01</rzPhaseCode></rzReturnInfo3009>', 'RzReturnInfo3009', 'collector://3009/0001',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0002', '3101', '00000002', 'idem_3101_0002',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000002</transitionNo></OutboundHeadFields>',
 '<ContractInfo3101><SerialNo>SN3101000000000000000000000002</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ContractNo>HT2026000001</ContractNo><ContractType>融资合同</ContractType><DigitalSeal>1</DigitalSeal><ContractFilename>contract.pdf</ContractFilename><jfqyName>甲方企业测试</jfqyName><yfqyName>乙方企业测试</yfqyName></ContractInfo3101>', 'ContractInfo3101', 'collector://3101/0002',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0003', '3102', '00000003', 'idem_3102_0003',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000003</transitionNo></OutboundHeadFields>',
 '<ArchiveInfo3102><SerialNo>SN3102000000000000000000000003</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ApplyMode>1</ApplyMode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode></ArchiveInfo3102>', 'ArchiveInfo3102', 'collector://3102/0003',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0004', '3105', '00000004', 'idem_3105_0004',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000004</transitionNo></OutboundHeadFields>',
 '<rzApplyInfo3105><SerialNo>SN3105000000000000000000000004</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ApplyMode>1</ApplyMode><PlatApplyNo>PLAT2026000004</PlatApplyNo><StdBizMode>01</StdBizMode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><rzpzNo>PZ20260526001</rzpzNo><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode><rzqyPlatNo>RZPLAT2026000004</rzqyPlatNo></rzApplyInfo3105>', 'RzApplyInfo3105', 'collector://3105/0004',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0005', '3107', '00000005', 'idem_3107_0005',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000005</transitionNo></OutboundHeadFields>',
 '<pzCheckQuery3107><SerialNo>SN3107000000000000000000000005</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><CheckDate>20260505</CheckDate><hxqyNum>1</hxqyNum><hxqyInfo><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode></hxqyInfo></pzCheckQuery3107>', 'PzCheckQuery3107', 'collector://3107/0005',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0006', '3109', '00000006', 'idem_3109_0006',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000006</transitionNo></OutboundHeadFields>',
 '<qyRegister3109><SerialNo>SN3109000000000000000000000006</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><qyFlag>1</qyFlag></qyRegister3109>', 'QyRegister3109', 'collector://3109/0006',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0007', '3112', '00000007', 'idem_3112_0007',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000007</transitionNo></OutboundHeadFields>',
 '<hxqyCreditAmt3112><SerialNo>SN3112000000000000000000000007</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><QueryDate>20260505</QueryDate><hxqyInfoNum>1</hxqyInfoNum><hxqyInfo><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode></hxqyInfo></hxqyCreditAmt3112>', 'HxqyCreditAmt3112', 'collector://3112/0007',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0008', '3116', '00000008', 'idem_3116_0008',
 '<OutboundHeadFields><sendOrgCode>BANK0010000001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000008</transitionNo></OutboundHeadFields>',
 '<BankCheckDay3116><SerialNo>SN3116000000000000000000000008</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><CheckDate>20260505</CheckDate><CheckDetailNum>1</CheckDetailNum><CheckDetailInfo><sid>1</sid><PlatNodeCode>PLAT0010000001</PlatNodeCode><BizType>01</BizType><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode><rzAmt>10000.00</rzAmt><rzRate>06.0000</rzRate><rzStartDate>20260101</rzStartDate><rzEndDate>20261231</rzEndDate><Amt>0.00</Amt></CheckDetailInfo></BankCheckDay3116>', 'BankCheckDay3116', 'collector://3116/0008',
 'PENDING', 0, NULL, NULL, NOW(), NOW());
