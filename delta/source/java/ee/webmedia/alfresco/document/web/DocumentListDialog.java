package ee.webmedia.alfresco.document.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Document list. <br>
 * <br>
 * This Class has logic of two diferent, but similar versions of documents(when parent is volume or case). <br>
 * Reason is that we don't have to worry about what the parent of document in jsp files.
 * 
 * @author Ats Uiboupin
 */
public class DocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    private static final String VOLUME_NODE_REF = "volumeNodeRef";
    private static final String CASE_NODE_REF = "caseNodeRef";

    private transient VolumeService volumeService;
    private transient CaseService caseService;

    // one of the following should always be null(depending of whether it is directly under volume or under case, that is under volume)
    private Volume parentVolume;
    private Case parentCase;

    public void setup(ActionEvent event) {
        final Map<String, String> parameterMap = ((UIActionLink) event.getSource()).getParameterMap();
        final String param;
        if (parameterMap.containsKey(VOLUME_NODE_REF)) {
            param = ActionUtil.getParam(event, VOLUME_NODE_REF);
            parentVolume = getVolumeService().getVolumeByNodeRef(param);
        } else {
            param = ActionUtil.getParam(event, CASE_NODE_REF);
            parentCase = getCaseService().getCaseByNoderef(param);
        }
        restored();
    }

    @Override
    public void restored() {
        final NodeRef parentRef ;
        if (parentCase != null) {
            parentRef = parentCase.getNode().getNodeRef();
        } else {// assuming that parentVolume is volume
            parentRef = parentVolume.getNode().getNodeRef();
        }
        documents = getChildNodes(parentRef);
        if(documents.size()<=2000) {//if sorting takes less than ca 6 sec (ca 15ms per document, 400doc*15ms==6sec)
            Collections.sort(documents);// sorting needs properties to be fetched from repo
        }
    }

    private List<Document> getChildNodes(NodeRef parentRef) {
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfParent = new ArrayList<Document>(childAssocs.size());
        for (ChildAssociationRef childAssocRef : childAssocs) {
            docsOfParent.add(new Document(childAssocRef.getChildRef()));
        }
        return docsOfParent;
    }

    @Override
    public String cancel() {
        parentVolume = null;
        parentCase = null;
        return super.cancel();
    }

    @Override
    public String getListTitle() {
        if (parentCase != null) {
            return parentCase.getTitle();
        } else if (parentVolume != null) {
            return parentVolume.getVolumeMark() + " " + parentVolume.getTitle();
        } else {
            return "";
        }
    }

    // END: jsf actions/accessors

    // START: getters / setters

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    // END: getters / setters
    
}
