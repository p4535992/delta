package ee.webmedia.alfresco.status.DependencyCheckers;

import java.io.*;
import java.net.*;



/**
 * 
 * @author viljar.tina
 *
 */
public class HTTPGETDependencyChecker extends DependencyChecker{

	
	/**
	 * 
	 */
	public HTTPGETDependencyChecker( String name, String uri, Boolean isFatal ) {
		super(name, uri, isFatal);
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			URL url = new URL(this.Uri);
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