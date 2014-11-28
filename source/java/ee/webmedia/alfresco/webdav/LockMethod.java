/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package ee.webmedia.alfresco.webdav;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.webdav.LockInfo;
import org.alfresco.repo.webdav.LockInfoImpl;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Implements the WebDAV LOCK method
 * 
 * @author gavinc
 */
public class LockMethod extends WebDAVMethod {
    private String m_strLockToken = null;

    protected int m_timeoutDuration = WebDAV.TIMEOUT_180_SECONDS;

    protected LockInfo lockInfo = new LockInfoImpl();

    protected boolean createExclusive;

    protected String lockToken= null;

    /**
     * Default constructor
     */
    public LockMethod() {
    }

    /**
     * Returns true if request has lock token in the If header
     *
     * @return boolean
     */
    protected final boolean hasLockToken()
    {
        if (m_conditions != null)
        {
            for (Condition condition : m_conditions)
            {
                if (!condition.getLockTokensMatch().isEmpty())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the lock token of an existing lock
     * 
     * @return String
     */
    protected final String getLockToken() {
        return m_strLockToken;
    }

    /**
     * Return the lock timeout, in seconds.
     * 
     * @return int
     */
    protected final int getLockTimeout()
    {
        return m_timeoutDuration;
    }

    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        // Get user Agent

        m_userAgent = m_request.getHeader(WebDAV.HEADER_USER_AGENT);

        // Get the depth

        parseDepthHeader();

        // According to the specification: "Values other than 0 or infinity MUST NOT be used with the Depth header on a LOCK method.".
        // The specification does not specify the error code for this case - so we use HttpServletResponse.SC_INTERNAL_SERVER_ERROR.
        if (m_depth != WebDAV.DEPTH_0 && m_depth != WebDAV.DEPTH_INFINITY) {
            throw new WebDAVServerException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // Parse Lock tokens and ETags, if any

        parseIfHeader();

        // Get the lock timeout value

        String strTimeout = m_request.getHeader(WebDAV.HEADER_TIMEOUT);

        // If the timeout header starts with anything other than Second
        // leave the timeout as the default

        if (strTimeout != null && strTimeout.startsWith(WebDAV.SECOND)) {
            try {
                // Some clients send header as Second-180 Seconds so we need to
                // look for the space

                int idx = strTimeout.indexOf(" ");

                if (idx != -1) {
                    // Get the bit after Second- and before the space

                    strTimeout = strTimeout.substring(WebDAV.SECOND.length(), idx);
                } else {
                    // The string must be in the correct format

                    strTimeout = strTimeout.substring(WebDAV.SECOND.length());
                }
                m_timeoutDuration = Integer.parseInt(strTimeout);
            } catch (Exception e) {
                // Warn about the parse failure and leave the timeout as the
                // default

                logger.warn("Failed to parse Timeout header: " + strTimeout);
            }
        }

        // DEBUG

        if (logger.isDebugEnabled()) {
            logger.debug("Lock lockToken=" + getLockToken() + ", request timeout=" + getLockTimeout()
                    + ", user-agent=" + m_request.getHeader(WebDAV.HEADER_USER_AGENT));
        }
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestBody() throws WebDAVServerException
    {
        if (m_request.getContentLength() == -1)
        {
            return;
        }

        Document body = getRequestBodyAsDocument();
        if (body != null)
        {
            OUTER: for (Node currentNode = body.getDocumentElement().getFirstChild(); currentNode != null; currentNode = currentNode.getNextSibling())
            {
                if (currentNode instanceof Element && WebDAV.DEFAULT_NAMESPACE_URI.equals(((Element) currentNode).getNamespaceURI())
                        && WebDAV.XML_LOCK_SCOPE.equals(((Element) currentNode).getLocalName()))
                {
                    for (Node propertiesNode = currentNode.getFirstChild(); propertiesNode != null; propertiesNode = propertiesNode.getNextSibling())
                    {
                        if (propertiesNode instanceof Element && WebDAV.DEFAULT_NAMESPACE_URI.equals(((Element) propertiesNode).getNamespaceURI()))
                        {
                            this.createExclusive = WebDAV.XML_EXCLUSIVE.equals(propertiesNode.getLocalName());
                            break OUTER;
                        }
                    }
                    break OUTER;
                }
            }
            if (!createExclusive)
            {
                // We do not support shared locks - return 412 (section 8.10.7 of RFC 2518)
                throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
            }
        }
    }

    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        attemptLock();
    }

    /**
     * The main lock implementation method.
     *
     * @throws WebDAVServerException
     * @throws Exception
     */
    protected void attemptLock() throws WebDAVServerException, Exception
    {
        final String path = getPath();
        NodeRef rootNodeRef = getRootNodeRef();
        // Get the active user
        String userName = UserUtil.getUsernameAndSession(getDAVHelper().getAuthenticationService().getCurrentUserName(), FacesContext.getCurrentInstance());

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
            lockNodeInfo = getDAVHelper().getNodeForPath(rootNodeRef, getPath(), m_request.getServletPath());
        }
        catch (FileNotFoundException e)
        {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }

        NodeRef fileRef = lockNodeInfo.getNodeRef();
        WebDAVCustomHelper.checkDocumentFileWritePermission(fileRef);

        // Check if this is a new lock or a lock refresh
        if (hasLockToken())
        {
            lockInfo = checkNode(lockNodeInfo);
            // If a request body is not defined and "If" header is sent we have createExclusive as false,
            // but we need to check a previous LOCK was an exclusive. I.e. get the property for node. It
            // is already has got in a checkNode method, so we need just get a scope from lockInfo.
            // This particular case was raised as ALF-4008.
            this.createExclusive = WebDAV.XML_EXCLUSIVE.equals(this.lockInfo.getScope());
            // Refresh an existing lock
            refreshLock(lockNodeInfo, userName);
        }
        else
        {
            lockInfo = checkNode(lockNodeInfo, true, createExclusive);
            // Create a new lock
            createLock(lockNodeInfo, userName);
        }

        m_response.setHeader(WebDAV.HEADER_LOCK_TOKEN, "<" + WebDAV.makeLockToken(lockNodeInfo.getNodeRef(), userName) + ">");
        m_response.setHeader(WebDAV.HEADER_CONTENT_TYPE, WebDAV.XML_CONTENT_TYPE);

        // We either created a new lock or refreshed an existing lock, send back the lock details
        generateResponse(lockNodeInfo, userName);
    }

    protected BehaviourFilter getBehaviourFilter() {
        BehaviourFilter behaviourFilter = (BehaviourFilter) getServiceRegistry().getService(QName.createQName("", "policyBehaviourFilter"));
        return behaviourFilter;
    }

    /**
     * Create a new lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    protected final void createLock(FileInfo lockNode, String userName) throws WebDAVServerException
    {

        if (isLockingDisabled(lockNode.getNodeRef())) {
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }

        // Create Lock token
        lockToken = WebDAV.makeLockToken(lockNode.getNodeRef(), userName);

        if (createExclusive)
        {
            // Lock the node
            lockInfo.setTimeoutSeconds(getLockTimeout());
            lockInfo.setExclusiveLockToken(lockToken);
        }
        else
        {
            // Shared lock creation should already have been prohibited when parsing the request body
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // Store lock depth
        lockInfo.setDepth(WebDAV.getDepthName(m_depth));
        // Store lock scope (shared/exclusive)
        String scope = createExclusive ? WebDAV.XML_EXCLUSIVE : WebDAV.XML_SHARED;
        lockInfo.setScope(scope);
        // Store the owner of this lock
        lockInfo.setOwner(userName);

        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().addVersionLockableAspect(lockNode.getNodeRef());
        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().setVersionLockableAspect(lockNode.getNodeRef(), false);
        // Lock the node
        BeanHelper.getDocLockService().lockFile(lockNode.getNodeRef(), lockInfo);

        if (logger.isDebugEnabled())
        {
            logger.debug("Locked node " + lockNode + ": " + lockInfo);
        }
    }

    private boolean isLockingDisabled(NodeRef lockNode) {
        FileService fileService = BeanHelper.getFileService();
        return (fileService.isFileGeneratedFromTemplate(lockNode) || fileService.isFileAssociatedWithDocMetadata(lockNode))
                && BeanHelper.getDocLockService().isGeneratedFileDocumentLocked(lockNode);
    }

    /**
     * Refresh an existing lock
     * 
     * @param lockNode NodeRef
     * @param userName String
     * @exception WebDAVServerException
     */
    private final void refreshLock(FileInfo lockNode, String userName) throws WebDAVServerException
    {
        if (this.createExclusive)
        {
            // Update the expiry for the lock
            lockInfo.setTimeoutSeconds(getLockTimeout());
            BeanHelper.getDocLockService().lockFile(lockNode.getNodeRef(), lockInfo);
        }
    }

    /**
     * Generates the XML lock discovery response body
     */
    protected void generateResponse(FileInfo lockNodeInfo, String userName) throws Exception
    {
        String scope;
        String lt;
        String owner;
        Date expiry;

        if (lockToken != null)
        {
            // In case of lock creation take the scope from request header
            scope = this.createExclusive ? WebDAV.XML_EXCLUSIVE : WebDAV.XML_SHARED;
            // Output created lock
            lt = lockToken;
        }
        else
        {
            // In case of lock refreshing take the scope from previously stored lock
            scope = this.lockInfo.getScope();
            // Output refreshed lock
            lt = this.lockInfo.getExclusiveLockToken();
        }
        owner = lockInfo.getOwner();
        expiry = lockInfo.getExpires();

        XMLWriter xml = createXMLWriter();

        xml.startDocument();

        String nsdec = generateNamespaceDeclarations(null);
        xml.startElement(WebDAV.DAV_NS, WebDAV.XML_PROP + nsdec, WebDAV.XML_NS_PROP + nsdec, getDAVHelper().getNullAttributes());

        // Output the lock details
        generateLockDiscoveryXML(xml, lockNodeInfo, false, scope, WebDAV.getDepthName(m_depth), lt, owner, expiry);

        // Close off the XML
        xml.endElement(WebDAV.DAV_NS, WebDAV.XML_PROP, WebDAV.XML_NS_PROP);

        // Send remaining data
        flushXML(xml);

    }

    /**
     * Generates a list of namespace declarations for the response
     */
    protected String generateNamespaceDeclarations(HashMap<String,String> nameSpaces)
    {
        StringBuilder ns = new StringBuilder();

        ns.append(" ");
        ns.append(WebDAV.XML_NS);
        ns.append(":");
        ns.append(WebDAV.DAV_NS);
        ns.append("=\"");
        ns.append(WebDAV.DEFAULT_NAMESPACE_URI);
        ns.append("\"");

        // Add additional namespaces

        if ( nameSpaces != null)
        {
            Iterator<String> namespaceList = nameSpaces.keySet().iterator();

            while (namespaceList.hasNext())
            {
                String strNamespaceUri = namespaceList.next();
                String strNamespaceName = nameSpaces.get(strNamespaceUri);

                ns.append(" ").append(WebDAV.XML_NS).append(":").append(strNamespaceName).append("=\"");
                ns.append(strNamespaceUri).append("\" ");
            }
        }

        return ns.toString();
    }
}
