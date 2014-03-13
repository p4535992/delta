<%
if (request.getMethod().equalsIgnoreCase("GET"))
{
   String destination = "/faces/jsp/dashboards/container.jsp";

   org.springframework.mobile.device.Device device = (org.springframework.mobile.device.Device) request.getAttribute(org.springframework.mobile.device.DeviceUtils.CURRENT_DEVICE_ATTRIBUTE);
   Boolean isMobile = device != null && (device.isTablet() || device.isMobile());
   Boolean override = session != null && Boolean.TRUE.equals(session.getAttribute(ee.webmedia.alfresco.app.AppConstants.DEVICE_DETECTION_OVERRIDE));

   // Check if user wants to toggle and update session
   String query = request.getQueryString();
   if (query != null && query.contains(ee.webmedia.alfresco.app.AppConstants.DEVICE_DETECTION_OVERRIDE)) {
       override = !override;
       session.setAttribute(ee.webmedia.alfresco.app.AppConstants.DEVICE_DETECTION_OVERRIDE, override);
       query = query.replaceFirst(ee.webmedia.alfresco.app.AppConstants.DEVICE_DETECTION_OVERRIDE, "");
   }

   if (isMobile && !override || !isMobile && override) {
       destination = "/m/";
   }
   // Send redirect
   response.sendRedirect(request.getContextPath() + destination + (query == null || query.length() < 1 ? "" : "?" + query));
}
// route WebDAV requests
else if (request.getMethod().equalsIgnoreCase("PROPFIND") ||
         request.getMethod().equalsIgnoreCase("OPTIONS"))
{
   response.sendRedirect(request.getContextPath() + "/webdav/");
}
%>