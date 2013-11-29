package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public class CompoundWorkflowDefinition extends CompoundWorkflow {
    private static final long serialVersionUID = 1L;
    private static final QName USER_FULL_NAME = RepoUtil.createTransientProp("userFullName");

    protected CompoundWorkflowDefinition(WmNode node, NodeRef parent) {
        super(node, parent);
    }

    @Override
    public CompoundWorkflowDefinition copy() {
        return copyImpl(new CompoundWorkflowDefinition(getNode().clone(), getParent()));
    }

    public String getName() {
        return getProp(WorkflowCommonModel.Props.NAME);
    }

    public String getNameAndUserFullName() {
        String name = getName();
        String userFullName = getUserFullName();
        if (StringUtils.isNotBlank(userFullName)) {
            name += " (" + userFullName + ")";
        }
        return name;
    }

    public void setName(String name) {
        setProp(WorkflowCommonModel.Props.NAME, name);
    }

    public List<String> getDocumentTypes() {
        return getPropList(WorkflowCommonModel.Props.DOCUMENT_TYPES);
    }

    public void setDocumentTypes(List<String> documentTypes) {
        setPropList(WorkflowCommonModel.Props.DOCUMENT_TYPES, documentTypes);
    }

    public List<String> getCaseFileTypes() {
        return getPropList(WorkflowCommonModel.Props.CASE_FILE_TYPES);
    }

    public String getUserId() {
        return getProp(WorkflowCommonModel.Props.USER_ID);
    }

    public void setUserId(String userId) {
        setProp(WorkflowCommonModel.Props.USER_ID, userId);
    }

    @Override
    protected String additionalToString() {
        return super.additionalToString() + "\n  name=" + getName() + "\n  documentTypes=" + WmNode.toString(getDocumentTypes());
    }

    public void setUserFullName(String userFullName) {
        setProp(USER_FULL_NAME, userFullName);
    }

    /** It is not quaranteed that userFullName is retrieved, even if valid userId exists */
    public String getUserFullName() {
        return getProp(USER_FULL_NAME);
    }

}
