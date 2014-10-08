package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.ui.common.component.PickerSearchParams;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
<<<<<<< HEAD

/**
 * @author Ats Uiboupin
 */
=======
import ee.webmedia.alfresco.utils.MessageUtil;

>>>>>>> develop-5.1
public class DocTypeListDialog extends DynamicTypeListDialog<DocumentType> {
    private static final long serialVersionUID = 1L;

    protected DocTypeListDialog() {
        super(DocumentType.class);
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchUsedDocTypes(PickerSearchParams params) {
        return searchUsedTypes(params, false);
    }

    /**
     * Used by the property sheet as a callback.
     */
    public List<SelectItem> getUsedDocTypes(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return Arrays.asList(searchUsedTypes(null, true));
    }

    @Override
    protected List<? extends DynamicType> loadUsedTypes() {
        return getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
    }

    @Override
    protected String getExportFileName() {
        return "documentTypes.xml";
    }
<<<<<<< HEAD
=======

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("doc_types_list");
    }

>>>>>>> develop-5.1
}
