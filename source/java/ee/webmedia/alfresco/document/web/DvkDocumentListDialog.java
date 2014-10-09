package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DvkDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DvkDocumentListDialog";

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getDocumentService().getAllDocumentFromDvk(), false);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_dvk_documents");
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/web/dvk-document-list-dialog-columns.jsp";
    }

}
