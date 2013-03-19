<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

         <a:panel id="props-panel" styleClass="column panel-100" label="#{msg.addressbook_org_data}">
            <r:propertySheetGrid id="node-props" value="#{DialogManager.bean.currentNode}" columns="1" externalConfig="true" mode="view" labelStyleClass="propertiesLabel" />
         </a:panel>

            <a:panel id="people-panel" styleClass="column panel-100 with-pager" label="#{msg.addressbook_org_persons}">

               <a:richList id="people-list" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" pageSize="#{BrowseBean.pageSizeContent}"
                  value="#{DialogManager.bean.orgPeople}" var="r" initialSortColumn="name" initialSortDescending="false" width="100%">
                  <%-- Primary column with name --%>
                  <a:column primary="true">
                     <f:facet name="header">
                        <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                     </f:facet>
                     <a:booleanEvaluator value="#{r['ab:activeStatus']}">
                        <h:graphicImage url="/images/icons/person.gif" />
                     </a:booleanEvaluator>
                     <a:booleanEvaluator value="#{!r['ab:activeStatus']}">
                        <h:graphicImage url="/images/icons/error.gif" />
                        <h:outputText value=" " />
                     </a:booleanEvaluator>
                     <a:actionLink id="ab-org-people-list-link1" value="#{r['ab:personFirstName']} #{r['ab:personLastName']} (#{r['ab:personId']})"
                        showLink="false" action="dialog:addressbookOrgPersonDetails" actionListener="#{AddressbookPersonDetailsDialog.setupViewEntry}">
                        <f:param id="ab-people-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                     </a:actionLink>
                     
                  </a:column>

                  <%-- Data columns --%>

                  <a:column>
                     <f:facet name="header">
                        <h:outputText value="#{msg.email}" />
                     </f:facet>
                     <h:outputText value="#{r['ab:email']}" />
                  </a:column>

                  <%-- Actions column --%>
                  <a:column actions="true">
                     <f:facet name="header">
                        <h:outputText value="#{msg.actions}" />
                     </f:facet>
                     <a:actionLink value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false" action="dialog:addressbookAddEdit"
                        actionListener="#{AddressbookAddEditDialog.setupEdit}" rendered="#{UserService.documentManager}">
                        <f:param name="nodeRef" value="#{r.nodeRef}" />
                     </a:actionLink>
                     <a:actionLink value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false" action="dialog:deleteDialog"
                        actionListener="#{DeleteDialog.setupDeleteDialog}" rendered="#{UserService.documentManager}">
                        <f:param name="nodeRef" value="#{r.nodeRef}" />
                        <f:param name="confirmMessagePlaceholder0" value="#{actionContext.name}" />
                        <f:param name="showObjectData" value="true" />
                        <f:param name="showConfirm" value="false" />
                     </a:actionLink>
                  </a:column>

                  <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
                  <a:dataPager styleClass="pager" />
               </a:richList>
            </a:panel>
         
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/addressbook/web/manage-contactgroups.jsp" />
         <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />