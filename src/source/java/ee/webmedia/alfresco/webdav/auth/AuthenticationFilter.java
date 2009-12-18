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

package ee.webmedia.alfresco.webdav.auth;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.InMemoryTicketComponentImpl;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.repo.webdav.auth.NTLMAuthenticationFilter;
import org.alfresco.repo.webdav.auth.WebDAVUser;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * WebDAV Authentication Filter Class
 * 
 * @author GKSpencer
 */
public class AuthenticationFilter implements DependencyInjectedFilter
{
    // Debug logging
    
    private static Log logger = LogFactory.getLog(NTLMAuthenticationFilter.class);
    
    // Authenticated user session object name

    public final static String AUTHENTICATION_USER = "_alfDAVAuthTicket";

    // Various services required by NTLM authenticator
    
    private AuthenticationService authService;
    private PersonService personService;
    private NodeService nodeService;
    private TransactionService transactionService;
    
    
    /**
     * @param authService the authService to set
     */
    public void setAuthenticationService(AuthenticationService authService)
    {
        this.authService = authService;
    }

    /**
     * @param personService the personService to set
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * @param nodeService the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param transactionService the transactionService to set
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Run the authentication filter
     * 
     * @param context ServletContext
     * @param req ServletRequest
     * @param resp ServletResponse
     * @param chain FilterChain
     * @exception ServletException
     * @exception IOException
     */
    public void doFilter(ServletContext context, ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException
    {
        // Assume it's an HTTP request

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        // Get the user details object from the session

        WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(AUTHENTICATION_USER);

        if (user != null)
        {
            try
            {
               // Setup the authentication context
               authService.validate(user.getTicket());

               // Set the current locale

               // I18NUtil.setLocale(Application.getLanguage(httpRequest.getSession()));
            }
            catch (Exception ex)
            {
               // No user/ticket, force the client to prompt for logon details
               
               user = null;
            }
        }

        if (user == null)
        {
            	// Check if the request includes an authentication ticket
            
				String ticket = null;

                String path = WebDAV.getRepositoryPath(httpReq);
                List<String> pathElements = WebDAVHelper.splitAllPaths(path);
                if (pathElements.size() > 0)
                {
                    ticket = pathElements.get(0);
                    if (!ticket.startsWith(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX))
                    {
                        ticket = InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX + ticket;
                    }
                }
            	
            	if ( ticket != null &&  ticket.length() > 0)
            	{
            	    // TODO check if PowerPoint bug fix is still needed

                	// Debug
                    
                    if ( logger.isDebugEnabled())
                        logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" +
                                req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
                    
            		UserTransaction tx = null;
            	    try
            	    {
            	    	// Validate the ticket
            	    	  
            	    	authService.validate(ticket);

            	    	// Need to create the User instance if not already available
            	    	  
            	        String currentUsername = authService.getCurrentUserName();

            	        // Start a transaction
            	          
          	            tx = transactionService.getUserTransaction();
            	        tx.begin();
            	            
            	        NodeRef personRef = personService.getPerson(currentUsername);
            	        user = new WebDAVUser( currentUsername, authService.getCurrentTicket(), personRef);
            	        NodeRef homeRef = (NodeRef) nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
            	            
            	        // Check that the home space node exists - else Login cannot proceed
            	            
            	        if (nodeService.exists(homeRef) == false)
            	        {
            	        	throw new InvalidNodeRefException(homeRef);
            	        }
            	        user.setHomeNode(homeRef);
            	            
            	        tx.commit();
            	        tx = null; 
            	            
            	        // Store the User object in the Session - the authentication servlet will then proceed
            	            
            	        httpReq.getSession().setAttribute( AUTHENTICATION_USER, user);
            	    }
	            	catch (AuthenticationException authErr)
	            	{
	            		// Clear the user object to signal authentication failure
	            		
	                    if (logger.isDebugEnabled())
	                        logger.debug("Logon via ticket failed: " + authErr.getMessage());

	            		user = null;
	            	}
	            	catch (Throwable e)
	            	{
	            		// Clear the user object to signal authentication failure

	            	    if (logger.isDebugEnabled())
                            logger.debug("Logon via ticket failed", e);
	            		
	            		user = null;
	            	}
	            	finally
	            	{
	            		try
	            	    {
	            			if (tx != null)
	            	        {
	            				tx.rollback();
	           	        	}
	            	    }
	            	    catch (Exception tex)
	            	    {
	            	    }
	            	}
            	}
            
            // Check if the user is authenticated, if not then prompt again
            
            if ( user == null)
            {
                // No user/ticket, force the client to prompt for logon details
    
                httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // Chain other filters

        chain.doFilter(req, resp);
    }

    /**
     * Cleanup filter resources
     */
    public void destroy()
    {
        // Nothing to do
    }
}
