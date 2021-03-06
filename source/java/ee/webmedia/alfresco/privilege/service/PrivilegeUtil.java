package ee.webmedia.alfresco.privilege.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.document.permissions.PublicDocumentDynamicAuthority.isPublicAccessRestriction;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

public class PrivilegeUtil {

    public static boolean isAdminOrDocmanagerWithViewDocPermission(Node docNode) {
        return isAdminOrDocmanagerWithPermission(docNode, Privilege.VIEW_DOCUMENT_META_DATA);
    }

    public static boolean isAdminOrDocmanagerWithPermission(Node docNode, Privilege... permissions) {
        if (docNode == null) {
            return false;
        }
        boolean isPublicAssessRestriction = isPublicAccessRestriction((String) docNode.getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION));
        return isAdminOrDocmanagerWithPermission(docNode.getNodeRef(), isPublicAssessRestriction, permissions);
    }

    public static boolean isAdminOrDocmanagerWithPermission(NodeRef docNodeRef, Privilege... permissions) {
        boolean isPublicAssessRestriction = isPublicAccessRestriction((String) getNodeService().getProperty(docNodeRef, DocumentCommonModel.Props.ACCESS_RESTRICTION));
        return isAdminOrDocmanagerWithPermission(docNodeRef, isPublicAssessRestriction, permissions);
    }

    private static boolean isAdminOrDocmanagerWithPermission(NodeRef docNodeRef, boolean isPublicAssessRestriction, Privilege... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("no permissions given for permissions check");
        }
        UserService userService = BeanHelper.getUserService();
        if (userService.isAdministrator()) {
            return true;
        }
        if (userService.isDocumentManager()) {
            boolean hasPermissions = true;
            for (Privilege permission : permissions) {
                boolean hasInheritedOrStaticPermissions = getPrivilegeService()
                        .hasPermissionOnAuthority(docNodeRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, permission);
                if (!hasInheritedOrStaticPermissions && isPublicAssessRestriction && Privilege.VIEW_DOCUMENT_FILES.equals(permission)) {
                    // viewDocumentFiles may be available dynamically, when document accessRestriction="Avalik",
                    // but it is taken in account here only if document managers have (at least) viewDocumentMetadata
                    // assigned as static or inherited privilege, i.e. doc. managers group is displayed in
                    // document's permissions managing dialog
                    boolean hasViewDocumentMetadataPermission = getPrivilegeService().hasPermissionOnAuthority(docNodeRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP,
                            Privilege.VIEW_DOCUMENT_META_DATA);
                    hasPermissions &= hasViewDocumentMetadataPermission;
                } else {
                    hasPermissions &= hasInheritedOrStaticPermissions;
                }
            }
            return hasPermissions;
        }
        return false;
    }

    public static Set<Privilege> getPrivsWithDependencies(Set<Privilege> permissions) {
        Set<Privilege> permissionsWithDependencies = new HashSet<Privilege>(permissions);
        for (Privilege permission : permissions) {
            Set<Privilege> privilegeDependencies = Privilege.PRIVILEGE_DEPENDENCIES.get(permission);
            if (privilegeDependencies != null) {
                permissionsWithDependencies.addAll(privilegeDependencies);
            }
        }
        return permissionsWithDependencies;
    }

    public static Set<Privilege> getRequiredPrivsForInprogressTask(Task task, NodeRef docRef, FileService fileService, boolean isForCaseFile,
            Map<NodeRef, Pair<Boolean, Boolean>> documentDigiDocStatus) {
        if (isStatus(task, Status.IN_PROGRESS)) {
            return getRequiredPrivsForTask(task, docRef, fileService, isForCaseFile, documentDigiDocStatus);
        }
        return new HashSet<Privilege>();
    }

    private static Set<Privilege> getRequiredPrivsForTask(Task task, NodeRef docRef, FileService fileService, boolean isForCaseFile,
            Map<NodeRef, Pair<Boolean, Boolean>> documentDigiDocStatus) {
        return getRequiredPrivsForTask(task, docRef, fileService, isForCaseFile, task.getParent().getParent().isCaseFileWorkflow(), documentDigiDocStatus);
    }

    public static Set<Privilege> getRequiredPrivsForTask(Task task, NodeRef docRef, FileService fileService, boolean isForCaseFile, boolean isUnderCaseFile,
            Map<NodeRef, Pair<Boolean, Boolean>> documentDigiDocStatus) {
        String taskOwnerId = task.getOwnerId();
        Set<Privilege> requiredPrivileges = new HashSet<Privilege>(4);
        if (!StringUtils.isBlank(taskOwnerId)) {
            // give permissions to task owner
            boolean isSignatureTaskWith1Digidoc = false;
            boolean isSignatureTaskWithFiles = false;
            if (task.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                if (documentDigiDocStatus.containsKey(docRef)) {
                    Pair<Boolean, Boolean> digiDocStatuses = documentDigiDocStatus.get(docRef);
                    isSignatureTaskWith1Digidoc = digiDocStatuses.getFirst();
                    isSignatureTaskWithFiles = digiDocStatuses.getSecond();
                } else {
                    List<File> allFiles = fileService.getAllActiveFiles(docRef);
                    if (allFiles.size() == 1 && FilenameUtil.isDigiDocFile(allFiles.get(0).getName())) {
                        isSignatureTaskWith1Digidoc = true;
                    } else if (!allFiles.isEmpty() || allFiles.size() == 1 && !FilenameUtil.isDigiDocFile(allFiles.get(0).getName())) {
                        isSignatureTaskWithFiles = true;
                    }
                    documentDigiDocStatus.put(docRef, Pair.newInstance(isSignatureTaskWith1Digidoc, isSignatureTaskWithFiles));
                }
            }
            boolean isResponsible = isResponsible(task);
            if (isUnderCaseFile) { // Check if task is under case file workflow
                if (task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK, WorkflowSpecificModel.Types.CONFIRMATION_TASK, WorkflowSpecificModel.Types.REVIEW_TASK,
                        WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK)) {
                    requiredPrivileges.add(Privilege.VIEW_DOCUMENT_FILES); // with dependencies
                    if (isForCaseFile) {
                        requiredPrivileges.add(Privilege.VIEW_CASE_FILE);
                    }
                } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                    requiredPrivileges.add(Privilege.EDIT_DOCUMENT); // with dependencies
                    if (isForCaseFile) {
                        requiredPrivileges.add(Privilege.EDIT_CASE_FILE); // with dependencies
                    }
                }
            } else if (isSignatureTaskWithFiles
                    || (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && isResponsible)
                    || task.isType(WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                    || (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && isResponsible)) {
                requiredPrivileges.add(Privilege.EDIT_DOCUMENT); // with dependencies
            } else if (isSignatureTaskWith1Digidoc // ... or under a document
                    || (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && !isResponsible)
                    || (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && !isResponsible)
                    || task.isType(WorkflowSpecificModel.Types.OPINION_TASK, WorkflowSpecificModel.Types.INFORMATION_TASK, WorkflowSpecificModel.Types.CONFIRMATION_TASK,
                            WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK, WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                requiredPrivileges.add(Privilege.VIEW_DOCUMENT_FILES); // with dependencies
            } else {
                MessageUtil.addWarningMessage("task " + task.getType().getLocalName()
                        + ": failed to determine required permissions for the owner of task that is now in progress. Task ownerId=" + taskOwnerId);
            }
        }
        return requiredPrivileges;
    }

    public static Boolean additionalDocumentFileWritePermission(NodeRef parent, NodeService nodeService) {
        DocumentService documentService = BeanHelper.getDocumentService();
        Node docNode = documentService.getDocument(parent);
        documentService.throwIfNotDynamicDoc(docNode);
        String docTypeId = (String) docNode.getProperties().get(Props.OBJECT_TYPE_ID);
        if (SystematicDocumentType.INCOMING_LETTER.getId().equals(docTypeId)) {
            return false;
        }

        if (!StringUtils.equals(DocumentStatus.WORKING.getValueName(), (String) nodeService.getProperty(parent, DocumentCommonModel.Props.DOC_STATUS))) {
            if (!getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.EDIT_FILES_OF_FINISHED_DOC_ENABLED, Boolean.class)) {
                return false;
            }
        }
        return null;
    }

    public static Map<String, List<String>> removePermission(NodeRef nodeRef, Map<String, Set<String>> replacements, Set<AccessPermission> allSetPermissions) {
        Map<String, List<String>> hashMap = new HashMap<String, List<String>>();
        for (AccessPermission accessPermission : allSetPermissions) {
            String existingPermission = accessPermission.getPermission();
            if (accessPermission.isSetDirectly() && replacements.containsKey(existingPermission)) {

                String authority = accessPermission.getAuthority();
                getPermissionService().deletePermission(nodeRef, authority, existingPermission);
                Set<String> replacementPermissions = replacements.get(existingPermission);
                if (replacementPermissions != null) {
                    for (String replacementPermission : replacementPermissions) {
                        if (replacementPermission != null) {
                            getPermissionService().setPermission(nodeRef, authority, replacementPermission, true);
                        }
                    }
                }
                List<String> authoritiesByFormerPermission = hashMap.get(existingPermission);
                if (authoritiesByFormerPermission == null) {
                    authoritiesByFormerPermission = new ArrayList<String>();
                    hashMap.put(existingPermission, authoritiesByFormerPermission);
                }
                authoritiesByFormerPermission.add(authority);
            }
        }
        return hashMap;
    }

}
