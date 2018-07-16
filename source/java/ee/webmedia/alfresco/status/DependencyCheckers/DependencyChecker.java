package ee.webmedia.alfresco.status.DependencyCheckers;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ee.webmedia.alfresco.status.model.Unit;




/**
 * 
 * @author viljar.tina
 *
 */
public abstract class DependencyChecker extends Unit{
	
	/**
	 * 
	 */
	public Boolean IsFatal;
	
	
	
	/**
	 * 
	 */
	DependencyChecker ( String name, String uri, Boolean isFatal ) {
		super(name);
        this.IsFatal = isFatal;
        if(uri.contains("null")){ uri = uri.replace("null", getHostName()); }
        this.Uri = uri;
    }
	
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		Status = STATUS_OK;
	    return true;
    }
	
	
	/**
	 * 
	 */
	public static String getHostName(){
		
		String hostName = "localhost";
		try{
			InetAddress inetAddr = InetAddress.getLocalHost();
			hostName = inetAddr.getCanonicalHostName();
		}catch(UnknownHostException e){
			System.out.println("Host not found: " + e.getMessage());
		}
		

		// ..
		return hostName;
	}
}