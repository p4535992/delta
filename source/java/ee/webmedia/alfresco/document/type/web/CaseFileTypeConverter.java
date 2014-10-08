<<<<<<< HEAD
package ee.webmedia.alfresco.document.type.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * @author Riina Tens
 */
public class CaseFileTypeConverter extends DocumentTypeConverter {

    @Override
    protected String getConvertedValue(String typeId) {
        return getDocumentTypeService().getCaseFileTypeProperty(typeId, DocumentAdminModel.Props.NAME, String.class);
    }

    @Override
    protected NodeRef getTypeNodeRef(String typeId) {
        return getDocumentTypeService().getCaseFileTypeRef(typeId);
    }

}
=======
package ee.webmedia.alfresco.document.type.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class CaseFileTypeConverter extends DocumentTypeConverter {

    @Override
    protected String getConvertedValue(String typeId) {
        return getDocumentTypeService().getCaseFileTypeProperty(typeId, DocumentAdminModel.Props.NAME, String.class);
    }

    @Override
    protected NodeRef getTypeNodeRef(String typeId) {
        return getDocumentTypeService().getCaseFileTypeRef(typeId);
    }

}
>>>>>>> develop-5.1
