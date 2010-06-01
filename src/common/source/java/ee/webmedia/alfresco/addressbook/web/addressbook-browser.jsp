<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">
function clearAddressbookDiv(){
   $('addressbook_entry').innerHTML = '';
}
function setAddressbookDiv(id){
   $('addressbook_entry').innerHTML = id.innerHTML;
}
</script>
</f:verbatim>

            <%-- Organizations List --%>
            <a:panel id="addressbook-panel" styleClass="panel-100" label="#{msg.addressbook}">
            <a:panel id="search-panel">
               <h:inputText id="search-text" value="#{AddressbookDialog.searchCriteria}" size="35" maxlength="1024" /><f:verbatim>&nbsp;</f:verbatim>
               <h:commandButton id="search-btn" value="#{msg.search}" action="#{AddressbookDialog.search}" disabled="false" /><f:verbatim>&nbsp;</f:verbatim>
               <h:commandButton id="show-all-button" value="#{msg.show_all}" action="#{AddressbookDialog.showAll}" />
            </a:panel>
                        <a:panel id="ab-first-column" styleClass="column panel-33">
                        <a:panel id="_org-panel" label="#{msg.addressbook_organizations}" styleClass="with-pager">

                        <a:actionLink id="add-org-button" value="#{msg.addressbook_add_org}" image="/images/icons/add_user.gif" showLink="false"
                           action="dialog:addressbookAddOrg" />
                        <a:richList id="_organizations-list" viewMode="details" width="100%"
                           binding="#{AddressbookDialog.orgRichList}" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow"
                           altRowStyleClass="recordSetRowAlt" value="#{AddressbookDialog.organizations}" var="r" initialSortColumn="name"
                           initialSortDescending="false">

                           <%-- Primary column with name --%>
                           <a:column id="org-list-column" primary="true">
                              <f:facet name="header">
                                 <a:sortLink id="org-sort-link" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                              </f:facet>
                              <f:verbatim>
                                 <span onmouseover="setAddressbookDiv(this.getElementsByTagName('div').item(0));" onmouseout="clearAddressbookDiv();">
                              </f:verbatim>
                              <a:outputText id="org-list-ref" style="position:absolute; display: none; float:left;" value="#{r.nodeRef}" />
                              <a:booleanEvaluator id="org-list-eval1" value="#{r['ab:activeStatus']}">
                                 <h:graphicImage id="org-list-eval1-image" url="/images/icons/person.gif" />
                              </a:booleanEvaluator>
                              <a:booleanEvaluator id="org-list-eval2" value="#{!r['ab:activeStatus']}">
                                 <h:graphicImage id="org-list-eval2-image" url="/images/icons/error.gif" />
                              </a:booleanEvaluator>
                              <a:actionLink id="org-list-link1" image="/images/icons/collapsed.gif" value="#{msg.addressbook_org_show_people}"
                                 showLink="false" actionListener="#{DialogManager.bean.selectOrg}">
                                 <f:param id="org-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                              <a:actionLink id="org-list-link2" value="#{r['ab:orgName']}, (#{r['ab:orgAcronym']})" showLink="false"
                                 actionListener="#{DialogManager.bean.selectContact}">
                                 <f:param id="org-list-link2-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                              <f:verbatim>
                                 <div style="position: absolute; float: left; display: none;">
                              </f:verbatim>
                              <a:panel id="entry-panel1" styleClass="mainSubTitle" label="#{msg.addressbook_entry}">
                                 <r:propertySheetGrid id="_1" value="#{r}" columns="1" externalConfig="true" mode="view" />
                              </a:panel>
                              <f:verbatim>
                                 </div>
                                 </span>
                              </f:verbatim>
                           </a:column>

                           <a:dataPager id="org-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>
                     
                     <a:booleanEvaluator id="search-eval" value="#{AddressbookDialog.search}">

                        <a:panel id="people-panel2" label="#{msg.addressbook_org_persons}" styleClass="with-pager">
                           <a:actionLink value="#{msg.addressbook_add_org_person}" image="/images/icons/add_user.gif" showLink="false"
                              action="dialog:addressbookAddOrgPerson" actionListener="#{AddressbookOrgPersonWizard.setupEntryOrg}">
                              <f:param name="org" value="#{AddressbookDialog.currentNode.nodeRef}" />
                           </a:actionLink>

                           <a:richList id="people-list2" viewMode="details" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                              value="#{AddressbookDialog.orgPeople}" var="r" initialSortColumn="name" initialSortDescending="false"
                              binding="#{AddressbookDialog.orgPeopleRichList}">
                              <%-- Primary column with name --%>
                              <a:column id="people-list2-column" primary="true" style="padding:2px;text-align:left">
                                 <f:facet name="header">
                                    <a:sortLink id="person2-sort-link" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                                 </f:facet>
                                 <f:verbatim>
                                    <span onmouseover="setAddressbookDiv(this.getElementsByTagName('div').item(0));" onmouseout="clearAddressbookDiv();">
                                 </f:verbatim>
                                 <a:outputText id="nodeRefId" style="position:absolute; float:left; display: none;" value="#{r.nodeRef}" />
                                 <a:booleanEvaluator id="people-list2-eval1" value="#{r['ab:activeStatus']}">
                                    <h:graphicImage id="people-list2-eval1-image" url="/images/icons/person.gif" />
                                 </a:booleanEvaluator>
                                 <a:booleanEvaluator id="people-list2-eval2" value="#{!r['ab:activeStatus']}">
                                    <h:graphicImage id="people-list2-eval2-image" url="/images/icons/error.gif" />
                                 </a:booleanEvaluator>
                                 <a:actionLink id="people-list2-link1" value="#{r['ab:personFirstName']} #{r['ab:personLastName']}" showLink="false"
                                    actionListener="#{DialogManager.bean.selectContact}">
                                    <f:param id="people-list2-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                                 </a:actionLink>
                                 <f:verbatim>
                                    <div style="position: absolute; float: left; display: none;">
                                 </f:verbatim>
                                 <a:panel id="entry-panel3" styleClass="mainSubTitle" label="#{msg.addressbook_entry}">
                                    <r:propertySheetGrid id="_3" value="#{r}" columns="1" externalConfig="true" mode="view" />
                                 </a:panel>
                                 <f:verbatim>
                                    </div>
                                    </span>
                                 </f:verbatim>
                              </a:column>

                              <a:dataPager id="person2-pager" styleClass="pager" />
                           </a:richList>
                        </a:panel>
                     </a:booleanEvaluator>                     
                     
                     </a:panel>
                     
                     <a:panel id="person-panel" styleClass="column panel-33 with-pager" label="#{msg.addressbook_private_persons}">
                        <a:actionLink value="#{msg.addressbook_add_person}" image="/images/icons/add_user.gif" showLink="false"
                           action="dialog:addressbookAddPerson" />

                        <a:richList id="people-list" viewMode="details" binding="#{AddressbookDialog.peopleRichList}"
                           rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{AddressbookDialog.people}" var="r" initialSortColumn="name" initialSortDescending="false">

                           <%-- Primary column with name --%>
                           <a:column id="people-list-column" primary="true" style="padding:2px;text-align:left">
                              <f:facet name="header">
                                 <a:sortLink id="person-sort-link" label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header" />
                              </f:facet>
                              <f:verbatim>
                                 <span onmouseover="setAddressbookDiv(this.getElementsByTagName('div').item(0));" onmouseout="clearAddressbookDiv();">
                              </f:verbatim>
                              <a:booleanEvaluator id="people-list-eval1" value="#{r['ab:activeStatus']}">
                                 <h:graphicImage id="people-list-eval1-image" url="/images/icons/person.gif" />
                              </a:booleanEvaluator>
                              <a:booleanEvaluator id="people-list-eval2" value="#{!r['ab:activeStatus']}">
                                 <h:graphicImage id="people-list-eval2-image" url="/images/icons/error.gif" />
                              </a:booleanEvaluator>
                              <a:actionLink id="people-list-link1" value="#{r['ab:personFirstName']} #{r['ab:personLastName']}" showLink="false"
                                 actionListener="#{DialogManager.bean.selectContact}">
                                 <f:param id="people-list-link1-param" name="nodeRef" value="#{r.nodeRef}" />
                              </a:actionLink>
                              <f:verbatim>
                                 <div style="position: absolute; float: left; display: none;">
                              </f:verbatim>
                              <a:panel id="entry-panel2" styleClass="mainSubTitle" label="#{msg.addressbook_entry}">
                                 <r:propertySheetGrid id="_2" value="#{r}" columns="1" externalConfig="true" mode="view" />
                              </a:panel>
                              <f:verbatim>
                                 </div>
                                 </span>
                              </f:verbatim>
                           </a:column>


                           <a:dataPager id="person-pager" styleClass="pager" />
                        </a:richList>

                     </a:panel>
                     
                     <a:panel styleClass="column panel-33-f" id="addressbook_entry"></a:panel>
                     <f:verbatim>
               <div class="clear"></div></f:verbatim>
         </a:panel>