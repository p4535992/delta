package ee.webmedia.alfresco.template.web.app.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.UploadFileBaseServlet;
import org.alfresco.web.bean.FileUploadBean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

public class UploadedFileServlet extends UploadFileBaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileName = request.getParameter(FILE_PARAM);
        if (StringUtils.isNotBlank(fileName)) {
            deleteFile(request, response, fileName);
        } else {
            returnListOfUploadedFiles(request, response);
        }
    }

    private void deleteFile(HttpServletRequest request, HttpServletResponse response, String fileName) throws IOException {
        boolean deleted = false;
        HttpSession session = request.getSession();
        FileUploadBean bean = getFileUploadBean(null, session);
        if (bean != null) {
            deleted = bean.removeFile(fileName);
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();
        if (deleted) {
            writer.println("{\"deletedFile\": \"" + fileName + "\"}");
        } else {
            writer.println("{\"error\":\"unable to delete temp file\"}");
        }
        writer.close();
    }

    private void returnListOfUploadedFiles(HttpServletRequest request, HttpServletResponse response) throws IOException {
        FileUploadBean bean = getFileUploadBean(null, request.getSession());
        List<Pair<File, String>> filez = null;

        if (bean != null && CollectionUtils.isNotEmpty(bean.getFiles())) {
            filez = new ArrayList<>();
            List<File> files = bean.getFiles();
            List<String> fileNames = bean.getFileNames();
            for (int i = 0; i < files.size(); i++) {
                filez.add(new Pair<>(files.get(i), fileNames.get(i)));
            }
        }
        createJsonResponse(request, response, filez);
    }
}
