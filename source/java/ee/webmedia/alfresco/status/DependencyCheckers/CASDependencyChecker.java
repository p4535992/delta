package ee.webmedia.alfresco.status.DependencyCheckers;

import java.io.*;
import java.net.*;



/**
 * 
 * @author viljar.tina
 *
 */
public class CASDependencyChecker extends DependencyChecker{

	
	/**
	 * 
	 */
	public CASDependencyChecker( String name, String uri, Boolean isFatal ) {
		super(name, uri, isFatal);
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			URL url = new URL(this.Uri+"status");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
			String line = rd.readLine();
			rd.close();
			
			Status = STATUS_NOK;
			if("Health: OK".equals(line)){
				Status = STATUS_OK;
				return true;
			}
			StatusMsg = line;
		    return false;
		}catch(Exception ex){
			StatusMsg = ex.getMessage();
			
			Status = STATUS_NOK;
		    return false;
		}
    }


}