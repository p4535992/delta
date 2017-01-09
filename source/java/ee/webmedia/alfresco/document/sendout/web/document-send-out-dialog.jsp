<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/message-input-helper-js.jsp" />

<h:panelGroup rendered="#{DocumentSendOutDialog.modalId != null}">
   <h:panelGroup id="dialog-modal-container" binding="#{DocumentSendOutDialog.modalContainer}" />
   <f:verbatim>
      <script type="text/javascript">
         $jQ(document).ready(function () {
            showModal("</f:verbatim><a:outputText value="#{DocumentSendOutDialog.modalId}" /><f:verbatim>");
         });
      </script>
   </f:verbatim>
</h:panelGroup>

<f:verbatim>
   <script type="text/javascript">
      var excludedElementsSpecific = new Array();
      function disableUnlockOnAddingPersonAndActionLinks() {
         var element = $jQ(".add-person")[0];
         excludedElementsSpecific.push(element.id);
         var elements = $jQ(".disableUnlock");
         for(var i = 0; elements != null && i < elements.length; i++) {
            excludedElementsSpecific.push(elements[i].id);
         }
      }
      $jQ(document).ready(function () {
         disableUnlockOnAddingPersonAndActionLinks();
      });
   </script>
</f:verbatim>

<a:panel id="send-out-panel" label="#{DocumentSendOutDialog.panelTitle}" styleClass="panel-100" progressive="true">

   <a:booleanEvaluator value="#{not empty DocumentSendOutDialog.model.sendDesc}">
      <f:verbatim><div class="message"><strong></f:verbatim><h:outputText value="#{msg.document_send_out_desc}" /><f:verbatim></strong> </f:verbatim><h:outputText value="#{DocumentSendOutDialog.model.sendDesc}" /><f:verbatim></div></f:verbatim>
   </a:booleanEvaluator>
   
   <h:panelGrid id="tableSendOut" columns="2" columnClasses="propertiesLabel," >
   
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_allFiles}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
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
      
      <h:panelGroup>
         <f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_recipients}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <wm:multiValueEditor 
         varName="DocumentSendOutDialog.model" 
         propsGeneration="
          recipientName¤TextAreaGenerator¤styleClass=expand19-200
         ,recipientId¤TextAreaGenerator¤styleClass=expand19-200 sendOutIdCode
         ,recipientEmail¤TextAreaGenerator¤styleClass=expand19-200
         ,recipientSendMode¤ClassificatorSelectorGenerator¤classificatorName=sendMode¤styleClass=width120 resetSendOutGroupSendMode
         ,recipientDvkCapable¤LabelGenerator¤defaultValue=Ei¤styleClass=tiny readonly
         "
         hiddenPropNames="recipientGroup"
         groupByColumnName="recipientGroup"
         groupRowControls="sendOut"
         titles="document_name,document_recipient_id,document_email,document_send_mode,document_dvk_capable" 
         pickerCallback="#{CompoundWorkflowDefinitionDialog.executeOwnerSearch}"
         preprocessCallback="#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}"
         setterCallback="#{DocumentSendOutDialog.fetchContactData}"
         dialogTitleId="contacts_search_title"
         filters="#{UserContactGroupSearchBean.usersGroupsContactsGroupsFilters}"
         filterIndex="4"
         styleClass="hiddenIdCode"
          />

      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_send_mode}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:panelGroup styleClass="no-icon">
         <h:selectOneMenu value="#{DocumentSendOutDialog.model.sendMode}">
            <f:selectItems value="#{DocumentSendOutDialog.sendModes}" />
         </h:selectOneMenu>
         <h:commandLink id="setSendModeBtn" value="#{msg.document_set_to_all}" actionListener="#{DocumentSendOutDialog.updateSendModes}" styleClass="disableUnlock"/>
      </h:panelGroup>
      
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_zip}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:selectBooleanCheckbox value="#{DocumentSendOutDialog.model.zip}" />
      
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_encrypt}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:selectBooleanCheckbox value="#{DocumentSendOutDialog.model.encrypt}" />
      
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText id="subject" value="#{msg.document_send_subject}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:inputTextarea value="#{DocumentSendOutDialog.model.subject}" styleClass="expand19-200 long"/>
      
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_send_template}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:panelGroup styleClass="no-icon">
         <h:selectOneMenu id="templateSelect" value="#{DocumentSendOutDialog.model.template}">
            <f:selectItems value="#{DocumentSendOutDialog.emailTemplates}" />
         </h:selectOneMenu>
         <h:commandLink id="setTemplateBtn" value="#{msg.document_set_template}" actionListener="#{DocumentSendOutDialog.updateTemplate}" styleClass="disableUnlock" />
         <a:booleanEvaluator value="#{not empty DocumentSendOutDialog.model.sendoutInfo}">
            <f:verbatim><br/><br/><div class="message"></f:verbatim><h:outputText value="#{DocumentSendOutDialog.model.sendoutInfo}" /><f:verbatim></div></f:verbatim>
         </a:booleanEvaluator>                  
      </h:panelGroup>
      
      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_send_content}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      <h:inputTextarea id="editor" value="#{DocumentSendOutDialog.model.content}" rows="5" cols="40" />
      
   </h:panelGrid>

   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />

</a:panel>
