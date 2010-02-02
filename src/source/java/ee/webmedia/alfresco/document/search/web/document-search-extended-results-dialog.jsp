<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%-- This JSP can be used from multiple dialogs, that's why DialogManager.bean reference is used --%>

<a:panel id="document-panel" styleClass="panel-100" label="#{DialogManager.bean.listTitle}" progressive="true">
   
   <a:panel id="document-panel-search-results" styleClass="overflow-wrapper">   
   <%-- Main List --%>
   <a:richList id="documentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      value="#{DialogManager.bean.documents}" var="r">
      
      <%-- Registration date --%>
      <a:column id="col1" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.document_regDateTime}" value="regDateTime" styleClass="header" />
         </f:facet>
         <h:outputText id="col1-text" value="#{r.regDateTime}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Registration number --%>
      <a:column id="col2" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.document_regNumber}" value="regNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col2-text" value="#{r.regNumber}" />
      </a:column>

      <%-- Document type --%>
      <a:column id="col3" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col3-sort" label="#{msg.document_docType}" value="documentTypeName" styleClass="header" />
         </f:facet>
         <h:outputText id="col3-text" value="#{r.documentTypeName}" />
      </a:column>

      <%-- Document status --%>
      <a:column id="col4" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col4-sort" label="#{msg.document_docStatus}" value="docStatus" styleClass="header" />
         </f:facet>
         <h:outputText id="col4-text" value="#{r.docStatus}" />
      </a:column>

      <%-- All Recipients --%>
      <a:column id="col5" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col5-sort" label="#{msg.document_allRecipients}" value="allRecipients" styleClass="header" />
         </f:facet>
         <h:outputText id="col5-text" value="#{r.allRecipients}" />
      </a:column>

      <%-- Title --%>
      <a:column id="col6" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col6-sort" label="#{msg.document_docName}" value="docName" styleClass="header" />
         </f:facet>
         <a:actionLink id="col6-text" value="#{r.docName}" action="dialog:document" tooltip="#{msg.document_details_info}"
            showLink="false" actionListener="#{DocumentDialog.open}" >
            <f:param name="nodeRef" value="#{r.node.nodeRef}" />
         </a:actionLink>
      </a:column>

      <%-- Sender registration date --%>
      <a:column id="col7" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col7-sort" label="#{msg.document_senderRegDate}" value="senderRegDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col7-text" value="#{r.senderRegDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Sender registration number --%>
      <a:column id="col8" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col8-sort" label="#{msg.document_senderRegNumber}" value="senderRegNumber" styleClass="header" />
         </f:facet>
         <h:outputText id="col8-text" value="#{r.senderRegNumber}" />
      </a:column>

      <%-- Due date 2 --%>
      <a:column id="col9" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col9-sort" label="#{msg.document_dueDate2}" value="dueDate2" styleClass="header" />
         </f:facet>
         <h:outputText id="col9-text" value="#{r.dueDate2}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Complience Date --%>
      <a:column id="col10" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col10-sort" label="#{msg.document_complienceDate3}" value="complienceDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col10-text" value="#{r.complienceDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Access restriction --%>
      <a:column id="col11" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col11-sort" label="#{msg.document_accessRestriction}" value="accessRestriction" styleClass="header" />
         </f:facet>
         <h:outputText id="col11-text" value="#{r.accessRestriction}" />
      </a:column>

      <%-- Access restriction reason --%>
      <a:column id="col12" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col12-sort" label="#{msg.document_accessRestrictionReason}" value="accessRestrictionReason" styleClass="header" />
         </f:facet>
         <h:outputText id="col12-text" value="#{r.accessRestrictionReason}" />
      </a:column>

      <%-- Access restriction begin date --%>
      <a:column id="col13" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col13-sort" label="#{msg.document_accessRestrictionBeginDate}" value="accessRestrictionBeginDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col13-text" value="#{r.accessRestrictionBeginDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Access restriction end date --%>
      <a:column id="col14" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col14-sort" label="#{msg.document_accessRestrictionEndDate}" value="accessRestrictionEndDate" styleClass="header" />
         </f:facet>
         <h:outputText id="col14-text" value="#{r.accessRestrictionEndDate}" >
            <a:convertXMLDate pattern="#{msg.date_pattern}" />
         </h:outputText>
      </a:column>

      <%-- Access restriction end description --%>
      <a:column id="col15" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col15-sort" label="#{msg.document_accessRestrictionEndDesc}" value="accessRestrictionEndDesc" styleClass="header" />
         </f:facet>
         <h:outputText id="col15-text" value="#{r.accessRestrictionEndDesc}" />
      </a:column>

      <%-- Owner name --%>
      <a:column id="col16" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col16-sort" label="#{msg.document_owner2}" value="" styleClass="header" />
         </f:facet>
         <h:outputText id="col16-text" value="#{r.ownerName}" />
      </a:column>

      <%-- OwnerOrgStructUnit --%>
      <a:column id="col17" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col17-sort" label="#{msg.document_ownerOrgStructUnit}" value="ownerOrgStructUnit" styleClass="header" />
         </f:facet>
         <h:outputText id="col17-text" value="#{r.ownerOrgStructUnit}" />
      </a:column>

      <%-- OwnerJobTitle --%>
      <a:column id="col18" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col18-sort" label="#{msg.document_ownerJobTitle}" value="ownerJobTitle" styleClass="header" />
         </f:facet>
         <h:outputText id="col18-text" value="#{r.ownerJobTitle}" />
      </a:column>

      <%-- SignerName --%>
      <a:column id="col19" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col19-sort" label="#{msg.document_signer}" value="signerName" styleClass="header" />
         </f:facet>
         <h:outputText id="col19-text" value="#{r.signerName}" />
      </a:column>

      <%-- SignerJobTitle --%>
      <a:column id="col20" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col20-sort" label="#{msg.document_signerJobTitle}" value="signerJobTitle" styleClass="header" />
         </f:facet>
         <h:outputText id="col20-text" value="#{r.signerJobTitle}" />
      </a:column>

      <%-- Keywords --%>
      <a:column id="col21" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col21-sort" label="#{msg.document_keywords}" value="keywords" styleClass="header" />
         </f:facet>
         <h:outputText id="col21-text" value="#{r.keywords}" />
      </a:column>

      <%-- StorageType --%>
      <a:column id="col22" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col22-sort" label="#{msg.document_storageType}" value="storageType" styleClass="header" />
         </f:facet>
         <h:outputText id="col22-text" value="#{r.storageType}" />
      </a:column>

      <%-- col23 Saatmisviis – dokumendi Saatmine blokis oleva kirje sendMode --%>

      <%-- CostManager --%>
      <a:column id="col24" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col24-sort" label="#{msg.document_costManager}" value="costManager" styleClass="header" />
         </f:facet>
         <h:outputText id="col24-text" value="#{r.costManager}" />
      </a:column>

      <%--
         col25 Taotleja – errandOrderAbroad, errandApplicationDomestic, trainingApplication ja tenderingApplication tüüpi dokumendi applicantName; tühi ülejäänud dokumendi liikide puhul
         
         col26 Lähetuse alguskuupäev – errandOrderAbroad ja errandApplicationDomestic tüüpi dokumendi travelBeginDate; tühi ülejäänud dokumendi liikide puhul
         
         col27 Lähetuse lõppkuupäev - errandOrderAbroad ja errandApplicationDomestic tüüpi dokumendi travelEndDate; tühi ülejäänud dokumendi liikide puhul
         
         col28 Lähetuse riik – errandOrderAbroad tüüpi dokumendi country; tühi ülejäänud dokumendi liikide puhul
         
         col29 Lähetuse maakond – errandApplicationDomestic tüüpi dokumendi county; tühi ülejäänud dokumendi liikide puhul
         
         col30 Lähetuse linnad  - errandOrderAbroad ja errandApplicationDomestic tüüpi dokumendi city; tühi ülejäänud dokumendi liikide puhul
      --%>

      <%-- ResponsibleName --%>
      <a:column id="col31" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col31-sort" label="#{msg.document_responsibleName}" value="responsibleName" styleClass="header" />
         </f:facet>
         <h:outputText id="col31-text" value="#{r.responsibleName}" />
      </a:column>

      <%-- CoResponsibles --%>
      <a:column id="col32" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col32-sort" label="#{msg.document_coResponsibles}" value="coResponsibles" styleClass="header" />
         </f:facet>
         <h:outputText id="col32-text" value="#{r.coResponsibles}" />
      </a:column>

      <%-- ContactPerson --%>
      <a:column id="col33" primary="true" styleClass="#{r.docTypeLocalName}" >
         <f:facet name="header">
            <a:sortLink id="col33-sort" label="#{msg.document_contactPerson}" value="contactPerson" styleClass="header" />
         </f:facet>
         <h:outputText id="col33-text" value="#{r.contactPerson}" />
      </a:column>

      <%-- col34 Hankemenetluse liik – tenderingApplication tüüpi dokumendi procurementType; tühi ülejäänud dokumendi liikide puhul --%>

      <%-- Files --%>
      <a:column id="col35" primary="true">
         <f:facet name="header">
            <h:outputText id="col35-header" value="#{msg.document_allFiles}" styleClass="header" />
         </f:facet>
         <r:permissionEvaluator value="#{r.files[0].node}" allow="ReadContent">
            <a:actionLink id="col35-act1" value="#{r.files[0].name}" href="#{r.files[0].downloadUrl}" target="new" showLink="false"
               image="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[0].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[0].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[0].name}"
               title="#{r.files[0].name}" rendered="#{r.files[0] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" allow="ReadContent">
            <a:actionLink id="col35-act2" value="#{r.files[1].name}" href="#{r.files[1].downloadUrl}" target="new" showLink="false"
               image="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[1].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[1].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[1].name}"
               title="#{r.files[1].name}" rendered="#{r.files[1] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" allow="ReadContent">
            <a:actionLink id="col35-act3" value="#{r.files[2].name}" href="#{r.files[2].downloadUrl}" target="new" showLink="false"
               image="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[2].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[2].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[2].name}"
               title="#{r.files[2].name}" rendered="#{r.files[2] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" allow="ReadContent">
            <a:actionLink id="col35-act4" value="#{r.files[3].name}" href="#{r.files[3].downloadUrl}" target="new" showLink="false"
               image="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[3].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[3].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[3].name}"
               title="#{r.files[3].name}" rendered="#{r.files[3] != null}" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" allow="ReadContent">
            <a:actionLink id="col35-act5" value="#{r.files[4].name}" href="#{r.files[4].downloadUrl}" target="new" showLink="false"
               image="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" styleClass="inlineAction webdav-readOnly" />
         </r:permissionEvaluator>
         <r:permissionEvaluator value="#{r.files[4].node}" deny="ReadContent">
            <h:graphicImage value="/images/icons/#{r.files[4].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}" alt="#{r.files[4].name}"
               title="#{r.files[4].name}" rendered="#{r.files[4] != null}" />
         </r:permissionEvaluator>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />
   </a:richList>

   </a:panel>
</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
