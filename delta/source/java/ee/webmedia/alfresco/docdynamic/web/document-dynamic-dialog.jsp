<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%--
<h:outputText value="node.type.localName=#{DocumentDynamicDialog.node.type.localName}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="documentTypeId=#{DocumentDynamicDialog.document.documentTypeId}" />
<f:verbatim><br/></f:verbatim>
<h:outputText value="documentTypeVersionNr=#{DocumentDynamicDialog.document.documentTypeVersionNr}" />
<f:verbatim><br/></f:verbatim>
--%>

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <%--TODO DLSeadist <r:permissionEvaluator value="#{DialogManager.bean.meta.document}" allow="editDocumentMetaData">--%>
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DocumentDynamicDialog.switchToEditMode}" rendered="#{!DocumentDynamicDialog.inEditMode}" />
      <%--TODO DLSeadist </r:permissionEvaluator>--%>
   </f:facet>
</h:panelGroup>

<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}" styleClass="panel-100 #{(DocumentDynamicDialog.inEditMode) ? 'edit-mode' : ''}" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{DocumentDynamicDialog.propertySheet}" value="#{DocumentDynamicDialog.node}" columns="1"
      mode="#{DocumentDynamicDialog.mode}" config="#{DocumentDynamicDialog.propertySheetConfigElement}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<%--
<a:panel id="test-panel" label="Test" styleClass="panel-100" progressive="true">
   <h:commandButton id="testButton1" value="Ei tee midagi" type="submit" actionListener="#{DocumentDynamicDialog.doNothing}" />
</a:panel>
--%>
<%--<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docdynamic/web/document-dynamic-test.jsp" />--%>

<a:booleanEvaluator value="#{!DocumentDynamicDialog.inEditMode}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/disable-dialog-finish-button.jsp" />
</a:booleanEvaluator>
