package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Bean used to create values for common components for searching documents, such as select containing used documentTypes
 */
public class DocumentSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<SelectItem> documentTypes;
    private transient DocumentTypeService documentTypeService;

    public void reset() {
        documentTypes = null;
    }

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<SelectItem> getDocumentTypes(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return getDocumentTypes();
    }

    // START: getters / setters

    public List<SelectItem> getDocumentTypes() {
        if (documentTypes == null) {
            List<DocumentType> types = getDocumentTypeService().getAllDocumentTypes(true);
            documentTypes = new ArrayList<SelectItem>(types.size());
            for (DocumentType documentType : types) {
                documentTypes.add(new SelectItem(documentType.getId(), documentType.getName()));
            }
            WebUtil.sort(documentTypes);
        }
        return documentTypes;
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
