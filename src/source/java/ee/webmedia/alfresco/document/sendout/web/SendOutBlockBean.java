package ee.webmedia.alfresco.document.sendout.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;

/**
 * @author Erko Hansar
 */
public class SendOutBlockBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private transient SendOutService sendOutService;
    
    private NodeRef document;
    private List<SendInfo> sendInfos;
    private boolean expanded = false;

    public void init(Node node) {
        reset();
        this.document = node.getNodeRef();
        restore();
    }

    public void restore() {
        sendInfos = getSendOutService().getSendInfos(document);
    }

    public void reset() {
        document = null;
        sendInfos = null;
        expanded = false;
    }

    public void expandedAction(ActionEvent event) {
        this.expanded = !this.expanded;
    }

    public boolean isRendered() {
        return this.sendInfos != null && this.sendInfos.size() > 0;
    }

    // START: getters / setters
    
    public boolean isExpanded() {
        return this.expanded;
    }
    
    public List<SendInfo> getSendInfos() {
        return this.sendInfos;
    }

    public SendOutService getSendOutService() {
        if (sendOutService == null) {
            sendOutService = (SendOutService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(SendOutService.BEAN_NAME);
        }
        return sendOutService;
    }

    // END: getters / setters
    
}
