<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<a:panel styleClass="column panel-100" id="file-change" label="#{msg.file_comment_blockname}">
   <h:panelGrid cellpadding="0" cellspacing="0" columns="2" columnClasses="propertiesLabel" >
      <h:outputText value="#{msg.comment}" />
      <h:inputTextarea id="name" value="#{DialogManager.bean.comment}" styleClass="focus expand19-200" />
   </h:panelGrid>
</a:panel>

