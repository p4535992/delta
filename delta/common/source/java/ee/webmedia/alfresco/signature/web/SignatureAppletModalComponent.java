package ee.webmedia.alfresco.signature.web;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class SignatureAppletModalComponent extends UICommand {

    private String operation = "";
    private String digestHex = "";
    private String certId = "";

    public void showModal() {
        operation = "PREPARE";
        digestHex = "";
        certId = "";
    }

    public void showModal(String digestHex, String certId) {
        operation = "FINALIZE";
        this.digestHex = digestHex;
        this.certId = certId;
    }

    public void closeModal() {
        operation = "";
        digestHex = "";
        certId = "";
    }

    public String getOperation() {
        return operation;
    }

    public String getDigestHex() {
        return digestHex;
    }

    public String getCertId() {
        return certId;
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (!isRendered() || StringUtils.isBlank(getOperation())) {
            return;
        }
        ResponseWriter out = context.getResponseWriter();
        ComponentUtil.writeModalHeader(out, getDialogId(context), Application.getMessage(context, "task_title_signatureTask"), "signatureModalWrap",
                "return cancelSign();");
        out.write(generateApplet(context));
        ComponentUtil.writeModalFooter(out);

        out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
        out.write("showModal('");
        out.write(getDialogId(context));
        out.write("');");
        out.write("});</script>");
    }

    private String generateApplet(FacesContext context) {
        StringBuilder sb = new StringBuilder();
        String path = context.getExternalContext().getRequestContextPath();

        sb.append("<div id=\"signature-div\" style=\"text-align: center\">\n");
        sb.append("   <p id=\"signWait\">Palun oodake...</p>\n");
        sb.append("</div>\n");

        sb.append("<script type=\"text/javascript\" src=\"" + path + "/scripts/idCard.js\"></script>\n");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("$jQ(document).ready(function() { performSigningPluginOperation('");
        sb.append(operation);
        sb.append("', '");
        sb.append(digestHex);
        sb.append("', '");
        sb.append(certId);
        sb.append("', '");
        sb.append(path);
        sb.append("') });\n");
        sb.append("</script>\n");

        return sb.toString();
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = operation;
        values[2] = digestHex;
        values[3] = certId;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        operation = (String) values[1];
        digestHex = (String) values[2];
        certId = (String) values[3];
    }

    private String getDialogId(FacesContext context) {
        return getClientId(context) + "_popup";
    }
}
