package ee.webmedia.alfresco.importer.excel.vo;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class SendInfo {
    /** doccom:sendMode */
    private String sendMode;

    public void setSendMode(String sendMode) {
        this.sendMode = sendMode;
    }

    public String getSendMode() {
        return sendMode;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}