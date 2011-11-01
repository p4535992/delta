package ee.webmedia.alfresco.common.propertysheet.upload;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.IClonable;

public class UploadFileInput extends UIInput implements NamingContainer {

    public static class FileWithContentType implements Serializable, IClonable<FileWithContentType> {
        private static final long serialVersionUID = 1L;

        public File file;
        public String contentType;
        public String fileName;

        public FileWithContentType(File file, String contentType, String fileName) {
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
        Object file = vb.getValue(context);

        if (file != null) {
            writer.write("<a class=\"icon-link\" ");
            writer.write("title=\"" + Application.getMessage(context, "delete") + "\" ");
            writer.write("style=\"background-image: url(/simdhs/images/icons/delete.gif);\" ");
            writer.write("onclick=\"");
            writer.write(Utils.generateFormSubmit(context, this, uniqueId, EVENT_REMOVE));
            writer.write("\">");
            writer.write("</a>");
            // FIXME: Kaarel - see pole küll õige koht töövoo spetsiifiliste teadete jaoks(pealegi seda teadet common'i projektis pole)
            String successMsgKey = "opinion_file_uploaded";
            String attrSuccessMessageKey = (String) getAttributes().get(UploadFileGenerator.ATTR_SUCCESS_MSG_KEY);
            if (StringUtils.isNotBlank(attrSuccessMessageKey)) {
                successMsgKey = attrSuccessMessageKey;
            }
            writer.write(Application.getMessage(context, successMsgKey));
        } else {
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

        ValueBinding value = getValueBinding("value");
        if (event.equals(EVENT_UPLOADED)) {
            FileUploadBean fileBean = (FileUploadBean) context.getExternalContext().getSessionMap().
                    get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
            if (fileBean != null) {
                // remove the file upload bean from the session
                // only this component instance now has the uploaded file
                context.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);

                value.setValue(context, new FileWithContentType(fileBean.getFile(), fileBean.getContentType(), fileBean.getFileName()));
            }
        } else if (event.equals(EVENT_REMOVE)) {
            value.setValue(context, null);
        }
    }
}
