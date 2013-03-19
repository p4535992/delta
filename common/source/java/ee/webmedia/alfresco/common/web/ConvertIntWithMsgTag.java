package ee.webmedia.alfresco.common.web;

import javax.faces.convert.Converter;
import javax.faces.webapp.ConverterTag;
import javax.servlet.jsp.JspException;

import org.alfresco.web.ui.common.component.data.UIColumn;

/**
 * Tag that creates converter that can show custom error message based on msgKey or if it is in {@link UIColumn}, then take label of the column.
 * Example: <code>
 * jsp:
 *          <a:column id="someColumn">
 *             <f:facet name="header">
 *                <a:sortLink id="someColumn-sort" label="#{msg.someColumn_header}" value="xxx" />
 *             </f:facet>
 *             <h:inputText value="#{object.myProp}" >
 *                <wm:convertIntWithMsg />
 *             </h:inputText>
 *          </a:column>
 * or
 *          <h:inputText value="#{object.myProp}" >
 *             <wm:convertIntWithMsg customMsgKey="myProp_error" />
 *          </h:inputText>
 * translation.properties:
 * myProp_error=expected number from my prop, but got {1} (from element with JSF id {0})
 * </code>
 * 
 * @author Ats Uiboupin
 */
public class ConvertIntWithMsgTag extends ConverterTag {
    private static final long serialVersionUID = 1L;
    private String customMsgKey;

    public ConvertIntWithMsgTag() {
        setConverterId(ConvertIntWithMsg.CONVERTER_ID);
    }

    public void setCustomMsgKey(String customMsgKey) {
        this.customMsgKey = customMsgKey;
    }

    @Override
    protected Converter createConverter() throws JspException {
        ConvertIntWithMsg converter = (ConvertIntWithMsg) super.createConverter();
        converter.setMsgKey(customMsgKey);
        return converter;
    }
}