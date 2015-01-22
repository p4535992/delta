package ee.webmedia.alfresco.notification.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.CollectionUtils;

public class NotificationResult {

    private NodeRef docRef;
    private List<Map<QName, Serializable>> sendInfoProps;
    private boolean notificationSent = false;

    public NodeRef getDocRef() {
        return docRef;
    }

    public void setDocRef(NodeRef docRef) {
        this.docRef = docRef;
    }

    public List<Map<QName, Serializable>> getSendInfoProps() {
        return sendInfoProps;
    }

    public void addSendInfoProps(Map<QName, Serializable> sendInfoProp) {
        if (sendInfoProps == null) {
            sendInfoProps = new ArrayList<>();
        }
        sendInfoProps.add(sendInfoProp);
    }

    public boolean containsSendInfos() {
        return notificationSent && CollectionUtils.isNotEmpty(sendInfoProps);
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void markSent() {
        notificationSent = true;
    }

}