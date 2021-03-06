<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>


<hr/>
<f:verbatim><b><br/><br/><h:outputText styleClass="mainTitle" value="Skriptid"/></b><br/></f:verbatim>


<f:verbatim><hr/></f:verbatim>
<h:outputText value="ÜLETEMBELDAMINE TERA: " style="font-weight: bold;" />
<f:verbatim>
    <br/>
    <ul>
        <li>Tembeldab üle kõik DDOC ja BDOC v1.0 failid (SHA-1 turvanõrkusega). Tembeldamiseks vajalik DigiSign teenuse liidestus.</li>
    </ul>
    <br/>
</f:verbatim>

<f:verbatim><br/></f:verbatim>
<f:verbatim>STEP 1 - DDOC, BDOC failide otsija... </f:verbatim>
<f:verbatim><br/><br/></f:verbatim>
<h:commandButton value="Käivita failide otsija..." type="submit"
                 actionListener="#{teraProcess.startFileImporterInBackground}"
                 rendered="#{!teraProcess.fileFinderRunning}" />
<h:commandButton value="Failide otsimise protsess käib... peata!" type="submit"
                 actionListener="#{teraProcess.stopFileFinder}"
                 rendered="#{teraProcess.fileFinderRunning}"
                 disabled="#{teraProcess.fileFinderStopping}" />

<f:verbatim><br/><br/></f:verbatim>
<f:verbatim>STEP 2 - failide ületembeldamise protsess (ASIC-S failide looja)... </f:verbatim>
<f:verbatim><br/><br/></f:verbatim>
<h:commandButton value="Käivita ületembeldamise protsess..." type="submit"
                 actionListener="#{teraProcess.startTeraImporterInBackground}"
                 rendered="#{!teraProcess.overStampingRunning}" />
<h:commandButton value="Ületembeldamise protsess käib... peata!" type="submit"
                 actionListener="#{teraProcess.stopOverStamping}"
                 rendered="#{teraProcess.overStampingRunning}"
                 disabled="#{teraProcess.overStampingStopping}" />

<f:verbatim><br/><br/></f:verbatim>
<f:verbatim>STEP 3 - (Optional) - Faili muutja nime (unknown) asendamine (DELTA) nimega. Lisaks parandab failide nimesid. </f:verbatim>
<f:verbatim><br/><br/></f:verbatim>
<h:commandButton value="Käivita muutaja kontrollimine..." type="submit"
                 actionListener="#{teraProcess.startDataFixImporterInBackground}"
                 rendered="#{!teraProcess.dataFixRunning}" />
<h:commandButton value="Muutja kontrollimise protsse käib... peata!" type="submit"
                 actionListener="#{teraProcess.stopDataFix}"
                 rendered="#{teraProcess.dataFixRunning}"
                 disabled="#{teraProcess.dataFixStopping}" />



<f:verbatim><hr/></f:verbatim>


	<h:outputText value="Tööülesannete kustutamine repost. NB!!! Enne kasutamist veendu, et kõik updaterid, mis tööülesannete andmeid repost andmebaasi tabelitesse kirjutavad, on edukalt lõpuni jooksnud!!!! Vastasel korral ei saa tööülesannete andmeid enam taastada!!!"/>
	<f:verbatim><br/></f:verbatim>
	<h:outputText value="numberOfTasksInSingleTransaction: "/>
    <h:inputText id="deleteAllTasksUpdaterBatchSize" value="#{deleteAllTasksFromRepo.batchSize}" size="4" />
    <f:verbatim><br/></f:verbatim>    
    <h:outputText value="Paus pärast iga tööülesande töötlemist (ms): "/>
    <h:inputText id="deleteAllTasksUpdaterSleepTime" value="#{deleteAllTasksFromRepo.sleepTime}" size="4" />
    <f:verbatim><br/></f:verbatim>
    <h:commandButton id="startDeleteAllTasksFromRepoUpdater" value="Käivita tööülesannete kustutamise skript" type="submit"
      actionListener="#{deleteAllTasksFromRepo.executeUpdaterInBackground}"
      rendered="#{!deleteAllTasksFromRepo.updaterRunning}" />
    <h:commandButton id="stopDeleteAllTasksFromRepoUpdater" value="Peata tööülesannete kustutamise skript" type="submit"
      actionListener="#{deleteAllTasksFromRepo.stopUpdater}"
      rendered="#{deleteAllTasksFromRepo.updaterRunning}"
      disabled="#{deleteAllTasksFromRepo.updaterStopping}" />
	<f:verbatim><br/></f:verbatim>
	
	<h:outputText value="Logikirjete kustutamine repost. NB!!! Enne kasutamist veendu, et kõik updaterid, mis logikirjete andmeid repost andmebaasi tabelitesse kirjutavad, on edukalt lõpuni jooksnud!!!! Vastasel korral ei saa logide andmeid enam taastada!!!"/>
	<f:verbatim><br/></f:verbatim>
	<h:outputText value="numberOfLogNodesInSingleTransaction: "/>
    <h:inputText id="deleteAllLogNodesUpdaterBatchSize" value="#{deleteAllLogNodesFromRepo.batchSize}" size="4" />
    <f:verbatim><br/></f:verbatim>    
    <h:outputText value="Paus pärast iga logikirje töötlemist (ms): "/>
    <h:inputText id="deleteAllLogNodesUpdaterSleepTime" value="#{deleteAllLogNodesFromRepo.sleepTime}" size="4" />
    <f:verbatim><br/></f:verbatim>
    <h:commandButton id="startDeleteAllLogNodesFromRepoUpdater" value="Käivita logikirjete kustutamise skript" type="submit"
      actionListener="#{deleteAllLogNodesFromRepo.executeUpdaterInBackground}"
      rendered="#{!deleteAllLogNodesFromRepo.updaterRunning}" />
    <h:commandButton id="stopDeleteAllLogNodesFromRepoUpdater" value="Peata logikirjete kustutamise skript" type="submit"
      actionListener="#{deleteAllLogNodesFromRepo.stopUpdater}"
      rendered="#{deleteAllLogNodesFromRepo.updaterRunning}"
      disabled="#{deleteAllLogNodesFromRepo.updaterStopping}" />
	<f:verbatim><br/></f:verbatim>	

   <f:verbatim><br/><hr/></f:verbatim>
   <h:outputText value="Dokumentide shortRegNumber korrigeerimine numbriliseks väärtuseks ja regNumber korrigeerimine vastavalt"/>
   <f:verbatim><br/></f:verbatim>
   <h:outputText value="Päriselt muuda andmeid: "/>
   <h:selectBooleanCheckbox value="#{invalidShortRegNumberUpdater.writeValues}" />
   <f:verbatim><br/></f:verbatim>
   <h:outputText value="numberOfDocumentsInSingleTransaction: "/>
    <h:inputText id="invalidShortRegNumberUpdaterBatchSize" value="#{invalidShortRegNumberUpdater.batchSize}" size="4" />
    <f:verbatim><br/></f:verbatim>    
    <h:outputText value="Paus pärast iga dokumendi töötlemist (ms): "/>
    <h:inputText id="invalidShortRegNumberUpdaterSleepTime" value="#{invalidShortRegNumberUpdater.sleepTime}" size="4" />
    <f:verbatim><br/></f:verbatim>
    <h:commandButton id="startinvalidShortRegNumberUpdaterUpdater" value="Käivita shortRegNumber korrigeerimise skript" type="submit"
      actionListener="#{invalidShortRegNumberUpdater.executeUpdaterInBackground}"
      rendered="#{!invalidShortRegNumberUpdater.updaterRunning}" />
    <h:commandButton id="stopinvalidShortRegNumberUpdaterUpdater" value="Peata shortRegNumber korrigeerimise skript" type="submit"
      actionListener="#{invalidShortRegNumberUpdater.stopUpdater}"
      rendered="#{invalidShortRegNumberUpdater.updaterRunning}"
      disabled="#{invalidShortRegNumberUpdater.updaterStopping}" />
   <f:verbatim><br/></f:verbatim>   
	
