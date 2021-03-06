/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.security.permissions.dynamic;

import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.EqualsHelper;
import org.springframework.beans.factory.InitializingBean;

/**
 * The owner dynamic authority
 * 
 * @author andyh
 */
public class OwnerDynamicAuthority implements DynamicAuthority, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OwnerDynamicAuthority.class);

    private OwnableService ownableService;

    /**
     * Standard construction
     */
    public OwnerDynamicAuthority() {
        super();
    }

    /**
     * Set the ownable service
     * 
     * @param ownableService
     */
    public void setOwnableService(OwnableService ownableService) {
        this.ownableService = ownableService;
    }

    public void afterPropertiesSet() throws Exception {
        if (ownableService == null) {
            throw new IllegalArgumentException("There must be an ownable service");
        }
    }

    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        final String actualOwner = AuthenticationUtil.runAs(new RunAsWork<String>() {

            public String doWork() throws Exception
            {
                return ownableService.getOwner(nodeRef);
            }
        }, AuthenticationUtil.getSystemUserName());
        if (EqualsHelper.nullSafeEquals(actualOwner, userName)) {
            return true;
        }
        final String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        if (EqualsHelper.nullSafeEquals(actualOwner, fullyAuthenticatedUser)) {
            log.info("granting authority '" + getAuthority() + "' based on fully authenticated user '" + fullyAuthenticatedUser
                    + "'(user that logged in) to node: " + nodeRef);
            return true;
        }
        return false;
    }

    public String getAuthority() {
        return PermissionService.OWNER_AUTHORITY;
    }

    public Set<PermissionReference> requiredFor() {
        return null;
    }

}
