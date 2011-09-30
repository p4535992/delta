package ee.webmedia.alfresco.document.type.web;

import javax.faces.context.FacesContext;

import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

public class DocumentTypeConverter extends MultiSelectConverterBase {
    private transient DocumentAdminService documentAdminService;

    @Override
    public String convertSelectedValueToString(Object value) {
        final DocumentType documentType = getDocumentTypeService().getDocumentType((String) value);
        if (documentType == null) {
            return value.toString();
        }
        return documentType.getName();
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
