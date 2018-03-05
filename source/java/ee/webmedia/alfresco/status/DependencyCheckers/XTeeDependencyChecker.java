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
			this.XTeeSecurityServer = String.format("%s://%s", url.getProtocol(), url.getAuthority());
		}catch(Exception ex){}
		
		// pathi's peab olema ka producer ..
		this.Uri = String.format("%s@%s/cgi-bin/consumer_proxy", this.Producer, this.XTeeSecurityServer);
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			
			// wsdl url ..
			URL url = new URL(String.format("%s/cgi-bin/uriproxy?producer=%s",this.XTeeSecurityServer, this.Producer));
			
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
				StatusMsg = this.XTeeSecurityServer;
			    return true;
			}
									
			// ..
			StatusMsg = statusCode + " " + statusMesg;
		    return false;
		}catch(Exception ex){
			StatusMsg = ex.getMessage();
			
			Status = STATUS_NOK;
		    return false;
		}
    }


}