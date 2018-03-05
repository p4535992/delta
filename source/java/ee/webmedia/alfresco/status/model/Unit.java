package ee.webmedia.alfresco.status.model;

public class Unit {
	public static final String STATUS_OK = "OK";
	public static final String STATUS_NOK = "NOK";
	
	
	public String name;
	

	public String Status = STATUS_NOK;
	
	
	public String StatusMsg = "";
	
	
	public String Uri;
	
	
	
	
	/**
	 * 
	 */
	public Unit(String name) {
		this.name = name;
    }
	
	
	

}