<%--   <br/>
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
<h:commandButton id="docList_updateDocCounters" value="Uuenda dokumentide loendureid" type="submit" 
      actionListener="#{FunctionsListDialog.updateDocCounters}" />
<h:commandButton id="docList_updateArchivedDocCounters" value="Uuenda arhiveeritud dokumentide loendureid" type="submit"
	actionListener="#{FunctionsListDialog.updateArchivedDocCounters}" />      

<f:verbatim><hr/></f:verbatim>

<h:commandButton id="volume_archive_pause_all" value="Peata kõik arhiveerimistööd" type="submit" 
   actionListener="#{ArchivalsService.pauseArchiving}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="volume_archive_continue_all" value="Jätka kõiki arhiveerimistöid" type="submit" 
   actionListener="#{ArchivalsService.continueArchiving}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="volume_archive_cancel_all" value="Tühjenda arhiveerimistööde nimekiri" type="submit" 
   actionListener="#{ArchivalsService.cancelAllArchivingJobs}" />

<f:verbatim><hr/></f:verbatim>

<h:commandButton id="volume_destruction_stop_all" value="Peata kõik hävitamistööd" type="submit" 
   actionListener="#{ArchivalsService.stopDestructing}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="volume_destruction_pause_all" value="Peata kõik hävitamistööd kuni \"Jätka\" vajutamiseni" type="submit" 
   actionListener="#{ArchivalsService.pauseDestruction}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="volume_destruction_continue_all" value="Jätka kõiki hävitamistöid" type="submit" 
   actionListener="#{ArchivalsService.continueDestructing}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="volume_destruction_cancel_all" value="Tühjenda hävitamistööde nimekiri" type="submit" 
   actionListener="#{ArchivalsService.cancelAllDestructingJobs}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Muuda arhiveeritud sarjade property 'volNumberPattern' (väli 'Asjatoimiku viida muster') tühjaks kui 'volType' (väli 'Sisaldab toimikuid') ei sisalda väärtust CASE_FILE (Asjatoimik)"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Kui väljal 'Sisaldab toimikuid' ei ole ühtegi väärtust valitud, lisatakse uuendamise käigus kõik väärtused peale väärtuse 'Asjatoimik'"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="archivedSeries_update" value="Uuenda arhiveeritud toimikuid" type="submit" 
   actionListener="#{archivedSeriesVolTypeAndVolNumberPatternUpdater.executeUpdaterInBackground}" 
   rendered="#{archivedSeriesVolTypeAndVolNumberPatternUpdater.updaterRunning == false}" />
<h:commandButton id="archivedSeries_update_stop" value="Peata uuendamine" type="submit"
   actionListener="#{archivedSeriesVolTypeAndVolNumberPatternUpdater.stopUpdater}"
   rendered="#{archivedSeriesVolTypeAndVolNumberPatternUpdater.updaterRunning == true}"
   disabled="#{archivedSeriesVolTypeAndVolNumberPatternUpdater.updaterStopping == true}" />

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Sarja elukäigu alusel toimiku elukäigu uuendamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Ruum: "/>
<h:inputText id="volumeEventPlanUpdaterStoreString" value="#{volumeEventPlanUpdater.storeString}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startVolumeEventPlanUpdater" value="Käivita" type="submit"
   actionListener="#{volumeEventPlanUpdater.executeUpdaterInBackground}"
   rendered="#{volumeEventPlanUpdater.updaterRunning == false}" />
<h:commandButton id="stopVolumeEventPlanUpdater" value="Peata" type="submit"
   actionListener="#{volumeEventPlanUpdater.stopUpdater}"
   rendered="#{volumeEventPlanUpdater.updaterRunning == true}"
   disabled="#{volumeEventPlanUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Dokumendi ADR pealkirjaks dokumendi liigi nimetuse määramine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Kuupäev (k.a., kujul pp.kk.aaaa), millest varem loodud dokumentidel määratakse välja 'ADR pealkiri' tühja väärtuse asemel väärtuseks dokumendi liigi pealkiri: "/>
<h:inputText id="endDateStr" value="#{adrDocNameUpdater.endDateStr}" size="12" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startAdrDocNameUpdater" value="Käivita" type="submit"
   actionListener="#{adrDocNameUpdater.executeUpdaterInBackground}"
   rendered="#{adrDocNameUpdater.updaterRunning == false}" />
<h:commandButton id="stopAdrDocNameUpdater" value="Peata" type="submit"
   actionListener="#{adrDocNameUpdater.stopUpdater}"
   rendered="#{adrDocNameUpdater.updaterRunning == true}"
   disabled="#{adrDocNameUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Dokumendi avalikku registrisse=Ei, lähe ADR-i määramine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Kuupäev (k.a., kujul pp.kk.aaaa), millest varem loodud dokumentidel määratakse avalikku registrisse=Ei, lähe ADR-i: "/>
<h:inputText id="ptaEndDateStr" value="#{publishToAdrUpdater.ptaEndDateStr}" size="12" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startPublishToAdrUpdater" value="Käivita" type="submit"
   actionListener="#{publishToAdrUpdater.executeUpdaterInBackground}"
   rendered="#{publishToAdrUpdater.updaterRunning == false}" />
<h:commandButton id="stopPublishToAdrUpdater" value="Peata" type="submit"
   actionListener="#{publishToAdrUpdater.stopUpdater}"
   rendered="#{publishToAdrUpdater.updaterRunning == true}"
   disabled="#{publishToAdrUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Dokumendi haldurite õiguste parandamine (DVK, Outlook, ...)"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startPermissionDocManagerUpdater" value="Käivita" type="submit"
   actionListener="#{foldersPermissionForDocManagerUpdater.executeUpdaterInBackground}"
   rendered="#{foldersPermissionForDocManagerUpdater.updaterRunning == false}" />
<h:commandButton id="stopPermissionDocManagerUpdater" value="Peata" type="submit"
   actionListener="#{foldersPermissionForDocManagerUpdater.stopUpdater}"
   rendered="#{foldersPermissionForDocManagerUpdater.updaterRunning == true}"
   disabled="#{foldersPermissionForDocManagerUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Paranda .ddoc failide mimetype"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startFixDdocMimetypeUpdater" value="Käivita" type="submit"
   actionListener="#{fixDdocMimetypeUpdater.executeUpdaterInBackground}"
   rendered="#{fixDdocMimetypeUpdater.updaterRunning == false}" />
<h:commandButton id="stopFixDdocMimetypeUpdater" value="Peata" type="submit"
   actionListener="#{fixDdocMimetypeUpdater.stopUpdater}"
   rendered="#{fixDdocMimetypeUpdater.updaterRunning == true}"
   disabled="#{fixDdocMimetypeUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Versioonide lahtilukustamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="versionUnlockUpdaterBatchSize" value="#{versionUnlockUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startVersionUnlockUpdater" value="Käivita" type="submit"
   actionListener="#{versionUnlockUpdater.executeUpdaterInBackground}"
   rendered="#{versionUnlockUpdater.updaterRunning == false}" />
<h:commandButton id="stopVersionUnlockUpdater" value="Peata" type="submit"
   actionListener="#{versionUnlockUpdater.stopUpdater}"
   rendered="#{versionUnlockUpdater.updaterRunning == true}"
   disabled="#{versionUnlockUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Aasta- ja objektipõhistel toimikutel \"Sisaldab teemasid\" = jah väärtustamine, kui toimikus on vähemalt üks teema"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="volumeContainsCasesUpdaterBatchSize" value="#{volumeContainsCasesUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startVolumeContainsCasesUpdater" value="Käivita" type="submit"
   actionListener="#{volumeContainsCasesUpdater.executeUpdaterInBackground}"
   rendered="#{volumeContainsCasesUpdater.updaterRunning == false}" />
