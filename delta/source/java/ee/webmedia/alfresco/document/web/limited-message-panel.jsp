<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator value="#{DialogManager.bean.documentListLimited}">
   <a:panel id="limited-message" styleClass="message">
      <h:outputText value="#{DialogManager.bean.limitedMessage}" />
      <h:outputText value="<br />" escape="false" />
      <a:actionLink id="remove-doc-limitation-link" value="#{msg.document_list_show_all}"  actionListener="#{DialogManager.bean.getAllDocsWithoutLimit}" />   
   </a:panel>
</a:booleanEvaluator>