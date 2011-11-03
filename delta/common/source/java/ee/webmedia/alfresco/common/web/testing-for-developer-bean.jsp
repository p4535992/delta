<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>


<%--
<hr/>
<b><h:outputText styleClass="mainTitle" value="Skriptid"/></b><br/>

   <br/>
   <u>Dokumendi õiguste uuendamise skript (enne 2.5 versiooni)</u>
   <br/>
   <br/>
   <h:commandButton id="startDocumentPrivilegesUpdater" value="Käivita dokumendi õiguste skript" type="submit"
      actionListener="#{documentPrivilegesUpdater.executeUpdaterInBackground}"
      rendered="#{documentPrivilegesUpdater.updaterRunning == false}" />
   <h:commandButton id="stopDocumentPrivilegesUpdater" value="Peata dokumendi õiguste skript" type="submit"
      actionListener="#{documentPrivilegesUpdater.stopUpdater}"
      rendered="#{documentPrivilegesUpdater.updaterRunning == true}"
      disabled="#{documentPrivilegesUpdater.updaterStopping == true}" />
   <br/>
   <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
   <h:inputText id="documentPrivilegesUpdaterSleepTime" value="#{documentPrivilegesUpdater.sleepTime}" size="4" />
   <h:commandButton id="updateDocumentPrivilegesUpdaterSleepTime" value="Uuenda" type="submit"
      actionListener="#{documentPrivilegesUpdater.updateSleepTime}" />

   <br/>
   <br/>
   <br/>
   <u>Dokumendi õiguste optimeerimise skript (alates 2.5 versioonist)</u>
   <br/>
   <br/>
   <h:commandButton id="startDocumentInheritPermissionsUpdater" value="Käivita dokumendi õiguste optimeerimise skript" type="submit"
      actionListener="#{documentInheritPermissionsUpdater.executeUpdaterInBackground}"
      rendered="#{documentInheritPermissionsUpdater.updaterRunning == false}" />
   <h:commandButton id="stopDocumentInheritPermissionsUpdater" value="Peata dokumendi õiguste optimeerimise skript" type="submit"
      actionListener="#{documentInheritPermissionsUpdater.stopUpdater}"
      rendered="#{documentInheritPermissionsUpdater.updaterRunning == true}"
      disabled="#{documentInheritPermissionsUpdater.updaterStopping == true}" />
   <br/>
   <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
   <h:inputText id="documentInheritPermissionsUpdaterSleepTime" value="#{documentInheritPermissionsUpdater.sleepTime}" size="4" />
   <h:commandButton id="updateDocumentInheritPermissionsUpdaterSleepTime" value="Uuenda" type="submit"
      actionListener="#{documentInheritPermissionsUpdater.updateSleepTime}" />

   <br/>
   <br/>
   <br/>
   <u>Lepingute väljasaatmise kirjete tekitamise skript (ContractSendInfoUpdater)</u>
   <br/>
   <br/>
   <h:commandButton id="startContractSendInfoUpdater" value="Käivita dokumendi õiguste optimeerimise skript" type="submit"
      actionListener="#{contractSendInfoUpdater.executeUpdaterInBackground}"
      rendered="#{contractSendInfoUpdater.updaterRunning == false}" />
   <h:commandButton id="stopContractSendInfoUpdater" value="Peata dokumendi õiguste optimeerimise skript" type="submit"
      actionListener="#{contractSendInfoUpdater.stopUpdater}"
      rendered="#{contractSendInfoUpdater.updaterRunning == true}"
      disabled="#{contractSendInfoUpdater.updaterStopping == true}" />
   <br/>
   <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
   <h:inputText id="contractSendInfoUpdaterSleepTime" value="#{contractSendInfoUpdater.sleepTime}" size="4" />
   <h:commandButton id="updateContractSendInfoUpdaterSleepTime" value="Uuenda" type="submit"
      actionListener="#{contractSendInfoUpdater.updateSleepTime}" />
--%>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Faili asukoht serveri kõvakettal, millest DVK dokument importida: "/>
<f:verbatim><br/></f:verbatim>
<h:inputText id="fileNameInputText" value="#{TestingForDeveloperBean.fileName}" size="70" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="receiveDocStub" value="Jäljenda DVK importi faili alusel" type="submit"
      actionListener="#{TestingForDeveloperBean.receiveDocStub}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText styleClass="mainTitle" value="Arendajale testimiseks"/>
<f:verbatim><br/></f:verbatim>

<a:actionLink value="TestingForDeveloper" actionListener="#{TestingForDeveloperBean.handleTestEvent}" rendered="#{ApplicationService.test}">
     <f:param name="testP" value="11" />
</a:actionLink>

<%-- <f:verbatim><br/></f:verbatim> --%>

 <%-- FIXME DLSeadist ajutine link test keskkonna jaoks --%>
<%-- <a:actionLink value="DeleteFieldAndFieldGroupsAndBootstrapInfo" actionListener="#{TestingForDeveloperBean.deleteFieldAndFieldGroupsAndBootstrapInfo}" /> --%>

<br/>
<h:commandButton id="processAccessRestrictionEndDateNotifications" value="processAccessRestrictionEndDateNotifications" type="submit"
   actionListener="#{NotificationService.processAccessRestrictionEndDateNotifications}" rendered="#{ApplicationService.test}" />
<br/>
<h:commandButton id="processTaskDueDateNotifications" value="processTaskDueDateNotifications" type="submit"
   actionListener="#{NotificationService.processTaskDueDateNotifications}" rendered="#{ApplicationService.test}" />
<br/>
<h:commandButton id="processVolumeDispositionDateNotifications" value="processVolumeDispositionDateNotifications" type="submit"
   actionListener="#{NotificationService.processVolumeDispositionDateNotifications}" rendered="#{ApplicationService.test}" />
