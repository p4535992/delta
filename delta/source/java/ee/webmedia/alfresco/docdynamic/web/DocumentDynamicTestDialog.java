package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.data.UIRichList;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentDynamicTestDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicTestDialog.class);

    public static final String BEAN_NAME = "DocumentDynamicTestDialog";

    private List<DocumentDynamic> drafts;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    @Override
    public void restored() {
        LOG.info("Creating drafts document list");
        NodeRef draftsRoot = getDocumentService().getDrafts();
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(draftsRoot);
        drafts = new ArrayList<DocumentDynamic>(childAssocs.size());
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            NodeRef docRef = childAssociationRef.getChildRef();
            QName type = getNodeService().getType(docRef);
            if (DocumentDynamicModel.Types.DOCUMENT_DYNAMIC.equals(type)) {
                DocumentDynamic doc = getDocumentDynamicService().getDocument(docRef);
                drafts.add(doc);
            } else {
                LOG.info("Ignoring node under drafts, type=" + type.toPrefixString(getNamespaceService()));
            }
        }
        if (draftsList != null) {
            draftsList.setValue(null);
        }
    }

    public List<DocumentDynamic> getDrafts() {
        return drafts;
    }

    public void deleteDocument(ActionEvent event) {
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentDynamicService().deleteDocument(docRef);
        MessageUtil.addInfoMessage("document_delete_success");
        restored();
    }

    private transient UIRichList draftsList;

    public UIRichList getDraftsList() {
        return draftsList;
    }

    public void setDraftsList(UIRichList draftsList) {
        this.draftsList = draftsList;
    }

}
