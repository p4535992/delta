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
package ee.webmedia.alfresco.simdhs.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.app.servlet.BaseServlet;
import org.alfresco.web.app.servlet.ajax.AjaxCommand;
import org.alfresco.web.app.servlet.ajax.GetCommand;
import org.alfresco.web.app.servlet.ajax.InvokeCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;

/**
 * Servlet responsible for processing AJAX requests.
 * The URL to the servlet should be in the form:
 * 
 * <pre>
 * /alfresco/ajax/command/Bean.binding.expression
 * </pre>
 * <p>
 * See http://wiki.alfresco.com/wiki/AJAX_Support for details.
 * <p>
 * Like most Alfresco servlets, the URL may be followed by a valid 'ticket' argument for authentication: ?ticket=1234567890
 * 
 * @author gavinc
 */
public class AjaxServlet extends BaseServlet {
    public static final String AJAX_LOG_KEY = "alfresco.ajax";

    protected enum Command {
        invoke, get, set
    }

    private static final long serialVersionUID = -7654769105419391840L;
    private static Log logger = LogFactory.getLog(AJAX_LOG_KEY);
    private static Log headersLogger = LogFactory.getLog(AJAX_LOG_KEY + ".headers");
    private static Log perfLogger = LogFactory.getLog(AJAX_LOG_KEY + ".performance");

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        // set default character encoding for the response
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/xml;charset=UTF-8");

        long startTime = 0;
        String uri = request.getRequestURI();
        if (logger.isDebugEnabled()) {
            final String queryString = request.getQueryString();
            logger.debug("Processing URL: " + uri +
                      ((queryString != null && queryString.length() > 0) ? ("?" + queryString) : ""));
        }

        // dump the request headers
        if (headersLogger.isDebugEnabled()) {
            @SuppressWarnings("unchecked")
            final Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                final String name = headers.nextElement();
                headersLogger.debug(name + ": " + request.getHeader(name));
            }
        }

        try {
            setNoCacheHeaders(response);

            uri = uri.substring(request.getContextPath().length() + "/".length());
            final String[] tokens = uri.split("/");
            if (tokens.length < 3) {
                throw new AlfrescoRuntimeException("Servlet URL did not contain all required args: " + uri);
            }

            // retrieve the command from the URL
            final String commandName = tokens[1];
            // retrieve the binding expression from the URL
            final String expression = tokens[2];

            // setup the faces context
            final FacesContext facesContext = FacesContext.getCurrentInstance();

            // start a timer
            if (perfLogger.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
            }

            // instantiate the relevant command
            AjaxCommand command = null;
            if (Command.invoke.toString().equals(commandName)) {
                command = new InvokeCommand();
            } else if (Command.get.toString().equals(commandName)) {
                command = new GetCommand();
            } else {
                throw new AlfrescoRuntimeException("Unrecognised command received: " + commandName);
            }

            StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.ACTION, commandName + "/" + expression);

            // execute the command
            command.execute(facesContext, expression, request, response);
        } catch (RuntimeException error) {
            handleError(response, error);
        } finally {
            // measure the time taken
            if (perfLogger.isDebugEnabled()) {
                perfLogger.debug("Time to execute command: " + (System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }

    /**
     * Handles any error that occurs during the execution of the servlet
     * 
     * @param response The response
     * @param cause The cause of the error
     */
    protected void handleError(HttpServletResponse response, RuntimeException cause)
            throws ServletException, IOException {
        // if we can send back the 500 error with the error from the top of the
        // stack as the error status message.

        // NOTE: if we use the built in support for generating error pages for
        // 500 errors we can tailor the output for AJAX calls so that the
        // body of the response can be used to show the error details.

        if (!response.isCommitted()) {
            // dump the error if debugging is enabled
            if (logger.isDebugEnabled()) {
                logger.error(cause);
                Throwable theCause = cause.getCause();
                if (theCause != null) {
                    logger.error("caused by: ", theCause);
                }
            }

            // extract a message from the exception
            String msg = cause.getMessage();
            if (msg == null) {
                msg = cause.toString();
            }

            // send the error
            MonitoringUtil.logError(MonitoredService.IN_WWW, cause);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        } else {
            // the response has already been comitted, not much we can do but
            // let the error through and let the container deal with it
            throw cause;
        }
    }
}