<h:commandButton id="stopVolumeContainsCasesUpdater" value="Peata" type="submit"
   actionListener="#{volumeContainsCasesUpdater.stopUpdater}"
   rendered="#{volumeContainsCasesUpdater.updaterRunning == true}"
   disabled="#{volumeContainsCasesUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Rahvusvahelise õigusabi dokumentidel publishToAdr = 'Ei lähe ADR-i' väärtustamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu dokumenti ühes transaktsioonis töödelda: "/>
<h:inputText id="documentPublishToAdrUpdaterBatchSize" value="#{documentPublishToAdrUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startDocumentPublishToAdrUpdater" value="Käivita" type="submit"
   actionListener="#{documentPublishToAdrUpdater.executeUpdaterInBackground}"
   rendered="#{documentPublishToAdrUpdater.updaterRunning == false}" />
<h:commandButton id="stopDocumentPublishToAdrUpdater" value="Peata" type="submit"
   actionListener="#{documentPublishToAdrUpdater.stopUpdater}"
   rendered="#{documentPublishToAdrUpdater.updaterRunning == true}"
   disabled="#{documentPublishToAdrUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="owner* väljade väärtustamine vastavalt SP impordi reeglitele"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Faili d_kasut_adsi.csv tee: "/>
<h:inputText id="ownerPropsUpdaterKasutAdsiPath" value="#{ownerPropsUpdater.kasutAdsiPath}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="defaultOwnerId: "/>
<h:inputText id="ownerPropsUpdaterDefaultOwnerId" value="#{ownerPropsUpdater.defaultOwnerId}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="ownerPropsUpdaterBatchSize" value="#{ownerPropsUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startOwnerPropsUpdater" value="Käivita" type="submit"
   actionListener="#{ownerPropsUpdater.executeUpdaterInBackground}"
   rendered="#{ownerPropsUpdater.updaterRunning == false}" />
<h:commandButton id="stopOwnerPropsUpdater" value="Peata" type="submit"
   actionListener="#{ownerPropsUpdater.stopUpdater}"
   rendered="#{ownerPropsUpdater.updaterRunning == true}"
   disabled="#{ownerPropsUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="ownerEmail 'test22@just.ee' -> vastavalt ownerId-le praeguse kasutaja e-maili aadressiga väärtustamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="ownerEmailUpdaterBatchSize" value="#{ownerEmailUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startOwnerEmailUpdater" value="Käivita" type="submit"
   actionListener="#{ownerEmailUpdater.executeUpdaterInBackground}"
   rendered="#{ownerEmailUpdater.updaterRunning == false}" />
<h:commandButton id="stopOwnerEmailUpdater" value="Peata" type="submit"
   actionListener="#{ownerEmailUpdater.stopUpdater}"
   rendered="#{ownerEmailUpdater.updaterRunning == true}"
   disabled="#{ownerEmailUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Dokumentide viida (regNumber) väärtustamine = '-', kui viit (regNumber) on väärtustamata ja registreerimise kuupäev (regDateTime) on väärtustatud" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="emptyDocumentRegNrUpdaterBatchSize" value="#{emptyDocumentRegNrUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startEmptyDocumentRegNrUpdater" value="Käivita" type="submit"
   actionListener="#{emptyDocumentRegNrUpdater.executeUpdaterInBackground}"
   rendered="#{emptyDocumentRegNrUpdater.updaterRunning == false}" />
<h:commandButton id="stopEmptyDocumentRegNrUpdater" value="Peata" type="submit"
   actionListener="#{emptyDocumentRegNrUpdater.stopUpdater}"
   rendered="#{emptyDocumentRegNrUpdater.updaterRunning == true}"
   disabled="#{emptyDocumentRegNrUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Dokumendi failide liigutamine taustainfo plokki"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Faili completed_docs.csv tee: "/>
<h:inputText id="fileActiveUpdaterCompletedDocsCsvPath" value="#{fileActiveUpdater.completedDocsCsvPath}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Faili files.csv tee: "/>
<h:inputText id="fileActiveUpdaterFilesCsvPath" value="#{fileActiveUpdater.filesCsvPath}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitme dokumendi failid ühes transaktsioonis töödelda: "/>
<h:inputText id="fileActiveUpdaterBatchSize" value="#{fileActiveUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startFileActiveUpdater" value="Käivita" type="submit"
   actionListener="#{fileActiveUpdater.executeUpdaterInBackground}"
   rendered="#{fileActiveUpdater.updaterRunning == false}" />
<h:commandButton id="stopFileActiveUpdater" value="Peata" type="submit"
   actionListener="#{fileActiveUpdater.stopUpdater}"
   rendered="#{fileActiveUpdater.updaterRunning == true}"
   disabled="#{fileActiveUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="'generalCaseFile' tüüpi asjatoimikute objectTypeVersionNr uuendamine uusima väärtuse peale"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="caseFileLatestVersionUpdaterBatchSize" value="#{caseFileLatestVersionUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startCaseFileLatestVersionUpdater" value="Käivita" type="submit"
   actionListener="#{caseFileLatestVersionUpdater.executeUpdaterInBackground}"
   rendered="#{caseFileLatestVersionUpdater.updaterRunning == false}" />
<h:commandButton id="stopCaseFileLatestVersionUpdater" value="Peata" type="submit"
   actionListener="#{caseFileLatestVersionUpdater.stopUpdater}"
   rendered="#{caseFileLatestVersionUpdater.updaterRunning == true}"
   disabled="#{caseFileLatestVersionUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="d_lisavaljaVaartus.csv järgi asjatoimikute keywordsString väärtustamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Faili d_lisavaljaVaartus.csv tee: "/>
<h:inputText id="caseFileKeywordsStringUpdaterLisavaljaVaartusCsvPath" value="#{caseFileKeywordsStringUpdater.lisavaljaVaartusCsvPath}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="caseFileKeywordsStringUpdaterBatchSize" value="#{caseFileKeywordsStringUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startCaseFileKeywordsStringUpdater" value="Käivita" type="submit"
   actionListener="#{caseFileKeywordsStringUpdater.executeUpdaterInBackground}"
   rendered="#{caseFileKeywordsStringUpdater.updaterRunning == false}" />
<h:commandButton id="stopCaseFileKeywordsStringUpdater" value="Peata" type="submit"
   actionListener="#{caseFileKeywordsStringUpdater.stopUpdater}"
   rendered="#{caseFileKeywordsStringUpdater.updaterRunning == true}"
   disabled="#{caseFileKeywordsStringUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Dokumendi storageType väärtustamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu dokumenti ühes transaktsioonis töödelda: "/>
<h:inputText id="storageTypeUpdaterBatchSize" value="#{storageTypeUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startStorageTypeUpdater" value="Käivita" type="submit"
   actionListener="#{storageTypeUpdater.executeUpdaterInBackground}"
   rendered="#{storageTypeUpdater.updaterRunning == false}" />
<h:commandButton id="stopStorageTypeUpdater" value="Peata" type="submit"
   actionListener="#{storageTypeUpdater.stopUpdater}"
   rendered="#{storageTypeUpdater.updaterRunning == true}"
   disabled="#{storageTypeUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Dokumentide content väljalt HTML spetsiifiliste sümbolite eemaldamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu dokumenti ühes transaktsioonis töödelda: "/>
<h:inputText id="documentContentPropertyHtmlUpdaterBatchSize" value="#{documentContentPropertyHtmlUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startDocumentContentPropertyHtmlUpdater" value="Käivita" type="submit"
   actionListener="#{documentContentPropertyHtmlUpdater.executeUpdaterInBackground}"
   rendered="#{documentContentPropertyHtmlUpdater.updaterRunning == false}" />
