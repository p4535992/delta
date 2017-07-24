package ee.webmedia.alfresco.permfix;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.service.Permission;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class NodePermissionsImporter extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(NodePermissionsImporter.class);
    
    private static final String GROUP_PREFIX = "GROUP_";
    
    private String storeString = "workspace://SpacesStore";
    private String dataFolder;
	private String mappingFileName = "groupsMapping.csv";
    private Map<String, String> groupsMap = new HashMap<>();

    
    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("getNodeLoadingResultSet not used in NodePermissionsImporter!");
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
    	Assert.notNull(storeString, "Store must be provided");
        storeString = StringUtils.trim(storeString);
        StoreRef storeRef = new StoreRef(storeString);
        List<StoreRef> allRefs = nodeService.getStores();
        if (!allRefs.contains(storeRef)) {
            throw new UnableToPerformException("User entered unknown storeRef: " + storeString);
        }
        return BeanHelper.getPrivilegeService().getAllNodesWithPermissions(storeRef);
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
    	BufferedWriter writer = null;
    	try {
    		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFolder + "importedNodePermissions.csv", true), "UTF-8"));
    		getNodePermissions(nodeRef, writer);
    	} catch (IOException e) {
    	    LOG.error("Failed to collect permissions for node: " + nodeRef, e);
    	} finally {
    		IOUtils.closeQuietly(writer);
    	}
    	
        return new String[] { "imported permissions for node: " + nodeRef};
    }
    
    
    private void getNodePermissions(NodeRef nodeRef, BufferedWriter writer) throws IOException {
    	List<Permission> permissions = BeanHelper.getPrivilegeService().getAllSetPrivileges(nodeRef);
    	
    	String line = createNodePermissionLine(nodeRef, permissions);
    	if (StringUtils.isNotBlank(line)) {
    		log.info("PERMFIX: line = " + line);
    		writer.write(line);
    		writer.newLine();
    		writer.flush();
    	}
    	
    	
    }
    
    private String createNodePermissionLine(NodeRef nodeRef, List<Permission> permissions) {
    	Map<String, String> authorities = new HashMap<>();
    	for (Permission permission: permissions) {
    		String authority = permission.getAuthority();
    		String groupsMapKey = StringUtils.substringAfter(authority, GROUP_PREFIX);
    		if (permission.isDirect() && (authority.startsWith(GROUP_PREFIX) && groupsMap.containsKey(groupsMapKey) || 
	    				!authority.startsWith(GROUP_PREFIX))) { 
	    		
	    		String newAuthority = ((authority.startsWith(GROUP_PREFIX)))?GROUP_PREFIX + groupsMap.get(groupsMapKey):authority;
	    		String privileges = authorities.get(newAuthority);
	    		if (StringUtils.isNotBlank(privileges)) {
	    			privileges += "," + permission.getPrivilege().getPrivilegeName();
	    		} else {
	    			privileges = permission.getPrivilege().getPrivilegeName();
	    		}
	    		authorities.put(newAuthority, privileges);
    		}
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	if (!authorities.isEmpty()) {
    		sb.append(nodeRef);
    		
	    	for (String authority: authorities.keySet()) {
	    		sb.append(";");
	    		sb.append(authority);
	    		sb.append("#");
	    		sb.append(authorities.get(authority));
	    	}	    	
    	}
    	return sb.toString();
    }

    @Override
    protected void executeUpdater() throws Exception {
    	try{

    		File importedNodePermissionsFile = new File(dataFolder + "importedNodePermissions.csv");

    		if(importedNodePermissionsFile.delete()){
    			log.info(importedNodePermissionsFile.getName() + " is deleted!");
    		}else{
    			log.info("Delete operation is failed.");
    		}

    	}catch(Exception e){

    		e.printStackTrace();

    	}
    	fillGroupsMap();
        super.executeUpdater();
        resetFields();
    }
    
    private void fillGroupsMap() throws Exception {
    	File file = new File(dataFolder + mappingFileName);
    	if (!file.exists()) {
            throw new UnableToPerformException("File does not exist:: " + file.getAbsolutePath());
        }

        log.info("Loading groups mapping from file " + file.getAbsolutePath());

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(file)), CSV_SEPARATOR, CSV_CHARSET);
        try {
            while (reader.readRecord()) {
                groupsMap.put(reader.get(0), reader.get(1));
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + groupsMap.size() + " groups from file " + file.getAbsolutePath());
    }

    @Override
    protected Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
        return null;
    }

    private void resetFields() {
        storeString = "workspace://SpacesStore";
        mappingFileName = "groupsMapping.csv";
        dataFolder = null;
        groupsMap = new HashMap<>();
    }

    public String getStoreString() {
        return storeString;
    }

    public void setStoreString(String storeString) {
        this.storeString = storeString;
    }
    
    public String getDataFolder() {
		return dataFolder;
	}

	public void setDataFolder(String dataFolder) {
		this.dataFolder = dataFolder;
	}

	public String getMappingFileName() {
		return mappingFileName;
	}

	public void setMappingFileName(String mappingFileName) {
		this.mappingFileName = mappingFileName;
	}

}
