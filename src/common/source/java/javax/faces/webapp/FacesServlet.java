/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package javax.faces.webapp;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/api/index.html">JSF Specification</a>
 *
 * @author Manfred Geiler (latest modification by $Author: grantsmith $)
 * @version $Revision: 482726 $ $Date: 2006-12-05 19:02:43 +0100 (Di, 05 Dez 2006) $
 */
public final class FacesServlet
        implements Servlet
{
    private static final Log log = LogFactory.getLog(FacesServlet.class);
    public static final String CONFIG_FILES_ATTR = "javax.faces.CONFIG_FILES";
    public static final String LIFECYCLE_ID_ATTR = "javax.faces.LIFECYCLE_ID";

    private static final String SERVLET_INFO = "FacesServlet of the MyFaces API implementation";
    private ServletConfig _servletConfig;
    private FacesContextFactory _facesContextFactory;
    private Lifecycle _lifecycle;

    private static final String SESSION_SYNCHRONIZED_ATTR = "javax.faces.SESSION_SYNCHRONIZED";

    public FacesServlet()
    {
        super();
    }

    public void destroy()
    {
        _servletConfig = null;
        _facesContextFactory = null;
        _lifecycle = null;
		if(log.isTraceEnabled()) log.trace("destroy");
    }

    public ServletConfig getServletConfig()
    {
        return _servletConfig;
    }

    public String getServletInfo()
    {
        return SERVLET_INFO;
    }

    private String getLifecycleId()
    {
        String lifecycleId = _servletConfig.getServletContext().getInitParameter(LIFECYCLE_ID_ATTR);
        return lifecycleId != null ? lifecycleId : LifecycleFactory.DEFAULT_LIFECYCLE;
    }

    public void init(ServletConfig servletConfig)
            throws ServletException
    {
		if(log.isTraceEnabled()) log.trace("init begin");
        _servletConfig = servletConfig;
        _facesContextFactory = (FacesContextFactory)FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        //TODO: null-check for Weblogic, that tries to initialize Servlet before ContextListener

        //Javadoc says: Lifecycle instance is shared across multiple simultaneous requests, it must be implemented in a thread-safe manner.
        //So we can acquire it here once:
        LifecycleFactory lifecycleFactory = (LifecycleFactory)FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        _lifecycle = lifecycleFactory.getLifecycle(getLifecycleId());
		if(log.isTraceEnabled()) log.trace("init end");
    }

    public void service(ServletRequest request,
                        ServletResponse response)
            throws IOException,
                   ServletException
    {

        HttpServletRequest httpRequest = ((HttpServletRequest) request);
        String pathInfo = httpRequest.getPathInfo();

        // if it is a prefix mapping ...
        if (pathInfo != null
                && (pathInfo.startsWith("/WEB-INF") || pathInfo
                        .startsWith("/META-INF")))
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append(" Someone is trying to access a secure resource : ").append(pathInfo);
            buffer.append("\n remote address is ").append(httpRequest.getRemoteAddr());
            buffer.append("\n remote host is ").append(httpRequest.getRemoteHost());
            buffer.append("\n remote user is ").append(httpRequest.getRemoteUser());
            buffer.append("\n request URI is ").append(httpRequest.getRequestURI());

            log.warn(buffer.toString());

            // Why does RI return a 404 and not a 403, SC_FORBIDDEN ?
            
            ((HttpServletResponse) response)
                    .sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

		if(log.isTraceEnabled()) log.trace("service begin");
        FacesContext facesContext
                = _facesContextFactory.getFacesContext(_servletConfig.getServletContext(),
                                                       request,
                                                       response,
                                                       _lifecycle);

        Object sessionSyncObject;
        synchronized (this) {
            HttpSession session = ((HttpServletRequest) request).getSession();
            sessionSyncObject = session.getAttribute(SESSION_SYNCHRONIZED_ATTR);
            if (sessionSyncObject == null) {
                sessionSyncObject = new SessionSyncObject();
                session.setAttribute(SESSION_SYNCHRONIZED_ATTR, sessionSyncObject);
            }
        }
        try {
            // Synchronize requests on session, i.e. queue simultaneous requests from the same session, so they are executed one at a time,
            // not parallel. Goal is to avoid concurrency issues in web layer.
            synchronized (sessionSyncObject) {
                _lifecycle.execute(facesContext);
                _lifecycle.render(facesContext);
            }
		}
        catch (Throwable e)
        {
            if (e instanceof IOException)
            {
                throw (IOException)e;
            }
            else if (e instanceof ServletException)
            {
                throw (ServletException)e;
            }
            else if (e.getMessage() != null)
            {
                throw new ServletException(e.getMessage(), e);
            }
            else
            {
                throw new ServletException(e);
            }
        }
        finally
        {
            facesContext.release();
        }
		if(log.isTraceEnabled()) log.trace("service end");
    }

    private static class SessionSyncObject implements Serializable {
        private static final long serialVersionUID = 1L;
    }

}
