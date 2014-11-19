<<<<<<< HEAD
package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Class that might help to solve problems related to inheriting permissions in LIVE environment
 * 
 * @author Ats Uiboupin
 */
public class ArrivedDocumentsPermissionsModifier extends ArrivedDocumentsPermissionsUpdateBootstrap {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArrivedDocumentsPermissionsModifier.class);
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");

    public String getValidationResults() {
        List<String> msgs = new ArrayList<String>();
        msgs.add("timestamp: " + dateFormat.format(new Date()));
        NodeService nodeService = BeanHelper.getNodeService();
        PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
        int indent = 1;
        int errors = 0;
        for (NodeRef nodeRef : getAllFolderRefs()) {
            errors += checkRecursively(nodeRef, msgs, nodeService, privilegeService, indent);
        }
        if (errors > 0) {
            msgs.add(0, "NB! " + errors + " ERRORS!!!!!!!!!!");
        } else {
            msgs.add(0, "OK");
        }
        String msg = StringUtils.join(msgs, "\n");
        return msg;
    }

    private int checkRecursively(NodeRef nodeRef, List<String> msgs, NodeService nodeService, PrivilegeService privilegeService, int indent) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        msgs.add(StringUtils.repeat(" -- ", indent) + "Checking docmanager permissions set for " + childAssocs.size() + " children of " + nodeRef);
        int errors = 0;
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            NodeRef childRef = childAssociationRef.getChildRef();
            if (!checkPermission(msgs, indent, childRef, Privileges.EDIT_DOCUMENT)) {
                errors++;
            }
            if (!checkPermission(msgs, indent, childRef, Privileges.VIEW_DOCUMENT_META_DATA)) {
                errors++;
            }
            if (!checkPermission(msgs, indent, childRef, Privileges.VIEW_DOCUMENT_FILES)) {
                errors++;
            }
            if (!BeanHelper.getPermissionService().getInheritParentPermissions(childRef)) {
                errors++;
                msgs.add(StringUtils.repeat(" -- ", indent) + ("ERROR: node doesn't inherit permissions from parent. node=" + childRef));
            }
            errors += checkRecursively(childRef, msgs, nodeService, privilegeService, indent + 1);
        }
        return errors;
    }

    private boolean checkPermission(List<String> msgs, int indent, NodeRef childRef, String privilege) {
        if (!getPrivilegeService().hasPermission(childRef, privilege, UserService.AUTH_DOCUMENT_MANAGERS_GROUP)) {
            msgs.add(StringUtils.repeat(" -- ", indent) + ("ERROR: DOCMANAGER DOESN'T HAVE " + privilege + " PERMISSION FOR " + childRef));
            return false;
        }
        String path = getNodeService().getPath(childRef).toPrefixString(getNamespaceService());
        msgs.add(StringUtils.repeat(" -- ", indent) + "For " + childRef + " - " + path + " - docmanager has permission " + privilege);
        return true;
    }

    // 1) test1 bootstrap undo+redo
    public void rerunArrivedDocumentsPermissionsUpdateBootstrap(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("rerunArrivedDocumentsPermissionsUpdateBootstrap");
        List<NodeRef> folderRefs = getAllFolderRefs();
        deletePermission(folderRefs);
        addPermissions(folderRefs);
    }

    // 2) eemaldada ArrivedDocumentsPermissionsUpdateBootstrap'i poolt /imap-root alamnodedele lisatud õigused ja anda õigused otse /imap-root nodele
    public void test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot");
        List<NodeRef> imapChildrenRefs = getImapChildrenRefs();
        deletePermission(imapChildrenRefs);
        addPermissions(Arrays.asList(getImapSpace()));
    }

    public void test2Undo(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test2Undo");
        deletePermission(Arrays.asList(getImapSpace()));
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
    }

    private NodeRef getImapSpace() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.IMAP_SPACE);
    }

    // 3) lisada õigused kõigile imapFolder tüüpi alamkataloogidele, mille ülemkataloogidele ArrivedDocumentsPermissionsUpdateBootstrap õiguseid andis
    public void test3UndoRedoAndGrandChildrenPermissions(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test3UndoRedoAndGrandChildrenPermissions");
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
        addPermissions(getGrandChildrenImapFolderRefs());
    }

    public void test3Undo(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test3Undo");
        deletePermission(getGrandChildrenImapFolderRefs());
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
    }

    private List<NodeRef> getGrandChildrenImapFolderRefs() {
        List<NodeRef> imapChildrenRefs = getImapChildrenRefs();
        NodeService nodeService = BeanHelper.getNodeService();

        List<NodeRef> grandChildrenImapFolderRefs = new ArrayList<NodeRef>();
        for (NodeRef imapChildrenRef : imapChildrenRefs) {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(imapChildrenRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef grandChildAssociationRef : childAssocs) {
                NodeRef grandChildRef = grandChildAssociationRef.getChildRef();
                if (ImapModel.Types.IMAP_FOLDER.equals(nodeService.getType(grandChildRef))) {
                    grandChildrenImapFolderRefs.add(grandChildRef);
                }
            }
        }
        return grandChildrenImapFolderRefs;
    }

    private void deletePermission(List<NodeRef> folderRefs) {
        PermissionService permissionService = BeanHelper.getPermissionService();
        for (NodeRef folderRef : folderRefs) {
            for (String permission : Arrays.asList(Privileges.VIEW_DOCUMENT_META_DATA, Privileges.EDIT_DOCUMENT, Privileges.VIEW_DOCUMENT_FILES)) {
                try {
                    LOG.info("Deleting permission from " + folderRef + " - " + getNodeService().getPath(folderRef).toPrefixString(getNamespaceService()) + " - "
                            + UserService.AUTH_DOCUMENT_MANAGERS_GROUP + " - " + permission);
                    permissionService.deletePermission(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, permission);
                } catch (IllegalStateException e) {
                    // this might happen when this permission is not directly set to this node
                    LOG.warn("Didn't manage to remove permission " + permission + " from " + UserService.AUTH_DOCUMENT_MANAGERS_GROUP
                            + " - maybe permission was not directly set for node ");
                }
            }
        }
    }
}
=======
package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Class that might help to solve problems related to inheriting permissions in LIVE environment
 */
