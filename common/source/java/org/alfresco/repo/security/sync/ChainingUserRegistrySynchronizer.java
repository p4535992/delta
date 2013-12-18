/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.security.sync;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.attributes.Attribute;
import org.alfresco.repo.attributes.LongAttributeValue;
import org.alfresco.repo.attributes.MapAttributeValue;
import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.person.PersonServiceImpl;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.client.WebServiceIOException;

import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * A <code>ChainingUserRegistrySynchronizer</code> is responsible for synchronizing Alfresco's local user (person) and
 * group (authority) information with the external subsystems in the authentication chain (most typically LDAP
 * directories). When the {@link #synchronize(boolean)} method is called, it visits each {@link UserRegistry} bean in
 * the 'chain' of application contexts, managed by a {@link ChildApplicationContextManager}, and compares its
 * timestamped user and group information with the local users and groups last retrieved from the same source. Any
 * updates and additions made to those users and groups are applied to the local copies. The ordering of each {@link UserRegistry} in the chain determines its precedence when it
 * comes to user and group name collisions.
 * <p>
 * The <code>force</code> argument determines whether a complete or partial set of information is queried from the {@link UserRegistry}. When <code>true</code> then <i>all</i>
 * users and groups are queried. With this complete set of information, the synchronizer is able to identify which users and groups have been deleted, so it will delete users and
 * groups as well as update and create them. Since processing all users and groups may be fairly time consuming, it is recommended this mode is only used by a background scheduled
 * synchronization job. When the argument is <code>false</code> then only those users and groups modified since the most recent modification date of all the objects last queried
 * from the same {@link UserRegistry} are retrieved. In this mode, local users and groups are created and updated, but not deleted (except where a name collision with a lower
 * priority {@link UserRegistry} is detected). This 'differential' mode is much faster, and by default is triggered by {@link #createMissingPerson(String)} when a user is
 * successfully authenticated who doesn't yet have a local person object in Alfresco. This should mean that new users and their group information are pulled over from LDAP servers
 * as and when required.
 * 
 * @author dward
 */
public class ChainingUserRegistrySynchronizer implements UserRegistrySynchronizer
{

    /** The logger. */
    private static final Log logger = LogFactory.getLog(ChainingUserRegistrySynchronizer.class);

    /** The path in the attribute service below which we persist attributes. */
    private static final String ROOT_ATTRIBUTE_PATH = ".ChainingUserRegistrySynchronizer";

    /** The label under which the last group modification timestamp is stored for each zone. */
    private static final String GROUP_LAST_MODIFIED_ATTRIBUTE = "GROUP";

    /** The label under which the last user modification timestamp is stored for each zone. */
    private static final String PERSON_LAST_MODIFIED_ATTRIBUTE = "PERSON";

    /** The manager for the autentication chain to be traversed. */
    private ChildApplicationContextManager applicationContextManager;

    /** The name used to look up a {@link UserRegistry} bean in each child application context. */
    private String sourceBeanName;

    /** The authority service. */
    private AuthorityService authorityService;

    /** The person service. */
    private PersonService personService;

    /** The attribute service. */
    private AttributeService attributeService;

    private ApplicationService applicationService;

    /** Should we trigger a sync when missing people log in? */
    private boolean syncWhenMissingPeopleLogIn = true;

    /** Should we auto create a missing person on log in? */
    private boolean autoCreatePeopleOnLogin = true;

    private String testEmail;

    /**
     * Sets the application context manager.
     * 
     * @param applicationContextManager
     *            the applicationContextManager to set
     */
    public void setApplicationContextManager(ChildApplicationContextManager applicationContextManager)
    {
        this.applicationContextManager = applicationContextManager;
    }

    /**
     * Sets the name used to look up a {@link UserRegistry} bean in each child application context.
     * 
     * @param sourceBeanName
     *            the bean name
     */
    public void setSourceBeanName(String sourceBeanName)
    {
        this.sourceBeanName = sourceBeanName;
    }

    /**
     * Sets the authority service.
     * 
     * @param authorityService
     *            the new authority service
     */
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    /**
     * Sets the person service.
     * 
     * @param personService
     *            the new person service
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * Sets the attribute service.
     * 
     * @param attributeService
     *            the new attribute service
     */
    public void setAttributeService(AttributeService attributeService)
    {
        this.attributeService = attributeService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Controls whether we auto create a missing person on log in
     * 
     * @param autoCreatePeopleOnLogin
     *            <code>true</code> if we should auto create a missing person on log in
     */
    public void setAutoCreatePeopleOnLogin(boolean autoCreatePeopleOnLogin)
    {
        this.autoCreatePeopleOnLogin = autoCreatePeopleOnLogin;
    }

    /**
     * Controls whether we trigger a sync when missing people log in
     * 
     * @param syncWhenMissingPeopleLogIn
     *            <codetrue</code> if we should trigger a sync when missing people log in
     */
    public void setSyncWhenMissingPeopleLogIn(boolean syncWhenMissingPeopleLogIn)
    {
        this.syncWhenMissingPeopleLogIn = syncWhenMissingPeopleLogIn;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.security.sync.UserRegistrySynchronizer#synchronize(boolean)
     */
    @Override
    public void synchronize(boolean force)
    {
        Set<String> visitedZoneIds = new TreeSet<String>();
        for (String id : applicationContextManager.getInstanceIds())
        {
            StringBuilder builder = new StringBuilder(32);
            builder.append(AuthorityService.ZONE_AUTH_EXT_PREFIX);
            builder.append(id);
            String zoneId = builder.toString();
            ApplicationContext context = applicationContextManager.getApplicationContext(id);
            try
            {
                UserRegistry plugin = (UserRegistry) context.getBean(sourceBeanName);
                if (!(plugin instanceof ActivateableBean) || ((ActivateableBean) plugin).isActive())
                {
                    ChainingUserRegistrySynchronizer.logger.info("Synchronizing users and groups with user registry '"
                            + id + "'");
                    if (force)
                    {
                        ChainingUserRegistrySynchronizer.logger
                                .warn("Forced synchronization with user registry '"
                                        + id
                                        + "'; some users and groups previously created by synchronization with this user registry may be removed.");
                    }
                    int personsProcessed = syncPersonsWithPlugin(zoneId, plugin, force, null, visitedZoneIds, true).processed;
                    int groupsProcessed = syncGroupsWithPlugin(zoneId, plugin, force, visitedZoneIds);
                    ChainingUserRegistrySynchronizer.logger
                            .info("Finished synchronizing users and groups with user registry '" + zoneId + "'");
                    ChainingUserRegistrySynchronizer.logger.info(personsProcessed + " user(s) and " + groupsProcessed
                            + " group(s) processed");
                }
            } catch (NoSuchBeanDefinitionException e)
            {
                // Ignore and continue
            }
            visitedZoneIds.add(zoneId);
        }
    }

    private String synchronize(String userName, boolean idCode) {
        Set<String> visitedZoneIds = new TreeSet<String>();
        for (String id : applicationContextManager.getInstanceIds())
        {
            StringBuilder builder = new StringBuilder(32);
            builder.append(AuthorityService.ZONE_AUTH_EXT_PREFIX);
            builder.append(id);
            String zoneId = builder.toString();
            ApplicationContext context = applicationContextManager.getApplicationContext(id);
            try
            {
                UserRegistry plugin = (UserRegistry) context.getBean(sourceBeanName);
                if (!(plugin instanceof ActivateableBean) || ((ActivateableBean) plugin).isActive())
                {
                    SyncPersonsResult result = syncPersonsWithPlugin(zoneId, plugin, true, userName, visitedZoneIds, idCode);
                    if (result.processed == 1) {
                        return result.userId;
                    }
                }
            } catch (NoSuchBeanDefinitionException e)
            {
                // Ignore and continue
            }
            visitedZoneIds.add(zoneId);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.security.sync.UserRegistrySynchronizer#ensureExists(java.lang.String)
     */
    @Override
    public boolean createMissingPerson(String userName)
    {
        // synchronize or auto-create the missing person if we are allowed
        if (userName != null && !userName.equals(AuthenticationUtil.getSystemUserName()))
        {
            if (syncWhenMissingPeopleLogIn)
            {
                synchronize(false);
                if (personService.personExists(userName))
                {
                    return true;
                }
            }
            if (autoCreatePeopleOnLogin && personService.createMissingPeople())
            {
                AuthorityType authorityType = AuthorityType.getAuthorityType(userName);
                if (authorityType == AuthorityType.USER)
                {
                    personService.getPerson(userName);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String createOrUpdatePersonByUsername(String username) {
        return createOrUpdatePerson(username, false);
    }

    @Override
    public String createOrUpdatePersonByIdCode(String idCode) {
        return createOrUpdatePerson(idCode, true);
    }

    private String createOrUpdatePerson(String idCodeOrUsername, boolean idCode) {
        // synchronize or auto-create the missing person if we are allowed
        if (idCodeOrUsername != null && !idCodeOrUsername.equals(AuthenticationUtil.getSystemUserName())) {
            AuthorityType authorityType = AuthorityType.getAuthorityType(idCodeOrUsername);
            if (authorityType == AuthorityType.USER) {
                String userId = null;
                try {
                    userId = synchronize(idCodeOrUsername, idCode);
                } catch (AuthenticationException e) {
                    // When LDAP connection fails, it throws AuthenticationException
                    logger.warn("User synchronization failed on login, ignoring", e);
                } catch (WebServiceIOException e) {
                    if (BeanHelper.getApplicationService().isTest()) {
                        userId = idCodeOrUsername;
                        logger.warn("User synchronization failed on login, ignoring and continuing with not-synchronized user " + idCodeOrUsername, e);
                    } else {
                        throw e;
                    }
                }
                if (userId != null && personService.personExists(userId)) {
                    return userId;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private static class SyncPersonsResult {
        public int processed;
        public String userId;
    }

    /**
     * Synchronizes local users (persons) with a {@link UserRegistry} for a particular zone.
     * 
     * @param zoneId
     *            the zone id. This identifier is used to tag all created users, so that in the future we can tell those
     *            that have been deleted from the registry.
     * @param userRegistry
     *            the user registry for the zone.
     * @param force
     *            <code>true</code> if all persons are to be queried. <code>false</code> if only those changed since the
     *            most recent queried user should be queried.
     * @param visitedZoneIds
     *            the set of zone ids already processed. These zones have precedence over the current zone when it comes
     *            to user name 'collisions'. If a user is queried that already exists locally but is tagged with one of
     *            the zones in this set, then it will be ignored as this zone has lower priority.
     * @return the number of users processed
     */
    private SyncPersonsResult syncPersonsWithPlugin(String zoneId, UserRegistry userRegistry, boolean force, String userId,
            Set<String> visitedZoneIds, boolean idCode)
    {
        SyncPersonsResult result = new SyncPersonsResult();
        int processedCount = 0;
        long lastModifiedMillis = force ? -1L : getMostRecentUpdateTime(
                ChainingUserRegistrySynchronizer.PERSON_LAST_MODIFIED_ATTRIBUTE, zoneId);
        Date lastModified = lastModifiedMillis == -1 ? null : new Date(lastModifiedMillis);
        if (userId != null)
        {
            ChainingUserRegistrySynchronizer.logger.info("Retrieving one user '" + userId + "' from user registry '" + zoneId + "'");
        }
        else if (lastModified == null)
        {
            ChainingUserRegistrySynchronizer.logger.info("Retrieving all users from user registry '" + zoneId + "'");
        }
        else
        {
            ChainingUserRegistrySynchronizer.logger.info("Retrieving users changed since "
                    + DateFormat.getDateTimeInstance().format(lastModified) + " from user registry '" + zoneId + "'");
        }
        Iterator<NodeDescription> persons;
        if (userId != null) {
            if (idCode) {
                persons = userRegistry.getPersonByIdCode(userId);
            } else {
                persons = userRegistry.getPersonByUsername(userId);
            }
        } else {
            persons = userRegistry.getPersons(lastModified);
        }
        Set<String> personsToDelete = authorityService.getAllAuthoritiesInZone(zoneId, AuthorityType.USER);
        while (persons.hasNext())
        {
            NodeDescription person = persons.next();
            PropertyMap personProperties = person.getProperties();
            if (applicationService.isTest()) {
                personProperties.put(ContentModel.PROP_EMAIL, testEmail);
            }
            String personName = (String) personProperties.get(ContentModel.PROP_USERNAME);
            result.userId = personName;
            if (personsToDelete.remove(personName))
            {
                // The person already existed in this zone: update the person
                ChainingUserRegistrySynchronizer.logger.info("Updating user '" + personName + "'");

                NodeRef personRef = personService.getPerson(personName); // creates home folder if necessary
                Map<QName, Serializable> personOldProperties = BeanHelper.getNodeService().getProperties(personRef);

                String diff = new PropDiffHelper()
                        .watchUser()
                        .diff(RepoUtil.getPropertiesIgnoringSystem(personOldProperties, BeanHelper.getDictionaryService()), personProperties);

                if (diff != null) {
                    BeanHelper.getLogService().addLogEntry(
                            LogEntry.create(LogObject.USER, personName, UserUtil.getPersonFullName1(personOldProperties), (NodeRef) null, "applog_user_edit",
                                    UserUtil.getUserFullNameAndId(personProperties), diff));
                }
                personService.setPersonProperties(personName, personProperties);
            }
            else
            {
                // The person does not exist in this zone, but may exist in another zone
                Set<String> zones = authorityService.getAuthorityZones(personName);
                if (zones != null)
                {
                    zones.retainAll(visitedZoneIds);
                    if (zones.size() > 0)
                    {
                        // A person that exists in a different zone with higher precedence
                        continue;
                    }
                    // The person existed, but in a zone with lower precedence
                    ChainingUserRegistrySynchronizer.logger
                            .warn("Recreating occluded user '"
                                    + personName
                                    + "'. This user was previously created manually or through synchronization with a lower priority user registry.");
                    personService.deletePerson(personName);
                }
                else
                {
                    // The person did not exist at all
                    ChainingUserRegistrySynchronizer.logger.info("Creating user '" + personName + "'");
                }
                PersonServiceImpl.validCreatePersonCall.set(Boolean.TRUE);
                try {
                    personService.createPerson(personProperties, getZones(zoneId));
                } finally {
                    PersonServiceImpl.validCreatePersonCall.set(null);
                }
                NodeRef personRef = personService.getPerson(personName); // creates home folder if necessary
            }
            // Increment the count of processed people
            processedCount++;

            // Maintain the last modified date
            Date personLastModified = person.getLastModified();
            if (personLastModified != null)
            {
                lastModifiedMillis = Math.max(lastModifiedMillis, personLastModified.getTime());
            }
        }

        // If syncing only one user, then dont delete other users
        if (force && !personsToDelete.isEmpty() && userId == null)
        {
            for (String personName : personsToDelete)
            {
                ChainingUserRegistrySynchronizer.logger.warn("Deleting user '" + personName + "'");
                personService.deletePerson(personName);
                processedCount++;
            }
        }

        if (lastModifiedMillis != -1)
        {
            setMostRecentUpdateTime(ChainingUserRegistrySynchronizer.PERSON_LAST_MODIFIED_ATTRIBUTE, zoneId,
                    lastModifiedMillis);
        }

        result.processed = processedCount;
        return result;
    }

    /**
     * Synchronizes local groups (authorities) with a {@link UserRegistry} for a particular zone.
     * 
     * @param zoneId
     *            the zone id. This identifier is used to tag all created groups, so that in the future we can tell
     *            those that have been deleted from the registry.
     * @param userRegistry
     *            the user registry for the zone.
     * @param force
     *            <code>true</code> if all groups are to be queried. <code>false</code> if only those changed since the
     *            most recent queried group should be queried.
     * @param visitedZoneIds
     *            the set of zone ids already processed. These zones have precedence over the current zone when it comes
     *            to group name 'collisions'. If a group is queried that already exists locally but is tagged with one
     *            of the zones in this set, then it will be ignored as this zone has lower priority.
     * @return the number of groups processed
     */
    private int syncGroupsWithPlugin(String zoneId, UserRegistry userRegistry, boolean force, Set<String> visitedZoneIds)
    {
        int processedCount = 0;
        long lastModifiedMillis = force ? -1L : getMostRecentUpdateTime(
                ChainingUserRegistrySynchronizer.GROUP_LAST_MODIFIED_ATTRIBUTE, zoneId);
        Date lastModified = lastModifiedMillis == -1 ? null : new Date(lastModifiedMillis);
        if (lastModified == null)
        {
            ChainingUserRegistrySynchronizer.logger.info("Retrieving all groups from user registry '" + zoneId + "'");
        }
        else
        {
            ChainingUserRegistrySynchronizer.logger.info("Retrieving groups changed since "
                    + DateFormat.getDateTimeInstance().format(lastModified) + " from user registry '" + zoneId + "'");
        }

        Iterator<NodeDescription> groups = userRegistry.getGroups(lastModified);
        Map<String, Set<String>> groupAssocsToCreate = new TreeMap<String, Set<String>>();
        Set<String> groupsToDelete = authorityService.getAllAuthoritiesInZone(zoneId, AuthorityType.GROUP);
        while (groups.hasNext())
        {
            NodeDescription group = groups.next();
            PropertyMap groupProperties = group.getProperties();
            String groupName = (String) groupProperties.get(ContentModel.PROP_AUTHORITY_NAME);

            // Handle 2 special groups - their system name is different from display name
            // The same names are used in DocumentManagersGroupBootstrap
            AuthorityType authorityType = AuthorityType.getAuthorityType(groupName);
            String documentManagersGroupDisplayName = I18NUtil.getMessage(UserService.DOCUMENT_MANAGERS_DISPLAY_NAME);
            String administratorsGroupDisplayName = I18NUtil.getMessage(UserService.ALFRESCO_ADMINISTRATORS_DISPLAY_NAME);
            String accountantsGroupDisplayName = I18NUtil.getMessage(UserService.ACCOUNTANTS_DISPLAY_NAME);
            if (groupName.equals(authorityService.getName(authorityType, documentManagersGroupDisplayName))) {
                groupName = authorityService.getName(authorityType, UserService.DOCUMENT_MANAGERS_GROUP);
                groupProperties.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, documentManagersGroupDisplayName);
            } else if (groupName.equals(authorityService.getName(authorityType, administratorsGroupDisplayName))) {
                groupName = authorityService.getName(authorityType, UserService.ADMINISTRATORS_GROUP);
                groupProperties.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, administratorsGroupDisplayName);
            } else if (groupName.equals(authorityService.getName(authorityType, accountantsGroupDisplayName))) {
                groupName = authorityService.getName(authorityType, UserService.ACCOUNTANTS_GROUP);
                groupProperties.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, accountantsGroupDisplayName);
            }

            if (groupsToDelete.remove(groupName))
            {
                // update an existing group in the same zone
                Set<String> oldChildren = authorityService.getContainedAuthorities(null, groupName, true);
                Set<String> newChildren = group.getChildAssociations();
                Set<String> toDelete = new TreeSet<String>(oldChildren);
                Set<String> toAdd = new TreeSet<String>(newChildren);
                toDelete.removeAll(newChildren);
                toAdd.removeAll(oldChildren);
                if (!toAdd.isEmpty())
                {
                    groupAssocsToCreate.put(groupName, toAdd);
                }
                for (String child : toDelete)
                {
                    ChainingUserRegistrySynchronizer.logger.info("Removing '"
                            + authorityService.getShortName(child) + "' from group '"
                            + authorityService.getShortName(groupName) + "'");
                    authorityService.removeAuthority(groupName, child);
                }
            }
            else
            {
                String groupShortName = authorityService.getShortName(groupName);
                Set<String> groupZones = authorityService.getAuthorityZones(groupName);
                if (groupZones != null)
                {
                    groupZones.retainAll(visitedZoneIds);
                    if (groupZones.size() > 0)
                    {
                        // A group that exists in a different zone with higher precedence
                        continue;
                    }
                    // The group existed, but in a zone with lower precedence
                    ChainingUserRegistrySynchronizer.logger
                            .warn("Recreating occluded group '"
                                    + groupShortName
                                    + "'. This group was previously created manually or through synchronization with a lower priority user registry.");
                    authorityService.deleteAuthority(groupName);
                }
                else
                {
                    ChainingUserRegistrySynchronizer.logger.info("Creating group '" + groupShortName + "'");
                }

                // create the group
                authorityService.createAuthority(AuthorityType.getAuthorityType(groupName), groupShortName,
                        (String) groupProperties.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME), getZones(zoneId));
                Set<String> children = group.getChildAssociations();
                if (!children.isEmpty())
                {
                    groupAssocsToCreate.put(groupName, children);
                }
            }

            // Increment the count of processed groups
            processedCount++;

            // Maintain the last modified date
            Date groupLastModified = group.getLastModified();
            if (groupLastModified != null)
            {
                lastModifiedMillis = Math.max(lastModifiedMillis, groupLastModified.getTime());
            }
        }

        // Add the new associations, now that we have created everything
        for (Map.Entry<String, Set<String>> entry : groupAssocsToCreate.entrySet())
        {
            for (String child : entry.getValue())
            {
                String groupName = entry.getKey();
                if (AuthorityType.getAuthorityType(child) == AuthorityType.USER && !personService.personExists(child))
                {
                    ChainingUserRegistrySynchronizer.logger.warn("Not adding '" + authorityService.getShortName(child)
                            + "' to group '" + authorityService.getShortName(groupName) + "', user does not exist");
                }
                else
                {
                    ChainingUserRegistrySynchronizer.logger.info("Adding '" + authorityService.getShortName(child)
                            + "' to group '" + authorityService.getShortName(groupName) + "'");
                    authorityService.addAuthority(groupName, child);
                }
            }

        }

        // Delete groups if we have complete information for the zone
        if (force && !groupsToDelete.isEmpty())
        {
            for (String group : groupsToDelete)
            {
                ChainingUserRegistrySynchronizer.logger.warn("Deleting group '"
                        + authorityService.getShortName(group) + "'");
                authorityService.deleteAuthority(group);
                processedCount++;
            }
        }

        if (lastModifiedMillis != -1)
        {
            setMostRecentUpdateTime(ChainingUserRegistrySynchronizer.GROUP_LAST_MODIFIED_ATTRIBUTE, zoneId,
                    lastModifiedMillis);
        }

        return processedCount;
    }

    /**
     * Gets the persisted most recent update time for a label and zone.
     * 
     * @param label
     *            the label
     * @param zoneId
     *            the zone id
     * @return the most recent update time in milliseconds
     */
    private long getMostRecentUpdateTime(String label, String zoneId)
    {
        Attribute attribute = attributeService.getAttribute(ChainingUserRegistrySynchronizer.ROOT_ATTRIBUTE_PATH
                + '/' + label + '/' + zoneId);
        return attribute == null ? -1 : attribute.getLongValue();
    }

    /**
     * Persists the most recent update time for a label and zone.
     * 
     * @param label
     *            the label
     * @param zoneId
     *            the zone id
     * @param lastModifiedMillis
     *            the update time in milliseconds
     */
    private void setMostRecentUpdateTime(String label, String zoneId, long lastModifiedMillis)
    {
        String path = ChainingUserRegistrySynchronizer.ROOT_ATTRIBUTE_PATH + '/' + label;
        if (!attributeService.exists(path))
        {
            if (!attributeService.exists(ChainingUserRegistrySynchronizer.ROOT_ATTRIBUTE_PATH))
            {
                attributeService.setAttribute("", ChainingUserRegistrySynchronizer.ROOT_ATTRIBUTE_PATH,
                        new MapAttributeValue());
            }
            attributeService.setAttribute(ChainingUserRegistrySynchronizer.ROOT_ATTRIBUTE_PATH, label,
                    new MapAttributeValue());
        }
        attributeService.setAttribute(path, zoneId, new LongAttributeValue(lastModifiedMillis));
    }

    /**
     * Gets the default set of zones to set on a person or group belonging to the user registry with the given zone ID.
     * We add the default zone as well as the zone corresponding to the user registry so that the users and groups are
     * visible in the UI.
     * 
     * @param zoneId
     *            the zone id
     * @return the zone set
     */
    private Set<String> getZones(String zoneId)
    {
        HashSet<String> zones = new HashSet<String>(2, 1.0f);
        zones.add(AuthorityService.ZONE_APP_DEFAULT);
        zones.add(zoneId);
        return zones;
    }
}