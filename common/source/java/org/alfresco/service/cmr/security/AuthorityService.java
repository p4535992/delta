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
package org.alfresco.service.cmr.security;

import java.util.Set;

import org.alfresco.service.Auditable;
import org.alfresco.service.NotAuditable;
import org.alfresco.service.PublicService;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.model.Cacheable;

/**
 * The service that encapsulates authorities granted to users.
 * This service will refuse to create any user authorities. These should be
 * managed using the AuthenticationService and PersonServce. Methods that try to
 * change alter users will throw an exception.
 * A string key is used to identify the authority. These follow the contract
 * defined in AuthorityType. If there are entities linked to these authorities
 * this key should be used to find them, as userName is used link user and
 * person.
 *
 * @author Andy Hind
 */
@PublicService
public interface AuthorityService
{
    public static String NON_TX_BEAN_NAME = "authorityService";
    /**
     * The default application zone.
     */
    public static String ZONE_APP_DEFAULT = "APP.DEFAULT";

    /**
     * The WCM application zone.
     */
    public static String ZONE_APP_WCM = "APP.WCM";

    /**
     * The SHARE application zone.
     */
    public static String ZONE_APP_SHARE = "APP.SHARE";

    /**
     * Default authentication
     */
    public static String ZONE_AUTH_ALFRESCO = "AUTH.ALF";

    /**
     * Prefix for external auth ids
     */
    public static String ZONE_AUTH_EXT_PREFIX = "AUTH.EXT.";

    /**
     * Check of the current user has admin authority.
     * There is no contract for who should have this authority, only that it can
     * be tested here. It could be determined by group membership, role,
     * authentication mechanism, ...
     *
     * @return true if the currently authenticated user has the admin authority
     */
    @Auditable
    public boolean hasAdminAuthority();

    /**
     * Does the given authority have admin authority.
     *
     * @param authorityName The name of the authority.
     * @return Whether the authority is an admin.
     */
    @Auditable(parameters = { "authorityName" })
    public boolean isAdminAuthority(String authorityName);

    /**
     * Get the authorities for the current user
     *
     * @return authorities for the current user
     */
    @Auditable
    public Set<String> getAuthorities();

    /**
     * Get the authorities for the given user
     */
    @Auditable(parameters = { "userName" })
    public Set<String> getAuthoritiesForUser(String userName);

    /**
     * Get all authorities by type.
     *
     * @param type -
     *            the type of authorities.
     * @return all authorities by type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> getAllAuthorities(AuthorityType type);

    /**
     * Find authorities by pattern matching (* and ?) against the authority name.
     *
     * @param type - the authority type
     * @param namePattern - the pattern which will be matched against the shortName.
     * @return the names of the authorities matching the pattern and type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> findAuthoritiesByShortName(AuthorityType type, String shortNamePattern);

    /**
     * Find authorities by pattern matching (* and ?) against the full authority name.
     *
     * @param type - the authority type
     * @param namePattern - the pattern which will be matched against the full authority name.
     * @return the names of the authorities matching the pattern and type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> findAuthorities(AuthorityType type, String namePattern);

    /**
     * Get all root authorities by type. Root authorities are ones that were
     * created without an authority as the parent authority;
     *
     * @param type -
     *            the type of the authority
     * @return all root authorities by type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> getAllRootAuthorities(AuthorityType type);

    /**
     * Create an authority.
     *
     * @param type -
     *            the type of the authority
     * @param shortName -
     *            the short name of the authority to create
     *            this will also be set as the default display name for the authority
     * @return the name of the authority (this will be the prefix, if any
     *         associated with the type appended with the short name)
     */
    @Auditable(parameters = { "type", "shortName" })
    public String createAuthority(AuthorityType type, String shortName);

    /**
     * Create an authority with a display name and zone.
     *
     * @param type
     *            the type of the authority
     * @param shortName
     *            the short name of the authority to create
     * @param authorityDisplayName
     *            the display name for the authority
     * @param authorityZone
     *            identifier for external user registry owning the authority or <code>null</code> if not applicable
     * @return the full name of the authority (this will be the prefix, if any associated with the type appended with
     *         the short name)
     */
    @Auditable(parameters = { "type", "shortName", "authorityDisplayName", "authorityZones" })
    public String createAuthority(AuthorityType type, String shortName, String authorityDisplayName, Set<String> authorityZones);
    
