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
package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;
import java.util.List;
import java.util.zip.CRC32;

import org.alfresco.repo.domain.DbAccessControlEntry;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.DbAccessControlListChangeSet;
import org.alfresco.repo.domain.DbAuthority;
import org.alfresco.repo.domain.DbPermission;
import org.alfresco.repo.security.permissions.ACLCopyMode;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.AccessControlEntry;
import org.alfresco.repo.security.permissions.AccessControlList;
import org.alfresco.repo.security.permissions.AccessControlListProperties;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.repo.security.permissions.impl.AclDaoComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.security.AccessStatus;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ee.webmedia.alfresco.privilege.service.NotSupportedPermissionSystemException;

/**
 * Hibernate DAO to manage ACL persistence
 *
 * @author andyh
 */
public class AclDaoComponentImpl extends HibernateDaoSupport implements AclDaoComponent
{

    static String QUERY_GET_AUTHORITY = "permission.GetAuthority";

    static String QUERY_GET_ACE_WITH_NO_CONTEXT = "permission.GetAceWithNoContext";

    // static String QUERY_GET_AUTHORITY_ALIAS = "permission.GetAuthorityAlias";

    // static String QUERY_GET_AUTHORITY_ALIASES = "permission.GetAuthorityAliases";

    static String QUERY_GET_ACES_AND_ACLS_BY_AUTHORITY = "permission.GetAcesAndAclsByAuthority";

    static String QUERY_GET_ACES_BY_AUTHORITY = "permission.GetAcesByAuthority";

    static String QUERY_GET_ACES_FOR_ACL = "permission.GetAcesForAcl";

    static String QUERY_LOAD_ACL = "permission.LoadAcl";

    static String QUERY_GET_ACLS_THAT_INHERIT_FROM_THIS_ACL = "permission.GetAclsThatInheritFromThisAcl";

    static String QUERY_GET_AVM_NODES_BY_ACL = "permission.FindAvmNodesByACL";

    static String QUERY_GET_LATEST_ACL_BY_ACLID = "permission.FindLatestAclByGuid";

    static String QUERY_GET_LAYERED_DIRECTORIES = "permission.GetLayeredDirectories";

    static String QUERY_GET_LAYERED_FILES = "permission.GetLayeredFiles";

    static String QUERY_GET_NEW_IN_STORE = "permission.GetNewInStore";

    public AclDaoComponentImpl()
    {
        super();
        // Wire up for annoying AVM hack to support copy and setting of ACLs as nodes are created
        DbAccessControlListImpl.setAclDaoComponent(this);
    }

