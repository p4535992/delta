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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Implements the WebDAV PUT method
 * 
 * @author Gavin Cornwell
 */
public class PutMethod extends WebDAVMethod {
    // Request parameters
    private String m_strContentType = null;
    
    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        m_strContentType = m_request.getHeader(WebDAV.HEADER_CONTENT_TYPE);

        // Get the lock token, if any
        parseIfHeader();
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestBody() throws WebDAVServerException {
        // Nothing to do in this method, the body contains
        // the content it will be dealt with later
    }

    /**
     * Exceute the WebDAV request
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        FileFolderService fileFolderService = getFileFolderService();

        // Get the status for the request path
        FileInfo contentNodeInfo = null;
        boolean created = false;
        try {
            contentNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
            // make sure that we are not trying to use a folder
            if (contentNodeInfo.isFolder()) {
                throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (FileNotFoundException e) {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }

        // Update the version if the node is unlocked
        ((WebDAVCustomHelper)getDAVHelper()).getVersionsService().updateVersion(contentNodeInfo.getNodeRef(), contentNodeInfo.getName());

        // Access the content
        ContentWriter writer = fileFolderService.getWriter(contentNodeInfo.getNodeRef());
        // set content properties
        String mimetype = null;
        if (m_strContentType != null) {
            mimetype = m_strContentType;
        } else {
            String guessedMimetype = getMimetypeService().guessMimetype(contentNodeInfo.getName());
            mimetype = guessedMimetype;
        }
        writer.setMimetype(mimetype);

        // Get the input stream from the request data
        InputStream is = m_request.getInputStream();
        is = is.markSupported() ? is : new BufferedInputStream(is);

        ContentCharsetFinder charsetFinder = getMimetypeService().getContentCharsetFinder();
        Charset encoding = charsetFinder.getCharset(is, mimetype);
        writer.setEncoding(encoding.name());

        // Write the new data to the content node
        writer.putContent(is);
        
        // add the user and date information to the custom aspect properties
        ((WebDAVCustomHelper)getDAVHelper()).getVersionsService().updateVersionModifiedAspect(contentNodeInfo.getNodeRef());
        
        // Update document search info
        NodeRef document = getNodeService().getPrimaryParent(contentNodeInfo.getNodeRef()).getParentRef();
        ((WebDAVCustomHelper)getDAVHelper()).getDocumentService().updateSearchableFiles(document);

        // Set the response status, depending if the node existed or not
        m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
    }

}
