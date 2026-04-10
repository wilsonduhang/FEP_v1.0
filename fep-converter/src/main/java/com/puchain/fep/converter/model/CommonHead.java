package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.regex.Pattern;

/**
 * CFX 报文头。参见 PRD v1.3 §3.2.2。
 *
 * <p>12 字段与 PRD 标识符逐一对应，字段顺序按 propOrder 序列化。</p>
 *
 * <p><b>访问策略</b>：使用 {@code @XmlAccessorType(XmlAccessType.PROPERTY)}
 * 而非 FIELD，确保 JAXB unmarshal 走 setter 路径，入站伪造/损坏报文在反序列化
 * 阶段即被 setter 的格式校验拒绝。</p>
 *
 * <p><b>jaxb-runtime 严格校验要求</b>：`jaxb-runtime` 4.x (Eclipse) 默认
 * 会将 setter 抛出的 {@code IllegalArgumentException} 包装为 ERROR 级
 * {@code ValidationEvent}，由默认 handler 静默吞掉（unmarshal 继续，字段留 null）。
 * 要让 setter 校验真正抛出异常，调用方必须在 {@code Unmarshaller} 上注册严格
 * handler：{@code unmarshaller.setEventHandler(event -> false)}。
 * 本模块的 {@code XmlCodec}（Task 5）会统一强制这一点，业务代码通过 XmlCodec
 * 反序列化即自动获得严格校验；直接使用 JAXB API 的调用方需自行配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {
        "version", "srcNode", "desNode", "app", "msgNo", "msgId",
        "corrMsgId", "workDate", "fileName", "fileContentHash", "fileSize", "reserve"
})
public class CommonHead {

    /** 金融机构节点代码长度（SrcNode/DesNode）。 */
    private static final int NODE_CODE_LENGTH = 14;

    /** 报文标识号长度（14 日期时间 + 6 顺序号）。 */
    private static final int MSG_ID_LENGTH = 20;

    /** 4 位数字报文号校验模式。 */
    private static final Pattern MSG_NO_PATTERN = Pattern.compile("\\d{4}");

    /** 8 位数字 YYYYMMDD 日期校验模式。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");

    private String version = "1.0";
    private String srcNode;
    private String desNode;
    private String app = "HNDEMP";
    private String msgNo;
    private String msgId;
    private String corrMsgId;
    private String workDate;
    private String fileName;
    private String fileContentHash;
    private Long fileSize;
    private String reserve;

    /**
     * 获取版本号。
     *
     * @return 版本号，固定 "1.0"
     */
    @XmlElement(name = "Version", required = true)
    public String getVersion() {
        return version;
    }

    /**
     * 设置版本号。
     *
     * @param version 版本号
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * 获取报文发起方 14 位金融机构代码。
     *
     * @return 报文发起方 14 位金融机构代码
     */
    @XmlElement(name = "SrcNode", required = true)
    public String getSrcNode() {
        return srcNode;
    }

    /**
     * 设置报文发起方 14 位金融机构代码。
     *
     * @param srcNode 14 位金融机构代码
     */
    public void setSrcNode(final String srcNode) {
        if (srcNode != null && srcNode.length() != NODE_CODE_LENGTH) {
            throw new IllegalArgumentException("SrcNode 必须为 14 位");
        }
        this.srcNode = srcNode;
    }

    /**
     * 获取报文接收方 14 位金融机构代码。
     *
     * @return 报文接收方 14 位金融机构代码
     */
    @XmlElement(name = "DesNode", required = true)
    public String getDesNode() {
        return desNode;
    }

    /**
     * 设置报文接收方 14 位金融机构代码。
     *
     * @param desNode 14 位金融机构代码
     */
    public void setDesNode(final String desNode) {
        if (desNode != null && desNode.length() != NODE_CODE_LENGTH) {
            throw new IllegalArgumentException("DesNode 必须为 14 位");
        }
        this.desNode = desNode;
    }

    /**
     * 获取应用代码。
     *
     * @return 应用代码，固定 "HNDEMP"
     */
    @XmlElement(name = "App", required = true)
    public String getApp() {
        return app;
    }

    /**
     * 设置应用代码。
     *
     * @param app 应用代码
     */
    public void setApp(final String app) {
        this.app = app;
    }

    /**
     * 获取 4 位数字报文号。
     *
     * @return 4 位数字报文号
     */
    @XmlElement(name = "MsgNo", required = true)
    public String getMsgNo() {
        return msgNo;
    }

    /**
     * 设置 4 位数字报文号。
     *
     * @param msgNo 4 位数字报文号
     */
    public void setMsgNo(final String msgNo) {
        if (msgNo != null && !MSG_NO_PATTERN.matcher(msgNo).matches()) {
            throw new IllegalArgumentException("MsgNo 必须为 4 位数字");
        }
        this.msgNo = msgNo;
    }

    /**
     * 获取 20 位报文标识号。
     *
     * @return 20 位报文标识号（14 日期时间 + 6 顺序号）
     */
    @XmlElement(name = "MsgId", required = true)
    public String getMsgId() {
        return msgId;
    }

    /**
     * 设置 20 位报文标识号。
     *
     * @param msgId 20 位报文标识号
     */
    public void setMsgId(final String msgId) {
        if (msgId != null && msgId.length() != MSG_ID_LENGTH) {
            throw new IllegalArgumentException("MsgId 必须为 20 位（14 日期时间 + 6 顺序号）");
        }
        this.msgId = msgId;
    }

    /**
     * 获取关联报文标识号。
     *
     * @return 关联报文标识号
     */
    @XmlElement(name = "CorrMsgId", required = true)
    public String getCorrMsgId() {
        return corrMsgId;
    }

    /**
     * 设置关联报文标识号。
     *
     * @param corrMsgId 关联报文标识号
     */
    public void setCorrMsgId(final String corrMsgId) {
        this.corrMsgId = corrMsgId;
    }

    /**
     * 获取工作日期。
     *
     * @return 工作日期 YYYYMMDD
     */
    @XmlElement(name = "WorkDate", required = true)
    public String getWorkDate() {
        return workDate;
    }

    /**
     * 设置工作日期。
     *
     * @param workDate 工作日期 YYYYMMDD
     */
    public void setWorkDate(final String workDate) {
        if (workDate != null && !DATE_PATTERN.matcher(workDate).matches()) {
            throw new IllegalArgumentException("WorkDate 必须为 YYYYMMDD");
        }
        this.workDate = workDate;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名（携带文件时填）
     */
    @XmlElement(name = "FileName")
    public String getFileName() {
        return fileName;
    }

    /**
     * 设置文件名。
     *
     * @param fileName 文件名
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * 获取文件 SM3 散列值。
     *
     * @return 文件 SM3 散列值（Hex）
     */
    @XmlElement(name = "FileContentHash")
    public String getFileContentHash() {
        return fileContentHash;
    }

    /**
     * 设置文件 SM3 散列值。
     *
     * @param fileContentHash 文件 SM3 散列值
     */
    public void setFileContentHash(final String fileContentHash) {
        this.fileContentHash = fileContentHash;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小（Byte）
     */
    @XmlElement(name = "FileSize")
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * 设置文件大小。
     *
     * @param fileSize 文件大小
     */
    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 获取保留字段。
     *
     * @return 保留字段
     */
    @XmlElement(name = "Reserve")
    public String getReserve() {
        return reserve;
    }

    /**
     * 设置保留字段。
     *
     * @param reserve 保留字段
     */
    public void setReserve(final String reserve) {
        this.reserve = reserve;
    }
}
