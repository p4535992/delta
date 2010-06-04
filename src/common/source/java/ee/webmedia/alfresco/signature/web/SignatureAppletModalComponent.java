package ee.webmedia.alfresco.signature.web;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.component.UIOutputText;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;

public class SignatureAppletModalComponent extends UIOutputText {

    private String operation; 
    private String digest; 
    
    public void showModal() {
        operation = "PREPARE";
        digest = "";
    }

    public void showModal(String digestHex) {
        operation = "FINALIZE";
        digest = digestHex;
    }

    public void closeModal() {
        operation = null;
        digest = null;
    }
    
    public String getDigestHex() {
        return digest;
    }
    
    public String getOperation() {
        return operation;
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
        
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("function processCert(cert, token) {\n");
        sb.append("  $jQ('#signApplet').hide();\n");
        sb.append("  $jQ('#signWait').show();\n");
        sb.append("  return oamSubmitForm('dialog','dialog:dialog-body:processCert',null,[['cert', cert]]);\n");
        sb.append("}\n");
        
        sb.append("function signDocument(signature) {\n");
        sb.append("  $jQ('#signApplet').hide();\n");
        sb.append("  $jQ('#signWait').show();\n");
        sb.append("  return oamSubmitForm('dialog','dialog:dialog-body:signDocument',null,[['signature', signature]]);\n");
        sb.append("}\n");
        
        sb.append("function cancelSign() {\n");
        sb.append("  $jQ('#signApplet').hide();\n");
        sb.append("  $jQ('#signWait').show();\n");
        sb.append("  return oamSubmitForm('dialog','dialog:dialog-body:cancelSign',null,[[]]);\n");
        sb.append("}\n");
        
        sb.append("function driverError() {\n");
        sb.append("}\n");
        sb.append("</script>\n");
        
        sb.append("      <div id=\"signature-div\" style=\"text-align: center\"><object classid=\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\" width=\"400\" height=\"80\"\n");
        sb.append("         codebase=\"https://java.sun.com/update/1.6.0/jinstall-6u18-windows-i586.cab#Version=1,4,0,0\" id=\"signApplet\">\n");
        sb.append("         <param name=\"java_code\" value=\"SignApplet.class\" />\n");
        sb.append("         <param name=\"java_codebase\" value=\"" + path + "/applet\" />\n");
        sb.append("         <param name=\"java_archive\" value=\"SignApplet_sig.jar, iaikPkcs11Wrapper_sig.jar\" />\n");
        sb.append("         <param name=\"name\" value=\"SignApplet1\" />\n");
        sb.append("         <param name=\"mayscript\" value=\"true\" />\n");
        sb.append("         <param name=\"java_type\" value=\"application/x-java-applet;version=1.4\" />\n");
        sb.append("         <param name=\"scriptable\" value=\"true\" />\n");
        sb.append("         <param name=\"LANGUAGE\" value=\"EST\" />\n");
        sb.append("         <param name=\"FUNC_SET_CERT\" value=\"window.processCert\" />\n");
        sb.append("         <param name=\"FUNC_SET_SIGN\" value=\"window.signDocument\" />\n");
        sb.append("         <param name=\"FUNC_CANCEL\" value=\"window.cancelSign\" />\n");
        sb.append("         <param name=\"FUNC_DRIVER_ERR\" value=\"window.driverError\" />\n");
        sb.append("         <param name=\"DEBUG_LEVEL\" value=\"3\" />\n");
        sb.append("         <param name=\"OPERATION\" value=\"");
        sb.append(getOperation());
        sb.append("\" />\n");
        sb.append("         <param name=\"HASH\" value=\"");
        sb.append(getDigestHex());
        sb.append("\" />\n");
        sb.append("         <param name=\"TOKEN_ID\" value=\"\" />\n");
        sb.append("         <param name=\"legacy_lifecycle\" value=\"true\" />\n");
        sb.append("\n");
        sb.append("         <embed id=\"signApplet\" type=\"application/x-java-applet;version=1.4\" width=\"400\" height=\"80\"\n");
        sb.append("            pluginspage=\"http://javadl.sun.com/webapps/download/GetFile/1.6.0_18-b07/windows-i586/xpiinstall.exe\" java_code=\"SignApplet.class\"\n");
        sb.append("            java_codebase=\"" + path + "/applet\" java_archive=\"SignApplet_sig.jar, iaikPkcs11Wrapper_sig.jar\" NAME=\"SignApplet\" MAYSCRIPT=\"true\"\n");
        sb.append("            LANGUAGE=\"EST\" FUNC_SET_CERT=\"window.processCert\" FUNC_SET_SIGN=\"window.signDocument\" FUNC_CANCEL=\"window.cancelSign\"\n");
        sb.append("            FUNC_DRIVER_ERR=\"window.driverError\" DEBUG_LEVEL=\"3\"\n");
        sb.append("            OPERATION=\"");
        sb.append(getOperation());
        sb.append("\"\n");
        sb.append("            HASH=\"");
        sb.append(getDigestHex());
        sb.append("\" TOKEN_ID=\"\" LEGACY_LIFECYCLE=\"true\">\n");
        sb.append("         <noembed></noembed>\n");
        sb.append("         </embed></object>\n");
        sb.append("         <p id=\"signWait\" style=\"display: none;\">Palun oodake...</p>\n");
        sb.append("      </div>\n");
        
        return sb.toString();
    }

    public Object saveState(FacesContext context)
    {
       Object[] values = new Object[3];
       values[0] = super.saveState(context);
       values[1] = operation;
       values[2] = digest;
       return values;
    }

    public void restoreState(FacesContext context, Object state)
    {
       Object[] values = (Object[]) state;
       super.restoreState(context, values[0]);
       operation = (String) values[1];
       digest = (String) values[2];
    }
    
    private String getDialogId(FacesContext context) {
        return getClientId(context) + "_popup";       
    }
}
