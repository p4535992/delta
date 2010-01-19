package ee.webmedia.alfresco.document.permissions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.ModelDAO;
import org.alfresco.repo.security.permissions.impl.PermissionServiceImpl;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.user.service.UserService;

public abstract class BaseDynamicAuthority implements DynamicAuthority, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(BaseDynamicAuthority.class);

    protected PermissionServiceImpl permissionServiceImpl;
    protected NodeService nodeService;
    protected DictionaryService dictionaryService;
    protected AuthorityService authorityService;
    protected UserService userService;
    protected ModelDAO modelDAO;
    protected List<String> requiredFor;
    protected Set<PermissionReference> whenRequired;
    protected String documentManagersGroup;

    @Override
    public void afterPropertiesSet() throws Exception {
        // buld the permission set
        if (requiredFor != null) {
            whenRequired = new HashSet<PermissionReference>();
            for (String permission : requiredFor) {
                PermissionReference permissionReference = modelDAO.getPermissionReference(null, permission);
                whenRequired.addAll(modelDAO.getGranteePermissions(permissionReference));
                whenRequired.addAll(modelDAO.getGrantingPermissions(permissionReference));
            }
        }
        log.debug("Built requiredFor permission set for " + getAuthority() + ": " + whenRequired);

        AuthenticationUtil.runAs(new RunAsWork<Object>() {
            public Boolean doWork() throws Exception {
                documentManagersGroup = userService.getDocumentManagersGroup();
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());

        permissionServiceImpl.addDynamicAuthority(this);
    }

    protected boolean isDocumentManager() {
        return authorityService.getAuthorities().contains(documentManagersGroup);
    }

    @Override
    public Set<PermissionReference> requiredFor() {
        return whenRequired;
    }

    public void setPermissionServiceImpl(PermissionServiceImpl permissionServiceImpl) {
        this.permissionServiceImpl = permissionServiceImpl;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
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
        this.requiredFor = requiredFor;
    }

}
