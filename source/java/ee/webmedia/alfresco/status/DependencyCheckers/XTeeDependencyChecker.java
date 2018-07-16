package ee.webmedia.alfresco.status.DependencyCheckers;

import java.io.*;
import java.net.*;


/**
 * 
 * @author viljar.tina
 *
 */
public class XTeeDependencyChecker extends DependencyChecker{

	/**
	 * 
	 */
	private String Producer;
	
	
	/**
	 * 
	 */
	private String XTeeSecurityServer;
	
	
	
	
	/**
	 * 
	 */
	public XTeeDependencyChecker( String name, String uri, String producer, Boolean isFatal ) {
		super(name, uri, isFatal);
		
		// ..	
		this.Producer = producer;
		try{
			URL url = new URL(this.Uri);
			this.XTeeSecurityServer = url.toString();
		}catch(Exception ex){}
		
		// pathi's peab olema ka producer ..
		this.Uri = String.format("%s@%s", this.Producer, this.XTeeSecurityServer);
	}
	
	public Boolean testDHXConnection(){
		try {
			URL v6Url = new URL(this.XTeeSecurityServer);
			URL DHXUrl = new URL(String.format("%s://%s/dhx-adapter-server/health", v6Url.getProtocol(), v6Url.getAuthority()));
			HttpURLConnection conn = (HttpURLConnection) DHXUrl.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
			String line = rd.readLine();
			Integer statusCode = conn.getResponseCode(); 
			String statusMesg = conn.getResponseMessage();
			
			rd.close();
			
			Status = STATUS_NOK;
			if(statusCode.equals(200)){
				Status = STATUS_OK;
				StatusMsg = "Saatmisviis: DHX " + this.XTeeSecurityServer;
			    return true;
			}
									
			// ..
			StatusMsg = statusCode + " " + statusMesg;
		    return false;
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			
			// wsdl url ..
			URL url = new URL(this.XTeeSecurityServer);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
			String line = rd.readLine();
			Integer statusCode = conn.getResponseCode(); 
			String statusMesg = conn.getResponseMessage();

			
			rd.close();
			
			Status = STATUS_NOK;
			if(statusCode.equals(200)){
				Status = STATUS_OK;
				StatusMsg = "Saatmisviis: V6 " + this.XTeeSecurityServer;
			    return true;
			}
									
			// ..
			StatusMsg = statusCode + " " + statusMesg;
		    return testDHXConnection();
		}catch(Exception ex){	
			
			StatusMsg = ex.getMessage();
				
			}
			Status = STATUS_NOK;
		    return testDHXConnection();
		}
    }