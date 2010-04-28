<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:booleanEvaluator id="workflowBlockEvaluator" value="#{DocumentDialog.meta.inEditMode == false}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-block.jsp" />
</a:booleanEvaluator>
<a:booleanEvaluator id="foundSimilarEvaluator" value="#{DocumentDialog.showFoundSimilar}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-similar-block.jsp" />
</a:booleanEvaluator>
<a:booleanEvaluator id="typeBlockEvaluator" value="#{DocumentDialog.showTypeBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/type/web/document-type-block.jsp" />
</a:booleanEvaluator>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/metadata/web/metadata-block.jsp" />
<a:booleanEvaluator id="metadataAddOrSelectCaseEvaluator" value="#{DocumentDialog.meta.inEditMode}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/metadata/web/metadata-addOrSelectCase.jsp" />
</a:booleanEvaluator>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-summary-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/send-out-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/log/web/document-log-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/review-note-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/workflow/web/opinion-note-block.jsp" />
<a:booleanEvaluator id="searchBlockEvaluator" value="#{DocumentDialog.showSearchBlock}">
   <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block.jsp" />
</a:booleanEvaluator>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/associations/web/assocs-block.jsp" />
