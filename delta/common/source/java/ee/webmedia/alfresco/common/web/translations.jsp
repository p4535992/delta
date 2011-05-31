<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>

<f:verbatim>
<script type="text/javascript">
      addTranslation("jQuery.condence.moreText", '<%= MessageUtil.getMessageAndEscapeJS("jQuery.condence.moreText")%>');
      addTranslation("jQuery.condence.lessText", '<%= MessageUtil.getMessageAndEscapeJS("jQuery.condence.lessText")%>');
      addTranslation("error_endDateBeforeBeginDate", '<%= MessageUtil.getMessageAndEscapeJS("error_endDateBeforeBeginDate")%>');
      addTranslation("webdav_openReadOnly", '<%= MessageUtil.getMessageAndEscapeJS("webdav_openReadOnly")%>');
</script>
</f:verbatim>