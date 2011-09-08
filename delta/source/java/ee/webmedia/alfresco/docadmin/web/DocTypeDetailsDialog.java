package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Details of document type
 * 
 * @author Ats Uiboupin
 */
public class DocTypeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    // START: Block beans
    //@formatter:off
    private FieldsListBean fieldsListBean;
    // TODO:
    //      FieldsListBean->FieldDetailsDialog        - andmevälja/andmeväljade grupi detailvaate
    //      FieldsListBean->FieldGroupDetailsDialog   - andmevälja/andmeväljade grupi detailvaate
    //          -> 
    //DocTypeFollowupAssocsBean                            - "Lubatud järgseosed"
    //DocTypeReplyAssocsBean                               - "Lubatud vastusseosed"
    //DocTypeVersionsBean                                  - "Versioonid"
    //@formatter:on

    // END: Block beans

    private DocumentType docType;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            getDocumentAdminService().saveOrUpdateDocumentType(docType);
            // resetFields();
            MessageUtil.addInfoMessage("save_success");
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    private boolean validate() {
        boolean valid = true;
        // TODO DLSeadist validation. (DocumentTypeId is already validated by converter)
        return valid;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    private void resetFields() {
        docType = null;
    }

    @Override
    public Object getActionsContext() {
        return docType;
    }

    // START: jsf actions/accessors
    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        resetFields();
        setDocType(getDocumentAdminService().createNewUnSaved());
    }

    public void showDetails(ActionEvent event) {
        resetFields();
        NodeRef docTypeRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        setDocType(getDocumentAdminService().getDocumentType(docTypeRef));
    }

    /** used by delete action to do actual deleting (after user has confirmed deleting in DeleteDialog) */
    public String deleteDocType(ActionEvent event) {
        NodeRef docTypeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentAdminService().deleteDocumentType(docTypeRef);
        return getCloseOutcome(2);
    }

    private void setDocType(DocumentType documentType) {
        docType = documentType;
        docType.addNewLatestDocumentTypeVersion();
        fieldsListBean.init(docType.getLatestDocumentTypeVersion());
    }

    /** used by jsp propertySheetGrid */
    public Node getCurrentNode() {
        return docType.getNode();
    }

    /** used by web-client propertySheet */
    public boolean isSaved() {
        return docType.isSaved();
    }

    /** used by jsp */
    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(docType.getSystematicComment());
    }

    /** used by jsp */
    public DocumentType getDocType() {
        return docType;
    }

    /** used by jsp */
    public FieldsListBean getFieldsListBean() {
        return fieldsListBean;
    }

    /** injected by spring */
    public void setFieldsListBean(FieldsListBean fieldsListBean) {
        this.fieldsListBean = fieldsListBean;
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return true;
    }
    // END: jsf actions/accessors
}
