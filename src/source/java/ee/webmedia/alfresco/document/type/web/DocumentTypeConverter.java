package ee.webmedia.alfresco.document.type.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.namespace.QName;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;

public class DocumentTypeConverter extends MultiSelectConverterBase {
    private transient DocumentTypeService documentTypeService;

    @Override
    protected String convertSelectedValueToString(Object value) {
        final DocumentType documentType = getDocumentTypeService().getDocumentType((QName) value);
        if (documentType == null) {
            return value.toString();
        }
        return documentType.getName();
    }

    // START: getters / setters
    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    private DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = (DocumentTypeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentTypeService.BEAN_NAME);
        }
        return documentTypeService;
    }
    // END: getters / setters
}
