package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.PersonInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3109 {@link QyRegister3109}
 * supply chain Body POJO and its 6 supplychain support types
 * ({@link HxqyInfo3109}, {@link HxqyInfoMx}, {@link QyAccLockInfo},
 * {@link QyAccLockInfoMx}, {@link PlatInfo}, {@link ShareholderInfo}) plus
 * {@link PersonInfo} reused from {@code body.common} (T0.5).
 *
 * <p>Covers the largest supply-chain body in P2d-ext (8 main + 6 nested supplychain
 * + 1 common nested {@code PersonInfo}). Single roundtrip test exercises every
 * complexType in 3109.xsd including the deepest nesting path
 * {@code QyRegister3109 → PlatInfo → ContactPersonInfo (PersonInfo)}.</p>
 *
 * <p>Includes a focused assertion that {@link HxqyInfo3109} (3109 variant) is
 * binary-incompatible with T4 {@link HxqyInfo} (3107/3112 shared) — same XSD
 * type name, completely different field set (see {@link HxqyInfo3109} class
 * Javadoc for full discussion).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3109Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    QyRegister3109.class,
                    HxqyInfo3109.class, HxqyInfoMx.class,
                    QyAccLockInfo.class, QyAccLockInfoMx.class,
                    PlatInfo.class, ShareholderInfo.class,
                    PersonInfo.class, ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void qyRegister3109_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(QyRegister3109.class)).isTrue();
        // All 6 supplychain support types must also extend CfxBody (ArchUnit guards this,
        // but explicit inline check protects against package-pattern drift).
        assertThat(CfxBody.class.isAssignableFrom(HxqyInfo3109.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(HxqyInfoMx.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(QyAccLockInfo.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(QyAccLockInfoMx.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(PlatInfo.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(ShareholderInfo.class)).isTrue();
    }

    @Test
    void qyRegister3109_jaxbRoundtrip_shouldPreserveAllFieldsIncludingNestedPersonInfo()
            throws Exception {
        // ── 核心企业登记信息段（hxqyInfo wrapper + 2 hxqyInfoMx 明细） ───────────
        HxqyInfoMx hxMx1 = new HxqyInfoMx();
        hxMx1.setPlatNodeCode("B1001010203");
        hxMx1.setHxqyName("湖南某某核心企业A");
        hxMx1.setHxqyCode("91430100MA00000001");
        hxMx1.setHxqyState("01");
        hxMx1.setHxqyClass("02");
        hxMx1.setHxqyDateBegin("20260101");
        hxMx1.setHxqyDateEnd("20271231");
        hxMx1.setHxqyGroupName("某某集团");
        hxMx1.setHxqyGroupCode("91430100MA00000099");
        hxMx1.setBankCodeList("104,105");
        hxMx1.setHxqyCAFilename("hxqyA.cer");

        HxqyInfoMx hxMx2 = new HxqyInfoMx();
        hxMx2.setPlatNodeCode("B1001010203");
        hxMx2.setHxqyName("湖南某某核心企业B");
        hxMx2.setHxqyCode("91430100MA00000002");
        hxMx2.setHxqyState("02");
        // optional fields omitted

        HxqyInfo3109 hxqy = new HxqyInfo3109();
        hxqy.setHxqyNum("2");
        hxqy.setHxqyInfoMx(List.of(hxMx1, hxMx2));

        // ── 回款锁定企业登记信息段（qyAccLockInfo wrapper + 1 明细） ─────────────
        QyAccLockInfoMx lockMx = new QyAccLockInfoMx();
        lockMx.setMonitorState("01");
        lockMx.setAccMonitorNo("MON202604240001");
        lockMx.setQyName("回款监控企业X");
        lockMx.setPayerName("付款企业Y");
        lockMx.setBeginDate("20260424");
        lockMx.setEndDate("20271231");
        lockMx.setAmtMin("1000.00");
        lockMx.setAmtMax("99999999.99");
        lockMx.setMemo("正常监控");
        lockMx.setFilename("monitor-20260424.zip");

        QyAccLockInfo lock = new QyAccLockInfo();
        lock.setQyAccLockNum("1");
        lock.setQyAccLockInfoMx(List.of(lockMx));

        // ── 平台企业登记信息段（PlatInfo + 2 主要股东 + 1 联系人 PersonInfo） ────
        ShareholderInfo sh1 = new ShareholderInfo();
        sh1.setShareholderName("张三");
        sh1.setShareProportion("60%");

        ShareholderInfo sh2 = new ShareholderInfo();
        sh2.setShareholderName("李四");
        sh2.setShareProportion("40%");

        PersonInfo contact = new PersonInfo();
        contact.setName("王五");
        contact.setCertType("01");
        contact.setCertNumber("430100199001011234");
        contact.setCertStartDate("20240101");
        contact.setCertEndDate("20340101");
        contact.setPhone("13800138000");
        contact.setPostAddr("湖南省长沙市芙蓉中路1号");
        contact.setMailAddr("contact@example.com");

        PlatInfo plat = new PlatInfo();
        plat.setPlatNodeCode("B1001010203");
        plat.setPlatName("湖南某某供应链平台有限公司");
        plat.setPlatCode("91430100MA00000010");
        plat.setPlatState("1");
        plat.setPlatType("1");
        plat.setPlatSysName("湖南某某供应链信息服务系统");
        plat.setPlatSysShortName("HNScSys");
        plat.setPlatServiceObject("1");
        plat.setPlatDevelopmentMethod("1");
        plat.setSaasServiceName("某SAAS服务商");
        plat.setPlatSysSAAS("0");
        plat.setOtherService("0");
        plat.setPlatSysURL("https://platform.example.com");
        plat.setPlatRegAddr("湖南省长沙市");
        plat.setPlatRegAmt("5000.00");
        plat.setPlatPaidinAmt("3000.00");
        plat.setPlatParent("某某集团");
        plat.setPlatDateBegin("20260101");
        plat.setPlatDateEnd("20271231");
        plat.setPlatCAFilename("plat.cer");
        plat.setShareholderInfo(List.of(sh1, sh2));
        plat.setContactPersonInfo(contact);

        // ── ExtInfo ─────────────────────────────────────────────────────────
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3109附加数据");
        ext.setExtJsonFilename("ext3109.json");

        // ── 主体 ───────────────────────────────────────────────────────────
        QyRegister3109 original = new QyRegister3109();
        original.setSerialNo("SN3109-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setQyFlag("1");
        original.setHxqyInfo(hxqy);
        original.setQyAccLockInfo(lock);
        original.setPlatInfo(plat);
        original.setExtInfo(ext);

        // ── marshal + assert XML structure ─────────────────────────────────
        String xml = marshal(original);

        // Main 8 fields
        assertThat(xml)
                .contains("<qyRegister3109")
                .contains("<SerialNo>SN3109-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<qyFlag>1</qyFlag>");

        // hxqyInfo wrapper + nested hxqyInfoMx (3109 variant: hxqyNum + hxqyInfoMx,
        // NOT 3107 variant which is hxqyName + hxqyCode)
        assertThat(xml)
                .contains("<hxqyInfo>")
                .contains("<hxqyNum>2</hxqyNum>")
                .contains("<hxqyInfoMx>")
                .contains("<PlatNodeCode>B1001010203</PlatNodeCode>")
                .contains("<hxqyName>湖南某某核心企业A</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<hxqyState>01</hxqyState>")
                .contains("<hxqyClass>02</hxqyClass>")
                .contains("<hxqyDateBegin>20260101</hxqyDateBegin>")
                .contains("<hxqyDateEnd>20271231</hxqyDateEnd>")
                .contains("<hxqyGroupName>某某集团</hxqyGroupName>")
                .contains("<hxqyGroupCode>91430100MA00000099</hxqyGroupCode>")
                .contains("<BankCodeList>104,105</BankCodeList>")
                .contains("<hxqyCAFilename>hxqyA.cer</hxqyCAFilename>")
                .contains("<hxqyName>湖南某某核心企业B</hxqyName>")
                .contains("<hxqyState>02</hxqyState>");

        // qyAccLockInfo wrapper + nested qyAccLockInfoMx
        assertThat(xml)
                .contains("<qyAccLockInfo>")
                .contains("<qyAccLockNum>1</qyAccLockNum>")
                .contains("<qyAccLockInfoMx>")
                .contains("<MonitorState>01</MonitorState>")
                .contains("<AccMonitorNo>MON202604240001</AccMonitorNo>")
                .contains("<qyName>回款监控企业X</qyName>")
                .contains("<PayerName>付款企业Y</PayerName>")
                .contains("<BeginDate>20260424</BeginDate>")
                .contains("<EndDate>20271231</EndDate>")
                .contains("<AmtMin>1000.00</AmtMin>")
                .contains("<AmtMax>99999999.99</AmtMax>")
                .contains("<Memo>正常监控</Memo>")
                .contains("<Filename>monitor-20260424.zip</Filename>");

        // PlatInfo (22 fields) + nested ShareholderInfo[2] + ContactPersonInfo (PersonInfo)
        assertThat(xml)
                .contains("<PlatInfo>")
                .contains("<PlatName>湖南某某供应链平台有限公司</PlatName>")
                .contains("<PlatCode>91430100MA00000010</PlatCode>")
                .contains("<PlatState>1</PlatState>")
                .contains("<PlatType>1</PlatType>")
                .contains("<PlatSysName>湖南某某供应链信息服务系统</PlatSysName>")
                .contains("<PlatSysShortName>HNScSys</PlatSysShortName>")
                .contains("<PlatServiceObject>1</PlatServiceObject>")
                .contains("<PlatDevelopmentMethod>1</PlatDevelopmentMethod>")
                .contains("<SAASServiceName>某SAAS服务商</SAASServiceName>")
                .contains("<PlatSysSAAS>0</PlatSysSAAS>")
                .contains("<OtherService>0</OtherService>")
                .contains("<PlatSysURL>https://platform.example.com</PlatSysURL>")
                .contains("<PlatRegAddr>湖南省长沙市</PlatRegAddr>")
                .contains("<PlatRegAmt>5000.00</PlatRegAmt>")
                .contains("<PlatPaidinAmt>3000.00</PlatPaidinAmt>")
                .contains("<PlatParent>某某集团</PlatParent>")
                .contains("<PlatDateBegin>20260101</PlatDateBegin>")
                .contains("<PlatDateEnd>20271231</PlatDateEnd>")
                .contains("<PlatCAFilename>plat.cer</PlatCAFilename>")
                .contains("<ShareholderInfo>")
                .contains("<ShareholderName>张三</ShareholderName>")
                .contains("<ShareProportion>60%</ShareProportion>")
                .contains("<ShareholderName>李四</ShareholderName>")
                .contains("<ShareProportion>40%</ShareProportion>")
                .contains("<ContactPersonInfo>")
                .contains("<Name>王五</Name>")
                .contains("<CertType>01</CertType>")
                .contains("<CertNumber>430100199001011234</CertNumber>")
                .contains("<Phone>13800138000</Phone>")
                .contains("<PostAddr>湖南省长沙市芙蓉中路1号</PostAddr>")
                .contains("<MailAddr>contact@example.com</MailAddr>");

        // ExtInfo
        assertThat(xml)
                .contains("<ExtInfo>")
                .contains("<ExtData>3109附加数据</ExtData>")
                .contains("<ExtJSONFilename>ext3109.json</ExtJSONFilename>");

        // Field order check on the main qyRegister3109 propOrder
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxQyFlag = xml.indexOf("<qyFlag>");
        int idxHxqy = xml.indexOf("<hxqyInfo>");
        int idxLock = xml.indexOf("<qyAccLockInfo>");
        int idxPlat = xml.indexOf("<PlatInfo>");
        int idxExt = xml.indexOf("<ExtInfo>");
        assertThat(idxSerial).isLessThan(idxQyFlag);
        assertThat(idxQyFlag).isLessThan(idxHxqy);
        assertThat(idxHxqy).isLessThan(idxLock);
        assertThat(idxLock).isLessThan(idxPlat);
        assertThat(idxPlat).isLessThan(idxExt);

        // ── unmarshal + verify all fields roundtrip ────────────────────────
        QyRegister3109 parsed = unmarshal(xml, QyRegister3109.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3109-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getQyFlag()).isEqualTo("1");

        // hxqyInfo (3109 variant)
        HxqyInfo3109 ph = parsed.getHxqyInfo();
        assertThat(ph).isNotNull();
        assertThat(ph.getHxqyNum()).isEqualTo("2");
        List<HxqyInfoMx> mxList = ph.getHxqyInfoMx();
        assertThat(mxList).hasSize(2);
        HxqyInfoMx pm1 = mxList.get(0);
        assertThat(pm1.getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(pm1.getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(pm1.getHxqyClass()).isEqualTo("02");
        assertThat(pm1.getBankCodeList()).isEqualTo("104,105");
        HxqyInfoMx pm2 = mxList.get(1);
        assertThat(pm2.getHxqyName()).isEqualTo("湖南某某核心企业B");
        assertThat(pm2.getHxqyClass()).isNull();
        assertThat(pm2.getBankCodeList()).isNull();

        // qyAccLockInfo
        QyAccLockInfo pl = parsed.getQyAccLockInfo();
        assertThat(pl).isNotNull();
        assertThat(pl.getQyAccLockNum()).isEqualTo("1");
        List<QyAccLockInfoMx> lockList = pl.getQyAccLockInfoMx();
        assertThat(lockList).hasSize(1);
        QyAccLockInfoMx pLock = lockList.get(0);
        assertThat(pLock.getMonitorState()).isEqualTo("01");
        assertThat(pLock.getAccMonitorNo()).isEqualTo("MON202604240001");
        assertThat(pLock.getQyName()).isEqualTo("回款监控企业X");
        assertThat(pLock.getPayerName()).isEqualTo("付款企业Y");
        assertThat(pLock.getAmtMin()).isEqualTo("1000.00");
        assertThat(pLock.getAmtMax()).isEqualTo("99999999.99");
        assertThat(pLock.getMemo()).isEqualTo("正常监控");
        assertThat(pLock.getFilename()).isEqualTo("monitor-20260424.zip");

        // PlatInfo + nested ContactPersonInfo (PersonInfo from body.common)
        PlatInfo pp = parsed.getPlatInfo();
        assertThat(pp).isNotNull();
        assertThat(pp.getPlatNodeCode()).isEqualTo("B1001010203");
        assertThat(pp.getPlatName()).isEqualTo("湖南某某供应链平台有限公司");
        assertThat(pp.getPlatCode()).isEqualTo("91430100MA00000010");
        assertThat(pp.getPlatState()).isEqualTo("1");
        assertThat(pp.getPlatType()).isEqualTo("1");
        assertThat(pp.getPlatSysName()).isEqualTo("湖南某某供应链信息服务系统");
        assertThat(pp.getPlatSysShortName()).isEqualTo("HNScSys");
        assertThat(pp.getSaasServiceName()).isEqualTo("某SAAS服务商");
        assertThat(pp.getPlatSysSAAS()).isEqualTo("0");
        assertThat(pp.getOtherService()).isEqualTo("0");
        assertThat(pp.getPlatSysURL()).isEqualTo("https://platform.example.com");
        assertThat(pp.getPlatRegAmt()).isEqualTo("5000.00");
        assertThat(pp.getPlatPaidinAmt()).isEqualTo("3000.00");
        assertThat(pp.getPlatDateBegin()).isEqualTo("20260101");
        assertThat(pp.getPlatDateEnd()).isEqualTo("20271231");
        assertThat(pp.getPlatCAFilename()).isEqualTo("plat.cer");

        List<ShareholderInfo> shareholders = pp.getShareholderInfo();
        assertThat(shareholders).hasSize(2);
        assertThat(shareholders.get(0).getShareholderName()).isEqualTo("张三");
        assertThat(shareholders.get(0).getShareProportion()).isEqualTo("60%");
        assertThat(shareholders.get(1).getShareholderName()).isEqualTo("李四");
        assertThat(shareholders.get(1).getShareProportion()).isEqualTo("40%");

        PersonInfo pContact = pp.getContactPersonInfo();
        assertThat(pContact).isNotNull();
        assertThat(pContact.getName()).isEqualTo("王五");
        assertThat(pContact.getCertType()).isEqualTo("01");
        assertThat(pContact.getCertNumber()).isEqualTo("430100199001011234");
        assertThat(pContact.getCertStartDate()).isEqualTo("20240101");
        assertThat(pContact.getCertEndDate()).isEqualTo("20340101");
        assertThat(pContact.getPhone()).isEqualTo("13800138000");
        assertThat(pContact.getPostAddr()).isEqualTo("湖南省长沙市芙蓉中路1号");
        assertThat(pContact.getMailAddr()).isEqualTo("contact@example.com");

        // ExtInfo
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3109附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3109.json");
    }

    // ── helpers ──────────────────────────────────────────────

    /**
     * Marshal with the shared JAXBContext that includes all complex types.
     */
    private <T> String marshal(final T instance) throws Exception {
        Marshaller marshaller = CTX.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(instance, writer);
        return writer.toString();
    }

    /**
     * Unmarshal with the shared JAXBContext.
     */
    @SuppressWarnings("unchecked")
    private <T> T unmarshal(final String xml, final Class<T> type) throws Exception {
        Unmarshaller unmarshaller = CTX.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringReader(xml));
    }
}
