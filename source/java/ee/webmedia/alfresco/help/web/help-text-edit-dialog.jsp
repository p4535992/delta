<%@page import="javax.faces.context.FacesContext"%>
<%@page import="org.alfresco.web.app.Application"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="metadata-panel" label="#{msg.help_text_mgmt_title}" styleClass="panel-100" progressive="true">
  <r:propertySheetGrid id="help-text-metatada" value="#{DialogManager.bean.node}" columns="1" mode="edit" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<f:verbatim>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/tiny_mce/tiny_mce.js"></script>
<script language="javascript" type="text/javascript">
   tinyMCE.init({
      theme : "advanced",
      language : "<%=Application.getLanguage(FacesContext.getCurrentInstance()).getLanguage()%>",
      mode : "exact",
      elements : "dialog:dialog-body:help-text-metatada:prop_hltx003a_content:hltx003a_content",
      relative_urls: false,
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
   function propSheetValidateCustom() {
      tinyMCE.triggerSave();
      return true;
   }
</script>
</f:verbatim>
