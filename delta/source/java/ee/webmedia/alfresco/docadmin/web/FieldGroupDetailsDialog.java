package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.commitToMetadataContainer;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDuplicateFieldIds;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/** FIXME DLSeadist based on {@link DocTypeDetailsDialog} */
public class FieldGroupDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "FieldGroupDetailsDialog";
    private static final Set<String> DEFAULT_USER_LOGGED_IN_VISIBLE_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            SystematicFieldGroupNames.USER, SystematicFieldGroupNames.SIGNER, SystematicFieldGroupNames.DOCUMENT_OWNER)));

    private FieldsListBean fieldsListBean;

    // START: fields that should be reset
    private DocumentTypeVersion parentDocTypeVersion;
    private FieldGroup fieldGroup;

    // END: fields that should be reset

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            // Don't persist changes to repository - field should be changed when parent documentType is changed
            commitToMetadataContainer(fieldGroup, parentDocTypeVersion);
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
                MessageUtil.addErrorMessage("fieldGroup_details_error_duplicateFields", TextUtil.collectionToString(duplicateFieldIds));
            }
        }
        return valid;
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

    /** used by jsp */
    public Node getCurrentNode() {
        return fieldGroup.getNode();
    }

    /** used by jsp, propertySheet */
    public FieldGroup getFieldGroup() {
        return fieldGroup;
    }

    /** used by propertySheet */
    public boolean isDefaultUserLoggedInVisible() {
        return fieldGroup.isSystematic() && DEFAULT_USER_LOGGED_IN_VISIBLE_NAMES.contains(fieldGroup.getName());
    }

    /** used by propertySheet */
    public boolean isThesaurusVisible() {
        return fieldGroup.isSystematic() && SystematicFieldGroupNames.THESAURUI.equals(fieldGroup.getName());
    }

    /** used by propertySheet */
    public boolean isReadOnlyInfoVisible() {
        return !fieldGroup.isSystematic() || fieldGroup.isReadonlyFieldsNameChangeable() || fieldGroup.isReadonlyFieldsRuleChangeable();
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return !fieldGroup.isSystematic();
    }

    /** injected by spring */
    public void setFieldsListBean(FieldsListBean fieldsListBean) {
        this.fieldsListBean = fieldsListBean;
    }

    // END: jsf actions/accessors
}
