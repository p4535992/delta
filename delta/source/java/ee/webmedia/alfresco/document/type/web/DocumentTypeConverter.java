package ee.webmedia.alfresco.document.type.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;

public class DocumentTypeConverter extends MultiSelectConverterBase {
    private transient DocumentAdminService documentAdminService;

    @Override
    public String convertSelectedValueToString(Object value) {
        String docTypeId = (String) value;
        if (StringUtils.isBlank(docTypeId)) {
            return "";
        }
        NodeRef docTypeRef = getDocumentTypeService().getDocumentTypeRef(docTypeId);
        if (docTypeRef == null) {
            return value.toString();
        }
        return getDocumentTypeService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.NAME, String.class);
    }

    // START: getters / setters
    public void setDocumentTypeService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    private DocumentAdminService getDocumentTypeService() {
        if (documentAdminService == null) {
            documentAdminService = (DocumentAdminService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentAdminService.BEAN_NAME);
        }
        return documentAdminService;
    }
    // END: getters / setters
}
