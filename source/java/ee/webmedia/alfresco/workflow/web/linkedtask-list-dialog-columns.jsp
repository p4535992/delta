<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

      <%-- dueDate --%>
      <a:column id="dueDate" primary="true" styleClass="#{r.cssStyleClass}" style="width: 10%;" >
         <f:facet name="header">
            <a:sortLink id="dueDate-sort" label="#{msg.task_property_dueDate}" value="dueDate" styleClass="header" />
         </f:facet>
         <h:outputText id="dueDate-text" value="#{r.dueDateStr}" />
      </a:column>

      <%-- resolution --%>
      <a:column id="resolution" primary="true" styleClass="#{r.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="resolution-sort" label="#{msg.task_property_resolution}" value="workflowResolution" styleClass="header" />
         </f:facet>
         <h:outputText id="resolution-text" value="#{r.workflowResolution}"/>                           
      </a:column>

      <%-- creatorName --%>
      <a:column id="creatorName" primary="true" styleClass="#{r.cssStyleClass}" style="width: 10%;">
         <f:facet name="header">
            <a:sortLink id="creatorName-sort" label="#{msg.task_property_creator_name}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText id="creatorName-text" value="#{r.creatorName}" />
      </a:column>

      <%-- Title --%>
      <a:column id="title" styleClass="#{r.cssStyleClass}" style="width: 15%;">
         <f:facet name="header">
            <a:sortLink id="title-sort" label="#{msg.document_docName}" value="compoundWorkflowTitle" styleClass="header" />
         </f:facet>
         <a:actionLink id="title-text2" value="#{r.compoundWorkflowTitle}" tooltip="#{r.compoundWorkflowTitle}" href="#{r.originalTaskObjectUrl}" target="_blank" styleClass="tooltip condence50- no-underline originalTaskObjectUrl" />
      </a:column>
      
      <a:column id="colx" primary="true" styleClass="#{r.cssStyleClass}" >
         <f:facet name="header">
            <a:sortLink id="colx-sort" label="#{msg.task_property_comment}" value="compoundWorkflowComment" styleClass="header" />
         </f:facet>
         <h:outputText id="creatorName-text" value="#{r.compoundWorkflowComment}" title="#{r.compoundWorkflowComment}" styleClass="tooltip condence50-" />         
      </a:column>      
