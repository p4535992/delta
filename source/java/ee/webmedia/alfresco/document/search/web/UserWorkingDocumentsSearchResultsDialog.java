package ee.webmedia.alfresco.document.search.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.richlist.PageLoadCallback;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class UserWorkingDocumentsSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserWorkingDocumentsSearchResultsDialog";

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_myWorkingDocuments");
    }

    @Override
    public void restored() {
        List<NodeRef> docs = getDocumentSearchService().searchInProcessUserDocuments();
        Set<QName> propsToLoad = new HashSet<QName>();
        propsToLoad.addAll(Arrays.asList(DocumentCommonModel.Props.DOC_NAME, DocumentAdminModel.Props.OBJECT_TYPE_ID, DocumentCommonModel.Props.VOLUME));
        propsToLoad.addAll(SENDER_PROPS);
        documentProvider = new DocumentListDataProvider(docs, new PageLoadCallback<NodeRef, Document>() {

            @Override
            public void doWithPageItems(Map<NodeRef, Document> pageItems) {
                BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
                Map<NodeRef, String> documentWorkflowStates = bulkLoadNodeService.loadDocumentWorkflowStates(new ArrayList<>(pageItems.keySet()));
                for (Map.Entry<NodeRef, Document> entry : pageItems.entrySet()) {
                    entry.getValue().setWorkflowStatus(documentWorkflowStates.get(entry.getKey()));
                }
            }
        }, false, propsToLoad);
    }

    public CustomChildrenCreator getDocumentRowFileGenerator() {
        return ComponentUtil.getDocumentRowFileGenerator(FacesContext.getCurrentInstance().getApplication(), 5);
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/user-working-documents-list-dialog-columns.jsp";
    }
}
