<%
if (request.getMethod().equalsIgnoreCase("GET"))
{
      // Send redirect
      String params = request.getQueryString();
      response.sendRedirect(request.getContextPath() + "/faces/jsp/dashboards/container.jsp" + (params==null?"":"?"+params));
}
// route WebDAV requests
else if (request.getMethod().equalsIgnoreCase("PROPFIND") ||
         request.getMethod().equalsIgnoreCase("OPTIONS"))
{
   response.sendRedirect(request.getContextPath() + "/webdav/");
}
%>