<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel label="#{msg.document_type_block}" id="types-panel" styleClass="panel-100" progressive="true">
   <h:panelGrid>
      <r:propertySheetGrid id="doc-types" value="#{DocumentDialog.type.selector}" columns="1" mode="edit" externalConfig="true"
         labelStyleClass="propertiesLabel" />
   </h:panelGrid>
</a:panel>
