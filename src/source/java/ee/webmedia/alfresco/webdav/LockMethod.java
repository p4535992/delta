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
package ee.webmedia.alfresco.webdav;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.dom4j.io.XMLWriter;

import ee.webmedia.alfresco.versions.model.VersionsModel;

/**
 * Implements the WebDAV LOCK method
 * 
 * @author gavinc
 */
public class LockMethod extends org.alfresco.repo.webdav.LockMethod
{

    @Override
    protected void executeImpl() throws WebDAVServerException, Exception
    {
        String path = getPath();
        // Get the active user
        String userName = getDAVHelper().getAuthenticationService().getCurrentUserName();

        if (logger.isDebugEnabled())
        {
            logger.debug("Locking node: \n" +
                    "   user: " + userName + "\n" +
                    "   path: " + path);
        }

        FileInfo lockNodeInfo = null;
        try
        {
            // Check if the path exists
            lockNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), m_request.getServletPath());
        }
        catch (FileNotFoundException e)
        {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }

        // Check if this is a new lock or a refresh
        if (hasLockToken())
        {
            // Refresh an existing lock
            refreshLock(lockNodeInfo.getNodeRef(), userName);
        }
        else
        {
            // Create a new lock
            createLock(lockNodeInfo.getNodeRef(), userName);
        }

        // We either created a new lock or refreshed an existing lock, send back the lock details
        generateResponse(lockNodeInfo.getNodeRef(), userName);
    }

    /**
     * Create a new lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    private final void createLock(NodeRef lockNode, String userName) throws WebDAVServerException
    {
        LockService lockService = getLockService();

        // Check the lock status of the node
        LockStatus lockSts = lockService.getLockStatus(lockNode);

        // DEBUG
        if (logger.isDebugEnabled())
            logger.debug("Create lock status=" + lockSts);

        if (lockSts == LockStatus.LOCKED || lockSts == LockStatus.LOCK_OWNER)
        {
            // Indicate that the resource is already locked
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }
        
        // Lock the node
        lockService.lock(lockNode, LockType.WRITE_LOCK, getLockTimeout());
        
        addVersionLockableAspect(lockNode);
    }
    
    private void addVersionLockableAspect(NodeRef lockNode) {
        if (getDAVHelper().getNodeService().hasAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE) == false) {
            getDAVHelper().getNodeService().addAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE, null);
            if (logger.isDebugEnabled()) {
                logger.debug("VERSION_LOCKABLE aspect added to " + lockNode);
            }
        }
        getDAVHelper().getNodeService().setProperty(lockNode, VersionsModel.Props.VersionLockable.LOCKED, false);
    }

    /**
     * Refresh an existing lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    private final void refreshLock(NodeRef lockNode, String userName) throws WebDAVServerException
    {
        LockService lockService = getLockService();

        // Check the lock status of the node
        LockStatus lockSts = lockService.getLockStatus(lockNode);

        // DEBUG
        if (logger.isDebugEnabled())
            logger.debug("Refresh lock status=" + lockSts);

        if (lockSts != LockStatus.LOCK_OWNER)
        {
            // Indicate that the resource is already locked
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }

        // Update the expiry for the lock
        lockService.lock(lockNode, LockType.WRITE_LOCK, getLockTimeout());
    }

    /**
     * Generates the XML lock discovery response body
     */
    private void generateResponse(NodeRef lockNode, String userName) throws Exception
    {
        XMLWriter xml = createXMLWriter();

        xml.startDocument();

        String nsdec = generateNamespaceDeclarations(null);
        xml.startElement(WebDAV.DAV_NS, WebDAV.XML_MULTI_STATUS + nsdec, WebDAV.XML_NS_MULTI_STATUS + nsdec,
                getDAVHelper().getNullAttributes());

        // Output the lock details
        generateLockDiscoveryXML(xml, lockNode);

        // Close off the XML
        xml.endElement(WebDAV.DAV_NS, WebDAV.XML_MULTI_STATUS, WebDAV.XML_NS_MULTI_STATUS);

        // Send the XML back to the client
        m_response.setStatus(HttpServletResponse.SC_OK);
        xml.flush();
    }
}
