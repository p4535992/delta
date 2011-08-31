package ee.webmedia.alfresco.common.web;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.IntegerConverter;

import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that can show custom error message based on msgKey
 * 
 * @author Ats Uiboupin
 */
public class ConvertIntWithMsg extends IntegerConverter implements StateHolder {
    @SuppressWarnings("hiding")
    public static final String CONVERTER_ID = ConvertIntWithMsg.class.getCanonicalName();
    private boolean trans;
    private String msgKey;

    public void setMsgKey(String msgKey) {
        this.msgKey = msgKey;
    }

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        try {
            return super.getAsObject(facesContext, uiComponent, value);
        } catch (ConverterException e) {
            // same order or placeholders as in parent class
            MessageDataImpl msgData = new MessageDataImpl(msgKey, uiComponent.getId(), value);
            throw new ConverterException(MessageUtil.getFacesMessage(msgData), e);
        }
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        msgKey = (String) values[1];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[2];
        values[0] = trans;
        values[1] = msgKey;
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
