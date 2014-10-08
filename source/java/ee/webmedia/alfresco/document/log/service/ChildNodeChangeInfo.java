package ee.webmedia.alfresco.document.log.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Information holding object for composing history log message about a child node being added or removed. This class is used internally by {@link DocumentPropertiesChangeHolder}.
 * 
 * @see DocumentPropertiesChangeHolder
<<<<<<< HEAD
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class ChildNodeChangeInfo {

    public static final ChildNodeChangeInfo APPLICANT = new ChildNodeChangeInfo("applicant", DocumentSpecificModel.Props.APPLICANT_NAME);

    public static final ChildNodeChangeInfo ERRAND = new ChildNodeChangeInfo("errand", null);

    public static final ChildNodeChangeInfo PARTY = new ChildNodeChangeInfo("party", DocumentSpecificModel.Props.PARTY_NAME);

    private static final Map<QName, ChildNodeChangeInfo> CHILD_NODE_CHANGE_INFO;

    private final QName msgParamProp;

    private final String addMessageKey;

    private final String removeMessageKey;

    /**
     * Creates a new information holder for logging a child node being added or removed.
     * <p>
     * The message key for localizing the description must be in standard format:
     * <ul>
     * <li><code>document_log_</code><i>&lt;messagePart&gt;</i><code>_add</code> - when child node is being added;
     * <li><code>document_log_</code><i>&lt;messagePart&gt;</i><code>_rem</code> - when child node is being added.
     * </ul>
     * Therefore, the message part information is required for successful localization.
     * <p>
     * Localized history messages may contain property values from the child node. The property to include at position 0 is defined by <code>msgParamProp</code>. Currently, the
     * property may contain only <code>String</code> values. When the property has no value or the property has not been specified, an empty value place-holder will be provided
     * instead for the message.
     * <p>
     * Currently only one child node property value is possible to use in the message, but technically it is possible to extend this solution for multiple property values.
     * 
     * @param messagePart A unique message part to use for composing child node add or remove messages.
     * @param msgParamProp Optional node property name for injecting its value to the history message.
     */
    public ChildNodeChangeInfo(String messagePart, QName msgParamProp) {
        addMessageKey = "document_log_" + messagePart + "_add";
        removeMessageKey = "document_log_" + messagePart + "_rem";
        this.msgParamProp = msgParamProp;
    }

    public QName getMsgParamProp() {
        return msgParamProp;
    }

    public String getAddMessageKey() {
        return addMessageKey;
    }

    public String getRemoveMessageKey() {
        return removeMessageKey;
    }

    public static ChildNodeChangeInfo getInstance(QName type) {
        return CHILD_NODE_CHANGE_INFO.get(type);
    }

    static {
        Map<QName, ChildNodeChangeInfo> childrenMap = new HashMap<QName, ChildNodeChangeInfo>(5, 1);
        childrenMap.put(DocumentChildModel.Assocs.APPLICANT_ABROAD, ChildNodeChangeInfo.APPLICANT);
        childrenMap.put(DocumentChildModel.Assocs.APPLICANT_DOMESTIC, ChildNodeChangeInfo.APPLICANT);
        childrenMap.put(DocumentChildModel.Assocs.APPLICANT_TRAINING, ChildNodeChangeInfo.APPLICANT);
<<<<<<< HEAD
        childrenMap.put(DocumentChildModel.Assocs.ERRAND_ABROAD, ChildNodeChangeInfo.ERRAND);
        childrenMap.put(DocumentChildModel.Assocs.ERRAND_DOMESTIC, ChildNodeChangeInfo.ERRAND);
=======
        childrenMap.put(DocumentChildModel.Assocs.APPLICANT_ERRAND, ChildNodeChangeInfo.APPLICANT);
        childrenMap.put(DocumentChildModel.Assocs.ERRAND_ABROAD, ChildNodeChangeInfo.ERRAND);
        childrenMap.put(DocumentChildModel.Assocs.ERRAND_DOMESTIC, ChildNodeChangeInfo.ERRAND);
        childrenMap.put(DocumentChildModel.Assocs.ERRAND, ChildNodeChangeInfo.ERRAND);
>>>>>>> develop-5.1
        childrenMap.put(DocumentChildModel.Assocs.CONTRACT_PARTY, ChildNodeChangeInfo.PARTY);
        CHILD_NODE_CHANGE_INFO = Collections.unmodifiableMap(childrenMap);
    }
}