    @Override
    public DbAccessControlList getDbAccessControlList(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Long createAccessControlList(AccessControlListProperties properties)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Used when deleting a user. No ACL is updated - the user has gone the aces and all related info is deleted.
     */
    @Override
    public List<AclChange> deleteAccessControlEntries(final String authority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void onDeleteAccessControlList(final long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> deleteAccessControlList(final Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> deleteLocalAccessControlEntries(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> deleteInheritedAccessControlEntries(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> deleteAccessControlEntries(Long id, AccessControlEntry pattern)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Search for access control lists
     *
     * @param pattern
     * @return the ids of the ACLs found
     */
    public Long[] findAccessControlList(AccessControlEntry pattern)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlList getAccessControlList(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * @param id
     * @return the access control list
     */
    @SuppressWarnings("unchecked")
    public AccessControlList getAccessControlListImpl(final Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public AccessControlListProperties getAccessControlListProperties(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Long getInheritedAccessControlList(Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> invalidateAccessControlEntries(final String authority)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AclChange> mergeInheritedAccessControlList(Long inherited, Long target)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AclChange> setAccessControlEntry(final Long id, final AccessControlEntry ace)
    {
        throw new NotSupportedPermissionSystemException();
    }

    private long getCrc(String str)
    {
        CRC32 crc = new CRC32();
        crc.update(str.getBytes());
        return crc.getValue();
    }

    @Override
    public List<AclChange> enableInheritance(Long id, Long parent)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<AclChange> disableInheritance(Long id, boolean setInheritedOnAcl)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public Long getCopy(Long toCopy, Long toInheritFrom, ACLCopyMode mode)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public DbAccessControlList getDbAccessControlListCopy(Long toCopy, Long toInheritFrom, ACLCopyMode mode)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<Long> getAvmNodesByACL(final Long id)
    {
        throw new NotSupportedPermissionSystemException();
    }

    private static final String RESOURCE_KEY_ACL_CHANGE_SET_ID = "hibernate.acl.change.set.id";

    /**
     * Support to get the current ACL change set and bind this to the transaction. So we only make one new version of an
     * ACL per change set. If something is in the current change set we can update it.
     */
    private DbAccessControlListChangeSet getCurrentChangeSet()
    {
        DbAccessControlListChangeSet changeSet = null;
        Serializable changeSetId = (Serializable) AlfrescoTransactionSupport.getResource(RESOURCE_KEY_ACL_CHANGE_SET_ID);
        if (changeSetId == null)
        {
            changeSet = new DbAccessControlListChangeSetImpl();
            changeSetId = getHibernateTemplate().save(changeSet);
            DirtySessionMethodInterceptor.flushSession(getSession(), true);
            changeSet = getHibernateTemplate().get(DbAccessControlListChangeSetImpl.class, changeSetId);
            // bind the id
            AlfrescoTransactionSupport.bindResource(RESOURCE_KEY_ACL_CHANGE_SET_ID, changeSetId);
            if (logger.isDebugEnabled())
            {
                logger.debug("New change set = " + changeSetId);
            }
        }
        else
        {
            changeSet = getHibernateTemplate().get(DbAccessControlListChangeSetImpl.class, changeSetId);
            if (logger.isDebugEnabled())
            {
                logger.debug("Existing change set = " + changeSetId);
            }
        }
        return changeSet;
    }

    /**
     * Does this <tt>Session</tt> contain any changes which must be synchronized with the store?
     *
     * @return true => changes are pending
     */
    @Override
    public boolean isDirty()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * NO-OP
     */
    @Override
    public void beforeCommit()
    {
        throw new NotSupportedPermissionSystemException();
    }

    static class AclChangeImpl implements AclChange
    {
        private Long before;

        private Long after;

        private ACLType typeBefore;

        private ACLType typeAfter;

        AclChangeImpl(Long before, Long after, ACLType typeBefore, ACLType typeAfter)
        {
            this.before = before;
            this.after = after;
            this.typeAfter = typeAfter;
            this.typeBefore = typeBefore;
        }

        @Override
        public Long getAfter()
        {
            return after;
        }

        @Override
        public Long getBefore()
        {
            return before;
        }

        /**
         * @param after
         */
        public void setAfter(Long after)
        {
            this.after = after;
        }

        /**
         * @param before
         */
        public void setBefore(Long before)
        {
            this.before = before;
        }

        @Override
        public ACLType getTypeAfter()
        {
            return typeAfter;
        }

        /**
         * @param typeAfter
         */
        public void setTypeAfter(ACLType typeAfter)
        {
            this.typeAfter = typeAfter;
        }

        @Override
        public ACLType getTypeBefore()
        {
            return typeBefore;
        }

        /**
         * @param typeBefore
         */
        public void setTypeBefore(ACLType typeBefore)
        {
            this.typeBefore = typeBefore;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("(").append(getBefore()).append(",").append(getTypeBefore()).append(")");
            builder.append(" - > ");
            builder.append("(").append(getAfter()).append(",").append(getTypeAfter()).append(")");
            return builder.toString();
        }

    }

    /**
     * Get the total number of head nodes in the repository
     *
     * @return count
     */
    public Long getAVMHeadNodeCount()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Get the max acl id
     *
     * @return - max acl id
     */
    @Override
    public Long getMaxAclId()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Does the underlyinf connection support isolation level 1 (dirty read)
     *
     * @return true if we can do a dirty db read and so track changes (Oracle can not)
     */
    @Override
    public boolean supportsProgressTracking()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Get the acl count canges so far for progress tracking
     *
     * @param above
     * @return - the count
     */
    public Long getAVMNodeCountWithNewACLS(Long above)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * How many nodes are noew in store (approximate)
     *
     * @return - the number of new nodes - approximate
     */
    @Override
    public Long getNewInStore()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Find layered directories Used to improve performance during patching and cascading the effect of permission
     * changes between layers
     *
     * @return - layered directories
     */
    public List<Indirection> getLayeredDirectories()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Find layered files Used to improve performance during patching and cascading the effect of permission changes
     * between layers
     *
     * @return - layered files
     */
    public List<Indirection> getLayeredFiles()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public List<Indirection> getAvmIndirections()
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void flush()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * Support to describe AVM indirections for permission performance improvements when permissions are set.
     *
     * @author andyh
     */
    public static class Indirection
    {
        Long from;

        String to;

        Integer toVersion;

        Indirection(Long from, String to, Integer toVersion)
        {
            this.from = from;
            this.to = to;
            this.toVersion = toVersion;
        }

        /**
         * @return - from id
         */
        public Long getFrom()
        {
            return from;
        }

        /**
         * @return - to id
         */
        public String getTo()
        {
            return to;
        }

        /**
         * @return - version
         */
        public Integer getToVersion()
        {
            return toVersion;
        }

    }

    /**
     * How many DM nodes are there?
     *
     * @return - the count
     */
    @Override
    public Long getDmNodeCount()
    {
        throw new NotSupportedPermissionSystemException();
    }

    /**
     * How many DM nodes are three with new ACls (to track patch progress)
     *
     * @param above
     * @return - the count
     */
    @Override
    public Long getDmNodeCountWithNewACLS(Long above)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void updateAuthority(String before, String after)
    {
        throw new NotSupportedPermissionSystemException();
    }

    private DbAuthority getAuthority(final String authority, boolean create)
    {
        // Find auth
        HibernateCallback callback = new HibernateCallback()
        {
            @Override
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_AUTHORITY);
                query.setParameter("authority", authority);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };

        DbAuthority dbAuthority = null;
        List<DbAuthority> authorities = (List<DbAuthority>) getHibernateTemplate().execute(callback);
        for (DbAuthority found : authorities)
        {
            if (found.getAuthority().equals(authority))
            {
                dbAuthority = found;
                break;
            }
        }
        if (create && (dbAuthority == null))
        {
            dbAuthority = createDbAuthority(authority);
        }
        return dbAuthority;
    }

    private DbAccessControlEntry getAccessControlEntry(final DbPermission permission, final DbAuthority authority, final AccessControlEntry ace, boolean create)
    {

        HibernateCallback callback = new HibernateCallback()
        {
            @Override
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_ACE_WITH_NO_CONTEXT);
                query.setParameter("permissionId", permission.getId());
                query.setParameter("authorityId", authority.getId());
                query.setParameter("allowed", (ace.getAccessStatus() == AccessStatus.ALLOWED) ? true : false);
                query.setParameter("applies", ace.getAceType().getId());
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        DbAccessControlEntry entry;
        try
        {
            entry = (DbAccessControlEntry) getHibernateTemplate().execute(callback);
        } catch (RuntimeException e)
        {
            logger.error("Failed to execute hibernate query: " + QUERY_GET_ACE_WITH_NO_CONTEXT
                    + "\n  permissionId=" + permission.getId()
                    + "\n  authorityId=" + authority.getId()
                    + "\n  allowed=" + ((ace.getAccessStatus() == AccessStatus.ALLOWED) ? true : false)
                    + "\n  applies=" + ace.getAceType().getId(), e);
            throw e;
        }
        if (create && (entry == null))
        {
            DbAccessControlEntryImpl newEntry = new DbAccessControlEntryImpl();
            newEntry.setAceType(ace.getAceType());
            newEntry.setAllowed((ace.getAccessStatus() == AccessStatus.ALLOWED) ? true : false);
            newEntry.setAuthority(authority);
            newEntry.setPermission(permission);
            entry = newEntry;
            getHibernateTemplate().save(newEntry);
            DirtySessionMethodInterceptor.flushSession(getSession(), true);
        }
        return entry;
    }

    @Override
    public void createAuthority(String authority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    public DbAuthority createDbAuthority(String authority)
    {
        throw new NotSupportedPermissionSystemException();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.security.permissions.impl.AclDaoComponent#setAccessControlEntries(java.lang.Long,
     * java.util.List)
     */
    @Override
    public List<AclChange> setAccessControlEntries(Long id, List<AccessControlEntry> aces)
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.security.permissions.impl.AclDaoComponent#createAccessControlList(org.alfresco.repo.security.permissions.AccessControlListProperties,
     * java.util.List, long)
     */
    @Override
    public Long createAccessControlList(AccessControlListProperties properties, List<AccessControlEntry> aces, Long inherited)
    {
        throw new NotSupportedPermissionSystemException();
    }

    @Override
    public void fixSharedAcl(Long shared, Long defining)
    {
        throw new NotSupportedPermissionSystemException();
    }

}