    /**
     * Create an authority with a display name and zone.
     *
     * @param type
     *            the type of the authority
     * @param shortName
     *            the short name of the authority to create
     * @param authorityDisplayName
     *            the display name for the authority
     * @param authorityZone
     *            identifier for external user registry owning the authority or <code>null</code> if not applicable
     * @param authorityEmail
     * 				common email for the group authority 
     * @return the full name of the authority (this will be the prefix, if any associated with the type appended with
     *         the short name)
     */
    @Auditable(parameters = { "type", "shortName", "authorityDisplayName", "authorityEmail", "authorityZones" })
    public String createAuthority(AuthorityType type, String shortName, String authorityDisplayName, String authorityEmail, Set<String> authorityZones);


    /**
     * Set an authority to include another authority. For example, adding a
     * group to a group or adding a user to a group.
     *
     * @param parentName -
     *            the full name string identifier for the parent.
     * @param childName -
     *            the string identifier for the child.
     */
    @Auditable(parameters = { "parentName", "childName" })
    public void addAuthority(String parentName, String childName);
    
    /**
     * Add an authority email.
     * 
     * @param authorityName
     * @param authorityEmail
     */
    @Auditable(parameters = { "authorityName", "authorityEmail" })
    void addAuthorityEmail(String authorityName, String authorityEmail);

    /**
     * Delete an authority email.
     *
     * @param authorityName
     */
    @Auditable(parameters = { "authorityName" })
    void deleteAuthorityEmail(String authorityName);

    /**
     * Remove an authority as a member of another authority. The child authority
     * will still exist. If the child authority was not created as a root
     * authority and you remove its creation link, it will be moved to a root
     * authority. If you want rid of it, use delete.
     *
     * @param parentName -
     *            the string identifier for the parent.
     * @param childName -
     *            the string identifier for the child.
     */
    @Auditable(parameters = { "parentName", "childName" })
    public void removeAuthority(String parentName, String childName);

    /**
     * Delete an authority and all its relationships. Note child authorities are not deleted.
     *
     * @param name
     */
    @Auditable(parameters = { "name" })
    public void deleteAuthority(String name);

    /**
     * Delete an authority and all its relationships, optionally recursively deleting child authorities of the same
     * type.
     *
     * @param name
     *            the authority long name
     * @param cascade
     *            should the delete be cascaded to child authorities of the same type?
     */
    @Auditable(parameters = { "name", "cascade" })
    public void deleteAuthority(String name, boolean cascade);

    /**
     * Get all the authorities that are contained by the given authority.
     * For a group you could get all the authorities it contains, just the users
     * it contains or just the other groups it includes.
     *
     * @param type -
     *            if not null, limit to the type of authority specified
     * @param name -
     *            the name of the containing authority
     * @param immediate -
     *            if true, limit the depth to just immediate child, if false
     *            find authorities at any depth
     * @return
     */
    @Auditable(parameters = { "type", "name", "immediate" })
    public Set<String> getContainedAuthorities(AuthorityType type, String name, boolean immediate);

    /**
     * Get the authorities that contain the given authority
     * For example, this can be used find out all the authorities that contain a
     * user.
     *
     * @param type -
     *            if not null, limit to the type of authority specified
     * @param name -
     *            the name of the authority for which the containing authorities
     *            are required.
     * @param immediate -
     *            limit to immediate parents or any ancestor.
     * @return
     */
    @Cacheable
    @Auditable(parameters = { "type", "name", "immediate" })
    public Set<String> getContainingAuthorities(AuthorityType type, String name, boolean immediate);

    /**
     * Extract the short name of an authority from its full identifier.
     *
     * @param name
     * @return
     */
    @Auditable(parameters = { "name" })
    public String getShortName(String name);

    /**
     * Create the full identifier for an authority given its short name and
     * type.
     *
     * @param type
     * @param shortName
     * @return
     */
    @Auditable(parameters = { "type", "shortName" })
    public String getName(AuthorityType type, String shortName);

