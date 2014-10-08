package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.List;

import javax.faces.model.SelectItem;

import org.alfresco.web.ui.common.component.PickerSearchParams;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
<<<<<<< HEAD

/**
 * @author Ats Uiboupin
 */
=======
import ee.webmedia.alfresco.utils.MessageUtil;

>>>>>>> develop-5.1
public class CaseFileTypeListDialog extends DynamicTypeListDialog<CaseFileType> {
    private static final long serialVersionUID = 1L;

    protected CaseFileTypeListDialog() {
        super(CaseFileType.class);
    }

    @Override
    protected String getExportFileName() {
        return "caseFileTypes.xml";
    }

    /**
     * Query callback method executed by the Generic Picker component.
     */
    public SelectItem[] searchUsedCaseFileTypes(PickerSearchParams params) {
        return searchUsedTypes(params, false);
    }

    @Override
    protected List<? extends DynamicType> loadUsedTypes() {
        return getDocumentAdminService().getUsedCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
    }

<<<<<<< HEAD
=======
    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("caseFileTypes");
    }

>>>>>>> develop-5.1
}
