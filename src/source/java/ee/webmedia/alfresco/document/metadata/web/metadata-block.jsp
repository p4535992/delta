<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.meta.document}" allow="WriteProperties">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DialogManager.bean.meta.edit}" rendered="#{!DialogManager.bean.meta.inEditMode && DialogManager.bean.meta.editAllowed}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}" styleClass="panel-100" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{DialogManager.bean.meta.propertySheet}" value="#{DialogManager.bean.meta.document}" columns="1"
      mode="#{DialogManager.bean.meta.mode}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<a:booleanEvaluator value="#{!DialogManager.bean.meta.inEditMode}">
   <f:verbatim>
      <script type="text/javascript">
         $jQ(document).ready(function () {
            var finishButton = $jQ('#' + escapeId4JQ('dialog:finish-button'));
            finishButton.next().remove();
            finishButton.remove();
         });
      </script>
   </f:verbatim>
</a:booleanEvaluator>
