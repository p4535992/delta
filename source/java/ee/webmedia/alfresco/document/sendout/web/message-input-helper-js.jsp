<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page import="javax.faces.context.FacesContext" %>
<%@ page import="org.alfresco.web.app.Application" %>

<f:verbatim>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/tiny_mce/tiny_mce.js"></script>
<script language="javascript" type="text/javascript">
   tinyMCE.init({
      theme : "advanced",
      language : "<%=Application.getLanguage(FacesContext.getCurrentInstance()).getLanguage()%>",
      mode : "exact",
      relative_urls: false,
      elements : "dialog:dialog-body:editor",
      plugins : "table",
      theme_advanced_toolbar_location : "top",
      theme_advanced_toolbar_align : "left",
      theme_advanced_buttons1_add : "fontselect,fontsizeselect",
      theme_advanced_buttons2_add : "separator,forecolor,backcolor",
      theme_advanced_buttons3_add_before : "tablecontrols,separator",
      theme_advanced_disable: "styleselect",
      extended_valid_elements : "a[href|target|name],font[face|size|color|style],span[class|align|style]",
      width : "600",
      height : "315",
      entity_encoding : "raw"
   });
</script>
</f:verbatim>