    /**
     * Check if an authority exists.
     *
     * @param name (the long name).
     * @return true, the authority exists.
     */
    @Auditable(parameters = { "name" })
    public boolean authorityExists(String name);

    /**
     * Get the display name for the given authority.
     *
     * @param name - the full authority string including any prefix (e.g. GROUP_woof)
     * @return - the display name
     */
    @Auditable(parameters = { "name" })
    public String getAuthorityDisplayName(String name);
    
    /**
     * Get the email for the given authority.
     *
     * @param name - the full authority string including any prefix (e.g. GROUP_woof)
     * @return - the email
     */
    @Auditable(parameters = { "name" })
    public String getAuthorityEmail(String name);

    /**
     * Set the display name for the given authority.
     * Setting the display name is only supported for authorities of type group
     *
     * @param authorityName
     * @param authorityDisplayName
     */
    @Auditable(parameters = { "authorityName", "authorityDisplayName" })
    public void setAuthorityDisplayName(String authorityName, String authorityDisplayName);

    /**
     * Gets or creates an authority zone node with the specified name
     *
     * @param zoneName
     *            the zone name
     * @return reference to the zone node
     */
    @Auditable(parameters = { "zoneName" })
    public NodeRef getOrCreateZone(String zoneName);

    /**
     * Gets an authority zone node with the specified name
     *
     * @param zoneName
     *            the zone name
     * @return reference to the zone node or null
     */
    @Auditable(parameters = { "zoneName" })
    public NodeRef getZone(String zoneName);

    /**
     * Gets the name of the zone containing the specified authority.
     *
     * @param name
     *            the authority long name
     * @return the the name of the zone containing the specified authority, {@link AuthorityService#DEFAULT_ZONE} if the
     *         authority exists but has no zone, or <code>null</code> if the authority does not exist.
     */
    @Auditable(parameters = { "name" })
    public Set<String> getAuthorityZones(String name);

    /**
     * Gets the names of all authorities in a zone, optionally filtered by type.
     *
     * @param zoneName
     *            the zone name
     * @param type
     *            the authority type to filter by or <code>null</code> for all authority types
     * @return the names of all authorities in a zone, optionally filtered by type
     */
    @Auditable(parameters = { "zoneName", "type" })
    public Set<String> getAllAuthoritiesInZone(String zoneName, AuthorityType type);

    /**
     * Gets the names of all authorities in a zone, optionally filtered by type.
     *
     * @param zoneName
     *            the zone name
     * @param type
     *            the authority type to filter by or <code>null</code> for all authority types
     * @return the names of all authorities in a zone, optionally filtered by type
     */
    @Auditable(parameters = { "zoneName", "type" })
    public Set<String> getAllRootAuthoritiesInZone(String zoneName, AuthorityType type);

    /**
     * Add a zone to an authority.
     *
     * @param authorityName
     * @param zone
     */
    @Auditable(parameters = { "authorityName", "zones" })
    public void addAuthorityToZones(String authorityName, Set<String> zones);

    /**
     * Remove a zone from an authority
     *
     * @param authorityName
     * @param zone
     */
    @Auditable(parameters = { "authorityName", "zones" })
    public void removeAuthorityFromZones(String authorityName, Set<String> zones);

    /**
     * Get the name of the default zone.
     *
     * @return the default zone
     */
    @NotAuditable
    public Set<String> getDefaultZones();

    /**
     * Find authorities by pattern matching (* and ?) against the full authority name in a particular zone
     *
     * @param type - the authority type
     * @param namePattern - the pattern which will be matched against the full authority name.
     * @param zone - the zone
     * @return the names of the authorities matching the pattern and type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> findAuthoritiesInZone(AuthorityType type, String namePattern, String zone);

    /**
     * Find authorities by pattern matching (* and ?) against the authority name.
     *
     * @param type - the authority type
     * @param shortNamePattern - the pattern which will be matched against the shortName.
     * @param zone
     * @return the names of the authorities matching the pattern and type.
     */
    @Auditable(parameters = { "type" })
    public Set<String> findAuthoritiesByShortNameInZone(AuthorityType type, String shortNamePattern, String zone);
}
