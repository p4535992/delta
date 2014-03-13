<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.faces.context.FacesContext" %>
<%@ page import="org.alfresco.web.app.Application" %>

<f:verbatim>
   <script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/tiny_mce/tiny_mce.js"></script>
</f:verbatim>

<a:panel id="notification-list-panel" label="#{msg.notification_important_notification}">
   <r:propertySheetGrid id="notificationPropSheet" value="#{NotificationDetailsDialog.notification}" externalConfig="true" labelStyleClass="propertiesLabel" columns="1" />
</a:panel>

<f:verbatim>
   <script language="javascript" type="text/javascript">
      tinyMCE.init({
         theme : "advanced",
         language : "<%=Application.getLanguage(FacesContext.getCurrentInstance()).getLanguage()%>",
         mode : "specific_textareas",
         editor_selector : "wysiwygEditor", 
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
         entity_encoding : "raw",
         auto_focus : "dialog:dialog-body:notificationPropSheet:prop_ntfx003a_message:ntfx003a_message",
         setup : function(editor) {
            editor.onKeyUp.add(function(editor) {
               if (editor.isDirty()) {
                  editor.save();
                  processButtonState();
               }
            });
         }
      });
   </script>
</f:verbatim>
