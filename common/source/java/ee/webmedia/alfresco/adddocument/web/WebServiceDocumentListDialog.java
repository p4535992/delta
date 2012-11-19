package ee.webmedia.alfresco.adddocument.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddDocumentService;

import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;

/**
 * @author Riina Tens
 */
public class WebServiceDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getAddDocumentService().getAllDocumentFromWebService();
    }

    @Override
    public String getListTitle() {
        return getAddDocumentService().getWebServiceDocumentsListTitle();
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/adddocument/web/web-service-document-list-dialog-columns.jsp";
    }

    @Override
    public boolean isShowComplienceDateColumn() {
        return false;
    }

    @Override
    public String getInitialSortColumn() {
        return "created";
    }

    @Override
    public String getContainerTitle() {
        return getAddDocumentService().getWebServiceDocumentsListTitle();
    }

}
