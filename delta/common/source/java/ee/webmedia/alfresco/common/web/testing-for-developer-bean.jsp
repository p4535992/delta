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

<h:outputText value="E-maili teavituste saatmine: "/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processAccessRestrictionEndDateNotifications" value="processAccessRestrictionEndDateNotifications" type="submit"
   actionListener="#{NotificationService.processAccessRestrictionEndDateNotifications}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processTaskDueDateNotifications" value="processTaskDueDateNotifications" type="submit"
   actionListener="#{NotificationService.processTaskDueDateNotifications}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processVolumeDispositionDateNotifications" value="processVolumeDispositionDateNotifications" type="submit"
   actionListener="#{NotificationService.processVolumeDispositionDateNotifications}" rendered="#{ApplicationService.test}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Taustatööd: "/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="updateOrganisationStructureBasedGroups" value="updateOrganisationStructureBasedGroups" type="submit"
   actionListener="#{OrganizationStructureService.updateOrganisationStructureBasedGroups}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/><br/><br/></f:verbatim>

<h:outputText value="Testandmete genereerimine: "/>
<f:verbatim>
<br/>
<ol>
<li>* Kopeerida SVN'ist delta/common/etc/testdata/*.csv failid dir.root kausta</li>
<li>* Kopeerida ADR failide kaust asukohta dir.root/contentstore/testfiles . Seega peaksid ADR failid asuma dir.root/contentstore/testfiles/1/1234 jne.</li>
<li>* Importida dok.liigid SVN'ist failist delta/common/etc/jmeter/documentTypes.xml</li>
<li>* Importida klassifikaatorid SVN'ist delta/common/etc/jmeter/classificators.xml</li>
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks, ja pärast teha ühekorraga järgi (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011).</li>
<li>* Vajutada Käivita testandmete genereerimine. Progressi saab jälgida logist.</li>
<li>* Andmete genereerimise käivitamisel määratakse employeeRegReceiveUsersPeriod parameetri väärtuseks 500000, et loodud kasutajaid sünkimisel ei kustutataks.</li>
<li>* (TODO struktuuriüksuste sünkimine lülitada välja, et loodud struktuuriüksusi sünkimisel ei kustutataks.)</li>
<li>* Kasutajate loomise lõpus kirjutatakse kõik eksisteerivad kasutajanimed faili dir.root/users.csv . Seda faili läheb vaja koormustestide sisendina.</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="Kasutajate e-mail: "/>
<h:inputText value="#{TestDataService.testEmail}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Struktuuriüksuste arv: "/>
<h:inputText value="#{TestDataService.orgUnitsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Kasutajate arv: "/>
<h:inputText value="#{TestDataService.usersCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Kontaktide arv: "/>
<h:inputText value="#{TestDataService.contactsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Registrite arv: "/>
<h:inputText value="#{TestDataService.registersCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Funktsioonide arv: "/>
<h:inputText value="#{TestDataService.functionsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Sarjade arv: "/>
<h:inputText value="#{TestDataService.seriesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Toimikute arv: "/>
<h:inputText value="#{TestDataService.volumesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Asjade arv: "/>
<h:inputText value="#{TestDataService.casesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Dokumentide arv: "/>
<h:inputText value="#{TestDataService.documentsCount}" converter="javax.faces.Integer" size="7" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton id="startTestDataGenerator" value="Käivita andmete genereerimine" type="submit"
      actionListener="#{TestDataService.executeUpdaterInBackground}"
      rendered="#{TestDataService.updaterRunning == false}" />
   <h:commandButton id="stopTestDataGenerator" value="Peata andmete genereerimine" type="submit"
      actionListener="#{TestDataService.stopUpdater}"
      rendered="#{TestDataService.updaterRunning == true}"
      disabled="#{TestDataService.updaterStopping == true}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText styleClass="mainTitle" value="Arendajale testimiseks"/>
<f:verbatim><br/></f:verbatim>

<a:actionLink value="TestingForDeveloper" actionListener="#{TestingForDeveloperBean.handleTestEvent}" rendered="#{ApplicationService.test}">
     <f:param name="testP" value="11" />
</a:actionLink>

<%-- <f:verbatim><br/></f:verbatim> --%>

 <%-- FIXME DLSeadist ajutine link test keskkonna jaoks --%>
<%-- <a:actionLink value="DeleteFieldAndFieldGroupsAndBootstrapInfo" actionListener="#{TestingForDeveloperBean.deleteFieldAndFieldGroupsAndBootstrapInfo}" /> --%>
