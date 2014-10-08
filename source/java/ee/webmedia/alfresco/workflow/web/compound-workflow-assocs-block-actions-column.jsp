<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:column id="col-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}" rendered="#{CompoundWorkflowDialog.showAssocActions}">
   <a:actionLink id="col-actions-act1" value="#{r.docName}" image="/images/icons/import.gif" showLink="false" action="#cwf-assoc-panel"
      actionListener="#{CompoundWorkflowAssocSearchBlock.addAssocDocHandler}" tooltip="#{msg.compoundWorkflow_search_add_assoc}" >
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:column id="col-actions" actions="true" styleClass="actions-column2 #{r.cssStyleClass}" rendered="#{CompoundWorkflowDialog.showAssocActions}">
   <a:actionLink id="col-actions-act1" value="#{r.docName}" image="/images/icons/import.gif" showLink="false" action="#cwf-assoc-panel"
      actionListener="#{CompoundWorkflowAssocSearchBlock.addAssocDocHandler}" tooltip="#{msg.compoundWorkflow_search_add_assoc}" >
      <f:param name="nodeRef" value="#{r.node.nodeRef}" />
   </a:actionLink>
>>>>>>> develop-5.1
</a:column>