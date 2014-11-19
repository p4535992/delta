<<<<<<< HEAD
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@page import="javax.faces.context.FacesContext"%>
<div id="footer">
   <p><h:outputText value="#{ApplicationService.footerText}" escape="false" /></p>
      <h:outputText value="#{ApplicationService.projectVersion}" />
      <%= FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("host") %>
</div>
=======
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@page import="javax.faces.context.FacesContext"%>
<div id="footer">
   <p><h:outputText value="#{ApplicationService.footerText}" escape="false" /></p>
      <h:outputText value="#{ApplicationService.projectVersion}" />
      <%= FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("host") %>
</div>
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
