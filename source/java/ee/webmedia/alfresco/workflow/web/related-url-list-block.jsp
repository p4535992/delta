<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="cwf-related-url-panel-facets" styleClass="nonfloating-element" >
   <f:facet name="title">
      <a:actionLink image="/images/icons/import.gif" id="act-link-add-url" showLink="false" tooltip="#{msg.compoundWorkflow_relatedUrl_add}" value="" 
         actionListener="#{RelatedUrlDetailsDialog.addRelatedUrl}" action="dialog:relatedUrlDetailsDialog" rendered="#{RelatedUrlListBlock.showLinkActions}" />
   </f:facet>
</h:panelGroup>

<a:panel id="cwf-related-url-panel" styleClass="panel-100 with-pager" label="#{RelatedUrlListBlock.listTitle}" progressive="true" facetsId="dialog:dialog-body:cwf-related-url-panel-facets"
	expanded="#{RelatedUrlListBlock.expanded}">

   <%-- Main List --%>
   <a:richList id="relatedUrlList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{RelatedUrlListBlock.relatedUrls}" var="r" refreshOnBind="true" >
      
      <a:column id="col0" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col0-sort" label="#{msg.compoundWorkflow_relatedUrl_url}" value="url" styleClass="header" />
         </f:facet>
         <a:actionLink id="col1-link" value="#{r.urlCondenced}" action="#{DocumentDialog.action}" href="#{r.url}" target="#{r.target}" tooltip="#{r.url}"/>         
      </a:column>
      
      <a:column id="col1" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.compoundWorkflow_relatedUrl_urlComment}" value="urlComment" styleClass="header" />
         </f:facet>
         <h:outputText  id="col1-text" value="#{r.urlComment}" styleClass="condence150"/>
      </a:column>       
      
      <a:column id="col2" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.compoundWorkflow_relatedUrl_urlCreatorName}" value="urlCreatorName" styleClass="header" />
         </f:facet>
         <h:outputText  id="col2-text" value="#{r.urlCreatorName}" />
      </a:column>
      
      <a:column id="col3" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.compoundWorkflow_relatedUrl_created}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText  id="col3-text" value="#{r.createdStr}" />
      </a:column>  
      
      <a:column id="col-related-url-actions" actions="true" styleClass="actions-column2">
            <a:actionLink id="col-actions-edit" value="" actionListener="#{RelatedUrlDetailsDialog.showDetails}" showLink="false" action="dialog:relatedUrlDetailsDialog"
                   image="/images/icons/edit_properties.gif" tooltip="#{msg.compoundWorkflow_relatedUrl_edit}" rendered="#{RelatedUrlListBlock.showLinkActions}" > 
                   <f:param id="url-param-1" name="urlNodeRef" value="#{r.nodeRef}"/>
                   <f:param id="url-param-2" name="urlIndexInWorkflow" value="#{r.indexInWorkflow}"/>
             </a:actionLink>       
            <a:actionLink id="col-actions-delete" value="" actionListener="#{RelatedUrlListBlock.delete}" showLink="false"
                   image="/images/icons/delete.gif" tooltip="#{msg.compoundWorkflow_relatedUrl_delete}" rendered="#{RelatedUrlListBlock.showLinkActions}" > 
                   <f:param id="url-param-3" name="nodeRef" value="#{r.nodeRef}"/>
                   <f:param id="url-param-4" name="urlIndexInWorkflow" value="#{r.indexInWorkflow}"/>
             </a:actionLink> 
       </a:column>       

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager-cwf-related-url-block" styleClass="pager" />
   </a:richList>

</a:panel>