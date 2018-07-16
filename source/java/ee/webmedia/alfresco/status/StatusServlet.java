package ee.webmedia.alfresco.status;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.alfresco.web.app.servlet.BaseServlet;


import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.nortal.jroad.client.service.XTeeDatabaseService;

//import ee.webmedia.xtee.client.service.XTeeDatabaseService;

import java.util.Properties;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.status.model.Unit;


import ee.webmedia.alfresco.common.web.BeanHelper;
import javax.sql.DataSource;
import ee.webmedia.alfresco.status.DependencyCheckers.*;



import javax.naming.*;
import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * 
 * @author viljar.tina
 *
 */
public class StatusServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    
    
    /**
     * 
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/xml;charset=UTF-8");
        setNoCacheHeaders(response);

        
        Resource resource = new ClassPathResource("alfresco-global.properties");
        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        
        Resource xRoadResource = new ClassPathResource("xroad.properties");
        Properties xRoadProps = PropertiesLoaderUtils.loadProperties(xRoadResource);
 
        String xRoadName = "X-Road";      
        
        // OO ..
        OpenOfficeConnection ooConnection = BeanHelper.getSpringBean(OpenOfficeConnection.class, "openOfficeConnection");
        String ooHost = ("" + props.getProperty("ooo.host")).trim();
        if(ooHost.length() == 0) ooHost = "127.0.0.1";
        String ooPort = ("" + props.getProperty("ooo.port")).trim();
        if(ooPort.length() == 0) ooPort = "8100";
        
        
        
        
        
        
        
        // ..
        ServletContext context = getServletContext();
        
        
 		String serverInfo = context.getServerInfo();
 		String appName = context.getServletContextName(); 
 	    if(appName == null || appName.trim().length() == 0) appName = "DELTA";
 	    String appVersion = BeanHelper.getApplicationService().getProjectVersion();
 	    String javaVersion = Runtime.class.getPackage().getImplementationVersion();
 	    
 	    
        
        
 	   List<DependencyChecker> externalDependencies = new ArrayList<>(Arrays.asList(
 			  new DBDependencyChecker("dataSource", BeanHelper.getDataSource(), true)
      		, new MSODependencyChecker("mso.url", props.getProperty("mso.url"), true)
      		, new CASDependencyChecker("cas.casServerUrl", props.getProperty("cas.casServerUrl"), true)
      		, new LDAPDependencyChecker("ldap"
      				, props.getProperty("ldap.authentication.java.naming.provider.url")
      				, props.getProperty("ldap.synchronization.java.naming.security.principal")
      				, props.getProperty("ldap.synchronization.java.naming.security.credentials"), true)
      		, new XTeeDependencyChecker(
      				"security-server", xRoadProps.getProperty("security-server") , xRoadName, true)
      		, new OODependencyChecker(
      				"OpenOfficeService", String.format("urp://%s:%s", ooHost, ooPort) , ooConnection, true)
      		
 			   
 			  ));
        		
        // ..
		publishSystemStatus(
				appName,
				appVersion,
				serverInfo,
				javaVersion,
				externalDependencies,
				response.getWriter());    		

    }
    
    
    
    
    /**
	 * 
	 * @param appName
	 * @param appVersion
	 * @param serverPlatform
	 * @param javaVersion
	 * @param externalDependencies
	 * @param output
	 */
	private void publishSystemStatus(
			String appName, String appVersion, String serverPlatform, String javaVersion
			, List<DependencyChecker> externalDependencies, PrintWriter output){
		
		
		String[] serverPlatformTMP = serverPlatform.split("/");
		
		
		Boolean fatal = false;
        for (DependencyChecker dependency : externalDependencies) {
        	if(!dependency.Test()){
        		//log.warn(String.format("Dependency '%s' test failed! Msg -> %s ", dependency.name, dependency.StatusMsg ));
        	
        		// ..
        		if(dependency.IsFatal) fatal = true;
        	}
        }
		
		// ..
		output.println(String.format("<app name=\"%s\" version=\"%s\" >", appName, appVersion));
		output.println(String.format("\t<status>%s</status>", fatal ? DependencyChecker.STATUS_NOK : DependencyChecker.STATUS_OK));
		
		output.println(String.format("\t<status_msg/>"));
		output.println(String.format("\t<server_platform version=\"%s\">%s</server_platform>", serverPlatformTMP[1], serverPlatformTMP[0]));
		output.println(String.format("\t<runtime_environment version=\"%s\">JAVA</runtime_environment>", javaVersion));
		output.println(String.format("\t<external_dependencies>"));
		
		for ( DependencyChecker dependency : externalDependencies ) {
			output.println(String.format("\t\t<unit name=\"%s\">", dependency.name ));
			output.println(String.format("\t\t<status>%s</status>", dependency.Status ));
			if(dependency.StatusMsg == null || dependency.StatusMsg.trim().length() == 0)
				output.println("\t\t<status_msg/>");
			else
				output.println(String.format("\t\t<status_msg><![CDATA[%s]]></status_msg>", dependency.StatusMsg ));
			output.println(String.format("\t\t<uri>%s</uri>", dependency.Uri ));
			output.println(String.format("\t\t</unit>" ));
        }
		
		output.println(String.format("\t</external_dependencies>"));
		output.println(String.format("</app>"));
		
	}
	   
    
}
