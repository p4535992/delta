package ee.webmedia.alfresco.document.sendout.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * @author Erko Hansar
 */
public class SendOutBlockBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private transient SendOutService sendOutService;
    
    private NodeRef document;
    private List<SendInfo> sendInfos;
    private List<CompoundWorkflow> compoundWorkflows;

    public void init(Node node, List<CompoundWorkflow> compoundWorkflows) {
        reset();
        this.document = node.getNodeRef();
        this.compoundWorkflows = compoundWorkflows;
        restore();
    }

    public void restore() {
        sendInfos = getSendOutService().getDocumentAndTaskSendInfos(document, compoundWorkflows);
    }

    public void reset() {
        document = null;
        sendInfos = null;
    }

    public boolean isRendered() {
        return this.sendInfos != null && this.sendInfos.size() > 0;
    }

    // START: getters / setters
    
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
