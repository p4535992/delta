package ee.webmedia.alfresco.gopro;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;


public class ImportSettings implements Cloneable {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ImportSettings.class);

    private String dataFolder;

    private String workFolder;

    private String mappingsFileName;

    private String defaultOwnerId;
    
    private boolean publishToAdr;
    
	private boolean allFilesActive;
	
	private boolean allWfFinished;

	private String taskOwnerStructUnitAuthority;
    
    private List<String> taskOwnerStructUnitAuthorityPrivileges;


    private int batchSize = 50;
    
    private Date docListArchivalsSeparatingDate;



    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    public String getWorkFolder() {
        return workFolder;
    }

    public void setWorkFolder(String workFolder) {
        this.workFolder = workFolder;
    }

    public String getMappingsFileName() {
        return mappingsFileName;
    }

    public void setMappingsFileName(String mappingsFileName) {
        this.mappingsFileName = mappingsFileName;
    }

    public String getDefaultOwnerId() {
        return defaultOwnerId;
    }

    public void setDefaultOwnerId(String defaultOwnerId) {
        this.defaultOwnerId = defaultOwnerId;
    }

    public String getTaskOwnerStructUnitAuthority() {
        return taskOwnerStructUnitAuthority;
    }

    public boolean isPublishToAdr() {
		return publishToAdr;
	}

	public void setPublishToAdr(boolean publishToAdr) {
		this.publishToAdr = publishToAdr;
	}

	public boolean isAllFilesActive() {
		return allFilesActive;
	}

	public void setAllFilesActive(boolean allFilesActive) {
		this.allFilesActive = allFilesActive;
	}
	
	public boolean isAllWfFinished() {
		return allWfFinished;
	}

	public void setAllWfFinished(boolean allWfFinished) {
		this.allWfFinished = allWfFinished;
	}
	
    public void setTaskOwnerStructUnitAuthority(String taskOwnerStructUnitAuthority) {
        this.taskOwnerStructUnitAuthority = taskOwnerStructUnitAuthority;
    }
    
    public List<String> getTaskOwnerStructUnitAuthorityPrivileges() {
        return taskOwnerStructUnitAuthorityPrivileges;
    }

    public void setTaskOwnerStructUnitAuthorityPrivileges(List<String> taskOwnerStructUnitAuthorityPrivileges) {
        this.taskOwnerStructUnitAuthorityPrivileges = taskOwnerStructUnitAuthorityPrivileges;
    }

    
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public Date getDocListArchivalsSeparatingDate() {
        return docListArchivalsSeparatingDate;
    }

    public void setDocListArchivalsSeparatingDate(Date docListArchivalsSeparatingDate) {
        this.docListArchivalsSeparatingDate = docListArchivalsSeparatingDate;
    }

    public boolean isValid() {
        
        return isNotBlank(dataFolder) && isNotBlank(workFolder) && isNotBlank(mappingsFileName) && isNotBlank(defaultOwnerId) 
        		&& docListArchivalsSeparatingDate != null && batchSize > 0;
    }

    @Override
    public ImportSettings clone() {
        try {
            return (ImportSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public File getMappingsFile() {
        return mappingsFileName.equals(FilenameUtils.getName(mappingsFileName)) ? getDataFolderFile(mappingsFileName) : new File(mappingsFileName);
    }

    public File getDataFolderFile(String file) {
        return new File(dataFolder, file);
    }

    public File getWorkFolderFile(String file) {
        return new File(workFolder, file);
    }
    
    public boolean isVolumeOpen(Date volumeEnded, String volumeStatus) {
    	if (StringUtils.isNotBlank(volumeStatus) && ("suletud".equalsIgnoreCase(volumeStatus) || "hävitatud".equalsIgnoreCase(volumeStatus))) {
    		return false;
    	}
        return (StringUtils.isNotBlank(volumeStatus) && "avatud".equalsIgnoreCase(volumeStatus)) || docListArchivalsSeparatingDate == null || volumeEnded == null || !volumeEnded.before(docListArchivalsSeparatingDate);
    }
}
