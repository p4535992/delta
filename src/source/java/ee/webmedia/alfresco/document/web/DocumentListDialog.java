package ee.webmedia.alfresco.document.web;

import java.util.Collections;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.component.UIActionLink;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Document list
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
    private boolean quickSearch = false;
    private String searchValue;

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
        refreshDocuments();
    }

    private void refreshDocuments() {
        if (parentCase != null) {
            documents = getDocumentService().getAllDocumentsByCase(parentCase.getNode().getNodeRef());
        } else {// assuming that parentVolume is volume
            documents = getDocumentService().getAllDocumentsByVolume(parentVolume.getNode().getNodeRef());
        }
        Collections.sort(documents);
    }


    @Override
    public void restored() {
        // Update list data
        refreshDocuments();
    }

    @Override
    public String cancel() {
        parentVolume = null;
        parentCase = null;
        return super.cancel();
    }

    @Override
    public String getListTitle() {
        if (quickSearch) {
            return MessageUtil.getMessage("document_search");
        } else if (parentCase != null) {
            return parentCase.getTitle();
        } else if (parentVolume != null) {
            return parentVolume.getVolumeMark() + " " + parentVolume.getTitle();
        } else {
            return "";
        }
    }
    
    private void resetFields() {
        parentVolume = null;
        parentCase = null;
        documents = null;
        quickSearch = false;
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
