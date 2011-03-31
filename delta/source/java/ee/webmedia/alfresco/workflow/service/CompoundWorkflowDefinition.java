package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public class CompoundWorkflowDefinition extends CompoundWorkflow {
    private static final long serialVersionUID = 1L;

    protected CompoundWorkflowDefinition(WmNode node, NodeRef parent) {
        super(node, parent);
    }

    @Override
    protected CompoundWorkflowDefinition copy() {
        return copyImpl(new CompoundWorkflowDefinition(getNode().copy(), getParent()));
    }

    public String getName() {
        return getProp(WorkflowCommonModel.Props.NAME);
    }

    public void setName(String name) {
        setProp(WorkflowCommonModel.Props.OWNER_ID, name);
    }

    public List<QName> getDocumentTypes() {
        return getPropList(WorkflowCommonModel.Props.DOCUMENT_TYPES);
    }

    public void setDocumentTypes(List<QName> documentTypes) {
        setPropList(WorkflowCommonModel.Props.DOCUMENT_TYPES, documentTypes);
    }

    public String getUserId() {
        return getProp(WorkflowCommonModel.Props.USER_ID);
    }

    @Override
    protected String additionalToString() {
        return super.additionalToString() + "\n  name=" + getName() + "\n  documentTypes=" + WmNode.toString(getDocumentTypes());
    }

}
