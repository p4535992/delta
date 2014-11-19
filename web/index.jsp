<<<<<<< HEAD
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
%>