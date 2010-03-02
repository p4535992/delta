package ee.webmedia.alfresco.document.log.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.log.model.DocumentLog;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;

public class DocumentLogBlockBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private transient DocumentLogService documentLogService;
    
    private NodeRef document;
    private List<DocumentLog> documentLogs;
    private boolean expanded = false;
    
    public void init(Node node) {
        reset();
        document = node.getNodeRef();
        restore();
    }
    
    public void restore() {
        documentLogs = getDocumentLogService().getDocumentLogs(document);
    }

    public void reset() {
        document = null;
        expanded = false;
        documentLogs = null;
    }

    public void expandedAction(ActionEvent event) {
        expanded = !expanded;
    }
    
    public boolean isRendered() {
        return documentLogs != null && documentLogs.size() > 0;
    }
    
    // START: getters / setters
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public List<DocumentLog> getDocumentLogs() {
        return documentLogs;
    }

    public DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    // END: getters / setters
}
