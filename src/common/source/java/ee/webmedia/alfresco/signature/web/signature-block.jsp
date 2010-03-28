<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.ddoc_container_documents}" id="data-items-panel" progressive="true" rendered="#{SignatureBlockBean.dataItemsRendered}">

   <a:richList id="data-items-list" viewMode="details" value="#{SignatureBlockBean.dataItems}" var="dataFile" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="name" initialSortDescending="false">

      <a:column id="filenameCol" style="text-align:left">
         <f:facet name="header">
            <a:sortLink id="sortLinkFilename" label="#{msg.ddoc_datafile_name}" value="filename" styleClass="header" />
         </f:facet>
         <a:actionLink id="downloadLink" value="#{dataFile.name}" href="#{dataFile.downloadUrl}" target="new" verticalAlign="middle" />
      </a:column>

      <a:column id="sizeCol" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink id="sortLinkSize" label="#{msg.ddoc_datafile_size}" value="filename" styleClass="header" />
         </f:facet>
         <h:outputText id="size" value="#{dataFile.size}">
            <f:converter converterId="org.alfresco.faces.ByteSizeConverter" />
         </h:outputText>
      </a:column>
   </a:richList>
</a:panel>

<a:panel label="#{msg.ddoc_existing_signatures}" id="signature-items-panel" progressive="true" rendered="#{SignatureBlockBean.signatureItemsRendered}">

   <a:richList id="signature-items-list" viewMode="details" value="#{SignatureBlockBean.signatureItems}" var="item" rowStyleClass="recordSetRow"
      altRowStyleClass="recordSetRowAlt" width="100%" initialSortColumn="signingTime" initialSortDescending="false">

      <a:column id="signerName" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_name}" value="name" styleClass="header" />
         </f:facet>
         <h:outputText id="itemName" value="#{item.name}" />
      </a:column>

      <a:column id="legalCode" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_idcode}" value="legalCode" styleClass="header" />
         </f:facet>
         <h:outputText id="itemLegalCode" value="#{item.legalCode}" />
      </a:column>

      <a:column id="claimedRoles" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_role}" value="role" styleClass="header" />
         </f:facet>
         <h:outputText id="itemRole" value="#{item.role}" />
      </a:column>

      <a:column id="address" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_address}" value="address" styleClass="header" />
         </f:facet>
         <h:outputText id="itemAddress" value="#{item.address}" />
      </a:column>

      <a:column id="signitionTime" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_date}" value="signingTime" styleClass="header" />
         </f:facet>
         <h:outputText id="itemDate" value="#{item.signingTime}">
            <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
         </h:outputText>
      </a:column>

      <a:column id="signatureStatus" width="120" style="text-align:left">
         <f:facet name="header">
            <a:sortLink label="#{msg.ddoc_signature_status}" value="status" styleClass="header" />
         </f:facet>
            <h:outputText id="itemValid" value="#{msg.ddoc_signature_valid}" style="color: green;" rendered="#{item.valid}" />
            <h:outputText id="itemNotValid" value="#{msg.ddoc_signature_invalid}" style="color: red;" rendered="#{item.notValid}" />
      </a:column>

   </a:richList>
</a:panel>
