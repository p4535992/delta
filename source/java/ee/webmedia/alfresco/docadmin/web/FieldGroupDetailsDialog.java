package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.commitToMetadataContainer;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDuplicateFieldIds;

import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Details dialog for editing {@link FieldGroup}
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class FieldGroupDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "FieldGroupDetailsDialog";

    private FieldsListBean fieldsListBean;

    // START: fields that should be reset
    private DocumentTypeVersion parentDocTypeVersion;
    private FieldGroup fieldGroup;

    // END: fields that should be reset

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            fieldsListBean.doReorder();
            // Don't persist changes to repository - field should be changed when parent documentType is changed
            commitToMetadataContainer(fieldGroup, parentDocTypeVersion, getDynTypeName());
            resetFields();
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    private boolean validate() {
        boolean valid = true;
        if (fieldGroup.getMetadata().isEmpty()) {
            throw new UnableToPerformException("fieldGroup_details_error_noFields");
        }
        { // validate that fields under fieldGroup are not already added to parentDocTypeVersion
            Set<String> duplicateFieldIds = getDuplicateFieldIds(fieldGroup.getFields(), parentDocTypeVersion);
            if (!duplicateFieldIds.isEmpty()) {
                valid = false;
                MessageUtil.addErrorMessage("fieldGroup_details_error_duplicateFieldsIn" + getDynTypeName(), TextUtil.collectionToString(duplicateFieldIds));
            }
        }
        return valid;
    }

    private String getDynTypeName() {
        return fieldGroup.getParent().getParent().getClass().getSimpleName();
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("fieldOrFieldGroup_details_affirm_changes");
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return getDynamicTypeDetailsDialog().isShowingLatestVersion();
    }

    private void resetFields() {
        fieldGroup = null;
        parentDocTypeVersion = null;
    }

    // START: jsf actions/accessors

    void addNewFieldGroup(DocumentTypeVersion metadataContainer) {
        editFieldGroupInner(new FieldGroup(metadataContainer), metadataContainer);
    }

    void editFieldGroup(FieldGroup fGroup, DocumentTypeVersion fGroupParent) {
        editFieldGroupInner(fGroup.clone(), fGroupParent);
    }

    private void editFieldGroupInner(FieldGroup fGroup, DocumentTypeVersion fGroupParent) {
        parentDocTypeVersion = fGroupParent;
        fieldGroup = fGroup;
        fieldsListBean.init(fieldGroup);
    }

    /** used by jsp */
    public FieldsListBean getFieldsListBean() {
        return fieldsListBean;
    }

    /** used by jsp */
    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(fieldGroup.getSystematicComment());
    }

    public boolean isShowAddExistingField() {
        if (!fieldGroup.isSystematic()) {
            return true;
        }
        FieldGroup fieldGroupDefinition = getDocumentAdminService().getFieldGroupDefinition(fieldGroup.getName());
        if (fieldGroupDefinition.getFieldDefinitionIds().size() > fieldGroup.getFields().size()) {
            return true;
        }
        return false;
    }

    public boolean isShowShowInTwoColumns() {
        return getDocumentAdminService().isGroupShowShowInTwoColumns(fieldGroup);
    }

    /** used by jsp */
    public Node getCurrentNode() {
        return fieldGroup.getNode();
    }

    /** used by jsp, propertySheet */
    public FieldGroup getFieldGroup() {
        return fieldGroup;
    }

    /** used by propertySheet */
    public boolean isThesaurusVisible() {
        return fieldGroup.isSystematic() && SystematicFieldGroupNames.THESAURI.equals(fieldGroup.getName());
    }

    private boolean isReadOnlyInfoVisible() {
        return !fieldGroup.isSystematic() || fieldGroup.isReadonlyFieldsNameChangeable() || fieldGroup.isReadonlyFieldsRuleChangeable();
    }

    private boolean isCaseFile() {
        return parentDocTypeVersion.getParent() instanceof CaseFileType;
    }

    /** used by propertySheet */
    public boolean isCaseFileTypeReadOnlyInfoVisible() {
        return isCaseFile() && isReadOnlyInfoVisible();
    }

    /** used by propertySheet */
    public boolean isDocTypeReadOnlyInfoVisible() {
        return !isCaseFile() && isReadOnlyInfoVisible();
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return !fieldGroup.isSystematic();
    }

    /** JSP */
    public DynamicTypeDetailsDialog getDynamicTypeDetailsDialog() {
        Class<? extends DynamicType> dynTypeClass = parentDocTypeVersion.getParent().getClass();
        return BeanHelper.getDynamicTypeDetailsDialog(dynTypeClass);
    }

    /** injected by spring */
    public void setFieldsListBean(FieldsListBean fieldsListBean) {
        this.fieldsListBean = fieldsListBean;
    }

    // END: jsf actions/accessors
}
