package ee.webmedia.alfresco.document.service.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * Performs operations on documents based on events related to workflow status changes
 * 
 * @author Alar Kvell
 */
public class DocumentWorkflowStatusEventListener implements WorkflowEventListener, InitializingBean {

    private WorkflowService workflowService;
    private NodeService nodeService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event, WorkflowEventQueue queue) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof CompoundWorkflow) {
            CompoundWorkflow cWorkflow = (CompoundWorkflow) object;
            if (event.getType().equals(WorkflowEventType.CREATED) || event.getType().equals(WorkflowEventType.UPDATED)) {
                workflowService.updateCompWorkflowDocsSearchProps(cWorkflow);
            }
            if (event.getType().equals(WorkflowEventType.STATUS_CHANGED) && cWorkflow.isCaseFileWorkflow() && cWorkflow.isStatus(Status.FINISHED)) {
                NodeRef caseFileRef = cWorkflow.getParent();
                Map<QName, Serializable> caseFileProps = nodeService.getProperties(caseFileRef);
                if (caseFileProps.get(DocumentDynamicModel.Props.WORKFLOW_END_DATE) == null) {
                    Pair<CaseFileType, DocumentTypeVersion> typeAndVersion = BeanHelper.getDocumentAdminService().getCaseFileTypeAndVersion(
                            (String) caseFileProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID),
                            (Integer) caseFileProps.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR));
                    Collection<Field> fields = typeAndVersion.getSecond().getFieldsById(Collections.singleton(DocumentDynamicModel.Props.WORKFLOW_END_DATE.getLocalName()));
                    if (!fields.isEmpty()) {
                        nodeService.setProperty(caseFileRef, DocumentDynamicModel.Props.WORKFLOW_END_DATE, new Date());
                    }
                }
            }
        }
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters/setters

}
