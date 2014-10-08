package ee.webmedia.alfresco.document.permissions;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
<<<<<<< HEAD
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DocumentFileWriteDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentFileWriteDynamicAuthority.class);
    public static final String BEAN_NAME = "documentFileWriteDynamicAuthority";

    public static final String DOCUMENT_FILE_WRITE_AUTHORITY = "ROLE_DOCUMENT_FILE_WRITE";
    protected WorkflowService workflowService;

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
=======
import ee.webmedia.alfresco.privilege.service.DynamicAuthority;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DocumentFileWriteDynamicAuthority extends DynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentFileWriteDynamicAuthority.class);
    public static final String BEAN_NAME = "documentFileWriteDynamicAuthority";

    protected WorkflowService workflowService;

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, QName type, final String userName) {
>>>>>>> develop-5.1
        if (!dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
            // log.trace("Node is not of type 'cm:content', type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent == null) {
<<<<<<< HEAD
            log.trace("File does not have a primary parent, type=" + type + ", refusing authority " + getAuthority());
=======
            log.trace("File does not have a primary parent, type=" + type + ", refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return false;
        }
        QName parentType = nodeService.getType(parent);

        if (!dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.DOCUMENT)) {
<<<<<<< HEAD
            log.trace("Node is not of type 'doccom:document', type=" + parentType + ", refusing authority " + getAuthority());
=======
            log.trace("Node is not of type 'doccom:document', type=" + parentType + ", refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return false;
        }
        if (!nodeService.hasAspect(parent, DocumentCommonModel.Aspects.OWNER)) {
            log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect: type=" + parentType
<<<<<<< HEAD
                    + " nodeRef=" + parent + ", refusing authority" + getAuthority());
=======
                    + " nodeRef=" + parent + ", refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return false;
        }

        Boolean additionalCheck = additional(parent);
        if (additionalCheck != null) {
            return additionalCheck;
        }

        String ownerId = (String) nodeService.getProperty(parent, DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId);
            return true;
        }

<<<<<<< HEAD
        log.trace("No conditions met, refusing authority " + getAuthority());
=======
        log.trace("No conditions met, refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
        return false;
    }

    public Boolean additional(NodeRef parent) {
        DocumentService documentService = BeanHelper.getDocumentService();
        Node docNode = documentService.getDocument(parent);
        documentService.throwIfNotDynamicDoc(docNode);
        String docTypeId = (String) docNode.getProperties().get(Props.OBJECT_TYPE_ID);
        if (SystematicDocumentType.INCOMING_LETTER.getId().equals(docTypeId)) {
<<<<<<< HEAD
            log.debug("Document is incoming letter, refusing authority " + getAuthority());
=======
            log.debug("Document is incoming letter, refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return false;
        }

        if (!StringUtils.equals(DocumentStatus.WORKING.getValueName(), (String) nodeService.getProperty(parent, DocumentCommonModel.Props.DOC_STATUS))) {
            if (!getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.EDIT_FILES_OF_FINISHED_DOC_ENABLED, Boolean.class)) {
<<<<<<< HEAD
                log.debug("Document status is not working, refusing authority " + getAuthority());
=======
                log.debug("Document status is not working, refusing permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
                return false;
            }
        }
        return null;
    }

<<<<<<< HEAD
    @Override
    public String getAuthority() {
        return DOCUMENT_FILE_WRITE_AUTHORITY;
    }

=======
>>>>>>> develop-5.1
    // START: getters / setters
    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    // END: getters / setters
<<<<<<< HEAD
=======

>>>>>>> develop-5.1
}
