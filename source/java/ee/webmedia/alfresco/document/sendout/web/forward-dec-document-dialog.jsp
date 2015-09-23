<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="forwardDecDocument-panel" label="#{msg.document_forward_dec_document_title}" styleClass="panel-100" progressive="true" >
	
    <h:panelGrid columns="2" border="0" columnClasses="propertiesLabel," width="100%">

      <h:panelGroup>
         <f:verbatim><span></f:verbatim><h:outputText value="#{msg.document_recipients}" /><f:verbatim></span></f:verbatim>
      </h:panelGroup>
      
      <wm:multiValueEditor 
         varName="ForwardDecDocumentDialog.model"
         propsGeneration="recipientName¤TextAreaGenerator¤styleClass=expand19-200¤readonly=true"
         hiddenPropNames="recipientGroup,recipientEmail"
         groupByColumnName="recipientGroup"
         groupRowControls="sendOut"
         titles="document_name"
         pickerCallback="#{CompoundWorkflowDefinitionDialog.executeDecDocumentForwardCapableSearch}"
         preprocessCallback="#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}"
         setterCallback="#{ForwardDecDocumentDialog.addContactData}"
         dialogTitleId="contacts_search_title"
         filterIndex="4"
         styleClass=""
          />
        
    </h:panelGrid>
  
    <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/metadata-block-lockRefresh.jsp" />
    
</a:panel>