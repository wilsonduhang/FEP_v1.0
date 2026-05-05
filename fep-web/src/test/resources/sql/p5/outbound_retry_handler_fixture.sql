-- P5 T7 fixture: 4 rows for OutboundRetryHandlerTest scenarios.
--
-- 14 required columns from V22__create_outbound_message_queue.sql.
-- Pre-state status / next_retry_at / error_message are overwritten by
-- OutboundRetryHandler.handleFailure(), so values only need to satisfy
-- NOT NULL + enum semantics.
--
-- Scenarios (Plan §Task 7 §Step 1, lines 1742-1800):
--   UUID_0010 - retry_count=2 → newRetryCount=3 → RETRY + NOW+240_000ms
--   UUID_0011 - retry_count=5 → newRetryCount=6 → DEAD_LETTER + null
--   UUID_0012 - retry_count=3 → newRetryCount=4 → RETRY + NOW+480_000ms
--   UUID_0013 - retry_count=0 → newRetryCount=1 → RETRY + truncate test
INSERT INTO outbound_message_queue (
    queue_id, message_type, transition_no, idempotency_key,
    message_head_xml, message_body_xml, payload_data_type, source_ref,
    status, retry_count, next_retry_at, error_message,
    created_at, updated_at
) VALUES
('aaaa1111bbbb2222cccc3333dddd0010', '3009', '00000010', 'k10aaaa1111bbbb2222cccc3333dddd0010',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260504</entrustDate><transitionNo>00000010</transitionNo></OutboundHeadFields>',
 '<RzReturnInfo3009/>', 'INVOICE_RETURN_3009', 'src-10',
 'RETRY', 2, DATEADD('MINUTE', -1, NOW()), 'prior', NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0011', '3101', '00000011', 'k11aaaa1111bbbb2222cccc3333dddd0011',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260504</entrustDate><transitionNo>00000011</transitionNo></OutboundHeadFields>',
 '<ContractInfo3101/>', 'INVOICE_CONTRACT_3101', 'src-11',
 'RETRY', 5, DATEADD('MINUTE', -1, NOW()), 'prior', NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0012', '3102', '00000012', 'k12aaaa1111bbbb2222cccc3333dddd0012',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260504</entrustDate><transitionNo>00000012</transitionNo></OutboundHeadFields>',
 '<ArchiveInfo3102/>', 'ARCHIVE_3102', 'src-12',
 'RETRY', 3, DATEADD('MINUTE', -1, NOW()), 'prior', NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0013', '3105', '00000013', 'k13aaaa1111bbbb2222cccc3333dddd0013',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260504</entrustDate><transitionNo>00000013</transitionNo></OutboundHeadFields>',
 '<RzApplyInfo3105/>', 'APPLY_3105', 'src-13',
 'PENDING', 0, NULL, NULL, NOW(), NOW());
