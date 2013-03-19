package ee.webmedia.alfresco.document.type.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Form backing bean for DocumentType details view
 * 
 * @author Ats Uiboupin
 */
public class DocumentTypeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentTypeDetailsDialog";
    private static final String CURRENT_DOC_TYPE_ID = "currentDocTypeId";

    private DocumentType docType;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            BeanHelper.getDocumentTypeService().saveOrUpdateDocumentType(docType);
            resetFields();
            MessageUtil.addInfoMessage("save_success");
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    private boolean validate() {
        boolean valid = true;
        // tmpId is by now already validated using JSF validator
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

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        resetFields();
        String currentDocTypeId = ActionUtil.getParam(event, CURRENT_DOC_TYPE_ID);
        docType = BeanHelper.getDocumentTypeService().getDocumentType(currentDocTypeId);
        docType.setTmpId(docType.getId().getLocalName());
    }

    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        resetFields();
        docType = BeanHelper.getDocumentTypeService().createNewUnSavedDocumentType();
    }

    public Node getCurrentNode() {
        return docType.getNode();
    }

    public boolean isSaved() {
        return docType.getId() != null;
    }

    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(docType.getSystematicComment());
    }

    public DocumentType getDocType() {
        return docType;
    }

    // END: jsf actions/accessors

}
