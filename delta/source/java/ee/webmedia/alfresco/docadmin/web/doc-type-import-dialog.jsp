<%@ page import="ee.webmedia.alfresco.docadmin.web.DynamicTypesImportDialog"%>
<%@ page import="ee.webmedia.alfresco.docadmin.web.DocumentTypesImportDialog"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator"%>
<%@ page import="ee.webmedia.alfresco.document.file.web.AddFileDialog"%>

<% request.setAttribute(DynamicTypesImportDialog.DYNAMIC_TYPES_IMPORT_DIALOG_BEAN_NAME, DocumentTypesImportDialog.BEAN_NAME); %>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/docadmin/web/dyn-type-import-dialog.jsp" />
<% request.removeAttribute(DynamicTypesImportDialog.DYNAMIC_TYPES_IMPORT_DIALOG_BEAN_NAME); %>
