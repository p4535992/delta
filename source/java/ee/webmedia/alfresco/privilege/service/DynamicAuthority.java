package ee.webmedia.alfresco.privilege.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.impl.ModelDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.service.UserService;

public abstract class DynamicAuthority implements InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DynamicAuthority.class);

    protected PrivilegeService privilegeService;
    protected NodeService nodeService;
    protected DictionaryService dictionaryService;
    protected NamespaceService namespaceService;
    protected AuthorityService authorityService;
    protected UserService userService;
    protected ModelDAO modelDAO;
    protected Set<Privilege> grantedPrivileges;
    protected String documentManagersGroup;

    @Override
    public void afterPropertiesSet() throws Exception {
        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            @Override
            public Boolean doWork() throws Exception {
                documentManagersGroup = authorityService.getName(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
        privilegeService.addDynamicAuthority(this);
    }

    protected boolean isDocumentManager() {
        final Set<String> authorities = authorityService.getAuthorities();
        return authorities.contains(documentManagersGroup) || authorities.contains(PermissionService.ADMINISTRATOR_AUTHORITY);
    }

    /**
     * Must return true or false exactly on same conditions as hasAuthority(NodeRef nodeRef, String userName)
     */
    public abstract boolean hasAuthority(NodeRef nodeRef, QName type, String userName, Map<String, Object> properties);

    public Set<Privilege> getGrantedPrivileges() {
        return grantedPrivileges;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setModelDAO(ModelDAO modelDAO) {
        this.modelDAO = modelDAO;
    }

    public void setRequiredFor(List<String> requiredFor) {
        grantedPrivileges = new HashSet<Privilege>();
        if (requiredFor != null) {
            for (String requiredPrivilege : requiredFor) {
                grantedPrivileges.add(Privilege.getPrivilegeByName(requiredPrivilege));
            }
        }
    }

}
