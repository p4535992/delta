package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Bean used to create values for common components for searching documents, such as select containing used documentTypes
 * 
 * @author Ats Uiboupin
 */
public class DocumentSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentSearchBean";

    private List<SelectItem> documentTypes;

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
            getDocumentTypeListItems();
        }
        return documentTypes;
    }

    public List<SelectItem> getDocumentTypeListItems() {
        List<DocumentType> types = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        documentTypes = new ArrayList<SelectItem>(types.size());
        for (DocumentType documentType : types) {
            documentTypes.add(new SelectItem(documentType.getId(), documentType.getName()));
        }
        WebUtil.sort(documentTypes);
        return documentTypes;
    }

    // END: getters / setters
}
