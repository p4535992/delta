package ee.webmedia.alfresco.status.DependencyCheckers;

import java.io.*;
import java.net.*;
import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;


/**
 * 
 * @author viljar.tina
 *
 */
public class OODependencyChecker extends DependencyChecker{

	
	/**
	 * 
	 */
	private OpenOfficeConnection ooConnection;
	
	
	
	
	/**
	 * 
	 */
	public OODependencyChecker( String name, String uri, OpenOfficeConnection ooConnection, Boolean isFatal ) {
		super(name, uri, isFatal);
		
		// ..
		this.ooConnection = ooConnection;
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			if (!ooConnection.isConnected())
				ooConnection.connect();
			
			Status = STATUS_OK;
		}catch(Exception ex){
			StatusMsg = ex.getMessage().replace("localhost",getHostName());
			
			Status = STATUS_NOK;
		}
		
		// ..
		return STATUS_OK.equals(Status); 
    }


}