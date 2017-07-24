package ee.webmedia.alfresco.permfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class NodePermissionsExporter extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(NodePermissionsExporter.class);
    
    private String storeString = "workspace://SpacesStore";
    private String dataFolder;
	private String mappingFileName = "importedNodePermissions.csv";
    private Map<NodeRef, Map<String, Set<Privilege>>> nodesGroupsMap = new HashMap<>();

    
    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("getNodeLoadingResultSet not used in NodePermissionsExporter!");
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
    	
        return nodesGroupsMap.keySet();
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
    	
    	try {
    		Map<String, Set<Privilege>> authorityPrivilegesMap = nodesGroupsMap.get(nodeRef);
    		for (String authority: authorityPrivilegesMap.keySet()) {
    			Set<Privilege> privileges = authorityPrivilegesMap.get(authority);
    			StringBuilder sbPrivs = new StringBuilder();
    			for (Privilege priv: privileges) {
    				sbPrivs.append(", " + priv.getPrivilegeName());
    			}
    			log.info("authority = " + authority + " exist = " + BeanHelper.getAuthorityService().authorityExists(authority));
    			log.info("adding for node: " + nodeRef + " for authority = " + authority + " permissions# " + sbPrivs);
    			BeanHelper.getPrivilegeService().setPermissions(nodeRef, authority, authorityPrivilegesMap.get(authority));
    			
    		}
    	} catch (Exception e) {
    	    LOG.error("Failed to collect permissions for node: " + nodeRef, e);
    	}
    	
        return new String[] { "updated permissions for node: " + nodeRef};
    }
    
    private NodeRef parseNodePermissionLine(String line, Map<String, Set<Privilege>> authorityPrivilegesMap) {
    	
    	String [] lineSplit = line.split(";");
    	if (lineSplit.length <= 1) {
    		return null;
    	}
    	
    	for (int i = 1; i < lineSplit.length; i++) {
    		String [] authLineSplit = lineSplit[i].split("#");
    		if (authLineSplit.length == 2) {
    			String authority = authLineSplit[0];
    			Set<Privilege> privileges = parsePrivileges(authLineSplit[1]);
    			if (!privileges.isEmpty()) {
    				authorityPrivilegesMap.put(authority, privileges);
    			}
    		}
    	}
    	
    	return new NodeRef(lineSplit[0]);
    }
    
    private Set<Privilege> parsePrivileges(String linePrivileges) {
    	Set<Privilege> privileges = new HashSet<>();
    	String [] linePrivilegesSplit = linePrivileges.split(",");
    	for (int i = 0; i < linePrivilegesSplit.length; i++) {
    		privileges.add(Privilege.getPrivilegeByName(linePrivilegesSplit[i]));
    	}
    	return privileges;
    }

    @Override
    protected void executeUpdater() throws Exception {
    	fillNodesGroupsMap();
        super.executeUpdater();
        resetFields();
    }
    
    private void fillNodesGroupsMap() throws Exception {
    	File file = new File(dataFolder + mappingFileName);
    	if (!file.exists()) {
            throw new UnableToPerformException("File does not exist:: " + file.getAbsolutePath());
        }

        log.info("Loading groups mapping from file " + file.getAbsolutePath());

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), CSV_CHARSET));
        try {
        	String line = null;
            while ((line = reader.readLine()) != null) {
            	Map<String, Set<Privilege>> authorityPrivilegesMap = new HashMap<>();
            	NodeRef nodeRef = parseNodePermissionLine(line, authorityPrivilegesMap);
            	if (nodeRef != null && !authorityPrivilegesMap.isEmpty()) {
            		nodesGroupsMap.put(nodeRef, authorityPrivilegesMap);
            	}
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + nodesGroupsMap.size() + " groups from file " + file.getAbsolutePath());
    }

    @Override
    protected Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
        return null;
    }

    private void resetFields() {
        storeString = "workspace://SpacesStore";
        mappingFileName = "importedNodePermissions.csv";
        dataFolder = null;
        nodesGroupsMap = new HashMap<>();
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
