<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">

window.onload = pageLoaded;

function pageLoaded()
{
   document.getElementById("dialog:dialog-body:search-text").focus();
   updateButtonState();
}

function updateButtonState()
{
   if (document.getElementById("dialog:dialog-body:search-text").value.length == 0)
   {
      document.getElementById("dialog:dialog-body:search-btn").disabled = true;
   }
   else
   {
      document.getElementById("dialog:dialog-body:search-btn").disabled = false;
   }
}
</script>
</f:verbatim>

            <%-- Organizations List --%>
            <a:panel id="ab-addressbook-panel" styleClass="mainSubTitle" label="#{msg.addressbook}">
            <a:panel id="search-panel">
            <h:inputText id="search-text" value="#{DialogManager.bean.searchCriteria}" size="35" maxlength="1024" onkeyup="updateButtonState();"
               onchange="updateButtonState();" /><f:verbatim>&nbsp;</f:verbatim>
            <h:commandButton id="search-btn" value="#{msg.search}" action="#{DialogManager.bean.search}" disabled="true" /><f:verbatim>&nbsp;</f:verbatim>
            <h:commandButton id="show-all-button" value="#{msg.show_all}" action="#{DialogManager.bean.showAll}" />
            </a:panel>
            <a:panel id="ab-org-panel" styleClass="column panel-50" label="#{msg.addressbook_organizations}">

                        <a:richList id="ab-organizations-list" viewMode="details" binding="#{DialogManager.bean.orgRichList}"
                           rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
                           value="#{DialogManager.bean.organizations}" var="r" initialSortColumn="name" initialSortDescending="false">

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
                                 action="dialog:addressbookViewEntry" actionListener="#{DialogManager.bean.setupViewEntry}">
                                 <f:param id="ab-org-list-param1" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>

                           <%-- Actions column --%>
                           <a:column id="ab-org-list-col2" actions="true">
                              <f:facet name="header">
                                 <h:outputText id="ab-org-list-ot1" value="#{msg.actions}" />
                              </f:facet>
                              <a:actionLink id="ab-org-list-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                 action="wizard:addressbookAddOrg" actionListener="#{AddressbookOrgWizard.setupEdit}">
                                 <f:param id="ab-org-list-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                              <a:actionLink id="ab-org-list-link-del" value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false"
                                 action="dialog:addressbookDeleteEntry" actionListener="#{AddressbookDeleteDialog.setupDelete}">
                                 <f:param id="ab-org-list-link-del-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>

                           <a:dataPager id="ab-org-list-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>
                     
                     <a:panel id="ab-person-panel" styleClass="column panel-50" label="#{msg.addressbook_private_persons}">

                        <a:richList id="ab-people-list" viewMode="details" binding="#{DialogManager.bean.peopleRichList}"
                           headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{DialogManager.bean.people}" var="r" initialSortColumn="name" initialSortDescending="false">

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
                                 showLink="false" action="dialog:addressbookViewEntryPerson" actionListener="#{DialogManager.bean.setupViewEntry}">
                                 <f:param id="ab-people-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>

                           <%-- Actions column --%>
                           <a:column id="ab-people-list-col2" actions="true" style="text-align:left">
                              <f:facet name="header">
                                 <h:outputText id="ab-people-list-ot1" value="#{msg.actions}" />
                              </f:facet>
                              <a:actionLink id="ab-people-list-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                 action="wizard:addressbookAddPerson" actionListener="#{AddressbookPersonWizard.setupEdit}">
                                 <f:param id="ab-people-list-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                              <a:actionLink id="ab-people-list-link-del" value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false"
                                 action="dialog:addressbookDeleteEntry" actionListener="#{AddressbookDeleteDialog.setupDelete}">
                                 <f:param id="ab-people-list-link-del-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                           </a:column>

                           <a:dataPager id="ab-people-list-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>

                     <a:booleanEvaluator id="search-eval" value="#{DialogManager.bean.search}">

                        <a:panel id="ab-people-panel2" styleClass="panel-100" label="#{msg.addressbook_org_persons}">


                           <a:richList id="ab-people-list2" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" 
                              value="#{DialogManager.bean.orgPeople}" var="r" initialSortColumn="name" initialSortDescending="false"
                              binding="#{DialogManager.bean.orgPeopleRichList}">
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
                                 <h:outputText id="ab-people-list2-ot1"
                                    value="#{r['ab:personTitle']} #{r['ab:personFirstName']} #{r['ab:personLastName']} (#{r['ab:personId']})" />
                              </a:column>

                              <%-- Data columns --%>
                              <a:column id="ab-people-list2-col2">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot2" value="#{msg.addressbook_location}" />
                                 </f:facet>
                                 <h:outputText id="ab-people-list2-ot3" value="#{r['ab:location']}" />
                              </a:column>

                              <a:column id="ab-people-list2-col3">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot4" value="#{msg.addressbook_phone}" />
                                 </f:facet>
                                 <h:outputText id="ab-people-list2-ot5" value="#{r['ab:phone']}" />
                              </a:column>

                              <a:column id="ab-people-list2-col4">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot6" value="#{msg.email}" />
                                 </f:facet>
                                 <h:outputText id="ab-people-list2-ot7" value="#{r['ab:email']}" />
                              </a:column>

                              <a:column id="ab-people-list2-col5">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot8" value="#{msg.addressbook_mobile_phone}" />
                                 </f:facet>
                                 <h:outputText id="ab-people-list2-ot9" value="#{r['ab:mobilePhone']}" />
                              </a:column>

                              <%-- Actions column --%>
                              <a:column id="ab-people-list2-col6" actions="true">
                                 <f:facet name="header">
                                    <h:outputText id="ab-people-list2-ot10" value="#{msg.actions}" />
                                 </f:facet>
                                 <a:actionLink id="ab-people-list2-link-mod" value="#{msg.modify}" image="/images/icons/edituser.gif" showLink="false"
                                    action="wizard:addressbookAddOrgPerson" actionListener="#{AddressbookOrgPersonWizard.setupEdit}">
                                    <f:param id="ab-people-list2-link-mod-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                                 <a:actionLink id="ab-people-list2-link-del" value="#{msg.delete}" image="/images/icons/delete_person.gif" showLink="false"
                                    action="dialog:addressbookDeleteEntry" actionListener="#{AddressbookDeleteDialog.setupDelete}">
                                    <f:param id="ab-people-list2-link-del-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                              </a:column>

                              <a:dataPager id="ab-people-list2-pager" styleClass="pager" />
                           </a:richList>
                        </a:panel>
                     </a:booleanEvaluator>
                     <f:verbatim><div class="clear"></div></f:verbatim>
         </a:panel>