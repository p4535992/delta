package ee.webmedia.alfresco.common.web;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.IntegerConverter;

import org.alfresco.web.ui.repo.component.property.UIProperty;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that can show custom error message based on customMsgKey
 * 
 * @author Ats Uiboupin
 */
public class ConvertIntWithMsg extends IntegerConverter implements StateHolder {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ConvertIntWithMsg.class);
    @SuppressWarnings("hiding")
    public static final String CONVERTER_ID = ConvertIntWithMsg.class.getCanonicalName();
    private boolean trans;
    private String customMsgKey;

    public void setMsgKey(String msgKey) {
        customMsgKey = msgKey;
    }

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        try {
            return super.getAsObject(facesContext, uiComponent, value);
        } catch (ConverterException e) {
            UIProperty property = ComponentUtil.getAncestorComponent(uiComponent, UIProperty.class);
            final String columnLabel;
            if (property == null) {
                columnLabel = ComponentUtil.getColumnLabel(uiComponent);
            } else {
                columnLabel = ComponentUtil.getPropertyLabel(property, "");
            }
            if (isNotBlank(customMsgKey) && isNotBlank(columnLabel)) {
                throw new RuntimeException("Should we prefer column name or customMsgKey for localisation of the error message? \ncustomMsgKey='"
                        + customMsgKey + "'\n columnLabel=" + columnLabel, e);
            }
            MessageDataImpl msgData = new MessageDataImpl("validation_is_int_number", columnLabel);
            throw new ConverterException(MessageUtil.getFacesMessage(msgData), e);
        }
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        customMsgKey = (String) values[1];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[2];
        values[0] = trans;
        values[1] = customMsgKey;
        return values;
    }

    @Override
    public boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(boolean trans) {
        this.trans = trans;
    }
}
