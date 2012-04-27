package ee.webmedia.alfresco.common.propertysheet.upload;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.IClonable;
import ee.webmedia.alfresco.document.file.model.File;

public class UploadFileInput extends UIInput implements NamingContainer {

    public static class FileWithContentType implements Serializable, IClonable<FileWithContentType> {
        private static final long serialVersionUID = 1L;

        public java.io.File file;
        public String contentType;
        public String fileName;

        public FileWithContentType(java.io.File file, String contentType, String fileName) {
            this.file = file;
            this.contentType = contentType;
            this.fileName = fileName;
        }

        @Override
        public FileWithContentType clone() {
            return new FileWithContentType(file, contentType, fileName);
        }
    }

    private static final String EVENT_UPLOADED = "uploaded";
    private static final String EVENT_REMOVE = "remove";

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String path = context.getExternalContext().getRequestContextPath();
        String uniqueId = getClientId(context);
        String jsSuffix = uniqueId.replaceAll("[:\\-]", "_");

        ValueBinding vb = getValueBinding("value");
        @SuppressWarnings("unchecked")
        List<Object> files = (List<Object>) vb.getValue(context);

        writer.write("<table>");
        if (hasFiles(files)) {
            int fileCounter = 0;
            for (Object fileObj : files) {
                writer.write("<tr>");
                writer.write("<td>");
                String fileName;
                if (fileObj instanceof FileWithContentType) {
                    fileName = ((FileWithContentType) fileObj).fileName;
                } else {
                    fileName = ((File) fileObj).getName();
                }
                writer.write(StringEscapeUtils.escapeHtml(fileName));
                writer.write("</td>");
                writer.write("<td>");
                writer.write("<a class=\"icon-link\" ");
                writer.write("title=\"" + Application.getMessage(context, "delete") + "\" ");
                writer.write("style=\"background: url('" + context.getExternalContext().getRequestContextPath() + "/images/icons/delete.gif') no-repeat;\" ");
                writer.write("onclick=\"");
                writer.write(Utils.generateFormSubmit(context, this, uniqueId, EVENT_REMOVE + fileCounter));
                writer.write("\">");
                writer.write("</a>");
                writer.write("</td>");
                writer.write("</tr>");
                fileCounter++;
            }
        }
        writer.write("</table>");

        if (hasFiles(files)) {
            String successMsgKey = "default_file_uploaded";
            String attrSuccessMessageKey = (String) getAttributes().get(UploadFileGenerator.ATTR_SUCCESS_MSG_KEY);
            if (StringUtils.isNotBlank(attrSuccessMessageKey)) {
                successMsgKey = attrSuccessMessageKey;
            }
            writer.write("<div>");
            writer.write(Application.getMessage(context, successMsgKey));
            writer.write("</div>");
        }

        // Javascript functions with unique names
        writer.write("<script type='text/javascript' src='");
        writer.write(path);
        writer.write("/scripts/upload_helper.js'></script>\n");

        writer.write("<script type='text/javascript'>");
        writer.write("function handle_upload_" + jsSuffix + "(target)\n");
        writer.write("{\n");
        writer.write("  handle_upload_helper(target, '', upload_complete_" + jsSuffix + ", '" + path + "')\n");
        writer.write("}\n");

        writer.write("function upload_complete_" + jsSuffix + "(id, path, filename)\n");
        writer.write("{\n  ");
        writer.write(Utils.generateFormSubmit(context, this, uniqueId, EVENT_UPLOADED));
        writer.write("\n}\n");
        writer.write("</script>\n");

        writer.write("\n<input id='" + uniqueId
                + "-body:file-input' contentEditable='false' type='file' size='35' name='alfFileInput' onchange='javascript:handle_upload_" +
                jsSuffix + "(this)'/>");
    }

    public boolean hasFiles(List<Object> files) {
        return files != null && !files.isEmpty();
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        // Do nothing
    }

    @Override
    @SuppressWarnings("unchecked")
    public void decode(FacesContext context) {
        Map requestMap = context.getExternalContext().getRequestParameterMap();
        String event = (String) requestMap.get(getClientId(context));
        if (StringUtils.isBlank(event)) {
            return;
        }

        ValueBinding vb = getValueBinding("value");
        if (event.equals(EVENT_UPLOADED)) {
            FileUploadBean fileBean = (FileUploadBean) context.getExternalContext().getSessionMap().
                    get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
            if (fileBean != null) {
                // remove the file upload bean from the session
                // only this component instance now has the uploaded file
                context.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
                FileWithContentType valueToAdd = new FileWithContentType(fileBean.getFile(), fileBean.getContentType(), fileBean.getFileName());
                addValueToValueBinding(context, vb, valueToAdd);
            }
        } else if (event.startsWith(EVENT_REMOVE)) {
            Integer fileIndex;
            try {
                fileIndex = Integer.parseInt(event.substring(EVENT_REMOVE.length()));
            } catch (Exception e) {
                throw new RuntimeException("Could not get file index for remove action.", e);
            }
            if (fileIndex != null) {
                List<Object> uploadedFiles = (List<Object>) vb.getValue(context);
                Object removedValue = uploadedFiles.remove((int) fileIndex);
                vb.setValue(context, uploadedFiles);
                if (removedValue instanceof File) {
                    ValueBinding removedValues = getValueBinding(UploadFileGenerator.ATTR_REMOVED_VALUES);
                    addValueToValueBinding(context, removedValues, removedValue);
                }
            }
        }
    }

    private void addValueToValueBinding(FacesContext context, ValueBinding vb, Object valueToAdd) {
        @SuppressWarnings("unchecked")
        List<Object> uploadedFiles = (List<Object>) vb.getValue(context);
        if (uploadedFiles == null) {
            uploadedFiles = new ArrayList<Object>();
        }
        uploadedFiles.add(valueToAdd);
        vb.setValue(context, uploadedFiles);
    }
}
