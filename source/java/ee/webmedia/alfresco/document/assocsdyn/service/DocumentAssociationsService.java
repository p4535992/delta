package ee.webmedia.alfresco.document.assocsdyn.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;

/**
 * Service class that deals with creating and deleting associations between documents
 */
public interface DocumentAssociationsService {
    String BEAN_NAME = "DocumentAssociationsService";

    List<AssociationModel> getAssocs(String documentTypeId, QName typeQNamePattern);

    /**
     * Create new document based on <code>baseDocRef</code>. <br>
     * Properties to be copied from <code>baseDocRef</code> and document type is defined by {@link AssociationModel} object referenced using <code>assocModelRef</code>
     * 
     * @param baseDocRef
     * @param assocModelRef
     * @return new Document that is saved into drafts folder, properties that are copied from <code>baseDocRef</code> are not saved (just stored in memory)
     */
    Pair<DocumentDynamic, AssociationModel> createAssociatedDocFromModel(NodeRef baseDocRef, NodeRef assocModelRef);

    void createAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName);

    /**
     * Deletes association between nodes
     * 
     * @param sourceNodeRef
     * @param targetNodeRef
     * @param assocQName if null, defaults to <code>DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT</code>
     */
    void deleteAssoc(NodeRef sourceNodeRef, NodeRef targetNodeRef, QName assocQName);

    List<DocAssocInfo> getAssocInfos(Node docNode);

    NodeRef getInitialDocumentRef(NodeRef docRef);

    DocAssocInfo getDocListUnitAssocInfo(AssociationRef assocRef, boolean isSourceAssoc);

    DocAssocInfo getDocListUnitAssocInfo(AssociationRef assocRef, boolean isSourceAssoc, boolean skipNotSearchable);

    void updateModifiedDateTime(NodeRef sourceNodeRef, NodeRef targetNodeRef);

    /** Return true if document is source or target node in followUp or reply association */
    boolean isBaseOrReplyOrFollowUpDocument(NodeRef docRef, Map<String, Map<String, AssociationRef>> addedAssocs);

    /** Return true if workflow mainDocument property was updated */
    boolean createWorkflowAssoc(NodeRef sourceRef, NodeRef targetRef, boolean updateMainDoc, boolean setOwnerProps);

    void deleteWorkflowAssoc(NodeRef sourceNodeRef, NodeRef targetNodeRef);

    void logDocumentWorkflowAssocRemove(NodeRef docRef, NodeRef workflowRef);

    List<NodeRef> getDocumentIndependentWorkflowAssocs(NodeRef docRef);

    boolean isAddCompoundWorkflowAssoc(NodeRef baseDocumentRef, String associatedDocTypeId, QName documentAssocType);

}