<h:commandButton id="stopDocumentContentPropertyHtmlUpdater" value="Peata" type="submit"
   actionListener="#{documentContentPropertyHtmlUpdater.stopUpdater}"
   rendered="#{documentContentPropertyHtmlUpdater.updaterRunning == true}"
   disabled="#{documentContentPropertyHtmlUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Testandmete kaevandamine - andmed mis lähevad ADR'i, kirjutataks csv failidesse testandmete generaatori jaoks" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="Mitu objekti ühes transaktsioonis töödelda: "/>
<h:inputText id="adrTestDataUpdaterBatchSize" value="#{adrTestDataUpdater.batchSize}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startadrTestDataUpdater" value="Käivita" type="submit"
   actionListener="#{adrTestDataUpdater.executeUpdaterInBackground}"
   rendered="#{adrTestDataUpdater.updaterRunning == false}" />
<h:commandButton id="stopadrTestDataUpdater" value="Peata" type="submit"
   actionListener="#{adrTestDataUpdater.stopUpdater}"
   rendered="#{adrTestDataUpdater.updaterRunning == true}"
   disabled="#{adrTestDataUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Restore data _from_ Delta specified by the following database and contentstore folder"/>
<f:verbatim><br/><br/></f:verbatim>
<h:outputText value="db.name="/>
<h:inputText value="#{UserDataRestoreService.dbName}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="db.username="/>
<h:inputText value="#{UserDataRestoreService.dbUsername}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="db.password="/>
<h:inputText value="#{UserDataRestoreService.dbPassword}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="db.host="/>
<h:inputText value="#{UserDataRestoreService.dbHost}" size="60" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="db.port="/>
<h:inputText value="#{UserDataRestoreService.dbPort}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="dir.contentstore="/>
<h:inputText value="#{UserDataRestoreService.otherContentstore}" size="60" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="kasutajate isikukoodid (tühikute või reavahetustega eraldatud): "/>
<h:inputTextarea id="validUsers" value="#{UserDataRestoreService.validUsers}" rows="5" cols="30" styleClass="expand19-200" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startUserDataRestore" value="executeUserDataRestore" type="submit"
   actionListener="#{UserDataRestoreService.execute}" />

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
   actionListener="#{NotificationService.processAccessRestrictionEndDateNotifications}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processTaskDueDateNotifications" value="processTaskDueDateNotifications" type="submit"
   actionListener="#{NotificationService.processTaskDueDateNotifications}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processVolumeDispositionDateNotifications" value="processVolumeDispositionDateNotifications" type="submit"
   actionListener="#{NotificationService.processVolumeDispositionDateNotifications}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="processContractDueDateNotifications" value="processContractDueDateNotifications" type="submit"
   actionListener="#{NotificationService.processContractDueDateNotifications}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Taustatööd: "/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="destroyArchivedVolumes" value="destroyArchivedVolumes" type="submit"
   actionListener="#{ArchivalsService.destroyArchivedVolumes}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="updateOrganisationStructures" value="updateOrganisationStructures" type="submit"
   actionListener="#{OrganizationStructureService.updateOrganisationStructures}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="updateUsersAndGroups" value="updateUsersAndGroups" type="submit"
   actionListener="#{TestingForDeveloperBean.updateUsersAndGroups}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="updateOrganisationStructureBasedGroups" value="updateOrganisationStructureBasedGroups" type="submit"
   actionListener="#{OrganizationStructureService.updateOrganisationStructureBasedGroups}" />
   <f:verbatim><br/></f:verbatim>
   
<h:commandButton id="repairDuplicateSubstituteRoots" value="Paranda dubleeritud asendajate asukohad" type="submit"
   actionListener="#{RepairSubstitutesAfterMerge.executeUpdaterInBackground}" />
<f:verbatim><br/></f:verbatim>

<h:outputText id="reportGenerationTitle" value="Aruannete genereerimine: " />
<f:verbatim><br/></f:verbatim>
<h:outputText id="reportGenerationStatus" value=" Selles klastri õlas aruannete genereerimine ei jookse." rendered="#{!ReportListDialog.reportGenerationEnabled}" />
<h:commandButton id="pauseReportGeneration" value="Peata aruannete genereerimine" type="submit"
   actionListener="#{ReportListDialog.pauseReportGeneration}" rendered="#{ReportListDialog.showPauseReportGeneration}" />
<h:commandButton id="continueReportGeneration" value="Jätka aruannete genereerimist" type="submit"
   actionListener="#{ReportListDialog.continueReportGeneration}" rendered="#{ReportListDialog.showContinueReportGeneration}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<f:verbatim><br/></f:verbatim>

<h:outputText value="numberOfObjectsInSingleTransaction: "/>
<h:inputText id="deleteAllDocumentsBatchSize" value="#{FunctionsListDialog.deleteBatchSize}" size="4" />
<f:verbatim><br/></f:verbatim>    
<h:commandButton id="deleteAllDocuments" value="deleteAllDocuments (delete permanently, skip trashcan)" type="submit"
   actionListener="#{FunctionsListDialog.deleteAllDocuments}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="deleteAllDocumentsAndStructure" value="deleteAllDocumentsAndStructure (delete permanently, skip trashcan)" type="submit"
   actionListener="#{FunctionsListDialog.deleteAllDocumentsAndStructure}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="deleteAllDocumentsAndStructureAndIndependentCompoundWorkflows" value="deleteAllDocumentsAndStructureAndIndependentCompoundWorkflows (delete permanently, skip trashcan)" type="submit"
   actionListener="#{FunctionsListDialog.deleteAllDocumentsAndStructureAndIndependentCompoundWorkflows}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="deleteAllIndependentCompoundWorkflows" value="deleteAllIndependentCompoundWorkflows (delete permanently, skip trashcan)" type="submit"
   actionListener="#{FunctionsListDialog.deleteAllIndependentCompoundWorkflows}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Testandmete genereerimine: "/>
