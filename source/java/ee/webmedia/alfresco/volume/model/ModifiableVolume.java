package ee.webmedia.alfresco.volume.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import org.alfresco.service.namespace.QName;

public class ModifiableVolume implements Serializable {
    
	private static final Map<QName, Long> parameterQNamesByQNameIds = initParameterQNamesByQNameIds();
	private static final long serialVersionUID = 1L;
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    
    private String volumeMark;
    private String title;
    private Date validFrom;
    private Date validTo;
    private String status;
    private NodeRef nodeRef;
    private boolean containsCases;
    private boolean casesMandatory;
    private boolean markedForDestruction;
    private boolean transferConfirmed;
    private String volumeType;
    private Date retainUntilDate;
    private boolean casesCreatebleByUser;
    private Long shortRegNumber;
    private int containingDocsCount;
    private String ownerName;
    private boolean isDynamic;  
    
	public ModifiableVolume(NodeRef nodeRef) {
		this.nodeRef = nodeRef;
		this.isDynamic = true;
	}
	
	public String getVolumeMark() {
		return volumeMark;
	}
	public void setVolumeMark(String volumeMark) {
		this.volumeMark = volumeMark;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}
	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public NodeRef getNodeRef() {
		return nodeRef;
	}
	public void setNodeRef(NodeRef nodeRef) {
		this.nodeRef = nodeRef;
	}
	public String getVolumeLabel() {
		return volumeMark + " " + title;
	}
	public boolean isDynamic() {
		return isDynamic;
	}
	public void setDynamic(boolean isDynamic) {
		this.isDynamic = isDynamic;
	}
	public boolean isContainsCases() {
		return containsCases;
	}
	public void setContainsCases(boolean containsCases) {
		this.containsCases = containsCases;
	}
	public boolean isCasesMandatory() {
		return casesMandatory;
	}
	public void setCasesMandatory(boolean casesMandatory) {
		this.casesMandatory = casesMandatory;
	}
	public boolean isMarkedForDestruction() {
		return markedForDestruction;
	}
	public void setMarkedForDestruction(boolean markedForDestruction) {
		this.markedForDestruction = markedForDestruction;
	}
	public boolean isTransferConfirmed() {
		return transferConfirmed;
	}
	public void setTransferConfirmed(boolean transferConfirmed) {
		this.transferConfirmed = transferConfirmed;
	}
	public String getVolumeType() {
		return volumeType;
	}
	public void setVolumeType(String volumeType) {
		this.volumeType = volumeType;
	}
	public Date getRetainUntilDate() {
		return retainUntilDate;
	}
	public void setRetainUntilDate(Date retainUntilDate) {
		this.retainUntilDate = retainUntilDate;
	}
	public boolean isCasesCreatebleByUser() {
		return casesCreatebleByUser;
	}
	public void setCasesCreatebleByUser(boolean casesCreatebleByUser) {
		this.casesCreatebleByUser = casesCreatebleByUser;
	}
	public Long getShortRegNumber() {
		return shortRegNumber;
	}
	public void setShortRegNumber(Long shortRegNumber) {
		this.shortRegNumber = shortRegNumber;
	}
	public int getContainingDocsCount() {
		return containingDocsCount;
	}
	public void setContainingDocsCount(int containingDocsCount) {
		this.containingDocsCount = containingDocsCount;
	}
	public String getOwnerName() {
		return ownerName;
	}
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public static FastDateFormat getDateformat() {
		return dateFormat;
	}
	
	public static Map<QName, Long> getParameterMap() {
		return parameterQNamesByQNameIds;
	}

	private static Map<QName, Long> initParameterQNamesByQNameIds() {
    	BulkLoadNodeService bulkLoadService = BeanHelper.getBulkLoadNodeService();
    	Map<QName, Long> volumeParameterQNamesByQNameIds = new HashMap<QName, Long>();
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.VOLUME_MARK, bulkLoadService.getQNameDbId(VolumeModel.Props.VOLUME_MARK));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.TITLE, bulkLoadService.getQNameDbId(VolumeModel.Props.TITLE));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.VALID_FROM, bulkLoadService.getQNameDbId(VolumeModel.Props.VALID_FROM));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.VALID_TO, bulkLoadService.getQNameDbId(VolumeModel.Props.VALID_TO));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.STATUS, bulkLoadService.getQNameDbId(VolumeModel.Props.STATUS));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.CONTAINS_CASES, bulkLoadService.getQNameDbId(VolumeModel.Props.CONTAINS_CASES));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.CASES_MANDATORY, bulkLoadService.getQNameDbId(VolumeModel.Props.CASES_MANDATORY));
    	volumeParameterQNamesByQNameIds.put(EventPlanModel.Props.TRANSFER_CONFIRMED, bulkLoadService.getQNameDbId(EventPlanModel.Props.TRANSFER_CONFIRMED));
    	volumeParameterQNamesByQNameIds.put(EventPlanModel.Props.RETAIN_UNTIL_DATE, bulkLoadService.getQNameDbId(EventPlanModel.Props.RETAIN_UNTIL_DATE));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.VOL_SHORT_REG_NUMBER, bulkLoadService.getQNameDbId(VolumeModel.Props.VOL_SHORT_REG_NUMBER));
    	volumeParameterQNamesByQNameIds.put(VolumeModel.Props.CONTAINING_DOCS_COUNT, bulkLoadService.getQNameDbId(VolumeModel.Props.CONTAINING_DOCS_COUNT));
    	volumeParameterQNamesByQNameIds.put(DocumentDynamicModel.Props.OWNER_NAME, bulkLoadService.getQNameDbId(DocumentDynamicModel.Props.OWNER_NAME));
    	volumeParameterQNamesByQNameIds.put(DocumentAdminModel.Props.OBJECT_TYPE_ID, bulkLoadService.getQNameDbId(DocumentAdminModel.Props.OBJECT_TYPE_ID));
    	return volumeParameterQNamesByQNameIds;
    }
}
