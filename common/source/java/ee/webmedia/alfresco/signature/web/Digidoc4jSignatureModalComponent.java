package ee.webmedia.alfresco.signature.web;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class Digidoc4jSignatureModalComponent extends UICommand {

    private String operation = "";
   
    public void showModal() {
        operation = "PREPARE";
    }

    public void showModal(String digestHex, String certId) {
        operation = "FINALIZE";
    }

    public void closeModal() {
        operation = "";
   
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (!isRendered() || StringUtils.isBlank(getOperation())) {
            return;
        }
        ResponseWriter out = context.getResponseWriter();
        ComponentUtil.writeModalHeader(out, getDialogId(context), Application.getMessage(context, "task_title_signatureTask"), "signatureModalWrap",
                "return cancelSign();");
        out.write(generateSignaturePlace(context));
        ComponentUtil.writeModalFooter(out);

        out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
        out.write("showModal('");
        out.write(getDialogId(context));
        out.write("');");
        out.write("});</script>");
    }

    private String generateSignaturePlace(FacesContext context) {
        StringBuilder sb = new StringBuilder();
        String path = context.getExternalContext().getRequestContextPath();

        sb.append("<div id=\"signature-div\" style=\"text-align: center\">\n");
        sb.append("   <p id=\"signWait\">Palun oodake...</p>\n");
        sb.append("</div>\n");
        
        sb.append("<script type=\"text/javascript\" src=\"" + path + "/scripts/hwcrypto.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + path + "/scripts/hwcrypto-legacy.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + path + "/scripts/hex2base.js\"></script>\n");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("$jQ(document).ready(function() { signDigidoc4j() });\n");
        sb.append("</script>\n");

        return sb.toString();
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = operation;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        operation = (String) values[1];
    }

    private String getDialogId(FacesContext context) {
        return getClientId(context) + "_popup";
    }
}
