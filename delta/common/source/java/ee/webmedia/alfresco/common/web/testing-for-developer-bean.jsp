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
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processContractDueDateNotifications" value="processContractDueDateNotifications" type="submit"
   actionListener="#{NotificationService.processContractDueDateNotifications}" rendered="#{ApplicationService.test}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Taustatööd: "/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="destroyArchivedVolumes" value="destroyArchivedVolumes" type="submit"
   actionListener="#{ArchivalsService.destroyArchivedVolumes}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="updateOrganisationStructureBasedGroups" value="updateOrganisationStructureBasedGroups" type="submit"
   actionListener="#{OrganizationStructureService.updateOrganisationStructureBasedGroups}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/><br/></f:verbatim>
<h:outputText id="reportGenerationTitle" value="Aruannete genereerimine: " />
<f:verbatim><br/></f:verbatim>
<h:outputText id="reportGenerationStatus" value=" Selles klastri õlas aruannete genereerimine ei jookse." rendered="#{!ReportListDialog.reportGenerationEnabled}" />
<h:commandButton id="pauseReportGeneration" value="Peata aruannete genereerimine" type="submit"
   actionListener="#{ReportListDialog.pauseReportGeneration}" rendered="#{ReportListDialog.showPauseReportGeneration}" />
<h:commandButton id="continueReportGeneration" value="Jätka aruannete genereerimist" type="submit"
   actionListener="#{ReportListDialog.continueReportGeneration}" rendered="#{ReportListDialog.showContinueReportGeneration}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Testandmete genereerimine: "/>
<f:verbatim>
<br/>
<ol>
<li>* Kopeerida SVN'ist delta/common/etc/testdata/*.csv failid \${dir.root} kausta</li>
<li>* Kopeerida ADR failide kaust asukohta \${dir.root}/contentstore/testfiles . Seega peaksid ADR failid asuma \${dir.root}/contentstore/testfiles/1/1234 jne.</li>
<li>* Importida dok.liigid SVN'ist failist delta/common/etc/jmeter/documentTypes.xml</li>
<li>* Importida klassifikaatorid SVN'ist delta/common/etc/jmeter/classificators.xml</li>
<li>* Kui kasutajaid ja/või struktuuriüksusi genereeritakse (genereeritakse siis kui lahtrites kasutajate arv / struktuuriüksuste on suuremad arvud kui praegu eksisteerivaid objekte), siis peab arvestama sellega, et järgmisel sünkimisel kustutatakse genereeritud objektid! Ainus võimalus sünkimise ära hoidmiseks on konf.failis \${amr.service.url} muuta mittetöötava/mitteeksisteeriva URL'i peale.
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks, ja pärast teha ühekorraga järgi (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011).</li>
<li>* Vajutada Käivita testandmete genereerimine. Progressi saab jälgida logist.</li>
<li>* (TODO struktuuriüksuste sünkimine lülitada välja, et loodud struktuuriüksusi sünkimisel ei kustutataks.)</li>
<li>* Kasutajate loomise lõpus kirjutatakse kõik eksisteerivad kasutajanimed faili \${dir.root}/users.csv . Seda faili läheb vaja koormustestide sisendina.</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="Kasutajate e-mail: "/>
<h:inputText value="#{TestDataService.testEmail}" size="40" />
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
<f:verbatim><br/></f:verbatim>

<h:outputText value="Failid: "/>
<h:selectBooleanCheckbox value="#{TestDataService.filesEnabled}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Dokumentide genereerimiseks paralleelsete lõimede arv: "/>
<h:inputText value="#{TestDataService.documentGeneratorThreads}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton id="startTestDataGenerator" value="Käivita andmete genereerimine" type="submit"
      actionListener="#{TestDataService.executeUpdaterInBackground}"
      rendered="#{TestDataService.updaterRunning == false}" />
   <h:commandButton id="stopTestDataGenerator" value="Peata andmete genereerimine" type="submit"
      actionListener="#{TestDataService.stopUpdater}"
      rendered="#{TestDataService.updaterRunning == true}"
      disabled="#{TestDataService.updaterStopping == true}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Postipoiss andmete importimine: " style="font-weight: bold;" />
