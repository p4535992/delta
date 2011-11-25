<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">

$jQ(document).ready(function(){
   updateButtonState();
})

function updateButtonState()
{
   if (document.getElementById("dialog:dialog-body:search-text").value.trim().length == 0)
   {
      document.getElementById("dialog:dialog-body:search-btn").disabled = true;
   }
   else
   {
      document.getElementById("dialog:dialog-body:search-btn").disabled = false;
   }
}
   $jQ("#" + escapeId4JQ("dialog:dialog-body:search-text")).live('keydown', function(event) {
      if (event.keyCode == 13) {
         $jQ("#" + escapeId4JQ("dialog:dialog-body:search-btn")).click();
          return false;
      }
   });
</script>
</f:verbatim>

            <%-- Organizations List --%>
            <a:panel id="ab-addressbook-panel" styleClass="mainSubTitle" label="#{msg.addressbook}">
            <a:panel id="search-panel">
            <h:inputText id="search-text" styleClass="admin-user-search-input focus" value="#{AddressbookListDialog.searchCriteria}" size="35" maxlength="1024" onkeyup="updateButtonState();" />
            <h:commandButton id="search-btn" value="#{msg.search}" action="#{AddressbookListDialog.search}" disabled="true" style="margin-left: 5px;"/>
            <h:commandButton id="show-all-button" value="#{msg.show_all}" action="#{AddressbookListDialog.showAll}" style="margin-left: 5px;"/>
            <f:verbatim>
            <script type="text/javascript">
               addSearchSuggest("dialog:dialog-body:search-text", "dialog:dialog-body:search-text", "AddressbookSearchBean.searchContacts", "<%=request.getContextPath()%>/ajax/invoke/AjaxSearchBean.invokeActionListener?actionListener=AddressbookSearchBean.setupViewEntry",
                     function() {
                        var type = this.slice(this.lastIndexOf("(") + 1, this.lastIndexOf(")")); // eraisik or organisatsioon
                        $jQ("#" + type).click();
               });
            </script>
            </f:verbatim>
            <a:actionLink id="eraisik" value="" showLink="false" action="dialog:addressbookPersonDetails" />
            <a:actionLink id="organisatsioon" value="" showLink="false" action="dialog:addressbookOrgDetails" />
            </a:panel>
            <a:panel id="ab-org-panel" styleClass="with-pager" label="#{msg.addressbook_organizations}">

                        <a:richList id="ab-organizations-list" viewMode="details" width="100%"
                           rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" pageSize="#{BrowseBean.pageSizeContent}" refreshOnBind="true"
                           value="#{AddressbookListDialog.organizations}" var="r" initialSortColumn="name" initialSortDescending="false">

                           <%-- Primary column with name --%>
                           <a:column id="ab-org-list-column" primary="true">
                              <f:facet name="header">
                                 <a:sortLink id="ab-org-list-sort" label="#{msg.name}" value="name" mode="case-insensitive" />
                              </f:facet>
                              <a:booleanEvaluator id="ab-org-list-eval1" value="#{r['ab:activeStatus']}">
                                 <h:graphicImage id="ab-org-list-eval1-img" url="/images/icons/person.gif" />
                              </a:booleanEvaluator>
                              <a:booleanEvaluator id="ab-org-list-eval2" value="#{!r['ab:activeStatus']}">
                                 <h:graphicImage id="ab-org-list-eval2-img" url="/images/icons/error.gif" />
                              </a:booleanEvaluator>
                              <a:actionLink id="ab-org-list-link1" value="#{r['ab:orgName']} (#{r['ab:orgAcronym']})" showLink="false"
                                 action="dialog:addressbookOrgDetails" actionListener="#{AddressbookOrgDetailsDialog.setupViewEntry}">
                                 <f:param id="ab-org-list-param1" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>
                           
                           <%-- e-mail --%>
                           <a:column id="ab-org-list-email-column">
                              <f:facet name="header">
                                 <h:outputText value="#{msg.document_email}" />
                              </f:facet>
                              <h:outputText id="ab-org-list-email" value="#{r['ab:email']}" />
                           </a:column>
                           
                           <%-- DVK --%>
                           <a:column id="ab-org-list-dvk-column">
                              <f:facet name="header">
                                 <h:outputText value="#{msg.addressbook_dvk_capable}" />
                              </f:facet>
                              <h:outputText id="ab-org-list-dvk" value="#{r['ab:dvkCapable']}">
                                 <a:convertBoolean />
                              </h:outputText>
                           </a:column>

                           <%-- Actions column --%>
                           <a:column id="ab-org-list-col2" actions="true">
                              <f:facet name="header">
                                 <r:permissionEvaluator value="#{AddressbookService.addressbookRoot}" allow="WriteProperties,DeleteNode">
                                    <h:outputText id="ab-org-list-ot1" value="#{msg.actions}" />
                                 </r:permissionEvaluator>
                              </f:facet>
                              <r:permissionEvaluator value="#{AddressbookService.addressbookRoot}" allow="WriteProperties">
                                 <a:actionLink id="ab-org-list-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                    action="dialog:addressbookAddEdit" actionListener="#{AddressbookAddEditDialog.setupEdit}">
                                    <f:param id="ab-org-list-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                              </r:permissionEvaluator>
