package ee.webmedia.alfresco.document.type.web;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;

public class DocumentTypeConverter extends MultiSelectConverterBase {
    private transient DocumentAdminService documentAdminService;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        return null;
    }

    @Override
    public String convertSelectedValueToString(Object value) {
        String docTypeId = (String) value;
        if (StringUtils.isBlank(docTypeId)) {
            return "";
        }
        NodeRef docTypeRef = getTypeNodeRef(docTypeId);
        if (docTypeRef == null) {
            return value.toString();
        }
        return getConvertedValue(docTypeId);
    }

    protected String getConvertedValue(String docTypeId) {
        return getDocumentTypeService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.NAME, String.class);
    }

    protected NodeRef getTypeNodeRef(String docTypeId) {
        return getDocumentTypeService().getDocumentTypeRef(docTypeId);
    }

    // START: getters / setters
    public void setDocumentTypeService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    protected DocumentAdminService getDocumentTypeService() {
        if (documentAdminService == null) {
            documentAdminService = (DocumentAdminService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentAdminService.BEAN_NAME);
        }
        return documentAdminService;
    }
    // END: getters / setters
}