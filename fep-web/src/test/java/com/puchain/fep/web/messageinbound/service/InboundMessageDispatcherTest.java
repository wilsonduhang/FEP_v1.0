package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransfer2101;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.body.common.MsgReturn9020;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InboundMessageDispatcher}.
 *
 * <p>Covers 5 paths (P3 Task 2 v1a verification §7):</p>
 * <ol>
 *   <li>pipeline COMPLETED → publishEvent invoked exactly once</li>
 *   <li>pipeline FAILED → publishEvent never invoked</li>
 *   <li>unknown messageType → throw {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_INVALID_TYPE})</li>
 *   <li>unmarshal failure → throw {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_DECODE_FAILURE})
 *       so the surrounding {@code @Transactional} rolls back</li>
 *   <li>event field values are populated from the unmarshalled body
 *       (asserted via {@link ArgumentCaptor})</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InboundMessageDispatcherTest {

    /**
     * Minimal CFX wrapper carrying a {@code BankCheckDay3116} body with only the
     * leading {@code SerialNo} populated. JAXB unmarshal accepts missing
     * non-mandatory elements as long as no unknown elements are present
     * (jaxb-runtime strict {@code ValidationEventHandler} rejects only
     * <em>unexpected</em> elements, not missing ones).
     */
    private static final String VALID_3116_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3116</MsgNo>"
                    + "<MsgId>20260428000000000001</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260428</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BankCheckDay3116>"
                    + "<SerialNo>SN20260428BANK</SerialNo>"
                    + "</BankCheckDay3116>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying an {@code InvoCheckQuery3007} body with only
     * the leading {@code SerialNo} populated. P4 T1 mirrors the 3116 template
     * shape: dispatcher unit test mocks {@link SyncMessageProcessorService}
     * away, so XSD-validated full envelopes (with {@code RealHead3007}
     * sibling) are out of scope here. Listener-side IT covers full envelope.
     */
    private static final String VALID_3007_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3007</MsgNo>"
                    + "<MsgId>20260507000000003007</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260507</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<InvoCheckQuery3007>"
                    + "<SerialNo>SN20260507INVO3007</SerialNo>"
                    + "</InvoCheckQuery3007>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying an {@code InvoCheckReturn3008} body with
     * only the leading {@code SerialNo} populated.
     */
    private static final String VALID_3008_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3008</MsgNo>"
                    + "<MsgId>20260507000000003008</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260507</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<InvoCheckReturn3008>"
                    + "<SerialNo>SN20260507INVO3008</SerialNo>"
                    + "</InvoCheckReturn3008>"
                    + "</MSG>"
                    + "</CFX>";

    private static final String VALID_2101_XML_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2101</MsgNo>
                <MsgId>20260509120000002101</MsgId>
                <CorrMsgId></CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2101>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead2101>
                <DataTransfer2101>
                  <MainClass>LSDX</MainClass>
                  <SecondClass>LSDX01</SecondClass>
                  <Period>01</Period>
                  <Type>01</Type>
                  <FileDate>20260509</FileDate>
                </DataTransfer2101>
              </MSG>
            </CFX>
            """;

    private static final String VALID_2102_XML_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2102</MsgNo>
                <MsgId>20260509120000000001</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2102>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2102>
                <DataTransferCheckResponse2102>
                  <DataTransferResult>
                    <ItemId>1</ItemId>
                    <MainClass>MainA01</MainClass>
                    <SecondClass>SubA0101</SecondClass>
                    <Period>01</Period>
                    <FileDate>20260509</FileDate>
                    <Status>01</Status>
                  </DataTransferResult>
                </DataTransferCheckResponse2102>
              </MSG>
            </CFX>
            """;

    private static final String VALID_2103_XML_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2103</MsgNo>
                <MsgId>20260509120000000003</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2103>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2103>
                <CompanyInfoBatchResponse2103>
                  <CompanyInfo>
                    <ItemId>1</ItemId>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <MainClass>MainA01</MainClass>
                    <SecondClass>SubA0101</SecondClass>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <QueryResult>90000</QueryResult>
                  </CompanyInfo>
                </CompanyInfoBatchResponse2103>
              </MSG>
            </CFX>
            """;

    private static final String VALID_2104_XML_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version><SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode><App>HNDEMP</App>
                <MsgNo>2104</MsgNo><MsgId>20260509120000000001</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId><WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2104>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2104>
                <CompanyAuthFileBatchResponse2104>
                  <CompanyAuthFileResponse>
                    <ItemId>1</ItemId>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <AuthBeginDate>20260101</AuthBeginDate>
                    <AuthEndDate>20261231</AuthEndDate>
                    <AuthNo>AUTH2026050500001</AuthNo>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <IsUpdate>0</IsUpdate>
                    <RecordResult>90000</RecordResult>
                  </CompanyAuthFileResponse>
                </CompanyAuthFileBatchResponse2104>
              </MSG>
            </CFX>
            """;

    /**
     * Minimal CFX wrapper carrying a {@code ProgressQuery3001} body with only
     * the leading {@code SerialNo} populated. P4-Plan-C T1 mirrors the 3007
     * template shape: dispatcher unit test mocks {@link SyncMessageProcessorService}
     * away, so XSD-validated full envelopes (with {@code RealHead3001} sibling)
     * are out of scope here. Listener-side IT covers full envelope.
     *
     * <p>Root element {@code <ProgressQuery3001>} per XSD grep (PascalCase,
     * not the camelCase exception that 3003-3006 use).</p>
     */
    private static final String VALID_3001_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3001</MsgNo>"
                    + "<MsgId>20260509120000000001</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<ProgressQuery3001>"
                    + "<SerialNo>SN2026050900000000000000003001</SerialNo>"
                    + "</ProgressQuery3001>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code ProgressQueryReturn3002} body.
     * Root element {@code <ProgressQueryReturn3002>} per XSD grep (PascalCase).
     */
    private static final String VALID_3002_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3002</MsgNo>"
                    + "<MsgId>20260509120000000002</MsgId>"
                    + "<CorrMsgId>20260509120000000001</CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<ProgressQueryReturn3002>"
                    + "<SerialNo>SN2026050900000000000000003002</SerialNo>"
                    + "</ProgressQueryReturn3002>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code PzInfoQuery3003} body.
     * Root element {@code <pzInfoQuery3003>} per XSD grep (camelCase exception,
     * leading lowercase). Plan v3 混合命名警告：不可凭"全 PascalCase"猜测。
     */
    private static final String VALID_3003_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3003</MsgNo>"
                    + "<MsgId>20260509120000000003</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<pzInfoQuery3003>"
                    + "<SerialNo>SN2026050900000000000000003003</SerialNo>"
                    + "</pzInfoQuery3003>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code PzInfoReturn3004} body.
     * Root element {@code <pzInfoReturn3004>} per XSD grep (camelCase exception).
     */
    private static final String VALID_3004_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3004</MsgNo>"
                    + "<MsgId>20260509120000000004</MsgId>"
                    + "<CorrMsgId>20260509120000000003</CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<pzInfoReturn3004>"
                    + "<SerialNo>SN2026050900000000000000003004</SerialNo>"
                    + "</pzInfoReturn3004>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code QyAccQuery3005} body.
     * Root element {@code <qyAccQuery3005>} per XSD grep (camelCase exception).
     */
    private static final String VALID_3005_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3005</MsgNo>"
                    + "<MsgId>20260509120000000005</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<qyAccQuery3005>"
                    + "<SerialNo>SN2026050900000000000000003005</SerialNo>"
                    + "</qyAccQuery3005>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code QyAccQueryReturn3006} body.
     * Root element {@code <qyAccQueryReturn3006>} per XSD grep (camelCase exception).
     */
    private static final String VALID_3006_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3006</MsgNo>"
                    + "<MsgId>20260509120000000006</MsgId>"
                    + "<CorrMsgId>20260509120000000005</CorrMsgId>"
                    + "<WorkDate>20260509</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<qyAccQueryReturn3006>"
                    + "<SerialNo>SN2026050900000000000000003006</SerialNo>"
                    + "</qyAccQueryReturn3006>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code HxqyCreditAmt3112} body with the
     * leading {@code SerialNo} plus a single {@code hxqyInfo} entry. Bank-side
     * inbound receive (PRD §4.6:841 mode 5). Root element {@code <hxqyCreditAmt3112>}
     * per XSD grep (camelCase exception, leading lowercase). Dispatcher unit
     * test mocks {@link SyncMessageProcessorService} away so XSD validation is
     * out of scope; only JAXB unmarshal runs against this body.
     */
    private static final String VALID_3112_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX>"
            + "<HEAD>"
            + "<Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode>"
            + "<App>HNDEMP</App>"
            + "<MsgNo>3112</MsgNo>"
            + "<MsgId>20260524000000000099</MsgId>"
            + "<CorrMsgId></CorrMsgId>"
            + "<WorkDate>20260524</WorkDate>"
            + "</HEAD>"
            + "<MSG>"
            + "<hxqyCreditAmt3112>"
            + "<SerialNo>SN20260524C3112</SerialNo>"
            + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
            + "<DesNodeCode>12345678901234</DesNodeCode>"
            + "<QueryDate>20260524</QueryDate>"
            + "<hxqyInfoNum>1</hxqyInfoNum>"
            + "<hxqyInfo>"
            + "<hxqyName>核心企业测试</hxqyName>"
            + "<hxqyCode>91110000100000000X</hxqyCode>"
            + "</hxqyInfo>"
            + "</hxqyCreditAmt3112>"
            + "</MSG>"
            + "</CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code RzApplyInfo3105} body. Bank-side
     * inbound receive (PRD §4.6:837 mode 2). Root element {@code <rzApplyInfo3105>}
     * per XSD grep (camelCase exception, leading lowercase). Dispatcher unit test
     * mocks {@link SyncMessageProcessorService} away so XSD validation is out of
     * scope; only JAXB unmarshal runs against this body. Nested list elements
     * omitted — unmarshal leaves them null.
     */
    private static final String VALID_3105_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX><HEAD><Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode><App>HNDEMP</App>"
            + "<MsgNo>3105</MsgNo><MsgId>20260525000000000095</MsgId>"
            + "<CorrMsgId></CorrMsgId><WorkDate>20260525</WorkDate></HEAD>"
            + "<MSG><rzApplyInfo3105>"
            + "<SerialNo>SN20260525A3105</SerialNo>"
            + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
            + "<DesNodeCode>12345678901234</DesNodeCode>"
            + "<ApplyMode>1</ApplyMode><PlatApplyNo>PA20260525001</PlatApplyNo>"
            + "<StdBizMode>01</StdBizMode>"
            + "<hxqyName>核心企业</hxqyName><hxqyCode>91110000100000000X</hxqyCode>"
            + "<rzpzNo>PZ20260525001</rzpzNo>"
            + "</rzApplyInfo3105></MSG></CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code RzReturnInfo3009} body. Supply-chain
     * inbound receive (PRD §4.6:833 mode 3). Root element {@code <rzReturnInfo3009>}
     * per XSD grep (camelCase exception, leading lowercase). Dispatcher unit test
     * mocks {@link SyncMessageProcessorService} away so XSD validation is out of
     * scope; only JAXB unmarshal runs against this body.
     */
    private static final String VALID_3009_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX><HEAD><Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode><App>HNDEMP</App>"
            + "<MsgNo>3009</MsgNo><MsgId>20260525000000000093</MsgId>"
            + "<CorrMsgId></CorrMsgId><WorkDate>20260525</WorkDate></HEAD>"
            + "<MSG><rzReturnInfo3009>"
            + "<SerialNo>SN20260525A3009</SerialNo>"
            + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
            + "<DesNodeCode>12345678901234</DesNodeCode>"
            + "<PlatApplyNo>PA20260525002</PlatApplyNo>"
            + "<hxqyName>核心企业</hxqyName>"
            + "<rzpzNo>PZ20260525002</rzpzNo>"
            + "<rzPhaseCode>01</rzPhaseCode>"
            + "</rzReturnInfo3009></MSG></CFX>";

    /**
     * Minimal CFX wrapper carrying an {@code ArchiveReturnInfo3103} body.
     * Supply-chain inbound receive (PRD §4.6:836 mode 2). Root element
     * {@code <ArchiveReturnInfo3103>} per XSD grep (PascalCase, leading uppercase
     * A — NOT the camelCase exception used by 3105/3009/3113). Dispatcher unit
     * test mocks {@link SyncMessageProcessorService} away so XSD validation is out
     * of scope; only JAXB unmarshal runs against this body.
     */
    private static final String VALID_3103_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX><HEAD><Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode><App>HNDEMP</App>"
            + "<MsgNo>3103</MsgNo><MsgId>20260525000000000099</MsgId>"
            + "<CorrMsgId></CorrMsgId><WorkDate>20260525</WorkDate></HEAD>"
            + "<MSG><ArchiveReturnInfo3103>"
            + "<SerialNo>SN20260525A3103</SerialNo>"
            + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
            + "<DesNodeCode>12345678901234</DesNodeCode>"
            + "<CreationRetCode>0</CreationRetCode>"
            + "<hxqyName>核心企业</hxqyName><hxqyCode>91110000100000000X</hxqyCode>"
            + "<rzqyName>融资企业</rzqyName><rzqyCode>91110000200000000Y</rzqyCode>"
            + "</ArchiveReturnInfo3103></MSG></CFX>";

    /**
     * Minimal CFX wrapper carrying a {@code HxqyCreditAmt3113} body. Supply-chain
     * inbound receive (PRD §4.6:842 mode 5 — the 9120-deferred reply to FEP's own
     * 3112 query). Root element {@code <hxqyCreditAmt3113>} per XSD grep (camelCase
     * exception, leading lowercase). Nested {@code CreditInfo} list omitted —
     * dispatcher unit test mocks {@link SyncMessageProcessorService} away so XSD
     * required-element validation is out of scope; JAXB unmarshal leaves the
     * missing list null.
     */
    private static final String VALID_3113_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX><HEAD><Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode><App>HNDEMP</App>"
            + "<MsgNo>3113</MsgNo><MsgId>20260525000000000091</MsgId>"
            + "<CorrMsgId></CorrMsgId><WorkDate>20260525</WorkDate></HEAD>"
            + "<MSG><hxqyCreditAmt3113>"
            + "<SerialNo>SN20260525A3113</SerialNo>"
            + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
            + "<DesNodeCode>12345678901234</DesNodeCode>"
            + "<QueryDate>20260525</QueryDate>"
            + "<CreditInfoNum>0</CreditInfoNum>"
            + "</hxqyCreditAmt3113></MSG></CFX>";

    private SyncMessageProcessorService syncProcessor;
    private ApplicationEventPublisher eventPublisher;
    private InboundMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        syncProcessor = mock(SyncMessageProcessorService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        dispatcher = new InboundMessageDispatcher(syncProcessor, eventPublisher);
    }

    @Test
    @DisplayName("pipeline COMPLETED → publishEvent invoked once with correct fields")
    void dispatch_completed_publishesEventOnce() {
        final byte[] xml = VALID_3116_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-001abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xml)))
                .thenReturn(completed);

        final InboundMessageResponse response = dispatcher.dispatch("3116", "20260428", xml);

        assertThat(response.recordId()).isEqualTo("rec-001abcdef0123456789abcdef01230000");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.eventPublished()).isTrue();

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3116);
        assertThat(event.transitionNo()).isEqualTo("20260428");
        assertThat(event.serialNo()).isEqualTo("SN20260428BANK");
        assertThat(event.body()).isInstanceOf(BankCheckDay3116.class);
        assertThat(event.occurredAt()).isAfter(Instant.now().minusSeconds(5));
    }

    @Test
    @DisplayName("pipeline FAILED → publishEvent is never invoked")
    void dispatch_failed_doesNotPublishEvent() {
        final byte[] xml = VALID_3116_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord failed = MessageProcessRecord.initial(
                        "rec-002abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withFailure(FepErrorCode.PROC_8501.getCode(), "xsd error", Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xml)))
                .thenReturn(failed);

        final InboundMessageResponse response = dispatcher.dispatch("3116", "20260428", xml);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.eventPublished()).isFalse();
        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("unknown messageType → throw FepBusinessException(MSG_8701) and never call processor")
    void dispatch_unknownMessageType_throwsBusinessException() {
        final byte[] xml = "<CFX/>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> dispatcher.dispatch("9999", "20260428", xml))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.MSG_INBOUND_INVALID_TYPE);
                });

        verify(syncProcessor, never())
                .processInbound(any(MessageType.class), any(String.class), any(byte[].class));
        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("unmarshal failure → throw FepBusinessException(MSG_8702) so @Transactional rolls back")
    void dispatch_unmarshalFailure_throwsDecodeFailure() {
        final byte[] malformedXml = "<not-cfx>broken</not-cfx>".getBytes(StandardCharsets.UTF_8);
        // Pipeline ran fine (XSD validator on caller side did not catch — e.g. mocked away
        // in this scope), but body unmarshal must still surface as a hard rollback signal.
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-003abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(malformedXml)))
                .thenReturn(completed);

        assertThatThrownBy(() -> dispatcher.dispatch("3116", "20260428", malformedXml))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.MSG_INBOUND_DECODE_FAILURE);
                });

        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("body type registry exposes 24 entries (P3 Phase 2 + P4-MSG-B-inbound 3007/3008 + P4-MSG-A-inbound 2102/2103/2104 + P4-MSG-D 2101 + P4-Plan-C 3001-3006 + P4-MSG-J 3112 + P4-MSG-K 3009/3103/3105/3113 + P4-MSG-L 9007/9009 + P4-MSG-M 9020)")
    void bodyTypeRegistry_contains24Entries() {
        // grep-asserted (feedback_doc_data_grep_first): registry must expose
        // exactly the 4 P3 Phase 2 messageTypes (3107/3108/3115/3116), the
        // 2 P4-MSG-B-inbound InvoCheck messageTypes (3007/3008), the 3
        // P4-MSG-A-inbound BATCH Response messageTypes (2102/2103/2104), the
        // 1 P4-MSG-D T4 messageType (2101), the 6 P4-Plan-C SUPPLY_CHAIN
        // BIDIRECTIONAL messageTypes (3001-3006), the 1 P4-MSG-J bank-side
        // inbound receive messageType (3112), the 4 P4-MSG-K inbound
        // acceptance messageTypes (3009/3103/3105/3113), and the 2 P4-MSG-L
        // node lifecycle ack messageTypes (9007/9009), and the 1 P4-MSG-M
        // realtime general response messageType (9020).
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(24);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry())
                .containsKeys("2101", "2102", "2103", "2104",
                              "3001", "3002", "3003", "3004", "3005", "3006",
                              "3007", "3008", "3107", "3108", "3115", "3116", "3112",
                              "3009", "3103", "3105", "3113", "9007", "9009", "9020");
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("2101"))
                .isEqualTo(DataTransfer2101.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("2102"))
                .isEqualTo(DataTransferCheckBatchResponse2102.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("2103"))
                .isEqualTo(CompanyInfoBatchResponse2103.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("2104"))
                .isEqualTo(CompanyAuthFileBatchResponse2104.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3001"))
                .isEqualTo(ProgressQuery3001.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3002"))
                .isEqualTo(ProgressQueryReturn3002.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3003"))
                .isEqualTo(PzInfoQuery3003.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3004"))
                .isEqualTo(PzInfoReturn3004.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3005"))
                .isEqualTo(QyAccQuery3005.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3006"))
                .isEqualTo(QyAccQueryReturn3006.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3116"))
                .isEqualTo(BankCheckDay3116.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3007"))
                .isEqualTo(InvoCheckQuery3007.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3008"))
                .isEqualTo(InvoCheckReturn3008.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3112"))
                .isEqualTo(HxqyCreditAmt3112.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3009"))
                .isEqualTo(RzReturnInfo3009.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3103"))
                .isEqualTo(ArchiveReturnInfo3103.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3105"))
                .isEqualTo(RzApplyInfo3105.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3113"))
                .isEqualTo(HxqyCreditAmt3113.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("9007"))
                .isEqualTo(LoginResponse9007.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("9009"))
                .isEqualTo(LogoutResponse9009.class);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("9020"))
                .isEqualTo(MsgReturn9020.class);
    }

    /**
     * P3 Task 5 regression guard for {@code feedback_dispatcher_payload_shape_blind_spot}.
     *
     * <p>Production CFX {@code <MSG>} envelope is XSD-mandated to carry
     * BatchHeadXxxx (DOM Element via lax-mode JAXB) <em>before</em> the body
     * POJO. The pre-fix {@code getBody()} call returned position-0 BatchHead
     * Element, masking the real body POJO and silently producing
     * {@code event.body=null}. This test ensures the {@code getBodies()::isInstance}
     * filter still picks the registered body class even when BatchHead is in
     * front, regardless of any future {@code getBody()} regression.</p>
     */
    @Test
    @DisplayName("CFX MSG with BatchHead+body siblings → dispatcher picks body POJO via isInstance filter")
    void dispatch_msgWithBatchHeadBeforeBody_picksBodyPojoNotBatchHead() {
        // BatchHead3116 sibling appears BEFORE BankCheckDay3116 — XSD-required
        // sequence. JAXB lax mode keeps unknown BatchHead3116 element as DOM
        // Element while BankCheckDay3116 deserializes to the registered POJO.
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                + "<DesNode>B2000456000204</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>3116</MsgNo>"
                + "<MsgId>20260428000000000099</MsgId>"
                + "<CorrMsgId></CorrMsgId>"
                + "<WorkDate>20260428</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + "<BatchHead3116>"
                + "<TotalNum>1</TotalNum>"
                + "<TotalAmt>100.00</TotalAmt>"
                + "</BatchHead3116>"
                + "<BankCheckDay3116>"
                + "<SerialNo>SN20260428BATCH</SerialNo>"
                + "</BankCheckDay3116>"
                + "</MSG>"
                + "</CFX>";
        final byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-099abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xmlBytes)))
                .thenReturn(completed);

        dispatcher.dispatch("3116", "20260428", xmlBytes);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        // body MUST be the registered POJO, not the BatchHead DOM Element
        assertThat(event.body())
                .as("dispatcher must pick BankCheckDay3116 instance, not the leading BatchHead3116 sibling")
                .isInstanceOf(BankCheckDay3116.class);
        assertThat(event.serialNo()).isEqualTo("SN20260428BATCH");
    }

    @Test
    @DisplayName("dispatch 3007 → publishEvent body is InvoCheckQuery3007 (FR-MSG-3007)")
    void dispatch_3007_shouldPublishEventWithInvoCheckQuery3007Body() {
        final byte[] xml = VALID_3007_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-007abcdef0123456789abcdef01230000",
                        MessageType.MSG_3007, "20260507", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3007), eq("20260507"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3007", "20260507", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3007);
        assertThat(event.transitionNo()).isEqualTo("20260507");
        assertThat(event.serialNo()).isEqualTo("SN20260507INVO3007");
        assertThat(event.body())
                .as("dispatcher must publish typed InvoCheckQuery3007 body (P4 T1 wire-in)")
                .isInstanceOf(InvoCheckQuery3007.class);
    }

    @Test
    @DisplayName("dispatch 3008 → publishEvent body is InvoCheckReturn3008 (FR-MSG-3008)")
    void dispatch_3008_shouldPublishEventWithInvoCheckReturn3008Body() {
        final byte[] xml = VALID_3008_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-008abcdef0123456789abcdef01230000",
                        MessageType.MSG_3008, "20260507", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3008), eq("20260507"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3008", "20260507", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3008);
        assertThat(event.transitionNo()).isEqualTo("20260507");
        assertThat(event.serialNo()).isEqualTo("SN20260507INVO3008");
        assertThat(event.body())
                .as("dispatcher must publish typed InvoCheckReturn3008 body (P4 T1 wire-in)")
                .isInstanceOf(InvoCheckReturn3008.class);
    }

    @Test
    @DisplayName("dispatch 2101 → publishEvent body is DataTransfer2101 (FR-MSG-2101 P4-MSG-D T4)")
    void dispatch_2101_shouldPublishEventWithDataTransfer2101Body() {
        final byte[] xml = VALID_2101_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-2101abcdef0123456789abcdef01230000",
                        MessageType.MSG_2101, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_2101), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("2101", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_2101);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        // DataTransfer2101 不实现 SerialNoBearing（5 fields: mainClass/secondClass/period/type/fileDate），
        // dispatcher.extractSerialNo 通过 instanceof 类型守卫判定后走 fallback 返回 transitionNo（E-3 重构后无反射）。
        assertThat(event.serialNo())
                .as("DataTransfer2101 lacks getSerialNo, dispatcher falls back to transitionNo")
                .isEqualTo("20260509");
        assertThat(event.body())
                .as("dispatcher must publish typed DataTransfer2101 body (P4-MSG-D T4)")
                .isInstanceOf(DataTransfer2101.class);
        final DataTransfer2101 body = (DataTransfer2101) event.body();
        assertThat(body.getMainClass()).isEqualTo("LSDX");
        assertThat(body.getSecondClass()).isEqualTo("LSDX01");
        assertThat(body.getPeriod()).isEqualTo("01");
        assertThat(body.getType()).isEqualTo("01");
        assertThat(body.getFileDate()).isEqualTo("20260509");
    }

    @Test
    @DisplayName("dispatch 2102 → publishEvent body is DataTransferCheckBatchResponse2102 (FR-MSG-2102)")
    void dispatch_2102_shouldPublishEventWithDataTransferCheckBatchResponse2102Body() {
        final byte[] xml = VALID_2102_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-2102abcdef0123456789abcdef01230000",
                        MessageType.MSG_2102, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_2102), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("2102", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_2102);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        // BATCH Response Body 无 SerialNo 字段（grep 实测仅 DataTransferResult 1 个 @XmlElement），
        // dispatcher.extractSerialNo line 223-237 走 NoSuchMethodException fallback 返回 transitionNo。
        assertThat(event.serialNo())
                .as("BATCH Response Body lacks getSerialNo, dispatcher falls back to transitionNo")
                .isEqualTo("20260509");
        assertThat(event.body())
                .as("dispatcher must publish typed DataTransferCheckBatchResponse2102 body (P4-MSG-A-inbound T1)")
                .isInstanceOf(DataTransferCheckBatchResponse2102.class);
    }

    @Test
    @DisplayName("dispatch 2103 → publishEvent body is CompanyInfoBatchResponse2103 (FR-MSG-2103)")
    void dispatch_2103_shouldPublishEventWithCompanyInfoBatchResponse2103Body() {
        final byte[] xml = VALID_2103_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-2103abcdef0123456789abcdef01230000",
                        MessageType.MSG_2103, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_2103), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("2103", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_2103);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo())
                .as("BATCH Response Body lacks getSerialNo, dispatcher falls back to transitionNo")
                .isEqualTo("20260509");
        assertThat(event.body())
                .as("dispatcher must publish typed CompanyInfoBatchResponse2103 body (P4-MSG-A-inbound T1)")
                .isInstanceOf(CompanyInfoBatchResponse2103.class);
    }

    @Test
    @DisplayName("dispatch 2104 → publishEvent body is CompanyAuthFileBatchResponse2104 (FR-MSG-2104)")
    void dispatch_2104_shouldPublishEventWithCompanyAuthFileBatchResponse2104Body() {
        final byte[] xml = VALID_2104_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-2104abcdef0123456789abcdef01230000",
                        MessageType.MSG_2104, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_2104), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("2104", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_2104);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo())
                .as("BATCH Response Body lacks getSerialNo, dispatcher falls back to transitionNo")
                .isEqualTo("20260509");
        assertThat(event.body())
                .as("dispatcher must publish typed CompanyAuthFileBatchResponse2104 body (P4-MSG-A-inbound T1)")
                .isInstanceOf(CompanyAuthFileBatchResponse2104.class);
    }

    @Test
    @DisplayName("dispatch 3001 → publishEvent body is ProgressQuery3001 (FR-MSG-3001)")
    void dispatch_3001_shouldPublishEventWithProgressQuery3001Body() {
        final byte[] xml = VALID_3001_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3001abcdef0123456789abcdef01230000",
                        MessageType.MSG_3001, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3001), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3001", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3001);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        // 3001 Body POJO has getSerialNo() — dispatcher extractSerialNo walks
        // the positive path and returns the <SerialNo> text from the fixture
        // XML (T1.1 grep sanity check: 6/6 SUPPLY_CHAIN BIDIRECTIONAL bodies
        // expose getSerialNo). NOT the transitionNo fallback.
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003001");
        assertThat(event.body())
                .as("dispatcher must publish typed ProgressQuery3001 body (P4-Plan-C T1 wire-in)")
                .isInstanceOf(ProgressQuery3001.class);
    }

    @Test
    @DisplayName("dispatch 3002 → publishEvent body is ProgressQueryReturn3002 (FR-MSG-3002)")
    void dispatch_3002_shouldPublishEventWithProgressQueryReturn3002Body() {
        final byte[] xml = VALID_3002_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3002abcdef0123456789abcdef01230000",
                        MessageType.MSG_3002, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3002), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3002", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3002);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003002");
        assertThat(event.body())
                .as("dispatcher must publish typed ProgressQueryReturn3002 body (P4-Plan-C T1 wire-in)")
                .isInstanceOf(ProgressQueryReturn3002.class);
    }

    @Test
    @DisplayName("dispatch 3003 → publishEvent body is PzInfoQuery3003 (FR-MSG-3003)")
    void dispatch_3003_shouldPublishEventWithPzInfoQuery3003Body() {
        final byte[] xml = VALID_3003_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3003abcdef0123456789abcdef01230000",
                        MessageType.MSG_3003, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3003), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3003", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3003);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003003");
        assertThat(event.body())
                .as("dispatcher must publish typed PzInfoQuery3003 body (P4-Plan-C T1 wire-in, "
                        + "camelCase root <pzInfoQuery3003> per XSD)")
                .isInstanceOf(PzInfoQuery3003.class);
    }

    @Test
    @DisplayName("dispatch 3004 → publishEvent body is PzInfoReturn3004 (FR-MSG-3004)")
    void dispatch_3004_shouldPublishEventWithPzInfoReturn3004Body() {
        final byte[] xml = VALID_3004_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3004abcdef0123456789abcdef01230000",
                        MessageType.MSG_3004, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3004), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3004", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3004);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003004");
        assertThat(event.body())
                .as("dispatcher must publish typed PzInfoReturn3004 body (P4-Plan-C T1 wire-in, "
                        + "camelCase root <pzInfoReturn3004> per XSD)")
                .isInstanceOf(PzInfoReturn3004.class);
    }

    @Test
    @DisplayName("dispatch 3005 → publishEvent body is QyAccQuery3005 (FR-MSG-3005)")
    void dispatch_3005_shouldPublishEventWithQyAccQuery3005Body() {
        final byte[] xml = VALID_3005_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3005abcdef0123456789abcdef01230000",
                        MessageType.MSG_3005, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3005), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3005", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3005);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003005");
        assertThat(event.body())
                .as("dispatcher must publish typed QyAccQuery3005 body (P4-Plan-C T1 wire-in, "
                        + "camelCase root <qyAccQuery3005> per XSD)")
                .isInstanceOf(QyAccQuery3005.class);
    }

    @Test
    @DisplayName("dispatch 3006 → publishEvent body is QyAccQueryReturn3006 (FR-MSG-3006)")
    void dispatch_3006_shouldPublishEventWithQyAccQueryReturn3006Body() {
        final byte[] xml = VALID_3006_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3006abcdef0123456789abcdef01230000",
                        MessageType.MSG_3006, "20260509", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3006), eq("20260509"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3006", "20260509", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3006);
        assertThat(event.transitionNo()).isEqualTo("20260509");
        assertThat(event.serialNo()).isEqualTo("SN2026050900000000000000003006");
        assertThat(event.body())
                .as("dispatcher must publish typed QyAccQueryReturn3006 body (P4-Plan-C T1 wire-in, "
                        + "camelCase root <qyAccQueryReturn3006> per XSD)")
                .isInstanceOf(QyAccQueryReturn3006.class);
    }

    @Test
    @DisplayName("dispatch 3112 → publishEvent body is HxqyCreditAmt3112 + serialNo 来自 body (FR-MSG-3112 inbound P4-MSG-J)")
    void dispatch_3112_shouldPublishEventWithHxqyCreditAmt3112Body() {
        final byte[] xml = VALID_3112_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3112abcdef0123456789abcdef0123000",
                        MessageType.MSG_3112, "20260524", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3112), eq("20260524"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3112", "20260524", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3112);
        assertThat(event.transitionNo()).isEqualTo("20260524");
        assertThat(event.serialNo())
                .as("3112 carries business SerialNo, dispatcher must surface it (not transitionNo)")
                .isEqualTo("SN20260524C3112");
        assertThat(event.body())
                .as("dispatcher must publish typed HxqyCreditAmt3112 body (P4-MSG-J wire-in)")
                .isInstanceOf(HxqyCreditAmt3112.class);
    }

    @Test
    @DisplayName("dispatch 3009 → publishEvent body is RzReturnInfo3009 + serialNo 来自 body (FR-MSG-3009 inbound P4-MSG-K)")
    void dispatch_3009_shouldPublishEventWithRzReturnInfo3009Body() {
        final byte[] xml = VALID_3009_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3009abcdef0123456789abcdef0123000",
                        MessageType.MSG_3009, "20260525", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3009), eq("20260525"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3009", "20260525", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3009);
        assertThat(event.transitionNo()).isEqualTo("20260525");
        assertThat(event.serialNo())
                .as("3009 carries business SerialNo, dispatcher must surface it (not transitionNo)")
                .isEqualTo("SN20260525A3009");
        assertThat(event.body())
                .as("dispatcher must publish typed RzReturnInfo3009 body (P4-MSG-K wire-in, "
                        + "camelCase root <rzReturnInfo3009> per XSD)")
                .isInstanceOf(RzReturnInfo3009.class);
    }

    @Test
    @DisplayName("dispatch 3103 → publishEvent body is ArchiveReturnInfo3103 + serialNo 来自 body (FR-MSG-3103 inbound P4-MSG-K)")
    void dispatch_3103_shouldPublishEventWithArchiveReturnInfo3103Body() {
        final byte[] xml = VALID_3103_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3103abcdef0123456789abcdef0123000",
                        MessageType.MSG_3103, "20260525", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3103), eq("20260525"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3103", "20260525", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3103);
        assertThat(event.transitionNo()).isEqualTo("20260525");
        assertThat(event.serialNo())
                .as("3103 carries business SerialNo, dispatcher must surface it (not transitionNo)")
                .isEqualTo("SN20260525A3103");
        assertThat(event.body())
                .as("dispatcher must publish typed ArchiveReturnInfo3103 body (P4-MSG-K wire-in, "
                        + "PascalCase root <ArchiveReturnInfo3103> per XSD)")
                .isInstanceOf(ArchiveReturnInfo3103.class);
    }

    @Test
    @DisplayName("dispatch 3105 → publishEvent body is RzApplyInfo3105 + serialNo 来自 body (FR-MSG-3105 inbound P4-MSG-K)")
    void dispatch_3105_shouldPublishEventWithRzApplyInfo3105Body() {
        final byte[] xml = VALID_3105_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3105abcdef0123456789abcdef0123000",
                        MessageType.MSG_3105, "20260525", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3105), eq("20260525"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3105", "20260525", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3105);
        assertThat(event.transitionNo()).isEqualTo("20260525");
        assertThat(event.serialNo())
                .as("3105 carries business SerialNo, dispatcher must surface it (not transitionNo)")
                .isEqualTo("SN20260525A3105");
        assertThat(event.body())
                .as("dispatcher must publish typed RzApplyInfo3105 body (P4-MSG-K wire-in, "
                        + "camelCase root <rzApplyInfo3105> per XSD)")
                .isInstanceOf(RzApplyInfo3105.class);
    }

    @Test
    @DisplayName("dispatch 3113 → publishEvent body is HxqyCreditAmt3113 + serialNo 来自 body (FR-MSG-3113 inbound P4-MSG-K)")
    void dispatch_3113_shouldPublishEventWithHxqyCreditAmt3113Body() {
        final byte[] xml = VALID_3113_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-3113abcdef0123456789abcdef0123000",
                        MessageType.MSG_3113, "20260525", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3113), eq("20260525"), eq(xml)))
                .thenReturn(completed);

        dispatcher.dispatch("3113", "20260525", xml);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3113);
        assertThat(event.transitionNo()).isEqualTo("20260525");
        assertThat(event.serialNo())
                .as("3113 carries business SerialNo, dispatcher must surface it (not transitionNo)")
                .isEqualTo("SN20260525A3113");
        assertThat(event.body())
                .as("dispatcher must publish typed HxqyCreditAmt3113 body (P4-MSG-K wire-in, "
                        + "camelCase root <hxqyCreditAmt3113> per XSD)")
                .isInstanceOf(HxqyCreditAmt3113.class);
    }
}