<!--                               TODO use new deleteDialog here -->
                              <r:permissionEvaluator id="ab-org-list-deleteEval" value="#{AddressbookService.addressbookRoot}" allow="DeleteNode">
                                 <a:actionLink value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false" action="dialog:deleteDialog"
                                    actionListener="#{DeleteDialog.setupDeleteDialog}">
                                    <f:param name="nodeRef" value="#{r.nodeRef}" />
                                    <f:param name="confirmMessagePlaceholder0" value="#{actionContext.name}" />
                                    <f:param name="showObjectData" value="true" />
                                    <f:param name="showConfirm" value="false" />
                                 </a:actionLink>
                                 
                              </r:permissionEvaluator>
                           </a:column>

                           <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
                           <a:dataPager id="ab-org-list-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>
                     
                     <a:panel id="ab-person-panel" styleClass="with-pager" label="#{msg.addressbook_private_persons}">

                        <a:richList id="ab-people-list" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" refreshOnBind="true"
                           headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{AddressbookListDialog.people}" var="r" initialSortColumn="name" initialSortDescending="false">

                           <%-- Primary column with name --%>
                           <a:column id="ab-people-list-col1" primary="true" style="padding:2px;text-align:left">
                              <f:facet name="header">
                                 <a:sortLink id="ab-people-list-sort" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                              </f:facet>
                              <a:booleanEvaluator id="ab-people-list-eval1" value="#{r['ab:activeStatus']}">
                                 <h:graphicImage id="ab-people-list-eval1-img" url="/images/icons/person.gif" />
                              </a:booleanEvaluator>
                              <a:booleanEvaluator id="ab-people-list-eval2" value="#{!r['ab:activeStatus']}">
                                 <h:graphicImage id="ab-people-list-eval2-img" url="/images/icons/error.gif" />
                              </a:booleanEvaluator>
                              <a:actionLink id="ab-people-list-link1" value="#{r['ab:personTitle']} #{r['ab:personFirstName']} #{r['ab:personLastName']}"
                                 showLink="false" action="dialog:addressbookPersonDetails" actionListener="#{AddressbookPersonDetailsDialog.setupViewEntry}">
                                 <f:param id="ab-people-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>
                           
                           <%-- e-mail --%>
                           <a:column id="ab-people-list-email-column">
                              <f:facet name="header">
                                 <h:outputText value="#{msg.document_email}" />
                              </f:facet>
                              <h:outputText id="ab-people-list-email" value="#{r['ab:email']}" />
                           </a:column>

                           <%-- Actions column --%>
                           <a:column id="ab-people-list-col2" actions="true" style="text-align:left">
                              <f:facet name="header">
                                 <r:permissionEvaluator value="#{AddressbookService.addressbookRoot}" allow="WriteProperties,DeleteNode">
                                    <h:outputText id="ab-people-list-ot12" value="#{msg.actions}" />
                                 </r:permissionEvaluator>
                              </f:facet>
                              <r:permissionEvaluator value="#{AddressbookService.addressbookRoot}" allow="WriteProperties">
                                 <a:actionLink id="ab-people-list-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                    action="dialog:addressbookAddEdit" actionListener="#{AddressbookAddEditDialog.setupEdit}">
                                    <f:param id="ab-people-list-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                              </r:permissionEvaluator>
                              <r:permissionEvaluator id="ab-people-list-deleteEval" value="#{AddressbookService.addressbookRoot}" allow="DeleteNode">
                                 <a:actionLink value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false" action="dialog:deleteDialog"
                                    actionListener="#{DeleteDialog.setupDeleteDialog}">
                                    <f:param name="nodeRef" value="#{r.nodeRef}" />
                                    <f:param name="confirmMessagePlaceholder0" value="#{actionContext.name}" />
                                    <f:param name="showObjectData" value="true" />
                                    <f:param name="showConfirm" value="false" />
                                 </a:actionLink>

                              </r:permissionEvaluator>
                           </a:column>
                           <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
                           <a:dataPager id="ab-people-list-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>

                        <a:panel id="ab-people-panel2" styleClass="panel-100 with-pager column" label="#{msg.addressbook_orgs_persons}" rendered="#{not empty AddressbookListDialog.orgPeople}">


                           <a:richList id="ab-people-list2" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" 
                              value="#{AddressbookListDialog.orgPeople}" var="r" initialSortColumn="name" initialSortDescending="false"
                              pageSize="#{BrowseBean.pageSizeContent}" refreshOnBind="true" >
                              <%-- Primary column with name --%>
                              <a:column id="ab-people-list2-col" primary="true">
                                 <f:facet name="header">
                                    <a:sortLink id="ab-people-list2-sort" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                                 </f:facet>
                                 <a:booleanEvaluator id="ab-people-list2-eval1" value="#{r['ab:activeStatus']}">
                                    <h:graphicImage id="ab-people-list2-eval1-img" url="/images/icons/person.gif" />
                                 </a:booleanEvaluator>
                                 <a:booleanEvaluator id="ab-people-list2-eval2" value="#{!r['ab:activeStatus']}">
                                    <h:graphicImage id="ab-people-list2-eval2-img" url="/images/icons/error.gif" />
                                 </a:booleanEvaluator>
                                 <a:actionLink id="ab-org-people-list-link1" value="#{r['ab:personTitle']} #{r['ab:personFirstName']} #{r['ab:personLastName']}"
                                    showLink="false" action="dialog:addressbookOrgPersonDetails" actionListener="#{AddressbookPersonDetailsDialog.setupViewEntry}">
                                    <f:param id="ab-people-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                              </a:column>

                              <%-- Data columns --%>
                              <a:column id="ab-people-list2-col4">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot6" value="#{msg.document_email}" />
                                 </f:facet>
                                 <h:outputText id="ab-people-list2-ot7" value="#{r['ab:email']}" />
                              </a:column>

                              <a:column id="ab-people-list2-col5">
                                 <f:facet name="header">
                                    <a:sortLink id="ab-people-list2-ot8-sort" label="#{msg.addressbook_org}" value="parentOrgName" mode="case-insensitive" styleClass="header" />
                                 </f:facet>
                                 <a:actionLink id="ab-people-list2-link-org" value="#{r.parentOrgName}"
                                    showLink="false" action="dialog:addressbookOrgDetails" actionListener="#{AddressbookOrgDetailsDialog.setupViewEntry}">
                                    <f:param name="nodeRef" value="#{r.parentOrgRef}" />
                                 </a:actionLink>
                                 
                              </a:column>
                              <%-- Actions column --%>
                              <a:column id="ab-people-list2-col6" actions="true">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot10" value="#{msg.actions}" />
                                 </f:facet>
                                 <a:actionLink id="ab-people-list2-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                    action="dialog:addressbookAddEdit" actionListener="#{AddressbookAddEditDialog.setupEdit}">
                                    <f:param id="ab-people-list2-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                                 <a:actionLink value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false" action="dialog:deleteDialog"
                                    actionListener="#{DeleteDialog.setupDeleteDialog}">
                                    <f:param name="nodeRef" value="#{r.nodeRef}" />
                                    <f:param name="confirmMessagePlaceholder0" value="#{actionContext.name}" />
                                    <f:param name="showObjectData" value="true" />
                                    <f:param name="showConfirm" value="false" />
                                 </a:actionLink>

                              </a:column>

                              <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
                              <a:dataPager id="ab-people-list2-pager" styleClass="pager" />
                           </a:richList>
                        </a:panel>
                     <f:verbatim><div class="clear"></div></f:verbatim>
         </a:panel>