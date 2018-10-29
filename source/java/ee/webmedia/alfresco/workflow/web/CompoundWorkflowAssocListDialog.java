package ee.webmedia.alfresco.workflow.web;

import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;

public class CompoundWorkflowAssocListDialog extends BaseDocumentListDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowAssocListDialog";

    private CompoundWorkflow compoundWorkflow;
    // this list is used only for not saved compound workflow
    private List<NodeRef> newAssocs;
    private boolean resetNewAssocs = true;
    private List<Document> documents;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        newAssocs = null;
    }

    public void setup(CompoundWorkflow compoundWorkflow) {
        this.compoundWorkflow = compoundWorkflow;
        restored(resetNewAssocs);
        resetNewAssocs = true;
    }

    @Override
    public void restored() {
        restored(true);
    }

    public void restored(boolean resetNewAssocs) {
        if ((compoundWorkflow != null && compoundWorkflow.isSaved()) || resetNewAssocs) {
            newAssocs = null;
        }
        if (compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow()) {
            List<Document> currentDocuments = documents;
            if (compoundWorkflow.isSaved()) {
                documents = BeanHelper.getWorkflowService().getCompoundWorkflowDocuments(compoundWorkflow.getNodeRef());
                if (!resetNewAssocs) {
                	Document firstDoc = documents.get(0);
                	if(!firstDoc.getDocumentTypeName().equals("Sissetulev kiri")  && documents.size() == 1){
                		firstDoc.setDocumentToSign(Boolean.TRUE);
                    }
                    for (Document document : documents) {
                        updateDocumentMarking(currentDocuments, document);
                    }
                }
            } else {
                documents = new ArrayList<Document>();
                if (newAssocs != null) {
                    for (NodeRef docRef : newAssocs) {
                        Document document = new Document(docRef);
                        if(!document.getDocumentTypeName().equals("Sissetulev kiri")  && newAssocs.size() == 1){
                        	document.setDocumentToSign(Boolean.TRUE);
                        }
                        documents.add(document);
                        updateDocumentMarking(currentDocuments, document);
                    }
                }
            }
            String mainDocumentRefId = compoundWorkflow.getMainDocument() != null ? compoundWorkflow.getMainDocument().getId() : null;
            List<String> documentsToSignRefId = compoundWorkflow.getDocumentsToSignNodeRefIds();
            for (Document document : documents) {
                String nodeRefId = document.getNodeRef().getId();
                if ((resetNewAssocs || documents.size() == 1) && nodeRefId.equals(mainDocumentRefId)) {
                    document.setMainDocument(Boolean.TRUE);
                }
                if (resetNewAssocs && documentsToSignRefId.contains(nodeRefId)) {
                    document.setDocumentToSign(Boolean.TRUE);
                }
                if (document.getNode().hasPermission(Privilege.VIEW_DOCUMENT_META_DATA)) {
                    document.setShowLink(true);
                }
            }
        } else {
            documents = new ArrayList<Document>();
        }
        Collections.sort(documents, new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                return o1 == null || o2 == null || o2.getCreated() == null || o1.getCreated() == null
                        ? -1 : o2.getCreated().compareTo(o1.getCreated());
            }
        });
    }

    public List<Document> getDocumentList() {
        return documents;
    }

    public void updateDocumentMarking(List<Document> currentDocuments, Document document) {
        Document currentDocument = getDocument(document.getNodeRef(), currentDocuments);
        if (currentDocument != null) {
            document.setMainDocument(currentDocument.getMainDocument());
            document.setDocumentToSign(currentDocument.getDocumentToSign());
        }
    }

    private Document getDocument(NodeRef nodeRef, List<Document> documents) {
        if (documents == null) {
            return null;
        }
        for (Document document : documents) {
            if (document.getNodeRef().equals(nodeRef)) {
                return document;
            }
        }
        return null;
    }

    @Override
    public String cancel() {
        newAssocs = null;
        return super.cancel();
    }

    public void removeUnsavedAssoc(NodeRef docRef) {
        if (newAssocs != null) {
            newAssocs.remove(docRef);
        }
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("compoundWorkflow_object_list");
    }

    public NodeRef getWorkflowRef() {
        return compoundWorkflow != null ? compoundWorkflow.getNodeRef() : null;
    }

    public String getAssocType() {
        return DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT.toString();
    }

    public boolean isDisableSignSelect() {
        return compoundWorkflow.isStatus(Status.FINISHED) || !(BeanHelper.getCompoundWorkflowDialog().isOwnerOrDocManager() || containsSignatureTaskAssignedToRunAsUser());
    }

    public boolean isDisableDocSelect() {
        return (documents != null && documents.size() <= 1) || isDisableSignSelect();
    }

    private boolean containsSignatureTaskAssignedToRunAsUser() {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        for (Workflow flow : compoundWorkflow.getWorkflows()) {
            for (Task task : flow.getTasks()) {
                if (Status.IN_PROGRESS.equals(task.getStatus()) && WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(task.getType()) && runAsUser.equals(task.getOwnerId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<NodeRef> getNewAssocs() {
        if (newAssocs == null) {
            newAssocs = new ArrayList<NodeRef>();
        }
        return newAssocs;
    }

    public void setResetNewAssocs(boolean resetNewAssocs) {
        this.resetNewAssocs = resetNewAssocs;
    }

}
