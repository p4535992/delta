package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * This updater combines 4 updaters:
 * 1) removes DocumentFileRead permission, adds permissions that are added when series is created
 * 2) Changes value of property series.volType like that:
 * TeemapÃƒÂµhine toimik -> SUBJECT_FILE
 * AastapÃƒÂµhine toimik -> ANNUAL_FILE
 * Asjatoimik -> CASE_FILE
 * and adds SUBJECT_FILE and ANNUAL_FILE if needed.
 * if volType is missing, then adds all 3: SUBJECT_FILE, ANNUAL_FILE, CASE_FILE
 * 3) checks all docTypes of all series docs and adds missing if any to series.docType
 * 4) adds caseFileContainer aspect
 * 
 */
public class SeriesUpdater extends AbstractNodeUpdater {
    @Deprecated
    public static final String DOCUMENT_FILE_READ = "DocumentFileRead";

    private boolean seriesUpdater1Executed = false;

    private boolean executeInBackground;
    private VolumeService volumeService;
    private BulkLoadNodeService bulkLoadNodeService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        NodeRef previousComponentRef = generalService.getNodeRef("/sys:system-registry/module:modules/module:simdhs/module:components/module:seriesUpdater", new StoreRef(
                "system://system"));
        seriesUpdater1Executed = previousComponentRef != null;

        String query = generateTypeQuery(SeriesModel.Types.SERIES);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef seriesRef) throws Exception {
        List<String> logInfo = new ArrayList<String>();

        if (!seriesUpdater1Executed) {
            getSeriesService().setSeriesDefaultPermissionsOnCreate(seriesRef);
            volumeTypesUpdater(seriesRef, logInfo);
            docTypesUpdater(seriesRef, logInfo);
        }
        addCaseFileContainerAspect(seriesRef, logInfo);
        if (nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS) == null) {
            nodeService.setProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS, Boolean.TRUE);
        }

        return logInfo.toArray(new String[logInfo.size()]);
    }

    private void addCaseFileContainerAspect(NodeRef seriesRef, List<String> logInfo) {
        QName aspect = CaseFileModel.Aspects.CASE_FILE_CONTAINER;
        if (nodeService.hasAspect(seriesRef, aspect)) {
            logInfo.add("caseFileContainerAspectExists");
        } else {
            nodeService.addAspect(seriesRef, aspect, null);
            logInfo.add("caseFileContainerAspectAdded");
        }
    }

    private void volumeTypesUpdater(NodeRef nodeRef, List<String> logInfo) {
        String logIsChanged = "volumeTypeChanged";
        String logIsAdded = "volumeTypeAdded";
        String newVolumeTypeValue;
        Set<String> newSeriesVolTypes = new HashSet<String>();
        newSeriesVolTypes.add(VolumeType.SUBJECT_FILE.name());
        newSeriesVolTypes.add(VolumeType.ANNUAL_FILE.name());
        newSeriesVolTypes.add(VolumeType.CASE_FILE.name());
        @SuppressWarnings("unchecked")
        List<String> oldSeriesVolTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.VOL_TYPE);
        if (oldSeriesVolTypes == null) {
            nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.ANNUAL_FILE.name());
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.SUBJECT_FILE.name());
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.CASE_FILE.name());
            return;
        }
        boolean isUpdateNeeded = true;
        Iterator<String> iter = oldSeriesVolTypes.iterator();
        while (iter.hasNext()) {
            String volType = iter.next();
            // if any of those volTypes are set then no need to update
            if (VolumeType.SUBJECT_FILE.name().equals(volType) || VolumeType.ANNUAL_FILE.name().equals(volType) || VolumeType.CASE_FILE.name().equals(volType)) {
            	isUpdateNeeded = false;
            }
            if ("objektipÃƒÂµhine".equals(volType) || "OBJECT".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
                isUpdateNeeded = true;
            }
            if ("aastapÃƒÂµhine".equals(volType) || "YEAR_BASED".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
                isUpdateNeeded = true;
            }
            if ("Asjatoimik".equals(volType) || "CASE".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.CASE_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
                isUpdateNeeded = true;
            }
            
        }
        if (isUpdateNeeded) {
        	newSeriesVolTypes.addAll(oldSeriesVolTypes);
        	nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
        }
    }
    
    /**
     * Collects all docTypes of child docs of the serie and adds missing docTypes if any to serie.docType
     * @param nodeRef
     * @param logInfo
     */
    private void docTypesUpdater(NodeRef nodeRef, List<String> logInfo) {
        String logIsAdded = "docTypeAdded";
        
        // get all doc types of all the documents of the serie
        Set<String> usedDocTypes = new HashSet<String>();
        List<NodeRef> seriesDocs = volumeService.getAllVolumeRefsBySeries(nodeRef);
        Set<QName> qNamesSet = new HashSet<QName>();
        qNamesSet.add(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        if (seriesDocs != null && !seriesDocs.isEmpty()) {
	        Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> resultMap = bulkLoadNodeService.loadChildNodes(seriesDocs, qNamesSet);
	        for (NodeRef volNodeRef: resultMap.keySet()) {
	        	Map<NodeRef, Map<QName, Serializable>> docsMap = resultMap.get(volNodeRef);
	        	for (NodeRef docNodeRef: docsMap.keySet()) {
	        		Map<QName, Serializable> docTypesMap = docsMap.get(docNodeRef);
	        		String docType = (String) docTypesMap.get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
	        		if (StringUtils.isNotBlank(docType)) {
	        			usedDocTypes.add(docType);
	        		}
	        	}
	        }
        }
        if (!usedDocTypes.isEmpty()) {
	        Set<String> newSeriesDocTypes = new HashSet<String>();
	        
	        @SuppressWarnings("unchecked")
	        List<String> oldSeriesDocTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.DOC_TYPE);
	        if (oldSeriesDocTypes == null) {
	            nodeService.setProperty(nodeRef, SeriesModel.Props.DOC_TYPE, new ArrayList<String>(usedDocTypes));
	            for (String addedDocType: usedDocTypes) {
	            	logInfo.add(logIsAdded);
	            	logInfo.add(addedDocType);
	            }
	            return;
	        } else {
	        	for (String docType: usedDocTypes) {
	        		if (!oldSeriesDocTypes.contains(docType)) {
	        			newSeriesDocTypes.add(docType);
	        			logInfo.add(logIsAdded);
		            	logInfo.add(docType);
	        		}
	        	}
	        }
	        
	        if (!newSeriesDocTypes.isEmpty()) {
	        	newSeriesDocTypes.addAll(oldSeriesDocTypes);
	        	nodeService.setProperty(nodeRef, SeriesModel.Props.DOC_TYPE, new ArrayList<String>(newSeriesDocTypes));
	        }
        }
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", DOCUMENT_FILE_READ + " removed from auths", "volumeType added/changed", "newVolumeTypeValue" };
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!isEnabled()) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        if (executeInBackground) {
            super.executeUpdaterInBackground();
        } else {
            super.executeUpdater();
        }
    }

    public void setExecuteInBackground(boolean executeInBackground) {
        this.executeInBackground = executeInBackground;
    }
    
    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }
    
    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
