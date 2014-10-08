package ee.webmedia.alfresco.adddocument.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddDocumentService;

import java.util.HashSet;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;

public class WebServiceDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    private static final Set<QName> WEB_SERVICE_DOC_PROPS_TO_LOAD = new HashSet<>();
    static {
        WEB_SERVICE_DOC_PROPS_TO_LOAD.add(ContentModel.PROP_CREATED);
        WEB_SERVICE_DOC_PROPS_TO_LOAD.addAll(DOC_PROPS_TO_LOAD);
    }

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getAddDocumentService().getAllDocumentFromWebService(), false, WEB_SERVICE_DOC_PROPS_TO_LOAD);
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
    public String getContainerTitle() {
        return getAddDocumentService().getWebServiceDocumentsListTitle();
    }

}
