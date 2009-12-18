<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="user-details-panel" styleClass="panel-100" label="#{msg.user_details}" progressive="true">

   <r:propertySheetGrid labelStyleClass="propertiesLabel" columns="1" mode="view" value="#{UserDetailsDialog.user}" externalConfig="true" />

</a:panel>

<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var finishButton = $jQ('#' + escapeId4JQ('dialog:finish-button'));
         finishButton.next().remove();
         finishButton.remove();
      });
   </script>
</f:verbatim>
