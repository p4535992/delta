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
    public static final String BEAN_NAME = "DocTypeDetailsDialog"; // FIXME DLSeadist final

    // START: Block beans
    //@formatter:off
    private FieldsListBean fieldsListBean;
    private DocTypeAssocsListBean docTypeAssocsListBean;
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
    protected String finishImpl(FacesContext context, String outcome) {
        if (validate()) {
            save(false);
            MessageUtil.addInfoMessage("save_success");
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    void save(boolean reInit) {
        DocumentType saveOrUpdateDocumentType = getDocumentAdminService().saveOrUpdateDocumentType(docType);
        if (reInit) {
            init(saveOrUpdateDocumentType, true);
        }
    }

    boolean validate() {
        boolean valid = true;
        // TODO DLSeadist validation. (DocumentTypeId is already validated by converter)
        return valid;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public void restored() {
        init(docType, false);
    }

    private void resetFields() {
        docType = null;
        docTypeAssocsListBean.reset();
        fieldsListBean.resetFields();
        // don't assign null to injected beans
    }

    @Override
    public Object getActionsContext() {
        return docType;
    }

    // START: jsf actions/accessors
    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        init(getDocumentAdminService().createNewUnSaved(), true);
    }

    /** used by delete action to do actual deleting (after user has confirmed deleting in DeleteDialog) */
    public String deleteDocType(ActionEvent event) {
        NodeRef docTypeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentAdminService().deleteDocumentType(docTypeRef);
        return getCloseOutcome(2);
    }

    public void showDetails(ActionEvent event) {
        init(ActionUtil.getParam(event, "nodeRef", NodeRef.class));
    }

    void init(NodeRef docTypeRef) {
        init(getDocumentAdminService().getDocumentType(docTypeRef), true);
    }

    void init(DocumentType documentType, boolean addNewLatestDocumentTypeVersion) {
        resetFields();
        docType = documentType;
        if (addNewLatestDocumentTypeVersion) {
            docType.addNewLatestDocumentTypeVersion();
        }
        fieldsListBean.init(docType.getLatestDocumentTypeVersion());
        docTypeAssocsListBean.init(this);
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

    /** used by jsp */
    public DocTypeAssocsListBean getAssocsListBean() {
        return docTypeAssocsListBean;
    }

    /** injected by spring */
    public void setAssocsListBean(DocTypeAssocsListBean docTypeAssocsListBean) {
        this.docTypeAssocsListBean = docTypeAssocsListBean;
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return true;
    }
    // END: jsf actions/accessors
}
