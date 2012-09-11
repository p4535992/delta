package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;

import org.alfresco.web.bean.generator.HeaderSeparatorGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.apache.commons.lang.StringEscapeUtils;

import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator that generates (info)message based on messageKey
 * 
 * @author Ats Uiboupin
 */
public class MessageSeparatorGenerator extends HeaderSeparatorGenerator {
    private String messageKey;
    private String[] messageValuesForHolders;
    private boolean escapeHtml = true;
    private boolean translateMessageValuesForHolders = true;

    @Override
    protected String getHtml(UIComponent component, PropertySheetItem item) {
        Object[] translatedMessageValuesForHolders = null;
        if (messageValuesForHolders != null) {
            if (!translateMessageValuesForHolders) {
                translatedMessageValuesForHolders = messageValuesForHolders;
            } else {
                translatedMessageValuesForHolders = new Object[messageValuesForHolders.length];
                int i = 0;
                for (String messageValuesForHolder : messageValuesForHolders) {
                    translatedMessageValuesForHolders[i++] = new MessageDataImpl(messageValuesForHolder);
                }
            }
        }
        String msgTranslated = MessageUtil.getMessage(messageKey, translatedMessageValuesForHolders);
        if (escapeHtml) {
            msgTranslated = StringEscapeUtils.escapeHtml(msgTranslated);
        }
        return "<div class='message'>" + msgTranslated + "</div>";
    }

    @Override
    protected String getResolvedHtml(UIComponent component, PropertySheetItem item) {
        // TODO Auto-generated method stub
        return getHtml(component, item);
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public void setMessageValuesForHolders(String[] messageValuesForHolders) {
        this.messageValuesForHolders = messageValuesForHolders;
    }

    public void setEscapeHtml(boolean escapeHtml) {
        this.escapeHtml = escapeHtml;
    }

    public void setTranslateMessageValuesForHolders(boolean translateMessageValuesForHolders) {
        this.translateMessageValuesForHolders = translateMessageValuesForHolders;
    }
}