<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<h:panelGroup id="orgCertificates-panel-facets">
   <f:facet name="title">
      <a:booleanEvaluator value="#{DialogManager.bean == AddressbookAddEditDialog}">
         <a:actionLink id="updateCertificates" tooltip="#{msg.addressbook_org_certificates_update_tooltip}" value="#{msg.addressbook_org_certificates_update}" 
            actionListener="#{AddressbookAddEditDialog.updateOrgCertificates}" rendered="#{AddressbookAddEditDialog.allowUpdateCertificates}">
         </a:actionLink>
      </a:booleanEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="orgCerts-panel" styleClass="column panel-100" label="#{msg.addressbook_org_certificates}" facetsId="dialog:dialog-body:orgCertificates-panel-facets">

   <a:richList id="certificates-list" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{DialogManager.bean.orgCertificates}" var="r" initialSortColumn="certName" initialSortDescending="false" width="100%">
      <%-- Primary column with name --%>
      <a:column primary="true">
         <f:facet name="header">
            <a:sortLink label="#{msg.addressbook_org_cert_name}" value="certName" mode="case-insensitive" styleClass="header" />
         </f:facet>
         <h:outputText value="#{r.certName}" />
      </a:column>

      <%-- Data columns --%>

      <a:column>
         <f:facet name="header">
            <h:outputText value="#{msg.addressbook_org_cert_valid_to}" />
         </f:facet>
         <h:outputText value="#{r.certValidTo}" />
      </a:column>
      
      <a:column>
         <f:facet name="header">
            <h:outputText value="#{msg.addressbook_org_cert_description}" />
         </f:facet>
         <h:inputText value="#{r.certDescription}" rendered="#{DialogManager.bean == AddressbookAddEditDialog}"/>
         <h:outputText value="#{r.certDescription}" rendered="#{DialogManager.bean != AddressbookAddEditDialog}"/>
      </a:column>

      <%-- Actions column --%>
      <a:column actions="true">
         <a:actionLink id="actionsCol-del" value="" actionListener="#{AddressbookAddEditDialog.removeOrgCert}" rendered="#{DialogManager.bean == AddressbookAddEditDialog}"
             showLink="false" image="/images/icons/delete.gif" tooltip="#{msg.addressbook_org_cert_action_remove}" styleClass="remove_doc_types_field" >
                  <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
      </a:column>
   </a:richList>
</a:panel>

<f:verbatim>
<script type="text/javascript">
   // confirm removing
   function confirmRemoveOrgCert(msg, e){
      var orgCertName = $jQ(e).closest('tr').children().eq(0).text();
      return confirmWithPlaceholders(msg, orgCertName);
   }
   prependOnclick($jQ(".remove_doc_types_field"), function(e) {
     var msg = '<%= MessageUtil.getMessageAndEscapeJS("addressbook_org_cert_action_remove_confirm") %>';
     return confirmRemoveOrgCert(msg, e);
   });
</script>
</f:verbatim>
