package ee.webmedia.alfresco.sharepoint;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

public class ImportSettings implements Cloneable {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ImportSettings.class);

    private String dataFolder;

    private String workFolder;

    private String mappingsFileName;

    private String defaultOwnerId;

    private String taskOwnerStructUnit;

    private Date docListArchivalsSeparatingDate;

    private Date publishToAdrStartingFromDate;

    private Date publishToAdrWithFilesStartingFromDate;

    private String seriesIdentifierForProcessToCaseFile;

    private String caseFileTypeIdForProcessToCaseFile;

    private DocumentTypeVersion caseFileTypeVersionForProcessToCaseFile;

    private int batchSize = 50;

    private boolean docsWithVersions;

    private String structAndDocsOrigin;

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

    public String getTaskOwnerStructUnit() {
        return taskOwnerStructUnit;
    }

    public void setTaskOwnerStructUnit(String taskOwnerStructUnit) {
        this.taskOwnerStructUnit = taskOwnerStructUnit;
    }

    public Date getDocListArchivalsSeparatingDate() {
        return docListArchivalsSeparatingDate;
    }

    public void setDocListArchivalsSeparatingDate(Date docListArchivalsSeparatingDate) {
        this.docListArchivalsSeparatingDate = docListArchivalsSeparatingDate;
    }

    public Date getPublishToAdrStartingFromDate() {
        return publishToAdrStartingFromDate;
    }

    public void setPublishToAdrStartingFromDate(Date publishToAdrStartingFromDate) {
        this.publishToAdrStartingFromDate = publishToAdrStartingFromDate;
    }

    public Date getPublishToAdrWithFilesStartingFromDate() {
        return publishToAdrWithFilesStartingFromDate;
    }

    public void setPublishToAdrWithFilesStartingFromDate(Date publishToAdrWithFilesStartingFromDate) {
        this.publishToAdrWithFilesStartingFromDate = publishToAdrWithFilesStartingFromDate;
    }

    public String getSeriesIdentifierForProcessToCaseFile() {
        return seriesIdentifierForProcessToCaseFile;
    }

    public void setSeriesIdentifierForProcessToCaseFile(String seriesIdentifierForProcessToCaseFile) {
        this.seriesIdentifierForProcessToCaseFile = seriesIdentifierForProcessToCaseFile;
    }

    public String getCaseFileTypeIdForProcessToCaseFile() {
        return caseFileTypeIdForProcessToCaseFile;
    }

    public void setCaseFileTypeIdForProcessToCaseFile(String caseFileTypeIdForProcessToCaseFile) {
        this.caseFileTypeIdForProcessToCaseFile = caseFileTypeIdForProcessToCaseFile;
    }

    public DocumentTypeVersion getCaseFileTypeVersionForProcessToCaseFile() {
        return caseFileTypeVersionForProcessToCaseFile;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isDocsWithVersions() {
        return docsWithVersions;
    }

    public void setDocsWithVersions(boolean docsWithVersions) {
        this.docsWithVersions = docsWithVersions;
    }

    public String getStructAndDocsOrigin() {
        return structAndDocsOrigin;
    }

    public void setStructAndDocsOrigin(String structAndDocsOrigin) {
        this.structAndDocsOrigin = structAndDocsOrigin;
    }

    public boolean isValid() {
        if (!isSharepointOrigin()) {
            docsWithVersions = false;
        }
        if (isNotBlank(caseFileTypeIdForProcessToCaseFile)) {
            CaseFileType caseFileType = getDocumentAdminService().getCaseFileType(caseFileTypeIdForProcessToCaseFile,
                    DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
            if (caseFileType == null || caseFileType.getLatestDocumentTypeVersion() == null) {
                LOG.error("CaseFile type '" + caseFileTypeIdForProcessToCaseFile + "' or latest version not found");
                return false;
            }
            caseFileTypeVersionForProcessToCaseFile = caseFileType.getLatestDocumentTypeVersion();
        } else {
            caseFileTypeVersionForProcessToCaseFile = null;
        }
        return isNotBlank(dataFolder) && isNotBlank(workFolder) && isNotBlank(mappingsFileName) && isNotBlank(defaultOwnerId) && docListArchivalsSeparatingDate != null
                && publishToAdrWithFilesStartingFromDate != null && batchSize > 0;
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

    public boolean isNotPublishToAdr(Date created) {
        return publishToAdrStartingFromDate != null && created != null && created.before(publishToAdrStartingFromDate);
    }

    public boolean isVolumeOpen(Date volumeEnded) {
        return docListArchivalsSeparatingDate == null || volumeEnded == null || !volumeEnded.before(docListArchivalsSeparatingDate);
    }

    public boolean isSharepointOrigin() {
        return "SharePoint".equals(structAndDocsOrigin);
    }

    public boolean isAmphoraOrigin() {
        return "Amphora".equals(structAndDocsOrigin);
    }

    public boolean isRiigikohusOrigin() {
        return "Riigikohtu infosüsteem".equals(structAndDocsOrigin);
    }
}
