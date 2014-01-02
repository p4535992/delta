package ee.webmedia.alfresco.document.sendout.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;

/**
 * @author Erko Hansar
 */
public class SendOutBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SendOutBlockBean";

    private transient SendOutService sendOutService;

    private NodeRef document;
    private List<SendInfo> sendInfos;

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getNode());
        }
    }

    public void init(Node node) {
        reset();
        document = node.getNodeRef();
    }

    public void restore() {
        sendInfos = null;
    }

    public void reset() {
        document = null;
        sendInfos = null;
    }

    public boolean isRendered() {
        return !BeanHelper.getDocumentDialogHelperBean().isInEditMode() && getSendInfos() != null && getSendInfos().size() > 0;
    }

    // START: getters / setters

    public List<SendInfo> getSendInfos() {
        if (sendInfos == null && document != null) {
            sendInfos = getSendOutService().getDocumentAndTaskSendInfos(document, BeanHelper.getWorkflowBlockBean().getCompoundWorkflows());
        }
        return sendInfos;
    }

    public SendOutService getSendOutService() {
        if (sendOutService == null) {
            sendOutService = (SendOutService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(SendOutService.BEAN_NAME);
        }
        return sendOutService;
    }

    // END: getters / setters

}
