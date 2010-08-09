<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<%@page import="ee.webmedia.alfresco.utils.MessageUtil"%>
<%@page import="javax.faces.context.FacesContext"%>

<%@page import="org.alfresco.web.app.Application"%><f:verbatim>
   <script type="text/javascript">
      addTranslation("jQuery.condence.moreText", "<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("jQuery.condence.moreText")%>");
      addTranslation("jQuery.condence.lessText", "<%=(Application.getBundle(FacesContext.getCurrentInstance())).getString("jQuery.condence.lessText")%>");
</script>
</f:verbatim>