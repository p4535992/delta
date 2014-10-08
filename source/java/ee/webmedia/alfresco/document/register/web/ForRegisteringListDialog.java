package ee.webmedia.alfresco.document.register.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.document.web.UnsentDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * List dialog for document available for registering.
 */
public class ForRegisteringListDialog extends BaseDocumentListDialog {

    private static final long serialVersionUID = 0L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getDocumentSearchService().searchDocumentsForRegistering(), true, UnsentDocumentListDialog.DOC_PROPS_WITH_OWNER_STRUCT_UNIT);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_register_docs");
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp";
    }

    @Override
    public boolean isShowOrgStructColumn() {
        return true;
    }
}
