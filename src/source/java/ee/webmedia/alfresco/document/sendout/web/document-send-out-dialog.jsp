<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/tiny_mce/tiny_mce.js"></script>
<script language="javascript" type="text/javascript">
   tinyMCE.init({
      theme : "advanced",
      language : "<%=request.getLocale().getLanguage()%>",
      mode : "exact",
      relative_urls: false,
      elements : "editor",
      save_callback : "saveContent",
      plugins : "table",
      theme_advanced_toolbar_location : "top",
      theme_advanced_toolbar_align : "left",
      theme_advanced_buttons1_add : "fontselect,fontsizeselect",
      theme_advanced_buttons2_add : "separator,forecolor,backcolor",
      theme_advanced_buttons3_add_before : "tablecontrols,separator",
      theme_advanced_disable: "styleselect",
      extended_valid_elements : "a[href|target|name],font[face|size|color|style],span[class|align|style]"
   });
   function saveContent(id, content) {
      document.getElementById("dialog:dialog-body:editorOutput").value=content;
      return content;
   }
</script>
</f:verbatim>

<a:panel id="send-out-panel" label="#{DocumentSendOutDialog.panelTitle}" styleClass="panel-100" progressive="true">

   <a:booleanEvaluator value="#{not empty DocumentSendOutDialog.model.sendDesc}">
      <f:verbatim><div class="message"><strong></f:verbatim><h:outputText value="#{msg.document_send_out_desc}" /><f:verbatim></strong> </f:verbatim><h:outputText value="#{DocumentSendOutDialog.model.sendDesc}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   
   <h:panelGrid id="tableSendOut" columns="3" columnClasses="requiredField,propertiesLabel," >
   
      <h:outputText value="" />
      <h:outputText value="#{msg.document_allFiles}" />
      <h:panelGroup>
         <a:booleanEvaluator value="#{not empty DocumentSendOutDialog.model.files}">
            <h:selectManyCheckbox value="#{DocumentSendOutDialog.model.selectedFiles}" layout="pageDirection" >
               <f:selectItems value="#{DocumentSendOutDialog.model.files}" />
            </h:selectManyCheckbox>
         </a:booleanEvaluator>
         <a:booleanEvaluator value="#{empty DocumentSendOutDialog.model.files}">
            <h:outputText value="-" />
         </a:booleanEvaluator>
      </h:panelGroup>
      
      <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" styleClass="requiredField" />
      <h:outputText value="#{msg.document_recipients}" />
      <wm:multiValueEditor 
         varName="DocumentSendOutDialog.model" 
         props="recipientName,recipientEmail,recipientSendMode" 
         titles="document_name,document_email,document_send_mode" 
         pickerCallback="#{AddressbookDialog.searchContacts}" 
         setterCallback="#{DocumentSendOutDialog.fetchContactData}" 
         dialogTitleId="contacts_search_title" 
         propTypes="input,input,classificator::sendMode" />

      <h:outputText value="" />
      <h:outputText value="#{msg.document_additional_recipients}" />
      <wm:multiValueEditor 
         varName="DocumentSendOutDialog.model" 
         props="additionalRecipientName,additionalRecipientEmail,additionalRecipientSendMode" 
         titles="document_name,document_email,document_send_mode" 
         pickerCallback="#{AddressbookDialog.searchContacts}" 
         setterCallback="#{DocumentSendOutDialog.fetchContactData}" 
         dialogTitleId="contacts_search_title"
         propTypes="input,input,classificator::sendMode" />

      <h:outputText value="" />
      <h:outputText value="#{msg.document_send_mode}" />
      <h:panelGroup styleClass="no-icon">
         <h:selectOneMenu value="#{DocumentSendOutDialog.model.sendMode}">
            <f:selectItems value="#{DocumentSendOutDialog.sendModes}" />
         </h:selectOneMenu>
         <h:commandLink id="setSendModeBtn" value="#{msg.document_set_to_all}" actionListener="#{DocumentSendOutDialog.updateSendModes}" />
      </h:panelGroup>
      
      <h:outputText value="" />
      <h:outputText value="#{msg.document_zip}" />
      <h:selectBooleanCheckbox value="#{DocumentSendOutDialog.model.zip}" />
      
      <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" styleClass="requiredField" />
      <h:outputText value="#{msg.document_senderEmail}" />
      <h:inputText value="#{DocumentSendOutDialog.model.senderEmail}" maxlength="1024" />
      
      <h:outputText value="" />
      <h:outputText value="#{msg.document_send_subject}" />
      <h:inputText value="#{DocumentSendOutDialog.model.subject}" maxlength="1024" />
      
      <h:graphicImage value="/images/icons/required_field.gif" alt="#{msg.required_field}" styleClass="requiredField" />
      <h:outputText value="#{msg.document_send_content}" />
      <h:panelGroup>
         <f:verbatim><div id="editor"></f:verbatim>
         <h:outputText value="#{DocumentSendOutDialog.model.content}" escape="false" />
         <f:verbatim></div></f:verbatim>
         <h:inputHidden id="editorOutput" value="#{DocumentSendOutDialog.model.content}" />
      </h:panelGroup>
      
      <h:outputText value="" />
      <h:outputText value="#{msg.document_send_template}" />
      <h:panelGroup styleClass="no-icon">
         <h:selectOneMenu value="#{DocumentSendOutDialog.model.template}">
            <f:selectItems value="#{DocumentSendOutDialog.emailTemplates}" />
         </h:selectOneMenu>
         <h:commandLink id="setTemplateBtn" value="#{msg.document_set_template}" actionListener="#{DocumentSendOutDialog.updateTemplate}" />
      </h:panelGroup>
      
   </h:panelGrid>

</a:panel>