<f:verbatim>
<br/>
<ul>
<li>* Kopeerida SVN'ist delta/common/etc/testdata/*.csv failid \${dir.root} kausta</li>
<li>* Kopeerida ADR failide kaust asukohta \${dir.root}/contentstore/testfiles . Seega peaksid ADR failid asuma \${dir.root}/contentstore/testfiles/1/1234 jne.</li>
<li>* Tagada, et dokumendi liikide all oleksid soovitud dokumendi liigid ja andmeväljad (näiteks importida SVN'ist failist delta/common/etc/jmeter/documentTypes.xml)</li>
<li>* Tagada, et klassifikaatorite all oleksid soovitud klassifikaatorid ja väärtused (näiteks importida SVN'ist failist delta/common/etc/jmeter/classificators.xml)</li>
<li>* Tagada, et asjatoimiku liikide all oleksid soovitud asjatoimikute liigid ja andmeväljad (näiteks luua vähemalt üks liik või importida)</li>
<li>* Kui kasutajaid ja/või struktuuriüksusi genereeritakse (genereeritakse siis kui lahtrites kasutajate arv / struktuuriüksuste on suuremad arvud kui praegu eksisteerivaid objekte), siis peab arvestama sellega, et järgmisel sünkimisel kustutatakse genereeritud objektid! Ainus võimalus sünkimise ära hoidmiseks on konf.failis \${amr.service.url} muuta mittetöötava/mitteeksisteeriva URL'i peale.
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011), ja pärast teha ühekorraga järgi. Koos indekseerimise välja lülitamisega lülitada välja ka öises taustatöös indeksi andmetes aukude otsimine (findHolesAndIndex.enabled=false).</li>
<li>* Vajutada Käivita testandmete genereerimine. Progressi saab jälgida logist.</li>
<li>* (TODO struktuuriüksuste sünkimine lülitada välja, et loodud struktuuriüksusi sünkimisel ei kustutataks.)</li>
<li>* Kasutajate loomise lõpus kirjutatakse kõik eksisteerivad kasutajanimed faili \${dir.root}/users.csv ja kasutajate ees- ja perenimed faili \${dir.root}/usersfirstlastnames.csv. Neid faile läheb vaja koormustestide sisendina.</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="Kasutajate e-mail: "/>
<h:inputText value="#{TestDataService.testEmail}" size="40" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Struktuuriüksuste arv: "/>
<h:inputText value="#{TestDataService.orgUnitsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Kasutajate arv (kui 0, siis asenduste genereerimist ei toimu): "/>
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

<h:outputText value="Asjatoimikute arv: "/>
<h:inputText value="#{TestDataService.caseFilesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Genereeri asjatoimiku terviktöövood: "/>
<h:selectBooleanCheckbox value="#{TestDataService.caseFileWorkflowsEnabled}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="s.h. suletud asjatoimikute arv: "/>
<h:inputText value="#{TestDataService.closedCaseFilesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Asjade arv: "/>
<h:inputText value="#{TestDataService.casesCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Dokumentide arv: "/>
<h:inputText value="#{TestDataService.documentsCount}" converter="javax.faces.Integer" size="7" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Genereeri dokumendi terviktöövood: "/>
<h:selectBooleanCheckbox value="#{TestDataService.documentWorkflowsEnabled}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Iseseisvate terviktöövoogude arv: "/>
<h:inputText value="#{TestDataService.independentWorkflowsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="s.h. lõpetatud iseseisvate terviktöövoogude arv: "/>
<h:inputText value="#{TestDataService.finishedIndependentWorkflowsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Maksimum dokumentide arv iseseisvas ühes terviktöövoos: "/>
<h:inputText value="#{TestDataService.maxDocumentsInIndependentWorkflow}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Failid: "/>
<h:selectBooleanCheckbox value="#{TestDataService.filesEnabled}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Dokumentide ja iseseisvate terviktöövoogude genereerimiseks paralleelsete lõimede arv: "/>
<h:inputText value="#{TestDataService.documentAndWorkflowGeneratorThreads}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Hävitamiseks valmis asjatoimikute arv: "/>
<h:inputText value="#{TestDataService.destructionReadyDossiersCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Dokumente igas hävitamiseks valmis asjatoimikus: "/>
<h:inputText value="#{TestDataService.destructionReadyDossiersDocumentsCount}" converter="javax.faces.Integer" size="4" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton id="startTestDataGenerator" value="Käivita andmete genereerimine" type="submit"
      actionListener="#{TestDataService.executeUpdaterInBackground}"
      rendered="#{TestDataService.updaterRunning == false}" />
   <h:commandButton id="stopTestDataGenerator" value="Peata andmete genereerimine" type="submit"
      actionListener="#{TestDataService.stopUpdater}" 
      rendered="#{TestDataService.updaterRunning == true}"
      disabled="#{TestDataService.updaterStopping == true}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Sharepoint andmete importimine: " style="font-weight: bold;" />
<f:verbatim>
<br/>
<ul>
<li>* Enne dokumentide importi peavad kõik kasutajad olema rakendusse tõmmatud.</li>
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011), ja pärast teha ühekorraga järgi. Koos indekseerimise välja lülitamisega lülitada välja ka öises taustatöös indeksi andmetes aukude otsimine (findHolesAndIndex.enabled=false).</li>
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

<h:outputText value="Sharepoint andmete importimise parameetrid: " style="font-weight: bold;" />
<f:verbatim>
<ul>
<li>* <strong>dataFolder</strong> &ndash; absoluutne tee kaustani rakenduse serveris, kus asub ka "struktuur.csv" fail, millest imporditakse.</li>
<li>* <strong>workFolder</strong> &ndash; absoluutne tee olemasoleva kaustani rakenduse serveris, kuhu kirjutatakse logifailid.</li>
<li>* <strong>mappingsFileName</strong> &ndash; absoluutne tee SharePointist eksporditud XML failide ja Delta dokumendi liikide vahelist vastavust kirjedava XML failini.</li>
<li>* <strong>defaultOwnerId</strong> &ndash; vaikimisi dokumendi/menetluse vastutajaks määratava kasutaja isikukood.</li>
<li>* <strong>taskOwnerStructUnit</strong> &ndash; kasutajagrupp, mis lisatakse sarjade õigustesse, et grupi liikmed saaksid sarja dokumente ja faile vaadata.</li>
<li>* <strong>docListArchivalsSeparatingDate</strong> &ndash; kuupäev (PP.KK.AAAA), millest varem loodud toimikud pannakse arhiivi.</li>
<li>* <strong>publishToAdrStartingFromDate</strong> &ndash; kuupäev (PP.KK.AAAA), millest varem loodud toimikud ei lähe ADR-i.</li>
<li>* <strong>publishToAdrWithFilesStartingFromDate</strong> &ndash; kuupäev (PP.KK.AAAA), millest varem loodud toimikud ei lähe ADR-i.</li>
<li>* <strong>seriesIdentifierForProcessToCaseFile</strong> &ndash; sarja tähis, mille alla menetluste impordil luuakse asjamenetluste kohta asjatoimikuid; kui tühi väärtus või sellist sarja menetluste impordi ajal ei leita, siis asjamenetluste kohta asjatoimikuid ei tekitata.</li>
<li>* <strong>caseFileTypeIdForProcessToCaseFile</strong> &ndash; asjatoimiku liigi identifikaator, millist liiki asjatoimikud asjamenetluste kohta menetluste impordil luuakse; kui tühi väärtus, siis asjamenetluste kohta asjatoimikuid ei tekitata.</li>
<li>* <strong>numberOfDocumentsProceduresInSingleTransaction</strong> &ndash; mitme dokumendi import toimub ühes transaktsioonis, nn <i>batch size</i> (naturaalarv).</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="dataFolder [*]: "/>
<h:inputText value="#{sharepointImporter.data.dataFolder}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="workFolder [*]: "/>
<h:inputText value="#{sharepointImporter.data.workFolder}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="mappingsFileName [*]: "/>
<h:inputText value="#{sharepointImporter.data.mappingsFileName}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="defaultOwnerId [*]: "/>
<h:inputText value="#{sharepointImporter.data.defaultOwnerId}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="taskOwnerStructUnit: "/>
<h:inputText value="#{sharepointImporter.data.taskOwnerStructUnit}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="docListArchivalsSeparatingDate [*]: "/>
<h:inputText value="#{sharepointImporter.data.docListArchivalsSeparatingDate}" size="60" converter="javax.faces.DateTime">
  <f:convertDateTime pattern="dd.MM.yyyy" />
</h:inputText>
<f:verbatim><br/></f:verbatim>

<h:outputText value="publishToAdrStartingFromDate: "/>
<h:inputText value="#{sharepointImporter.data.publishToAdrStartingFromDate}" size="60" converter="javax.faces.DateTime">
  <f:convertDateTime pattern="dd.MM.yyyy" />
</h:inputText>
<f:verbatim><br/></f:verbatim>

<h:outputText value="publishToAdrWithFilesStartingFromDate [*]: "/>
<h:inputText value="#{sharepointImporter.data.publishToAdrWithFilesStartingFromDate}" size="60" converter="javax.faces.DateTime">
  <f:convertDateTime pattern="dd.MM.yyyy" />
</h:inputText>
<f:verbatim><br/></f:verbatim>

