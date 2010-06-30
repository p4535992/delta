package ee.webmedia.alfresco.importer.excel.vo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.document.model.CommonDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;

/**
 * Base class for Importable documents
 * 
 * @author Ats Uiboupin
 */
public class ImportDocument extends CommonDocument {
    @AlfrescoModelProperty(isMappable = false)
    private SendInfo sendInfo;
    @AlfrescoModelProperty(isMappable = false)
    private List<String> fileLocations;
    private Map<String/* fileName */, String/* debugInformation */> fileLocationsMissing;
    @AlfrescoModelProperty(isMappable = false)
    private long orderOfAppearance;
    @AlfrescoModelProperty(isMappable = false)
    private String nodeRefInRepo;
    private File rowSourceFile;
    private String rowSourceSheet;
    /** NB! 0-based PHYSICAL (not logical/visible) number of excel row */
    private int rowSourceNumber;

    public void setSendInfo(SendInfo sendInfo) {
        this.sendInfo = sendInfo;
    }

    public SendInfo getSendInfo() {
        return sendInfo;
    }

    public void addFileLocation(String fileLocation) {
        if (StringUtils.isBlank(fileLocation)) {
            return;
        }
        if (fileLocations == null) {
            fileLocations = new ArrayList<String>(1);
        }
        fileLocations.add(fileLocation);
    }

    public List<String> getFileLocations() {
        return fileLocations;
    }

    public void addFileLocationsMissing(String fileLocationMissing, String debugInformation) {
        if (StringUtils.isBlank(fileLocationMissing)) {
            return;
        }
        if (fileLocationsMissing == null) {
            fileLocationsMissing = new HashMap<String, String>(1);
        }
        fileLocationsMissing.put(fileLocationMissing, debugInformation);
    }

    public Map<String, String> getFileLocationsMissing() {
        return fileLocationsMissing;
    }

    public void setFileLocationsMissing(Map<String, String> fileLocationsMissing) {
        this.fileLocationsMissing = fileLocationsMissing; // method needed form mapper
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public File getRowSourceFile() {
        return rowSourceFile;
    }

    public void setRowSourceFile(File rowSourceFile) {
        this.rowSourceFile = rowSourceFile;
    }

    public String getRowSourceSheet() {
        return rowSourceSheet;
    }

    public void setRowSourceSheet(String rowSourceSheet) {
        this.rowSourceSheet = rowSourceSheet;
    }

    public void setRowSourceNumber(int rowSourceNumber) {
        this.rowSourceNumber = rowSourceNumber;
    }

    public int getRowSourceNumber() {
        return rowSourceNumber;
    }

    public void setNodeRefInRepo(String nodeRefInRepo) {
        this.nodeRefInRepo = nodeRefInRepo;
    }

    public String getNodeRefInRepo() {
        return nodeRefInRepo;
    }

    public void setOrderOfAppearance(long orderOfAppearance) {
        this.orderOfAppearance = orderOfAppearance;
    }

    public long getOrderOfAppearance() {
        return orderOfAppearance;
    }
}