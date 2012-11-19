<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="user-details-panel" styleClass="panel-100" label="#{msg.user_details}" progressive="true">

   <r:propertySheetGrid labelStyleClass="propertiesLabel" columns="1" mode="view" value="#{UserDetailsDialog.user}" externalConfig="true" />

</a:panel>

<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/substitute/web/substitute-list-dialog.jsp" />

<h:outputText value="#{AssignResponsibilityBean.setFromOwnerUserDetails}" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/assignresponsibility/web/assign-responsibility.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/user/web/users-groups.jsp" />
<f:verbatim><div class="clear"></div></f:verbatim>