<h:outputText value="seriesIdentifierForProcessToCaseFile: "/>
<h:inputText value="#{sharepointImporter.data.seriesIdentifierForProcessToCaseFile}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="caseFileTypeIdForProcessToCaseFile: "/>
<h:inputText value="#{sharepointImporter.data.caseFileTypeIdForProcessToCaseFile}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="numberOfDocumentsProceduresInSingleTransaction [*]: "/>
<h:inputText value="#{sharepointImporter.data.batchSize}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="docsWithVersions: "/>
<h:selectBooleanCheckbox value="#{sharepointImporter.data.docsWithVersions}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="structureAndDocumentsComeFrom: "/>
<h:selectOneMenu value="#{sharepointImporter.data.structAndDocsOrigin}">
  <f:selectItem id="s1" itemValue="SharePoint"/>
  <f:selectItem id="s2" itemValue="Amphora"/>
  <f:selectItem id="s3" itemValue="Riigikohtu infosüsteem"/>
</h:selectOneMenu>
<f:verbatim><br/></f:verbatim>

<h:messages />
<f:verbatim><br/></f:verbatim>
<h:outputText escape="false" value="#{sharepointImporter.status}" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton value="Käivita SP andmete import" type="submit"
      actionListener="#{sharepointImporter.startImporterInBackground}"
      rendered="#{!sharepointImporter.importerRunning}" />
   <h:commandButton value="Peata SP andmete import" type="submit"
      actionListener="#{sharepointImporter.stopImporter}"
      rendered="#{sharepointImporter.importerRunning}"
      disabled="#{sharepointImporter.importerStopping}" />

<f:verbatim><hr/></f:verbatim>

<h:outputText value="Postipoiss andmete importimine: " style="font-weight: bold;" />
<f:verbatim>
<br/>
<ul>
<li>* Enne dokumentide importi peavad kõik kasutajad olema rakendusse tõmmatud.</li>
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011), ja pärast teha ühekorraga järgi. Koos indekseerimise välja lülitamisega lülitada välja ka öises taustatöös indeksi andmetes aukude otsimine (findHolesAndIndex.enabled=false).</li>
<li>* Käivitamise nupp võimalusel jätkab pooleli jäänud kohast (kui workFolder-ites on csv faile kus eelnev progress on kirjas).</li>
<li>* Peatamise nupp peatab esimesel võimalusel (konktaktide impordi keskel, struktuuri impordi keskel, iga 50 dokumendi importimise või faili importimise või faili indekseerimise vahel)</li>
<li>* Kui ükskõik milline parameeter grupis (...DataFolder, ...WorkFolder, ...ArchivalsStore, ...MappingsFileName) on tühi, siis liigutakse järgmise parameetrite grupi juurde.</li>
<li>* Kui ...DataFolder asukohas leidub fail kontaktid.csv, siis teostatakse kontaktide import. Kui faili ei leidu, siis liigutakse järgmisesse sammu, viga ei teki. Kui kontaktide import õnnestub, kirjutatakse ...WorkFolder asukohta fail completed_kontaktid.csv.</li>
<li>* Struktuuri impordi jaoks loetakse sisse asukohas ...DataFolder olevad failid struktuur.csv ja toimikud.csv. Tekitatakse funktsioonid/sarjad/toimikud arhiivimoodustaja alla, mis on määratud parameetris ...ArchivalsStore. Impordi käigus kirjutatakse asukohta ...WorkFolder fail completed_toimikud.csv.</li>
<li>* Dokumentide impordil luuakse asukohas ...DataFolder olevad dokumendid ja failid. Mappings.xml faili nimetus on parameetris ...MappingsFileName. Impordi käigus kirjutatakse asukohta ...WorkFolder järgmised failid - completed_docs.csv, completed_files.csv, indexed_files.csv, users_found.csv, users_not_found.csv, postponed_assocs.csv.</li>
<li>* Impordi progressi, infoteateid ja veateateid saab jälgida rakenduse logist.</li>
<li>* publishToAdrWithFilesStartingFromDates - kuupäev (PP.KK.AAAA). kui parameetri väärtus &gt; imporditava dokumendi reg kuupäev ja dokumendi JP =Avalik, siis määratakse dokumendi publishToAdr ="Avalik, väljastatakse teabenõude korras". muul juhul publishToAdr ="Läheb ADR-i"</li>
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

<h:outputText value="firstSetPublicFilesToBackgroundFiles: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.publicFilesToBackgroundFiles[0]}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="firstPublishToAdrWithFilesStartingFromDates: "/>
<h:inputText value="#{postipoissImporter.publishToAdrWithFilesStartingFromDates[0]}" size="12" />
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

<h:outputText value="secondSetPublicFilesToBackgroundFiles: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.publicFilesToBackgroundFiles[1]}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="secondPublishToAdrWithFilesStartingFromDates: "/>
<h:inputText value="#{postipoissImporter.publishToAdrWithFilesStartingFromDates[1]}" size="12" />
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

<h:outputText value="thirdSetPublicFilesToBackgroundFiles: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.publicFilesToBackgroundFiles[2]}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="thirdPublishToAdrWithFilesStartingFromDates: "/>
<h:inputText value="#{postipoissImporter.publishToAdrWithFilesStartingFromDates[2]}" size="12" />
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

<h:outputText value="fourthSetPublicFilesToBackgroundFiles: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.publicFilesToBackgroundFiles[3]}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fourthPublishToAdrWithFilesStartingFromDates: "/>
<h:inputText value="#{postipoissImporter.publishToAdrWithFilesStartingFromDates[3]}" size="12" />
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

<h:outputText value="fifthSetPublicFilesToBackgroundFiles: "/>
<h:selectBooleanCheckbox value="#{postipoissImporter.publicFilesToBackgroundFiles[4]}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="fifthPublishToAdrWithFilesStartingFromDates: "/>
<h:inputText value="#{postipoissImporter.publishToAdrWithFilesStartingFromDates[4]}" size="12" />
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

<f:verbatim><hr/></f:verbatim>

<h:outputText value="GoPro andmete importimine: " style="font-weight: bold;" />
<f:verbatim>
<br/>
<ul>
<li>* Enne dokumentide importi peavad kõik kasutajad olema rakendusse tõmmatud.</li>
<li>* Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni 2011), ja pärast teha ühekorraga järgi. Koos indekseerimise välja lülitamisega lülitada välja ka öises taustatöös indeksi andmetes aukude otsimine (findHolesAndIndex.enabled=false).</li>
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

<h:outputText value="GoPro andmete importimise parameetrid: " style="font-weight: bold;" />
<f:verbatim>
<ul>
<li>* <strong>dataFolder</strong> &ndash; absoluutne tee kaustani rakenduse serveris, kus asub ka "struktuur.csv" fail, millest imporditakse.</li>
<li>* <strong>workFolder</strong> &ndash; absoluutne tee olemasoleva kaustani rakenduse serveris, kuhu kirjutatakse logifailid.</li>
<li>* <strong>mappingsFileName</strong> &ndash; absoluutne tee GoPro-st eksporditud XML failide ja Delta dokumendi liikide vahelist vastavust kirjedava XML failini.</li>
<li>* <strong>defaultOwnerId</strong> &ndash; vaikimisi dokumendi/menetluse vastutajaks määratava kasutaja isikukood.</li>
<li>  <strong>publishToAdr</strong> &ndash; true/false väärtus, Kui väärtus = false, määratakse kõik dokumendid ADRi mitteavadatavateks, true väärtuse korral rakendatakse avalikustamise reeglistikku.</li>
<li>  <strong>Kõik failid failide plokki</strong> &ndash; true/false</li>
<li>  <strong>Kõik tööülesanded teostatuks</strong> &ndash; true/false</li>
<li>* <strong>docListArchivalsSeparatingDate</strong> &ndash; kuupäev (PP.KK.AAAA), millest varem loodud toimikud pannakse arhiivi, kui sisendfaili veerg "Toimiku staatus" on täitmata</li>
<li> <strong>taskOwnerStructUnitAuthority</strong> &ndash; kasutajagrupp, mis lisatakse sarjade õigustesse, et grupi liikmed saaksid sarja dokumente ja faile vaadata ning muuta.</li>
<li> <strong>taskOwnerStructUnitAuthorityPrivileges</strong> &ndash; privileegid, mis lisatakse sarjade õigustesse, et grupi liikmed saaksid sarja dokumente ja faile vaadata ning muuta.</li>
</ul>
<br/>
</f:verbatim>

