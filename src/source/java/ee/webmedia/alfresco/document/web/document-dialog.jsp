<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="org.alfresco.web.app.Application"%>
<%@page import="javax.faces.context.FacesContext"%>
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/metadata/web/metadata-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/metadata/web/metadata-addOrSelectCase.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />
<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/sendout/web/sendOut-block.jsp" />
<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function(){
        var finishButton = $jQ('#' + escapeId4JQ('dialog:finish-button'));
        var registerButton = $jQ('#' + escapeId4JQ('dialog:document_register_button'));

        registerButton.click(function() {
            if(finishButton.attr('disabled')) {
                alert("<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("document_mandatory_fields_are_empty")%>");
                return false;
            }
        }); 
         
      });
   </script>
</f:verbatim>