<f:verbatim>
<br/>
<ol>
<li>* Enne dokumentide importi peavad kõik kasutajad olema rakendusse tõmmatud.</li>
<li>* Parema kiiruse saavutamiseks lülitada Lucene indekseerimine importimise ajaks välja, ja pärast importimise lõppu teha ühekorraga järgi (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011).</li>
<li>* Käivitamise nupp võimalusel jätkab pooleli jäänud kohast (kui workFolder-ites on csv faile kus eelnev progress on kirjas).</li>
<li>* Peatamise nupp peatab esimesel võimalusel (konktaktide impordi keskel, struktuuri impordi keskel, iga 50 dokumendi importimise või faili importimise või faili indekseerimise vahel)</li>
<li>* Kui ükskõik milline parameeter grupis (...DataFolder, ...WorkFolder, ...ArchivalsStore, ...MappingsFileName) on tühi, siis liigutakse järgmise parameetrite grupi juurde.</li>
<li>* Kui ...DataFolder asukohas leidub fail kontaktid.csv, siis teostatakse kontaktide import. Kui faili ei leidu, siis liigutakse järgmisesse sammu, viga ei teki. Kui kontaktide import õnnestub, kirjutatakse ...WorkFolder asukohta fail completed_kontaktid.csv.</li>
<li>* Struktuuri impordi jaoks loetakse sisse asukohas ...DataFolder olevad failid struktuur.csv ja toimikud.csv. Tekitatakse funktsioonid/sarjad/toimikud arhiivimoodustaja alla, mis on määratud parameetris ...ArchivalsStore. Impordi käigus kirjutatakse asukohta ...WorkFolder fail completed_toimikud.csv.</li>
<li>* Dokumentide impordil luuakse asukohas ...DataFolder olevad dokumendid ja failid. Mappings.xml faili nimetus on parameetris ...MappingsFileName. Impordi käigus kirjutatakse asukohta ...WorkFolder järgmised failid - completed_docs.csv, completed_files.csv, indexed_files.csv, users_found.csv, users_not_found.csv, postponed_assocs.csv.</li>
<li>* Impordi progressi, infoteateid ja veateateid saab jälgida rakenduse logist.</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="firstDataFolder: "/>
<h:inputText value="#{postipoissImporter.dataFolders[0]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstWorkFolder: "/>
<h:inputText value="#{postipoissImporter.workFolders[0]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstMappingsFileName: "/>
<h:inputText value="#{postipoissImporter.mappingsFileNames[0]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstDefaultOwnerId: "/>
<h:inputText value="#{postipoissImporter.defaultOwnerIds[0]}" size="12" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstArchivalsStore: "/>
<h:inputText value="#{postipoissImporter.archivalsStores[0]}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstOpenUnit: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.openUnits[0]}" />
<f:verbatim><br/><br/></f:verbatim>

<h:outputText value="secondDataFolder: "/>
<h:inputText value="#{postipoissImporter.dataFolders[1]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondWorkFolder: "/>
<h:inputText value="#{postipoissImporter.workFolders[1]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondMappingsFileName: "/>
<h:inputText value="#{postipoissImporter.mappingsFileNames[1]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondDefaultOwnerId: "/>
<h:inputText value="#{postipoissImporter.defaultOwnerIds[1]}" size="12" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondArchivalsStore: "/>
<h:inputText value="#{postipoissImporter.archivalsStores[1]}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondOpenUnit: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.openUnits[1]}" />
<f:verbatim><br/><br/></f:verbatim>

<h:outputText value="thirdDataFolder: "/>
<h:inputText value="#{postipoissImporter.dataFolders[2]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdWorkFolder: "/>
<h:inputText value="#{postipoissImporter.workFolders[2]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdMappingsFileName: "/>
<h:inputText value="#{postipoissImporter.mappingsFileNames[2]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdDefaultOwnerId: "/>
<h:inputText value="#{postipoissImporter.defaultOwnerIds[2]}" size="12" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdArchivalsStore: "/>
<h:inputText value="#{postipoissImporter.archivalsStores[2]}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdOpenUnit: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.openUnits[2]}" />
<f:verbatim><br/><br/></f:verbatim>

