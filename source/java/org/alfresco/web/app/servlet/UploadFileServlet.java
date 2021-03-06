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
 * http://www.alfresco.com/legal/licensing" */
package org.alfresco.web.app.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.config.ConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.config.ClientConfigElement;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that takes a file uploaded via a browser and represents it as an
 * UploadFileBean in the session
 * 
 * @author gavinc
 */
public class UploadFileServlet extends UploadFileBaseServlet
{
   private static final long serialVersionUID = -5482538466491052875L;
   private static final Log logger = LogFactory.getLog(UploadFileServlet.class);
   private static final String TASK_REF_HEADER = "X-TaskRef";
   
   private ConfigService configService;
   
   
   /**
    * @see javax.servlet.GenericServlet#init()
    */
   @Override
   public void init(ServletConfig sc) throws ServletException
   {
      super.init(sc);
      
      WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(sc.getServletContext());
      this.configService = (ConfigService)ctx.getBean("webClientConfigService");
   }

   /**
    * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
   {
      String uploadId = null; 
      String returnPage = null;
      final RequestContext requestContext = new ServletRequestContext(request);
      boolean isMultipart = ServletFileUpload.isMultipartContent(requestContext);
      try
      {
         AuthenticationStatus status = servletAuthenticate(request, response);
         if (status == AuthenticationStatus.Failure)
         {
            return;
         }
         
         if (!isMultipart)
         {
            throw new AlfrescoRuntimeException("This servlet can only be used to handle file upload requests, make" +
                                        "sure you have set the enctype attribute on your form to multipart/form-data");
         }

         if (logger.isDebugEnabled())
            logger.debug("Uploading servlet servicing...");
         
         HttpSession session = request.getSession();
         ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
         
         // ensure that the encoding is handled correctly
         upload.setHeaderEncoding("UTF-8");
         
         List<FileItem> fileItems = upload.parseRequest(request);
         
         FileUploadBean bean = getFileUploadBean(uploadId, session);
         @SuppressWarnings("cast")
         String taskRefHeader = (String) request.getHeader(TASK_REF_HEADER);
         NodeRef taskRef = (taskRefHeader != null && NodeRef.isNodeRef(taskRefHeader)) ? new NodeRef(taskRefHeader) : null;
         
         // XXX - dialog that uses this bean must remove it from session after cancel/complete condition
         if (bean == null) {
             bean = new FileUploadBean();
         } else {
             // FXIME: miks multiple=true määratakse? ma tegin hetkel häki, et kui problematicFile, siis ei tehtaks seda
             if(!bean.isProblematicFile()) {
                 bean.setMultiple(true);
             }
         }

         List<Pair<File, String>> addedFiles = new ArrayList<>();

         for (FileItem item : fileItems)
         {
            if(item.isFormField())
            {
               if (item.getFieldName().equalsIgnoreCase("return-page"))
               {
                  returnPage = item.getString();
               }
               else if (item.getFieldName().equalsIgnoreCase("upload-id"))
               {
                  uploadId = item.getString();
               }
            }
            else
            {
               String filename = item.getName();
               if (filename != null && filename.length() != 0)
               {
                  if (logger.isDebugEnabled())
                  {
                     logger.debug("Processing uploaded file: " + filename);
                  }
                  
                  // ADB-41: Ignore non-existent files i.e. 0 byte streams.
                  if (allowZeroByteFiles() == true || item.getSize() > 0)
                  {
                     // workaround a bug in IE where the full path is returned
                     // IE is only available for Windows so only check for the Windows path separator
                     filename = FilenameUtils.getName(filename);
                     final File tempFile = TempFileProvider.createTempFile("alfresco", ".upload");
                     item.write(tempFile);
                     bean.setFile(tempFile);
                     bean.setFileName(filename);
                     bean.setFilePath(tempFile.getAbsolutePath());
                     bean.setContentType(item.getContentType());
                     if(taskRef != null) {
                         bean.setTaskRef(taskRef);
                     }
                     if (logger.isDebugEnabled())
                     {
                        logger.debug("Temp file: " + tempFile.getAbsolutePath() + 
                                     " size " + tempFile.length() +  
                                     " bytes created from upload filename: " + filename);
                     }
                     addedFiles.add(new Pair<>(tempFile, filename));
                  }
                  else
                  {
                     if (logger.isWarnEnabled())
                        logger.warn("Ignored file '" + filename + "' as there was no content, this is either " +
                              "caused by uploading an empty file or a file path that does not exist on the client.");
                  }
               }
            }
         }

         // examine the appropriate session to try and find the User object
         if (Application.inPortalServer() == false)
         {
            session.setAttribute(FileUploadBean.getKey(uploadId), bean);
         }
         else
         {
            // naff solution as we need to enumerate all session keys until we find the one that
            // should match our User objects - this is weak but we don't know how the underlying
            // Portal vendor has decided to encode the objects in the session
            Enumeration<?> enumNames = session.getAttributeNames();
            while (enumNames.hasMoreElements())
            {
               String name = (String)enumNames.nextElement();
               // find an Alfresco value we know must be there...
               if (name.startsWith("javax.portlet.p") && name.endsWith(AuthenticationHelper.AUTHENTICATION_USER))
               {
                  String key = name.substring(0, name.lastIndexOf(AuthenticationHelper.AUTHENTICATION_USER));
                  session.setAttribute(key + FileUploadBean.getKey(uploadId), bean);
                  break;
               }
            }
         }
         
         if (bean.getFiles() == null && uploadId != null && logger.isWarnEnabled())
         {
            logger.warn("no file uploaded for upload id: " + uploadId);
         }
         
         if (returnPage == null || returnPage.length() == 0)
         {
            if(addedFiles.size() > 0)
            {
               returnPage = "json";
            }
            else
            {
               throw new AlfrescoRuntimeException("return-page parameter has not been supplied");
            }
         }
         
         if (returnPage.startsWith("javascript:"))
         {
            returnPage = returnPage.substring("javascript:".length());
            // finally redirect
            if (logger.isDebugEnabled())
            {
               logger.debug("Sending back javascript response " + returnPage);
            }
            response.setContentType(MimetypeMap.MIMETYPE_HTML);
            response.setCharacterEncoding("utf-8");
            final PrintWriter out = response.getWriter();
            out.println("<html><body><script type=\"text/javascript\">");
            out.println(returnPage);
            out.println("</script></body></html>");
            out.close();
         }
         else if(returnPage.equals("ajax")) // Send response, so flash understands, that upload is successful
         {
             response.setContentType(MimetypeMap.MIMETYPE_HTML);
             response.setCharacterEncoding("utf-8");
             final PrintWriter out = response.getWriter();
             out.println("<html><body>");
             out.println("OK");
             out.println("</body></html>");
             out.close();
         }
         else if ("json".equals(returnPage))
         {
             createJsonResponse(request, response, addedFiles);
         }
         else
         {
            // finally redirect
            if (logger.isDebugEnabled())
            {
               logger.debug("redirecting to: " + returnPage);
            }
            
            response.sendRedirect(returnPage);
         }
      }
      catch (Throwable error)
      {
         Application.handleServletError(getServletContext(), (HttpServletRequest)request,
                                        (HttpServletResponse)response, error, logger, returnPage);
      }

      if (logger.isDebugEnabled())
      {
         logger.debug("upload complete");
      }
   }
   
   private boolean allowZeroByteFiles()
   {
      ClientConfigElement clientConfig = (ClientConfigElement)configService.getGlobalConfig().getConfigElement(
            ClientConfigElement.CONFIG_ELEMENT_ID);
      return clientConfig.isZeroByteFileUploads();
   }
}
