package ee.webmedia.alfresco.signature.web;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.component.UIOutputText;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;

public class SignatureAppletModalComponent extends UIOutputText {

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
    public void encodeEnd(FacesContext context) throws IOException
    {
       if (!isRendered() || StringUtils.isBlank(getOperation())) return;
       ResponseWriter out = context.getResponseWriter();

       out.write("<div id=\"overlay\" style=\"display: block;\"></div>");
       out.write("<div id=\"");
       out.write(getDialogId(context));
       out.write("\" class=\"modalpopup modalwrap\" style=\"display: block; height: 143px;\">");
       out.write("<div class=\"modalpopup-header clear\"><h1>");
       out.write(org.alfresco.web.app.Application.getMessage(context, "task_title_signatureTask"));
       out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"return cancelSign();\">");
       out.write(org.alfresco.web.app.Application.getMessage(context, SearchRenderer.CLOSE_WINDOW_MSG));
       out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner\">");

       out.write(generateApplet(context));

       out.write("</div></div></div>");

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
        sb.append("   <div id=\"pluginLocation\" style=\"display: none;\"></div>\n");
        sb.append("   <p id=\"signWait\">Palun oodake...</p>\n");
        sb.append("</div>\n");

        sb.append("<script type=\"text/javascript\">\n");
        sb.append("$jQ(document).ready(function() { loadSigningPlugin('");
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

    public Object saveState(FacesContext context)
    {
       Object[] values = new Object[4];
       values[0] = super.saveState(context);
       values[1] = operation;
       values[2] = digestHex;
       values[3] = certId;
       return values;
    }

    public void restoreState(FacesContext context, Object state)
    {
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