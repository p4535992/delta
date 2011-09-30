package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * @author Ats Uiboupin
 */
public class DocTypeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    // private transient DocumentTypeService documentTypeService;

    private List<DocumentType> documentTypes;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initDocumentTypes();
    }

    private void initDocumentTypes() {
        documentTypes = getDocumentAdminService().getDocumentTypes();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        documentTypes = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        initDocumentTypes();
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param substring Text from the search textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchUsedDocTypes(int filterIndex, String substring) {
        return searchUsedDocTypes(substring, false);
    }

    /**
     * Used by the property sheet as a callback.
     */
    public List<SelectItem> getUsedDocTypes(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return Arrays.asList(searchUsedDocTypes(null, true));
    }

    private SelectItem[] searchUsedDocTypes(String substring, boolean addEmptyItem) {
        final List<DocumentType> usedDocTypes = getDocumentAdminService().getDocumentTypes(true);
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
                results.add(new SelectItem(documentType.getDocumentTypeId(), name));
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
    }

    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<DocumentType> getDocumentTypes() {
        return documentTypes;
    }

    // END: getters / setters

}
