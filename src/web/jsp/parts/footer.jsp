<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<div id="footer">
   <p><h:outputText value="#{ApplicationService.footerText}" escape="false" /></p>
      <!-- 
      <h:outputText value="#{ApplicationService.projectName} versioon #{ApplicationService.projectVersion}" />
      |
      <h:outputText value="common versioon #{ApplicationService.commonVersion}" />
       -->
</div>
