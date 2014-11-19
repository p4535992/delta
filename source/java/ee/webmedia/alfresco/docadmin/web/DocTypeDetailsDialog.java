package ee.webmedia.alfresco.docadmin.web;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Details of document type.
 * To open this dialog just call actionListener. You must not set action attribute on actionLink that opens this dialog nor any other way perform navigation, as actionListener
 * handles navigation
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class DocTypeDetailsDialog extends DynamicTypeDetailsDialog<DocumentType, DocTypeDetailsDialog.DocTypeDialogSnapshot> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocTypeDetailsDialog";

    // START: Block beans
    private FollowupAssocsListBean followupAssocsListBean;
    private ReplyAssocsListBean replyAssocsListBean;
<<<<<<< HEAD
    private DefaultLocationBean defaultLocationBean;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public DocTypeDetailsDialog() {
        super(DocumentType.class);
    }

    /**
     * Contains fields that contain state to be used when restoring dialog
<<<<<<< HEAD
     * 
     * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
     */
    static class DocTypeDialogSnapshot extends DynamicTypeDetailsDialog.DynTypeDialogSnapshot<DocumentType> {
        private static final long serialVersionUID = 1L;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "docTypeDetailsDialog";
        }
    }

    @Override
    protected DocTypeDialogSnapshot newSnapshot() {
        return new DocTypeDialogSnapshot();
    }

    @Override
    protected void resetFields() {
        super.resetFields();
        followupAssocsListBean.resetOrInit(null);
        replyAssocsListBean.resetOrInit(null);
<<<<<<< HEAD
        defaultLocationBean.resetOrInit(null);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // don't assign null to injected beans
    }

    // START: jsf actions/accessors

    @Override
    protected void updateDialogState(DocumentType documentType, DocTypeDialogSnapshot currentSnapshot, NodeRef docTypeVersionRef) {
        super.updateDialogState(documentType, currentSnapshot, docTypeVersionRef);
        followupAssocsListBean.resetOrInit(this);
        replyAssocsListBean.resetOrInit(this);
<<<<<<< HEAD
        defaultLocationBean.resetOrInit(this);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    /** used by jsp */
    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(getCurrentSnapshot().getDynType().getSystematicComment());
    }

    /** used by jsp */
    public FollowupAssocsListBean getFollowupAssocsListBean() {
        return followupAssocsListBean;
    }

    /** injected by spring */
    public void setFollowupAssocsListBean(FollowupAssocsListBean followupAssocsListBean) {
        this.followupAssocsListBean = followupAssocsListBean;
    }

    /** used by jsp */
    public ReplyAssocsListBean getReplyAssocsListBean() {
        return replyAssocsListBean;
    }

    /** injected by spring */
    public void setReplyAssocsListBean(ReplyAssocsListBean replyAssocsListBean) {
        this.replyAssocsListBean = replyAssocsListBean;
    }

<<<<<<< HEAD
    /** used by jsp */
    public DefaultLocationBean getDefaultLocationBean() {
        return defaultLocationBean;
    }

    /** injected by spring */
    public void setDefaultLocationBean(DefaultLocationBean defaultLocationBean) {
        this.defaultLocationBean = defaultLocationBean;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    /** JSP */
    @Override
    public boolean isAddFieldVisible() {
        return true;
    }

    // END: jsf actions/accessors
}
