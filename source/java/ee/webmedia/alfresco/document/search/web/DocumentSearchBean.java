<<<<<<< HEAD
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

    // this is used as value list for multiselect, so initial value null would generate error
    private List<SelectItem> documentTypes = new ArrayList<SelectItem>();

    public void reset() {
        // this is used as value list for multiselect, so initial value null would generate error
        documentTypes = new ArrayList<SelectItem>();
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
        if (documentTypes == null || documentTypes.isEmpty()) {
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
