package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

<<<<<<< HEAD
import java.util.Arrays;
=======
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.ui.common.component.PickerSearchParams;
<<<<<<< HEAD

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;

/**
 * @author Ats Uiboupin
 */
=======
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        return searchUsedTypes(params, false);
=======
        return searchUsedDocTypes(params, false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    /**
     * Used by the property sheet as a callback.
     */
    public List<SelectItem> getUsedDocTypes(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
<<<<<<< HEAD
        return Arrays.asList(searchUsedTypes(null, true));
    }

    @Override
    protected List<? extends DynamicType> loadUsedTypes() {
        return getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
=======
        return Arrays.asList(searchUsedDocTypes(null, true));
    }

    private SelectItem[] searchUsedDocTypes(PickerSearchParams params, boolean addEmptyItem) {
        String substring = params == null ? null : params.getSearchString();
        final List<DocumentType> usedDocTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        substring = StringUtils.trimToNull(substring);
        substring = (substring != null ? substring.toLowerCase() : null);
        int size = addEmptyItem ? usedDocTypes.size() + 1 : usedDocTypes.size();
        final ArrayList<SelectItem> results = new ArrayList<SelectItem>(size);
        if (addEmptyItem) {
            results.add(new SelectItem("", ""));
        }
        for (DocumentType documentType : usedDocTypes) {
            final String name = documentType.getName();
            if (substring == null || name.toLowerCase().contains(substring)) {
                results.add(new SelectItem(documentType.getId(), name));
            }
            if (params != null && results.size() == (params.getLimit() + (addEmptyItem ? 1 : 0))) {
                break;
            }
        }
        SelectItem[] resultArray = new SelectItem[results.size()];
        int i = 0;
        for (SelectItem selectItem : results) {
            resultArray[i++] = selectItem;
        }

        Arrays.sort(resultArray, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem a, SelectItem b) {
                return a.getLabel().compareTo(b.getLabel());
            }
        });
        return resultArray;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    protected String getExportFileName() {
        return "documentTypes.xml";
    }
}