<h:outputText value="dataFolder [*]: "/>
<h:inputText value="#{goproImporter.data.dataFolder}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="workFolder [*]: "/>
<h:inputText value="#{goproImporter.data.workFolder}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="mappingsFileName [*]: "/>
<h:inputText value="#{goproImporter.data.mappingsFileName}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="defaultOwnerId [*]: "/>
<h:inputText value="#{goproImporter.data.defaultOwnerId}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="publishToAdr: "/>
<h:selectBooleanCheckbox value="#{goproImporter.data.publishToAdr}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Kõik failid failide plokki: "/>
<h:selectBooleanCheckbox value="#{goproImporter.data.allFilesActive}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="Kõik tööülesanded teostatuks: "/>
<h:selectBooleanCheckbox value="#{goproImporter.data.allWfFinished}" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="docListArchivalsSeparatingDate [*]: "/>
<h:inputText value="#{goproImporter.data.docListArchivalsSeparatingDate}" size="60" converter="javax.faces.DateTime">
  <f:convertDateTime pattern="dd.MM.yyyy" />
</h:inputText>
<f:verbatim><br/></f:verbatim>

<h:outputText value="taskOwnerStructUnitAuthority: "/>
<h:inputText value="#{goproImporter.data.taskOwnerStructUnitAuthority}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:outputText value="taskOwnerStructUnitAuthorityPrivileges: " style="align: top"/>
<h:selectManyListbox size="5" value="#{goproImporter.data.taskOwnerStructUnitAuthorityPrivileges}">
  <f:selectItem id="viewCaseFile" itemValue="viewCaseFile"/>
  <f:selectItem id="editCaseFile" itemValue="editCaseFile"/>
  <f:selectItem id="viewDocumentMetaData" itemValue="viewDocumentMetaData"/>
  <f:selectItem id="viewDocumentFiles" itemValue="viewDocumentFiles"/>
  <f:selectItem id="editDocument" itemValue="editDocument"/>
</h:selectManyListbox>
<f:verbatim><br/></f:verbatim>

<h:outputText value="numberOfDocumentsProceduresInSingleTransaction [*]: "/>
<h:inputText value="#{goproImporter.data.batchSize}" size="60" />
<f:verbatim><br/></f:verbatim>

<h:messages />
<f:verbatim><br/></f:verbatim>
<h:outputText escape="false" value="#{goproImporter.status}" />
<f:verbatim><br/><br/></f:verbatim>

   <h:commandButton value="Käivita GoPro andmete import" type="submit"
      actionListener="#{goproImporter.startImporterInBackground}"
      rendered="#{!goproImporter.importerRunning}" />
   <h:commandButton value="Peata GoPro andmete import" type="submit"
      actionListener="#{goproImporter.stopImporter}"
      rendered="#{goproImporter.importerRunning}"
      disabled="#{goproImporter.importerStopping}" />


<f:verbatim><br/></f:verbatim>
<f:verbatim><br/></f:verbatim>
<f:verbatim><br/></f:verbatim>

<h:outputText value="Maintenance tasks for data" style="font-weight: bold;" />

<f:verbatim><br/><br/></f:verbatim>
<h:commandButton id="deleteDrafts" value="deleteDrafts" type="submit" actionListener="#{deleteDraftsBootstrap.execute}"/>  

<f:verbatim><br/><br/></f:verbatim>
<h:commandButton id="findAndFixInvalidNodes" value="findAndFixInvalidNodes" type="submit" actionListener="#{invalidNodeFixerBootstrap.execute}"/>  


<f:verbatim><br/><br/><u></f:verbatim>
<h:outputText value="Nightly 02:30 data maintenance job (runs only on primary cluster node, aka where jobs.enabled=true): "/>
<f:verbatim></u><br/></f:verbatim>
<h:outputText value="1) deleteDrafts"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="2) findAndFixInvalidNodes"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="runNightly0230DataMaintenanceJobNow" value="runNightly0230DataMaintenanceJobNow" type="submit" actionListener="#{TestingForDeveloperBean.runNightly0230DataMaintenanceJobNow}" />

<f:verbatim><br/><br/></f:verbatim>
<f:verbatim><hr/></f:verbatim>

<f:verbatim><br/></f:verbatim>
<h:outputText value="Maintenance tasks for Lucene indexes" style="font-weight: bold;" />

<f:verbatim><br/><br/></f:verbatim>
   <h:dataTable value="#{TestingForDeveloperBean.storeRefs}" var="row" rowClasses="selectedItemsRow,selectedItemsRowAlt" headerClass="selectedItemsHeader">
      <h:column>
         <h:outputText value="#{row}" />
      </h:column>
      <h:column>
         <h:commandButton id="checkIndexIntegrity" value="checkIndexIntegrity" type="submit" actionListener="#{indexIntegrityCheckerBootstrap.execute}">
           <f:param name="storeRef" value="#{row}" />
         </h:commandButton>
         <f:verbatim>&nbsp;</f:verbatim>
         <h:commandButton id="checkIndexIntegrityAndReindex" value="checkIndexIntegrityAndReindex" type="submit" actionListener="#{indexIntegrityCheckerBootstrap.executeWithReindex}">
           <f:param name="storeRef" value="#{row}" />
         </h:commandButton>
         <f:verbatim>&nbsp;</f:verbatim>
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
<h:commandButton id="checkIndexIntegrityOnAllIndexes" value="checkIndexIntegrityOnAllIndexes" type="submit" actionListener="#{indexIntegrityCheckerBootstrap.execute}"/>  
<f:verbatim><br/></f:verbatim>
<h:commandButton id="checkIndexIntegrityOnAllIndexesAndReindex" value="checkIndexIntegrityOnAllIndexesAndReindex" type="submit" actionListener="#{indexIntegrityCheckerBootstrap.executeWithReindex}"/>  

<f:verbatim><br/><br/></f:verbatim>
<h:outputText value="lookBackMinutes: "/>
<h:inputText value="#{customReindexComponent.lookBackMinutes}" size="6" converter="javax.faces.Integer" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="changeTxnIds (mitu eralda tühikuga; kui see on täidetud, siis lookBackMinutes ei arvestata): "/>
<h:inputText value="#{customReindexComponent.customChangeTxnIds}" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="searchHolesOnAllIndexesAndReindex" value="searchHolesOnAllIndexesAndReindex" type="submit" actionListener="#{TestingForDeveloperBean.searchHolesAndIndex}" />

<f:verbatim><br/><br/><u></f:verbatim>
<h:outputText value="Nightly 03:00 index maintenance job (runs on all cluster nodes): "/>
<f:verbatim></u><br/></f:verbatim>
<h:outputText value="1) searchHoldesOnAllIndexesAndReindex (lookBackMinutes=1500, aka 25 hours)"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="2) checkIndexIntegrityOnAllIndexesAndReindex"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="3) runMergeOnAllIndexes"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="4) performIndexBackupOnAllIndexes (copies clean state into backup-lucene-indexes folder)"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="5) checkIndexIntegrityOnAllIndexes"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="runNightly0300IndexMaintenanceJobNow" value="runNightly0300IndexMaintenanceJobNow" type="submit" actionListener="#{TestingForDeveloperBean.runNightly0300IndexMaintenanceJobNow}" />

