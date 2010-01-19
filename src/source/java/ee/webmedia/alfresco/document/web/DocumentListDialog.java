package ee.webmedia.alfresco.document.web;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Document list
 * 
 * @author Ats Uiboupin
 */
public class DocumentListDialog extends BaseDialogBean {

    private static final String VOLUME_NODE_REF = "volumeNodeRef";
    private static final String CASE_NODE_REF = "caseNodeRef";
    private static final long serialVersionUID = 1L;
    private transient DocumentService documentService;
    private transient VolumeService volumeService;
    private transient CaseService caseService;
    private List<Document> documents;
    // one of the following should always be null(depending of whether it is directly under volume or under case, that is under volume)
    private Volume parentVolume;
    private Case parentCase;
    private boolean quickSearch = false;
    private String searchValue;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // Finish button is always hidden
        return null;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }
    
    @Override
    public Object getActionsContext() {
        return null; // since we are using actions, but not action context, we don't need instance of NavigationBean that is used in the overloadable method
    }

    // START: jsf actions/accessors
    public void showAll(ActionEvent event) {
        final Map<String, String> parameterMap = ((UIActionLink) event.getSource()).getParameterMap();
        final String param;
        if (parameterMap.containsKey(VOLUME_NODE_REF)) {
            param = ActionUtil.getParam(event, VOLUME_NODE_REF);
            parentVolume = getVolumeService().getVolumeByNoderef(param);
        } else {
            param = ActionUtil.getParam(event, CASE_NODE_REF);
            parentCase = getCaseService().getCaseByNoderef(param);
        }
        if (parentCase != null) {
            documents = getDocumentService().getAllDocumentsByCase(parentCase.getNode().getNodeRef());
        } else {// assuming that parentVolume is volume
            documents = getDocumentService().getAllDocumentsByVolume(parentVolume.getNode().getNodeRef());
        }
        Collections.sort(documents);
        quickSearch = false;
    }

    public void quickSearch(ActionEvent event) {
        // Quick search must "reset the current dialog stack" and put the document list dialog as the base dialog into the stack.
        // Also in case of quick search the cancel button is not displayed (the whole button container is not rendered through 
        // container.jsp hack for DocumentListDialog). If there are more beans that need to sometimes display some buttons and sometimes
        // not. Then the hack should be refactored into new DialogManager isAnyButtonVisible method that asks this from then current 
        // bean (BaseDialogBean always returns true).
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(UIMenuComponent.VIEW_STACK, new Stack<String>());

        quickSearch = true;
        documents = getDocumentService().searchDocumentsQuick(searchValue);
    }
    
    public List<Document> getEntries() {
        return documents;
    }

    public String getListTitle() {
        if (quickSearch) {
            return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search");
        } else if (parentCase != null) {
            return parentCase.getTitle();
        } else if (parentVolume != null) {
            return parentVolume.getVolumeMark() + " " + parentVolume.getTitle();
        } else  {
            return "";
        }
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parentVolume = null;
        parentCase = null;
        documents = null;
        quickSearch = false;
    }

    // START: getters / setters
    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public boolean isQuickSearch() {
        return quickSearch;
    }

    // END: getters / setters
}
