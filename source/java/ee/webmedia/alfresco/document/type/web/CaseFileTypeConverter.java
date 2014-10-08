package ee.webmedia.alfresco.document.type.web;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class CaseFileTypeConverter extends DocumentTypeConverter {

    @Override
    protected String getConvertedValue(String typeId) {
        return getDocumentAdminService().getCaseFileTypeProperty(typeId, DocumentAdminModel.Props.NAME, String.class);
    }

}