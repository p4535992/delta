<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<a:panel styleClass="column panel-100" id="file-change" label="#{msg.file_change_blockname}">
   <h:panelGrid cellpadding="0" cellspacing="0" columns="2" columnClasses="propertiesLabel" >
      <h:panelGroup>
         <f:verbatim escape="false"><span class="red">* </span></f:verbatim>
         <h:outputText value="#{msg.name}" />            
      </h:panelGroup>         
      <h:inputTextarea id="name" value="#{DialogManager.bean.fileName}" styleClass="focus expand19-200" />
   </h:panelGrid>
</a:panel>

