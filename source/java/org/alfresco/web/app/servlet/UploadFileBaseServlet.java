package org.alfresco.web.app.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.util.Pair;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.FileUploadBean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class UploadFileBaseServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    public static final String FILE_PARAM = "file";

    protected FileUploadBean getFileUploadBean(String uploadId, HttpSession session) {
        FileUploadBean bean = null;
        if (Application.inPortalServer() == false)
        {
            bean = (FileUploadBean) session.getAttribute(FileUploadBean.getKey(uploadId));
        }
        else
        {
            // naff solution as we need to enumerate all session keys until we find the one that
            // should match our User objects - this is weak but we don't know how the underlying
            // Portal vendor has decided to encode the objects in the session
            Enumeration<?> enumNames = session.getAttributeNames();
            while (enumNames.hasMoreElements())
            {
                String name = (String) enumNames.nextElement();
                // find an Alfresco value we know must be there...
                if (name.startsWith("javax.portlet.p") && name.endsWith(AuthenticationHelper.AUTHENTICATION_USER))
                {
                    String key = name.substring(0, name.lastIndexOf(AuthenticationHelper.AUTHENTICATION_USER));
                    bean = (FileUploadBean) session.getAttribute(key + FileUploadBean.getKey(uploadId));
                    break;
                }
            }
        }
        return bean;
    }

    protected void createJsonResponse(HttpServletRequest request, HttpServletResponse response, List<Pair<File, String>> addedFiles) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        final PrintWriter out = response.getWriter();

        out.println("{\"files\": [");
        if (CollectionUtils.isNotEmpty(addedFiles)) {

            int lastItem = addedFiles.size() - 1;

            for (int i = 0; i < addedFiles.size(); i++) {
                File item = addedFiles.get(i).getFirst();
                String filename = addedFiles.get(i).getSecond();
                out.println("{");
                out.println("\"name\": \"" + filename + "\",");
                out.println("\"size\": \"" + FileUtils.byteCountToDisplaySize(item.length()) + "\",");
                String deleteUrl = BeanHelper.getApplicationService().getServerUrl() + request.getContextPath() + "/uploadedFileServlet?" + FILE_PARAM + "=" + item.getName();
                out.println("\"deleteUrl\": \"" + deleteUrl + "\",");
                out.println("\"deleteType\": \"POST\"");
                if (i < lastItem) {
                    out.println("},");
                } else {
                    out.println("}");
                }
            }
        }
        out.println("]}");
        out.close();
    }

}
