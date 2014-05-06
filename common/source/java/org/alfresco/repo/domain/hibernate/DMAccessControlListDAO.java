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
package org.alfresco.repo.domain.hibernate;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import ee.webmedia.alfresco.privilege.service.AccessControlListExtDAO;
import ee.webmedia.alfresco.privilege.service.NotSupportedPermissionSystemException;

/**
 * DAO layer for the improved ACL implemtentation. This layer is responsible for setting ACLs and any cascade behaviour
 * required. It also implements the migration from the old implementation to the new.
 * 
 * @author andyh
 */
public class DMAccessControlListDAO implements AccessControlListExtDAO
{

    public void forceCopy(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public DbAccessControlList getAccessControlList(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public DbAccessControlList getAccessControlList(StoreRef storeRef)
    {
        return null;
    }

    public Long getIndirectAcl(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public Long getInheritedAcl(NodeRef nodeRef)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public Map<ACLType, Integer> patchAcls()
    {
        throw new NotSupportedPermissionSystemException();
    }

    public void setAccessControlList(NodeRef nodeRef, Long aclId)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public void setAccessControlList(NodeRef nodeRef, DbAccessControlList acl)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public void setAccessControlList(StoreRef storeRef, DbAccessControlList acl)
    {
        throw new UnsupportedOperationException();
    }

    public List<AclChange> setInheritanceForChildren(NodeRef parent, Long inheritFrom)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public void updateChangedAcls(NodeRef startingPoint, List<AclChange> changes)
    {
        throw new NotSupportedPermissionSystemException();
    }
    
    public void fixAclInheritFromNull(Long childAclId, Long primaryParentAclId, NodeRef parentNodeRef) {
        throw new NotSupportedPermissionSystemException();
    }
    
    public Long getInheritedAccessControlList(Long aclId) {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Support to set a shared ACL on a node and all of its children
     * 
     * @param nodeRef
     *            the parent node
     * @param inheritFrom
     *            the parent node's ACL
     * @param mergeFrom
     *            the shared ACL, if already known. If <code>null</code>, will be retrieved / created lazily
     * @param changes
     *            the list in which to record changes
     * @param set
     *            set the shared ACL on the parent ?
     */
    public void setFixedAcls(NodeRef nodeRef, Long inheritFrom, Long mergeFrom, List<AclChange> changes, boolean set)
    {
        throw new NotSupportedPermissionSystemException();
    }
}
