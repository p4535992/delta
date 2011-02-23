<%
if (request.getMethod().equalsIgnoreCase("GET"))
{
      // Send redirect
      response.sendRedirect(request.getContextPath() + "/faces/jsp/dashboards/container.jsp");
}
// route WebDAV requests
else if (request.getMethod().equalsIgnoreCase("PROPFIND") ||
         request.getMethod().equalsIgnoreCase("OPTIONS"))
{
   response.sendRedirect(request.getContextPath() + "/webdav/");
}
%>