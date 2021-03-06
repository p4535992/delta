<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="metadata-panel-facets">
   <f:facet name="title">
      <r:permissionEvaluator value="#{DialogManager.bean.meta.document}" allow="editDocument">
         <a:actionLink id="metadata-link-edit" showLink="false" image="/images/icons/edit_properties.gif" value="#{msg.modify}" tooltip="#{msg.modify}"
            actionListener="#{DialogManager.bean.meta.edit}" action="#{MetadataBlockBean.editAction}" rendered="#{!DialogManager.bean.meta.inEditMode}" />
      </r:permissionEvaluator>
   </f:facet>
</h:panelGroup>

<a:panel id="metadata-panel" facetsId="dialog:dialog-body:metadata-panel-facets" label="#{msg.document_metadata}" styleClass="panel-100 #{(DialogManager.bean.meta.inEditMode) ? 'edit-mode' : ''}" progressive="true">
   <r:propertySheetGrid id="doc-metatada" binding="#{DialogManager.bean.meta.propertySheet}" value="#{DialogManager.bean.meta.document}" columns="1"
      mode="#{DialogManager.bean.meta.mode}" externalConfig="true" labelStyleClass="propertiesLabel" />
</a:panel>

<f:verbatim>
<script type="text/javascript">
   function postProcessButtonState(){
      var registerBtn = $jQ("#" + escapeId4JQ("dialog:documentRegisterButton"));
      if (registerBtn) {
         var finishBtn = $jQ("#"+escapeId4JQ("dialog:finish-button"));
         var finishDisabled = finishBtn.prop("disabled");
         registerBtn.prop("disabled", finishDisabled);

         var registerBtn2 = $jQ("#" + escapeId4JQ("dialog:documentRegisterButton-2"));
         if (registerBtn2) {
            registerBtn2.prop("disabled", finishDisabled);
         }
      }
   }
</script>
</f:verbatim>
