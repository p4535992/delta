package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for editing objects of type {@link AssociationModel}
 * 
 * @author Ats Uiboupin
 */
public class AssociationModelDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AssociationModelDetailsDialog";
    private AssociationModel associationModel;
    private DocumentType documentType;
    private FieldMappingsListBean fieldMappingsListBean;

    void init(AssociationModel assocModel) {
        resetFields();
        associationModel = assocModel;
        documentType = assocModel.getParent();
        fieldMappingsListBean.init(assocModel);
    }

    @Override
    public String cancel() {
        resetFields();
        BeanHelper.getDocTypeDetailsDialog().refreshDocType(); // docType object might be screwed up because of errors that might be shown
        return super.cancel();
    }

    private void resetFields() {
        fieldMappingsListBean.reset();
        documentType = null;
        associationModel = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            fieldMappingsListBean.save();
            associationModel = getDocumentAdminService().saveOrUpdateAssocToDocType(associationModel);
            // replace docType in memory with fresh copy from repo
            // so that added assocs would be visible when navigating back
            BeanHelper.getDocTypeDetailsDialog().refreshDocType();
            MessageUtil.addInfoMessage("save_success");
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    private boolean validate() {
        boolean valid = true;
        // associationModel.docType is validated by GUI
        valid &= fieldMappingsListBean.validate();
        return valid;
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("associationModel_details_title_" + associationModel.getAssociationType());
    }

    public AssociationModel getAssociationModel() {
        return associationModel;
    }

    /** used by property sheet */
    public List<SelectItem> getDocTypes(FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        List<DocumentType> docTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        List<SelectItem> results = new ArrayList<SelectItem>(docTypes.size() + 1);
        Set<String> associatedDocTypes = new HashSet<String>();
        DocTypeAssocType associationTypeEnum = associationModel.getAssociationType();
        for (AssociationModel assocModel : documentType.getAssociationModels(associationTypeEnum)) {
            associatedDocTypes.add(assocModel.getDocType());
        }
        for (DocumentType docType : docTypes) {
            if (associatedDocTypes.contains(docType.getId())) {
                continue;
            }
            SelectItem selectItem = new SelectItem(docType.getId(), docType.getNameAndId());
            results.add(selectItem);
        }

        Collections.sort(results, new Comparator<SelectItem>() {

            @Override
            public int compare(SelectItem o1, SelectItem o2) {
                return AppConstants.DEFAULT_COLLATOR.compare(o1.getLabel(), o2.getLabel());
            }

        });
        addDefault(results, context);
        return results;
    }

    public void setFieldMappingsListBean(FieldMappingsListBean fieldMappingsListBean) {
        this.fieldMappingsListBean = fieldMappingsListBean;
    }

    /** used by property sheet */
    public void docTypeChanged(ValueChangeEvent e) {
        String newRelatedDocType = (String) e.getNewValue();
        associationModel.setDocType(newRelatedDocType); // property is needs to be manually updated to be used before request values are applied
        fieldMappingsListBean.init(associationModel);
    }
}
