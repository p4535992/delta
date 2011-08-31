package ee.webmedia.alfresco.common.web;

import javax.faces.convert.Converter;
import javax.faces.webapp.ConverterTag;
import javax.servlet.jsp.JspException;

/**
 * Tag that creates converter that can show custom error message based on msgKey.
 * Example: <code>
 * jsp:
 *          <h:inputText value="#{object.myProp}" >
 *             <wm:convertIntWithMsg msgKey="myProp_error" />
 *          </h:inputText>
 * translation.properties:
 * myProp_error=expected number from my prop, but got {1} (from element with JSF id {0})
 * </code>
 * 
 * @author Ats Uiboupin
 */
public class ConvertIntWithMsgTag extends ConverterTag {
    private static final long serialVersionUID = 1L;
    private String msgKey;

    public ConvertIntWithMsgTag() {
        setConverterId(ConvertIntWithMsg.CONVERTER_ID);
    }

    public void setMsgKey(String msgKey) {
        this.msgKey = msgKey;
    }

    @Override
    protected Converter createConverter() throws JspException {
        ConvertIntWithMsg converter = (ConvertIntWithMsg) super.createConverter();
        converter.setMsgKey(msgKey);
        return converter;
    }
}