<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * this is only needed when updating from 3.3.1 to 3.5.3 (because it was possible to delete this group)
 * 
 * @author Ats Uiboupin
 */
public class SupervisionGroupRestoreBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        AuthorityService authorityService = BeanHelper.getAuthorityService();
        Set<String> zones = new HashSet<String>(authorityService.getDefaultZones());
        for (Iterator<String> it = zones.iterator(); it.hasNext();) {
            String zone = it.next();
            Set<String> authorities = authorityService.getAllRootAuthoritiesInZone(zone, AuthorityType.GROUP);
            if (authorities.contains(UserService.AUTH_SUPERVISION_GROUP)) {
                it.remove();
            }
        }
        if (!zones.isEmpty()) {
            LOG.info("adding authority " + UserService.AUTH_SUPERVISION_GROUP + " to zones " + zones.toString());
            authorityService.createAuthority(AuthorityType.GROUP, UserService.SUPERVISION_GROUP, I18NUtil.getMessage(UserService.SUPERVISION_DISPLAY_NAME), zones);
        }
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * this is only needed when updating from 3.3.1 to 3.5.3 (because it was possible to delete this group)
 */
public class SupervisionGroupRestoreBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        AuthorityService authorityService = BeanHelper.getAuthorityService();
        Set<String> zones = new HashSet<String>(authorityService.getDefaultZones());
        for (Iterator<String> it = zones.iterator(); it.hasNext();) {
            String zone = it.next();
            Set<String> authorities = authorityService.getAllRootAuthoritiesInZone(zone, AuthorityType.GROUP);
            if (authorities.contains(UserService.AUTH_SUPERVISION_GROUP)) {
                it.remove();
            }
        }
        if (!zones.isEmpty()) {
            LOG.info("adding authority " + UserService.AUTH_SUPERVISION_GROUP + " to zones " + zones.toString());
            authorityService.createAuthority(AuthorityType.GROUP, UserService.SUPERVISION_GROUP, I18NUtil.getMessage(UserService.SUPERVISION_DISPLAY_NAME), zones);
        }
    }

}
>>>>>>> develop-5.1
