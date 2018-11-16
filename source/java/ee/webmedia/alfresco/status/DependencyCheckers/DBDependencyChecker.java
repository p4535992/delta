package ee.webmedia.alfresco.status.DependencyCheckers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;
//TODO: REMOVE PostgreSQL direct connection!!
import org.postgresql.ds.PGPoolingDataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
/**
 * 
 * @author viljar.tina
 *
 */
public class DBDependencyChecker extends DependencyChecker{

	/**
	 * 
	 */
	private DataSource dataSource;
	
	
	/**
	 * 
	 */
	public DBDependencyChecker( String name, DataSource dataSource, Boolean isFatal ) {
		super(name, getURL(dataSource), isFatal);
		

		// ..
		this.dataSource = dataSource;
	}
	
	
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		//TODO: REMOVE PostgreSQL direct connection!!
		PGPoolingDataSource source = null;
		Connection con = null;
		ResultSet rs = null;
		try{
			Resource resource = new ClassPathResource("alfresco-global.properties");
	        Properties props = PropertiesLoaderUtils.loadProperties(resource);

			//TODO: REMOVE PostgreSQL direct connection!!
			source = new PGPoolingDataSource();
			source.setDataSourceName("Status");
			source.setServerName(props.getProperty("db.host"));
			source.setPortNumber(Integer.valueOf(props.getProperty("db.port")));
			source.setDatabaseName(props.getProperty("db.name"));
			source.setUser(props.getProperty("db.username"));
			source.setPassword(props.getProperty("db.password"));
			source.setMaxConnections(10);
			source.setLoginTimeout(5);
			source.setSocketTimeout(5);
			source.setConnectTimeout(5);
			
			con = source.getConnection();
			
			Statement stmt = con.createStatement();
			stmt.setQueryTimeout(5);
			rs = stmt.executeQuery("select version();");
			rs.next();
			
			String ver = rs.getString(1);
			
			StatusMsg = "ver:" + ver;
			
		}catch(Exception ex){
			StatusMsg = ex.getMessage();
			
			// ..
			Status = STATUS_NOK;
		    return false;
		} finally {
			try{
				if (rs != null) { rs.close(); }
			}catch(Exception e){}
			try{
				if (con != null) { con.close(); }
			}catch(Exception e){}
			try{
				if (source != null) { source.close(); }
			}catch(Exception e){}
	    }

        
		
		// ..
		Status = STATUS_OK;
	    return true;
    }


	
	
	/**
	 * 
	 */
	private static String getURL(DataSource dataSource){
		java.sql.Connection connection = null;
		java.sql.DatabaseMetaData dbMeta;
		String url="";
		try {
			connection = dataSource.getConnection();
			dbMeta = connection.getMetaData();
			url = dbMeta.getURL();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try {
				if(connection != null) connection.close();
			} catch (SQLException e) {}
		}

		// ..
		Integer intx = url.indexOf("://");
		if(intx > -1){
			String[] urlParts = (""+url.substring(intx+3)).split("/");
			String hostName = urlParts[0]; 

			if(hostName.contains("localhost")) hostName = hostName.replace("localhost", getHostName());
			if(hostName.contains("127.0.0.1")) hostName = hostName.replace("127.0.0.1", getHostName());
			
			return String.format("%s@%s", urlParts[1], hostName);
		}
		return url;
	}	
}