<h:outputText value="fourthDataFolder: "/>
<h:inputText value="#{postipoissImporter.dataFolders[3]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthWorkFolder: "/>
<h:inputText value="#{postipoissImporter.workFolders[3]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthMappingsFileName: "/>
<h:inputText value="#{postipoissImporter.mappingsFileNames[3]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthDefaultOwnerId: "/>
<h:inputText value="#{postipoissImporter.defaultOwnerIds[3]}" size="12" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthArchivalsStore: "/>
<h:inputText value="#{postipoissImporter.archivalsStores[3]}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthOpenUnit: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.openUnits[3]}" />
<f:verbatim><br/><br/></f:verbatim>

<h:outputText value="fifthDataFolder: "/>
<h:inputText value="#{postipoissImporter.dataFolders[4]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthWorkFolder: "/>
<h:inputText value="#{postipoissImporter.workFolders[4]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthMappingsFileName: "/>
<h:inputText value="#{postipoissImporter.mappingsFileNames[4]}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthDefaultOwnerId: "/>
<h:inputText value="#{postipoissImporter.defaultOwnerIds[4]}" size="12" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthArchivalsStore: "/>
<h:inputText value="#{postipoissImporter.archivalsStores[4]}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthOpenUnit: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.openUnits[4]}" />
<f:verbatim><br/><br/></f:verbatim>

<h:outputText value="Mitu dokumenti / faili ühes transaktsioonis luua: "/>
<h:inputText value="#{postipoissImporter.batchSize}" converter="javax.faces.Integer" size="4" />
<%-- <f:verbatim><br/></f:verbatim> --%>
<%-- <h:outputText value="Dokumentide importimiseks paralleelsete lõimede arv: "/> --%>
<%-- <h:inputText value="#{postipoissImporter.threadsCount}" converter="javax.faces.Integer" size="4" /> --%>
<f:verbatim><br/></f:verbatim>

<h:outputText value="Sarjade võrdlemisel kasutatakse lisaks viidale ka pealkirja: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.seriesComparisonIncludesTitle}" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton id="startPostipoissImporter" value="Käivita PP andmete import" type="submit"
      actionListener="#{postipoissImporter.startImporterInBackground}"
      rendered="#{postipoissImporter.importerRunning == false}" />
   <h:commandButton id="stopPostipoissImporter" value="Peata PP andmete import" type="submit"
      actionListener="#{postipoissImporter.stopImporter}"
      rendered="#{postipoissImporter.importerRunning == true}"
      disabled="#{postipoissImporter.importerStopping == true}" />