public class ArrivedDocumentsPermissionsModifier extends ArrivedDocumentsPermissionsUpdateBootstrap {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArrivedDocumentsPermissionsModifier.class);
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");

    public String getValidationResults() {
        List<String> msgs = new ArrayList<String>();
        msgs.add("timestamp: " + dateFormat.format(new Date()));
        NodeService nodeService = BeanHelper.getNodeService();
        PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
        int indent = 1;
        int errors = 0;
        for (NodeRef nodeRef : getAllFolderRefs()) {
            errors += checkRecursively(nodeRef, msgs, nodeService, privilegeService, indent);
        }
        if (errors > 0) {
            msgs.add(0, "NB! " + errors + " ERRORS!!!!!!!!!!");
        } else {
            msgs.add(0, "OK");
        }
        String msg = StringUtils.join(msgs, "\n");
        return msg;
    }

    private int checkRecursively(NodeRef nodeRef, List<String> msgs, NodeService nodeService, PrivilegeService privilegeService, int indent) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        msgs.add(StringUtils.repeat(" -- ", indent) + "Checking docmanager permissions set for " + childAssocs.size() + " children of " + nodeRef);
        int errors = 0;
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            NodeRef childRef = childAssociationRef.getChildRef();
            if (!checkPermission(msgs, indent, childRef, Privileges.EDIT_DOCUMENT)) {
                errors++;
            }
            if (!checkPermission(msgs, indent, childRef, Privileges.VIEW_DOCUMENT_META_DATA)) {
                errors++;
            }
            if (!checkPermission(msgs, indent, childRef, Privileges.VIEW_DOCUMENT_FILES)) {
                errors++;
            }
            if (!BeanHelper.getPermissionService().getInheritParentPermissions(childRef)) {
                errors++;
                msgs.add(StringUtils.repeat(" -- ", indent) + ("ERROR: node doesn't inherit permissions from parent. node=" + childRef));
            }
            errors += checkRecursively(childRef, msgs, nodeService, privilegeService, indent + 1);
        }
        return errors;
    }

    private boolean checkPermission(List<String> msgs, int indent, NodeRef childRef, String privilege) {
        if (!getPrivilegeService().hasPermission(childRef, privilege, UserService.AUTH_DOCUMENT_MANAGERS_GROUP)) {
            msgs.add(StringUtils.repeat(" -- ", indent) + ("ERROR: DOCMANAGER DOESN'T HAVE " + privilege + " PERMISSION FOR " + childRef));
            return false;
        }
        String path = getNodeService().getPath(childRef).toPrefixString(getNamespaceService());
        msgs.add(StringUtils.repeat(" -- ", indent) + "For " + childRef + " - " + path + " - docmanager has permission " + privilege);
        return true;
    }

    // 1) test1 bootstrap undo+redo
    public void rerunArrivedDocumentsPermissionsUpdateBootstrap(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("rerunArrivedDocumentsPermissionsUpdateBootstrap");
        List<NodeRef> folderRefs = getAllFolderRefs();
        deletePermission(folderRefs);
        addPermissions(folderRefs);
    }

    // 2) eemaldada ArrivedDocumentsPermissionsUpdateBootstrap'i poolt /imap-root alamnodedele lisatud õigused ja anda õigused otse /imap-root nodele
    public void test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot");
        List<NodeRef> imapChildrenRefs = getImapChildrenRefs();
        deletePermission(imapChildrenRefs);
        addPermissions(Arrays.asList(getImapSpace()));
    }

    public void test2Undo(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test2Undo");
        deletePermission(Arrays.asList(getImapSpace()));
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
    }

    private NodeRef getImapSpace() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.IMAP_SPACE);
    }

    // 3) lisada õigused kõigile imapFolder tüüpi alamkataloogidele, mille ülemkataloogidele ArrivedDocumentsPermissionsUpdateBootstrap õiguseid andis
    public void test3UndoRedoAndGrandChildrenPermissions(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test3UndoRedoAndGrandChildrenPermissions");
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
        addPermissions(getGrandChildrenImapFolderRefs());
    }

    public void test3Undo(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("test3Undo");
        deletePermission(getGrandChildrenImapFolderRefs());
        rerunArrivedDocumentsPermissionsUpdateBootstrap(null);
    }

    private List<NodeRef> getGrandChildrenImapFolderRefs() {
        List<NodeRef> imapChildrenRefs = getImapChildrenRefs();
        NodeService nodeService = BeanHelper.getNodeService();

        List<NodeRef> grandChildrenImapFolderRefs = new ArrayList<NodeRef>();
        for (NodeRef imapChildrenRef : imapChildrenRefs) {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(imapChildrenRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef grandChildAssociationRef : childAssocs) {
                NodeRef grandChildRef = grandChildAssociationRef.getChildRef();
                if (ImapModel.Types.IMAP_FOLDER.equals(nodeService.getType(grandChildRef))) {
                    grandChildrenImapFolderRefs.add(grandChildRef);
                }
            }
        }
        return grandChildrenImapFolderRefs;
    }

    private void deletePermission(List<NodeRef> folderRefs) {
        PermissionService permissionService = BeanHelper.getPermissionService();
        for (NodeRef folderRef : folderRefs) {
            for (String permission : Arrays.asList(Privileges.VIEW_DOCUMENT_META_DATA, Privileges.EDIT_DOCUMENT, Privileges.VIEW_DOCUMENT_FILES)) {
                try {
                    LOG.info("Deleting permission from " + folderRef + " - " + getNodeService().getPath(folderRef).toPrefixString(getNamespaceService()) + " - "
                            + UserService.AUTH_DOCUMENT_MANAGERS_GROUP + " - " + permission);
                    permissionService.deletePermission(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, permission);
                } catch (IllegalStateException e) {
                    // this might happen when this permission is not directly set to this node
                    LOG.warn("Didn't manage to remove permission " + permission + " from " + UserService.AUTH_DOCUMENT_MANAGERS_GROUP
                            + " - maybe permission was not directly set for node ");
                }
            }
        }
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
