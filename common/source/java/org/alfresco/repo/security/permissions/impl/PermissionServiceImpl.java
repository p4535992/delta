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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.security.permissions.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.NodePermissionEntry;
import org.alfresco.repo.security.permissions.PermissionEntry;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionContext;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.privilege.service.NotSupportedPermissionSystemException;

/**
 * The Alfresco implementation of a permissions service against our APIs for the permissions model and permissions
 * persistence.
 *
 * @author andyh
 */
public class PermissionServiceImpl implements PermissionServiceSPI, InitializingBean
{

    static SimplePermissionReference OLD_ALL_PERMISSIONS_REFERENCE = new SimplePermissionReference(QName.createQName("", PermissionService.ALL_PERMISSIONS),
            PermissionService.ALL_PERMISSIONS);

    /**
     * Standard spring construction.
     */
    public PermissionServiceImpl()
    {
        // allow construction to avoid redefining Spring beans
        super();
    }

    /**
     * Set the dynamic authorities
     *
     * @param dynamicAuthorities
     */
    public void setDynamicAuthorities(List<DynamicAuthority> dynamicAuthorities)
    {
        // do nothing; don't throw exception to avoid redefining Spring beans
    }

    /**
     * Set the dynamic authorities
     *
     * @param dynamicAuthorities
     */
    public void addDynamicAuthority(DynamicAuthority dynamicAuthority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Cache clear on move node
     *
     * @param oldChildAssocRef
     * @param newChildAssocRef
     */
    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        // do nothing; don't throw exception to avoid redefining Spring beans

    }

    public void init()
    {
        // do nothing; don't throw exception to avoid redefining Spring beans

    }

    //
    // Permissions Service
    //

    @Override
    public String getOwnerAuthority()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public String getAllAuthorities()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public String getAllPermission()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<AccessPermission> getPermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<AccessPermission> getAllSetPermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<AccessPermission> getAllSetPermissions(StoreRef storeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<String> getSettablePermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<String> getSettablePermissions(QName type)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public NodePermissionEntry getSetPermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public NodePermissionEntry getSetPermissions(StoreRef storeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public AccessStatus hasPermission(NodeRef passedNodeRef, final PermissionReference permIn)
    {
        throw new NotSupportedPermissionSystemException();

    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.security.PermissionService#hasPermission(java.lang.Long, java.lang.String,
     * java.lang.String)
     */
    @Override
    public AccessStatus hasPermission(Long aclID, PermissionContext context, String permission)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Control permissions cache - only used when we do old style permission evaluations
     * - which should only be in DM stores where no permissions have been set
     *
     * @author andyh
     */
    enum CacheType
    {
        /**
         * cache full check
         */
        HAS_PERMISSION,
        /**
         * Cache single permission check
         */
        SINGLE_PERMISSION,
        /**
         * Cache single permission check for global permission checks
         */
        SINGLE_PERMISSION_GLOBAL;
    }

    /**
     * Key for a cache object is built from all the known Authorities (which can change dynamically so they must all be
     * used) the NodeRef ID and the permission reference itself. This gives a unique key for each permission test.
     */
    static Serializable generateKey(Set<String> auths, NodeRef nodeRef, PermissionReference perm, CacheType type)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public NodePermissionEntry explainPermission(NodeRef nodeRef, PermissionReference perm)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void clearPermission(StoreRef storeRef, String authority)
    {
        throw new NotSupportedPermissionSystemException();

    }

    @Override
    public void deletePermission(StoreRef storeRef, String authority, String perm)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void deletePermissions(StoreRef storeRef)
    {
        throw new NotSupportedPermissionSystemException();

    }

    @Override
    public void setPermission(StoreRef storeRef, String authority, String perm, boolean allow)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void deletePermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void deletePermissions(NodePermissionEntry nodePermissionEntry)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * @see #deletePermission(NodeRef, String, PermissionReference)
     */
    @Override
    public void deletePermission(PermissionEntry permissionEntry)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void clearPermission(NodeRef nodeRef, String authority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void setPermission(PermissionEntry permissionEntry)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void setPermission(NodePermissionEntry nodePermissionEntry)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void setInheritParentPermissions(NodeRef nodeRef, boolean inheritParentPermissions)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * @see org.alfresco.service.cmr.security.PermissionService#getInheritParentPermissions(org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public boolean getInheritParentPermissions(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public PermissionReference getPermissionReference(QName qname, String permissionName)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public PermissionReference getAllPermissionReference()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public String getPermission(PermissionReference permissionReference)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public PermissionReference getPermissionReference(String permissionName)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<PermissionReference> getSettablePermissionReferences(QName type)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<PermissionReference> getSettablePermissionReferences(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void deletePermission(NodeRef nodeRef, String authority, String perm)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public AccessStatus hasPermission(NodeRef nodeRef, String perm)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void setPermission(NodeRef nodeRef, String authority, String perm, boolean allow)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void deletePermissions(String recipient)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Map<NodeRef, Set<AccessPermission>> getAllSetPermissionsForCurrentUser()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Map<NodeRef, Set<AccessPermission>> getAllSetPermissionsForAuthority(String authority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<NodeRef> findNodesByAssignedPermissionForCurrentUser(String permission, boolean allow, boolean includeContainingAuthorities, boolean exactPermissionMatch)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Set<NodeRef> findNodesByAssignedPermission(String authority, String permission, boolean allow, boolean includeContainingAuthorities, boolean includeContainingPermissions)
    {
        throw new NotSupportedPermissionSystemException();
    }
}