<f:verbatim><br/><br/></f:verbatim>
<h:inputTextarea id="indexInfo" value="#{TestingForDeveloperBean.indexInfoText}" readonly="true" styleClass="expand19-200" style="font-family: monospace;" />

<f:verbatim><hr/></f:verbatim>
<f:verbatim><br/><br/></f:verbatim>
<h:outputText value="Puuduva ownerId väljaga dokumendid:"/>
<f:verbatim><br /></f:verbatim>
<h:commandButton id="searchMissingOwnerId" value="searchMissingOwnerId" type="submit" actionListener="#{TestingForDeveloperBean.searchMissingOwnerId}" />
<f:verbatim><br /><br /></f:verbatim>
<h:inputTextarea id="missingOwnerIdArea" value="#{TestingForDeveloperBean.missingOwnerId}" readonly="true" styleClass="expand19-200" style="font-family: monospace;" />

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Sarja organizatsiooni ja seotud grippide uuendamine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="* Ruum: "/>
<h:inputText id="seriesStructUnitUpdaterStoreString" value="#{seriesStructUnitUpdater.storeString}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* DataFolder: "/>
<h:inputText id="seriesStructUnitUpdaterDataFolder" value="#{seriesStructUnitUpdater.dataFolder}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* MappingsFile: "/>
<h:inputText id="seriesStructUnitUpdaterStructUnitMappingFileName" value="#{seriesStructUnitUpdater.mappingFileName}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* GroupsMappingsFile: "/>
<h:inputText id="seriesStructUnitUpdaterGroupsMappingFileName" value="#{seriesStructUnitUpdater.groupsMappingFileName}" size="50" />
<f:verbatim><br/></f:verbatim>

<h:commandButton id="startSeriesStructUnitUpdater" value="Käivita" type="submit"
   actionListener="#{seriesStructUnitUpdater.executeUpdaterInBackground}"
   rendered="#{seriesStructUnitUpdater.updaterRunning == false}" />
<h:commandButton id="stopSeriesStructUnitUpdater" value="Peata" type="submit"
   actionListener="#{seriesStructUnitUpdater.stopUpdater}"
   rendered="#{seriesStructUnitUpdater.updaterRunning == true}"
   disabled="#{seriesStructUnitUpdater.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Nodeide õiguste importimine (vana AD)"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="* Ruum: "/>
<h:inputText id="nodePermissionsImporterStoreString" value="#{nodePermissionsImporter.storeString}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* DataFolder: "/>
<h:inputText id="nodePermissionsImporterDataFolder" value="#{nodePermissionsImporter.dataFolder}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* MappingsFile: "/>
<h:inputText id="nodePermissionsImporterMappingFileName" value="#{nodePermissionsImporter.mappingFileName}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startNodePermissionsImporter" value="Käivita" type="submit"
   actionListener="#{nodePermissionsImporter.executeUpdaterInBackground}"
   rendered="#{nodePermissionsImporter.updaterRunning == false}" />
<h:commandButton id="stopNodePermissionsImporter" value="Peata" type="submit"
   actionListener="#{nodePermissionsImporter.stopUpdater}"
   rendered="#{nodePermissionsImporter.updaterRunning == true}"
   disabled="#{nodePermissionsImporter.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>

<f:verbatim><hr/></f:verbatim>
<h:outputText value="Nodeide õiguste exportimine (uus AD)"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="*Ruum: "/>
<h:inputText id="nodePermissionsExporterStoreString" value="#{nodePermissionsExporter.storeString}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* DataFolder: "/>
<h:inputText id="nodePermissionsExporterDataFolder" value="#{nodePermissionsExporter.dataFolder}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="* MappingsFile: "/>
<h:inputText id="nodePermissionsExporterMappingFileName" value="#{nodePermissionsExporter.mappingFileName}" size="50" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startNodePermissionsExporter" value="Käivita" type="submit"
   actionListener="#{nodePermissionsExporter.executeUpdaterInBackground}"
   rendered="#{nodePermissionsExporter.updaterRunning == false}" />
<h:commandButton id="stopNodePermissionsExporter" value="Peata" type="submit"
   actionListener="#{nodePermissionsExporter.stopUpdater}"
   rendered="#{nodePermissionsExporter.updaterRunning == true}"
   disabled="#{nodePermissionsExporter.updaterStopping == true}" />
<f:verbatim><br/></f:verbatim>
<f:verbatim><hr/></f:verbatim>
<h:outputText value="Hävitamisakti genereerimine"/>
<f:verbatim><br/></f:verbatim>
<h:outputText value="* CSV faili asukoht serveri kõvakettal: "/>
<h:inputText id="nodeRefsFileGenerationFileName" value="#{TestingForDeveloperBean.dispositionFileName}" size="100" />
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startNodeRefsFileGeneration" value="Genereeri" type="submit"
                 actionListener="#{TestingForDeveloperBean.generateDispositionReport}" />
<f:verbatim><hr/></f:verbatim>

<h:outputText value="\"Märgitud hävitamiseks\" = \"Jah\" (markedForDestruction = true) checkboxi eemaldamine toimikutel, mis ei ole lisatud hävitamise järjekorda"/>
<f:verbatim><br/></f:verbatim>
<h:commandButton id="startResetMarkedForDestruction" value="Käivita" type="submit"
                 actionListener="#{TestingForDeveloperBean.resetMarkedForDestruction}" />
<f:verbatim><hr/></f:verbatim>

<h:outputText styleClass="mainTitle" value="Arendajale testimiseks"/>
<f:verbatim><br/></f:verbatim>

<a:actionLink value="Lisa sessiooni mitteserialiseeruv objekt" actionListener="#{TestingForDeveloperBean.addNonSerializableObjectToSession}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>

<a:actionLink value="executeCacheStatistics" actionListener="#{TestingForDeveloperBean.executeCacheStatistics}" />

<f:verbatim><br/></f:verbatim>

<a:actionLink value="TestingForDeveloper" actionListener="#{TestingForDeveloperBean.handleTestEvent}" rendered="#{ApplicationService.test}">
     <f:param name="testP" value="11" />
</a:actionLink>

<f:verbatim><br/></f:verbatim>
<a:actionLink value="executeDocumentUpdater" actionListener="#{TestingForDeveloperBean.executeDocumentUpdater}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>
<a:actionLink value="executeSearchableSendInfoUpdater" actionListener="#{TestingForDeveloperBean.executeSearchableSendInfoUpdater}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>
<a:actionLink value="executeCompoundWorkflowOwnerPropsUpdater" actionListener="#{TestingForDeveloperBean.executeCompoundWorkflowOwnerPropsUpdater}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>
<a:actionLink value="executeTaskUpdater" actionListener="#{TestingForDeveloperBean.executeTaskUpdater}" rendered="#{ApplicationService.test}" />

<f:verbatim><br/></f:verbatim>

<a:actionLink value="deleteTestSystemTemplatesBootstrapAndSystemTemplates" actionListener="#{TestingForDeveloperBean.deleteTestTemplatesBootstrapAndTemplates}" rendered="#{ApplicationService.test}" />
<f:verbatim><br/></f:verbatim>

<a:actionLink id="runADMLuceneTestTestMaskDeletes" value="ADMLuceneTest.testMaskDeletes" actionListener="#{TestingForDeveloperBean.runADMLuceneTestTestMaskDeletes}" />

<%-- <f:verbatim><br/></f:verbatim> --%>

 <%-- FIXME DLSeadist ajutine link test keskkonna jaoks --%>
<%-- <a:actionLink value="DeleteFieldAndFieldGroupsAndBootstrapInfo" actionListener="#{TestingForDeveloperBean.deleteFieldAndFieldGroupsAndBootstrapInfo}" /> --%>