<f:verbatim><br/><br/></f:verbatim>
<f:verbatim><hr/></f:verbatim>
   <h:outputText value="PPA live: Dokumendihaldurite grupil puuduvate õiguste parandamiseks." />
   
   <h:inputTextarea id="inheritanceProblems" value="#{ArrivedDocumentsPermissionsModifier.validationResults}" readonly="true" styleClass="expand19-200" />

   <h:outputText value="Kui teksti kastis on esimesel real OK, siis tõenäoliselt probleemi pole." />
   <f:verbatim><br/></f:verbatim>
   <h:outputText value="Kui teksti kastis on esimesel real ERRORS, siis võiks katsetada järgnevaid võimalusi. NB! enne iga järgnevat võimaluse proovimist kontrollige, kas jätkamiseks on põhjust(OK või ERRORS)" />
   <f:verbatim><br/><br/></f:verbatim>

   <h:outputText value="Esimene võimalus - proovida olemasolevad õigused eemaldada ja tagasi panna. Vajuta järgnevat nuppu kui teksti kastis on esimesel real ERRORS" />
   <f:verbatim><br/></f:verbatim>
   <h:commandButton id="rerunArrivedDocumentsPermissionsUpdateBootstrap" value="võimalus 1: eemalda õigused ja lisa uuesti" type="submit" actionListener="#{ArrivedDocumentsPermissionsModifier.rerunArrivedDocumentsPermissionsUpdateBootstrap}" />

   <f:verbatim><br/><br/></f:verbatim>

   <h:outputText value="Kui eelneva nupu vajutusest polnud kasu proovige järgmist lahendust" />
   <f:verbatim><br/></f:verbatim>
   <h:commandButton id="test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot" value="võimalus 2: eemalda õigused imap-root alamkataloogidelt ja lisa imap-root kataloogile" type="submit" actionListener="#{ArrivedDocumentsPermissionsModifier.test2RemoveImapFolderChildrenPermissionsAndAddPermissionsToImapRoot}" />
   <f:verbatim><br/></f:verbatim>
   <h:outputText value="Kui sellest tekstist üleval asuva nupu vajutusest polnud kasu, siis vajutage järgnevat nuppu, et eelneva nupu tegevused tühistada" />
   <h:commandButton id="test2Undo" value="võimalus 2 undo: lisa õigused imap-root alamkataloogidele ja eemalda imap-root kataloogilt" type="submit" actionListener="#{ArrivedDocumentsPermissionsModifier.test2Undo}" />

   <f:verbatim><br/><br/></f:verbatim>

   <h:outputText value="Kui eelnevate nuppude vajutusest polnud kasu proovige järgmist lahendust:" />
   <f:verbatim><br/></f:verbatim>
   <h:commandButton id="test3UndoRedoAndGrandChildrenPermissions" value="võimalus 3: lisa õigused imap-root kataloogi otseste alamkataloogide alamkataloogidele" type="submit" actionListener="#{ArrivedDocumentsPermissionsModifier.test3UndoRedoAndGrandChildrenPermissions}" />
   <f:verbatim><br/></f:verbatim>
   <h:outputText value="Loodetavasti oli ühest eelnevatest lahendustest kasu. Kui ka sellest tekstist üleval asuva nupu vajutusest ka polnud kasu, siis vajutage järgnevat nuppu, et eelneva nupu tegevused tühistada" />
   <h:commandButton id="test3Undo" value="võimalus 3 undo: eemalda õigused imap-root kataloogi otseste alamkataloogide alamkataloogidelt" type="submit" actionListener="#{ArrivedDocumentsPermissionsModifier.test3Undo}" />

   <f:verbatim><br/><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText styleClass="mainTitle" value="Indeksite mergemine"/>
   <h:dataTable value="#{TestingForDeveloperBean.storeRefs}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" headerClass="selectedItemsHeader">
      <h:column>
         <h:outputText value="#{row}" />
      </h:column>
      <h:column>
         <h:commandButton id="runMergeNow" value="runMergeNow" type="submit" actionListener="#{TestingForDeveloperBean.runMergeNow}">
           <f:param name="storeRef" value="#{row}" />
         </h:commandButton>
         <f:verbatim>&nbsp;</f:verbatim>
         <h:commandButton id="printInfo" value="printInfo" type="submit" actionListener="#{TestingForDeveloperBean.printIndexInfo}">
           <f:param name="storeRef" value="#{row}" />
         </h:commandButton>
      </h:column>
   </h:dataTable>

<f:verbatim><br/></f:verbatim>
<h:commandButton id="runMergeNowOnAllIndexesAndPerformIndexBackup" value="runMergeNowOnAllIndexesAndPerformIndexBackup" type="submit" actionListener="#{TestingForDeveloperBean.runMergeNowOnAllIndexesAndPerformIndexBackup}" />

<f:verbatim><br/><br/></f:verbatim>
<h:inputTextarea id="indexInfo" value="#{TestingForDeveloperBean.indexInfoText}" readonly="true" styleClass="expand19-200" style="font-family: monospace;" />

<f:verbatim><hr/></f:verbatim>

<h:outputText styleClass="mainTitle" value="Arendajale testimiseks"/>
<f:verbatim><br/></f:verbatim>

<a:actionLink value="Lisa sessiooni mitteserialiseeruv objekt" actionListener="#{TestingForDeveloperBean.addNonSerializableObjectToSession}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>

<a:actionLink value="TestingForDeveloper" actionListener="#{TestingForDeveloperBean.handleTestEvent}" rendered="#{ApplicationService.test}">
     <f:param name="testP" value="11" />
</a:actionLink>
<f:verbatim><br/></f:verbatim>

<a:actionLink id="runADMLuceneTestTestMaskDeletes" value="ADMLuceneTest.testMaskDeletes" actionListener="#{TestingForDeveloperBean.runADMLuceneTestTestMaskDeletes}" />

<%-- <f:verbatim><br/></f:verbatim> --%>

 <%-- FIXME DLSeadist ajutine link test keskkonna jaoks --%>
<%-- <a:actionLink value="DeleteFieldAndFieldGroupsAndBootstrapInfo" actionListener="#{TestingForDeveloperBean.deleteFieldAndFieldGroupsAndBootstrapInfo}" /> --%>
