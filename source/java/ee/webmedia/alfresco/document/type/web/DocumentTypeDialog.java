package ee.webmedia.alfresco.document.type.web;

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
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentTypeDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient DocumentTypeService documentTypeService;

    private List<DocumentType> documentTypes;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        documentTypes = getDocumentTypeService().getAllDocumentTypes();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getDocumentTypeService().updateDocumentTypes(documentTypes);
        documentTypes = null;
        MessageUtil.addInfoMessage("save_success");
        // We need to stay on the same dialog
        return "dialog:close:dialog:documentTypeDialog";
    }

    @Override
    public String cancel() {
        documentTypes = null;
        return super.cancel();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
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
        final List<DocumentType> usedDocTypes = getDocumentTypeService().getAllDocumentTypes(true);
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
                results.add(new SelectItem(documentType.getId().toString(), name));
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
                return AppConstants.DEFAULT_COLLATOR.compare(a.getLabel(), b.getLabel());
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

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = (DocumentTypeService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentTypeService.BEAN_NAME);
        }
        return documentTypeService;
    }
    // END: getters / setters

}
