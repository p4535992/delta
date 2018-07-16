package ee.webmedia.alfresco.status.DependencyCheckers;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * 
 * @author viljar.tina
 *
 */
public class LDAPDependencyChecker extends DependencyChecker{

	
	private String userName;
	
	
	private String passwd;
	
	/**
	 * 
	 */
	public LDAPDependencyChecker( String name, String uri, String userName, String passwd, Boolean isFatal ) {
		super(name, uri, isFatal);
		
		// ..
		this.userName = userName;
		this.passwd = passwd;
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, this.Uri);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, this.userName);
        env.put(Context.SECURITY_CREDENTIALS, this.passwd);
		
		
		try{
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
			
            // ..
            Status = STATUS_OK;
			return true;
			
		}catch(Exception ex){
			StatusMsg = ex.getMessage();
			
			Status = STATUS_NOK;
		    return false;
		}
    }


}