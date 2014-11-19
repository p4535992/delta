<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="limited-message" styleClass="message" rendered="#{DialogManager.bean.limited}">
   <h:outputText id="limited-message1" value="#{DialogManager.bean.limitedMessage}" />
   <h:outputText id="limited-message2" value="<br />" escape="false" rendered="#{DialogManager.bean.showShowAll}" />
   <a:actionLink id="remove-doc-limitation-link" value="#{msg.document_list_show_all}" actionListener="#{DialogManager.bean.getAllRows}" rendered="#{DialogManager.bean.showShowAll}" />   
</a:panel>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="limited-message" styleClass="message" rendered="#{DialogManager.bean.limited}">
   <h:outputText id="limited-message1" value="#{DialogManager.bean.limitedMessage}" />
   <h:outputText id="limited-message2" value="<br />" escape="false" rendered="#{DialogManager.bean.showShowAll}" />
   <a:actionLink id="remove-doc-limitation-link" value="#{msg.document_list_show_all}" actionListener="#{DialogManager.bean.getAllRows}" rendered="#{DialogManager.bean.showShowAll}" />   
</a:panel>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
