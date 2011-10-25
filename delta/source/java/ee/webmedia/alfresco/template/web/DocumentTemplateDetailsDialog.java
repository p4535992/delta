package ee.webmedia.alfresco.template.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Form backing bean for DocumentTemplate details view.
 * 
 * @author Vladimir Drozdik
 */
public class DocumentTemplateDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private Node docTemplNode;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getDocumentTemplateService().updateDocTemplate(docTemplNode);
        return outcome;
    }

    /**
     * Action listener for JSP pages
     * 
     * @param event ActionLink
     */
    public void setupDocTemplate(ActionEvent event) {
        NodeRef docTemplateNodeRef = ActionUtil.getParam(event, "docTemplateNodeRef", NodeRef.class);
        docTemplNode = BeanHelper.getGeneralService().fetchNode(docTemplateNodeRef);
    }

    public Node getCurrentNode() {
        return docTemplNode;
    }
}
