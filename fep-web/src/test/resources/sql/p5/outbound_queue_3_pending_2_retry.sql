-- P5 T2 fixture: 3 PENDING + 2 due-RETRY + 1 future-RETRY rows for
-- OutboundQueueRepositoryIntegrationTest.claimBatch verification.
--
-- 14 required columns from V22__create_outbound_message_queue.sql.
-- next_retry_at uses H2 DATEADD relative to NOW() so the same fixture
-- works regardless of when the test executes.
--
-- 5 expected to be claimed (UUID_1..UUID_5):
--   UUID_1..UUID_3 - status=PENDING, next_retry_at=NULL
--   UUID_4         - status=RETRY,   next_retry_at=NOW()-1 minute (due)
--   UUID_5         - status=RETRY,   next_retry_at=NOW()-1 second (due)
-- 1 expected to be skipped:
--   UUID_99        - status=RETRY,   next_retry_at=NOW()+1 hour   (future)
--
-- T10 isolation fix: pre-DELETE LIKE 'aaaa1111bbbb2222cccc3333dddd0%' to
-- clear any rows committed by sibling P5OutboundEndToEndIntegrationTest
-- (no @Transactional class-level rollback). Both fixtures share the same
-- queue_id family in shared in-JVM H2 in-memory DB.
DELETE FROM outbound_message_queue
WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd0%';

INSERT INTO outbound_message_queue (
    queue_id, message_type, transition_no, idempotency_key,
    message_head_xml, message_body_xml, payload_data_type, source_ref,
    status, retry_count, next_retry_at, error_message,
    created_at, updated_at
) VALUES
('aaaa1111bbbb2222cccc3333dddd0001', '3009', '00000001', 'k1aaaa1111bbbb2222cccc3333dddd0001',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000001</transitionNo></OutboundHeadFields>',
 '<RzReturnInfo3009/>', 'INVOICE_RETURN_3009', 'src-1',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0002', '3101', '00000002', 'k2aaaa1111bbbb2222cccc3333dddd0002',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000002</transitionNo></OutboundHeadFields>',
 '<ContractInfo3101/>', 'INVOICE_CONTRACT_3101', 'src-2',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0003', '3102', '00000003', 'k3aaaa1111bbbb2222cccc3333dddd0003',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000003</transitionNo></OutboundHeadFields>',
 '<ArchiveInfo3102/>', 'ARCHIVE_3102', 'src-3',
 'PENDING', 0, NULL, NULL, NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0004', '3105', '00000004', 'k4aaaa1111bbbb2222cccc3333dddd0004',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000004</transitionNo></OutboundHeadFields>',
 '<RzApplyInfo3105/>', 'APPLY_3105', 'src-4',
 'RETRY', 1, DATEADD('MINUTE', -1, NOW()), 'transient', NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0005', '3107', '00000005', 'k5aaaa1111bbbb2222cccc3333dddd0005',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000005</transitionNo></OutboundHeadFields>',
 '<PzCheckQuery3107/>', 'PZ_CHECK_3107', 'src-5',
 'RETRY', 2, DATEADD('SECOND', -1, NOW()), 'transient', NOW(), NOW()),
('aaaa1111bbbb2222cccc3333dddd0099', '3116', '00000099', 'k99aaaa1111bbbb2222cccc3333dddd0099',
 '<OutboundHeadFields><sendOrgCode>BANK001</sendOrgCode><entrustDate>20260505</entrustDate><transitionNo>00000099</transitionNo></OutboundHeadFields>',
 '<BankCheckDay3116/>', 'BANK_CHECK_3116', 'src-99',
 'RETRY', 1, DATEADD('HOUR', 1, NOW()), 'transient', NOW(), NOW());
