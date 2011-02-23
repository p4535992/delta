package ee.webmedia.alfresco.document.log.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.log.model.DocumentLog;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.SeriesModel;

public class LogBlockBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient DocumentLogService documentLogService;
    private transient DictionaryService dictionaryService;

    private NodeRef parentRef;
    private List<DocumentLog> logs;

    private QName parentNodeType;


    public void init(Node node) {
        reset();
        parentRef = node.getNodeRef();
        parentNodeType = node.getType();
        restore();
    }

    public void restore() {
        if (SeriesModel.Types.SERIES.equals(parentNodeType)) {
            logs = getDocumentLogService().getSeriesLogs(parentRef);
        } else if (getDictionaryService().isSubClass(parentNodeType, DocumentCommonModel.Types.DOCUMENT)) {
            logs = getDocumentLogService().getDocumentLogs(parentRef);
        } else {
            throw new IllegalArgumentException("Unexpected type of parent node for loging block. type='"+parentNodeType+"'");
        }
    }

    public void reset() {
        parentRef = null;
        logs = null;
        parentNodeType = null;
    }

    public boolean isRendered() {
        return logs != null && logs.size() > 0;
    }

    // START: getters / setters

    public List<DocumentLog> getLogs() {
        return logs;
    }

    public DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    protected DictionaryService getDictionaryService() {
        if (this.dictionaryService == null) {
            this.dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
        }
        return this.dictionaryService;
    }

    // END: getters / setters
}
