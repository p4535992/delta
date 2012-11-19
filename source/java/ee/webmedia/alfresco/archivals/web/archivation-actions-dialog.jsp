<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="archivation-actions-panel" styleClass="panel-100 with-pager" label="#{msg.archivation_actions}" >

   <a:richList id="archivationActionsList" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
      value="#{ArchivationActionsDialog.archivationActions}" var="r" refreshOnBind="true">
      
      <a:column id="col1" primary="true">
         <f:facet name="header">
            <h:outputText id="col1-sort" value="#{msg.archivation_actions}" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-text" value="#{r.first}" action="#{r.getSecond}" />
      </a:column>
      
   </a:richList>
   
</a:panel>