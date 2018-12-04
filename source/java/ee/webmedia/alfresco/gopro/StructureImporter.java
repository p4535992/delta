package ee.webmedia.alfresco.gopro;

import static ee.webmedia.alfresco.common.web.BeanHelper.getArchivalsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getRegisterService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.gopro.ImportUtil.checkAnyOf;
import static ee.webmedia.alfresco.gopro.ImportUtil.checkDate;
import static ee.webmedia.alfresco.gopro.ImportUtil.checkInteger;
import static ee.webmedia.alfresco.gopro.ImportUtil.checkMandatory;
import static ee.webmedia.alfresco.gopro.ImportUtil.docsError;
import static ee.webmedia.alfresco.gopro.ImportUtil.dropLogFile;
import static ee.webmedia.alfresco.gopro.ImportUtil.getDate;
import static ee.webmedia.alfresco.gopro.ImportUtil.getInteger;
import static ee.webmedia.alfresco.gopro.ImportUtil.getString;
import static ee.webmedia.alfresco.gopro.ImportUtil.structError;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Imports structure (functions, series, volumes and cases) from GoPro.
 */
public class StructureImporter {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(StructureImporter.class);

    private static final String SOURCE_FILENAME = "struktuur.csv";
    public static final String COMPLETED_FILENAME = "completed_volumes_cases.csv";
    private static final String FAILED_FILENAME = "failed_import.csv";

    private static final String REGISTER_NAME = "Üldregister";
    private static final String[] VOLUME_TYPES = { "aastane toimik", "asjatoimik", "objektitoimik" };
    private static final String[] VOLUME_TYPE_CODES = { VolumeType.ANNUAL_FILE.name(), VolumeType.CASE_FILE.name(), VolumeType.SUBJECT_FILE.name() };
    private static final String[] RESTRICTIONS = { "", AccessRestriction.AK.getValueName(), AccessRestriction.OPEN.getValueName(), AccessRestriction.INTERNAL.getValueName(),
        AccessRestriction.LIMITED.getValueName() };

    static {
        // These arrays are used for binary search; they must be sorted to be useful.
        Arrays.sort(RESTRICTIONS);
    }

    private final NodeService nodeService = getNodeService();
    private final GeneralService generalService = getGeneralService();
    private final FunctionsService functionsService = getFunctionsService();
    private final SeriesService seriesService = getSeriesService();
    private final RegisterService registerService = getRegisterService();
    private final DocumentAdminService documentAdminService = getDocumentAdminService();
    private final PrivilegeService privilegeService = getPrivilegeService();
    private final CaseFileService caseFileService = getCaseFileService();
    private final SearchService searchService = getSearchService();
    private final ArchivalsService archivalsService = getArchivalsService();
    private final UserService userService = getUserService();
    private final DocumentSearchService documentSearchService = getDocumentSearchService();

    private Integer registerId;

    public void doImport(final ImportSettings data, ImportStatus status) {
        try {
            importStructure(data, status);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkSeriesOrders() {
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES));
        queryParts.add(SearchUtil.generatePropertyNullQuery(SeriesModel.Props.ORDER));

        List<UnmodifiableFunction> functions = functionsService.getAllFunctions();
        functions.addAll(archivalsService.getArchivedFunctions());

        for (UnmodifiableFunction f : functions) {
            queryParts.add(SearchUtil.generatePrimaryParentQuery(f.getNodeRef()));
            String q = SearchUtil.joinQueryPartsAnd(queryParts);
            queryParts.remove(2);

            if (searchService.query(f.getNodeRef().getStoreRef(), SearchService.LANGUAGE_LUCENE, q).length() == 0) {
                continue;
            }

            List<ChildAssociationRef> children = nodeService.getChildAssocs(f.getNodeRef(), SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
            List<String> seriesIds = new ArrayList<String>(children.size());

            for (ChildAssociationRef assoc : children) {
                seriesIds.add((String) nodeService.getProperty(assoc.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER));
            }

            Collections.sort(seriesIds, SeriesIdentifierComparator.INSTANCE);

            for (ChildAssociationRef assoc : children) {
                String id = (String) nodeService.getProperty(assoc.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER);
                nodeService.setProperty(assoc.getChildRef(), SeriesModel.Props.ORDER, seriesIds.indexOf(id) + 1);
            }
        }
    }

    private void importStructure(ImportSettings data, ImportStatus status) throws IOException {
        File sourceFile = data.getDataFolderFile(SOURCE_FILENAME);
        File errorsFile = data.getWorkFolderFile(FAILED_FILENAME);
        File logFile = data.getWorkFolderFile(COMPLETED_FILENAME);

        // Structure import is done before other imports. Other imports check whether import errors file exist and will not start in that case.
        if (errorsFile.exists()) {
            errorsFile.delete();
        }
        
        try {
            if (userService.getPerson(data.getDefaultOwnerId()) == null) {
                throw new ImportValidationException("Default user with id = " + data.getDefaultOwnerId() + " does not exist");
            }
        } catch (ImportValidationException e) {
            docsError(errorsFile, e);
            throw new RuntimeException(e);
        }

        if (logFile.exists()) {
            LOG.info(COMPLETED_FILENAME + " was found in work folder. Skipping structure import");
            return;
        }

        if (!sourceFile.exists()) {
            structError(errorsFile, "struktuur.csv cannot be found");
            return;
        }

        NodeRef mainDocsList = generalService.getNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE, generalService.getStore());
        NodeRef archDocsList = generalService.getNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE, generalService.getArchivalsStoreRef());
        List<String> documentTypes = new ArrayList<String>();
        documentTypes.add("otherDocument");
        
        String taskOwnerStructUnitAuthority = null;
        Set<Privilege> taskOwnerStructUnitAuthorityPrivileges = getStructUnitAuthorityPrivileges(data.getTaskOwnerStructUnitAuthorityPrivileges());;
        if (taskOwnerStructUnitAuthorityPrivileges != null && StringUtils.isNotBlank(data.getTaskOwnerStructUnitAuthority())) {
            List<String> authorities = documentSearchService.searchAuthorityGroupsByExactName(data.getTaskOwnerStructUnitAuthority());
            if (authorities.size() != 1) {
                structError(errorsFile, "For authorityDisplayName '" + data.getTaskOwnerStructUnitAuthority() + "' found " + authorities.size() + " groups with authorityName values: "
                        + authorities);
                return;
            }
            taskOwnerStructUnitAuthority = authorities.get(0);
            LOG.info("For authorityDisplayName '" + data.getTaskOwnerStructUnitAuthority() + "' found group with authorityName '" + taskOwnerStructUnitAuthority + "'");
            
            
        }

        ImportContext context = new ImportContext(mainDocsList, archDocsList, documentTypes, data, taskOwnerStructUnitAuthority, taskOwnerStructUnitAuthorityPrivileges);

        CsvReader reader = ImportUtil.createDataReader(sourceFile);

        boolean canContinue = reader.readHeaders() && reader.readRecord();
        

        LOG.info("Structure import: preparations done; starting to import...");

        CsvWriter logWriter = initLogWriter(logFile);

        try {
            Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
            while (canContinue) {
                if (reader.getCurrentRecord() % 500 == 0) {
                    LOG.info("Structure import reached row " + reader.getCurrentRecord());
                }

                status.incrCount();

                validateStructureLine(reader, data);

                NodeRef function = doFunction(reader, context, propertyTypes);
                NodeRef series = doSeries(reader, function, context);
                NodeRef volume = doVolume(reader, series, context);
                NodeRef caseRef = doCase(reader, volume);

                // Record logging:

                String functionRef = (String) nodeService.getProperty(function, FunctionsModel.Props.MARK);
                String seriesRef = (String) nodeService.getProperty(series, SeriesModel.Props.SERIES_IDENTIFIER);
                String seriesTitle = (String) nodeService.getProperty(series, SeriesModel.Props.TITLE);
                String url = null;

                if (caseRef == null) {
                    logVolume(logWriter, volume, url, functionRef, seriesRef, seriesTitle);
                } else {
                	nodeService.setProperty(volume, VolumeModel.Props.CONTAINS_CASES, Boolean.TRUE);
                    logCase(logWriter, caseRef, url, functionRef, seriesRef, seriesTitle);
                }

                canContinue = reader.readRecord();
            }

            for (NodeRef functionRef : context.getCloseFunctions()) {
                nodeService.setProperty(functionRef, FunctionsModel.Props.STATUS, "suletud");
            }

        } catch (Exception e) {
            logWriter.close();
            dropLogFile(logFile);
            structError(errorsFile, e);
            status.incrFailed();
            throw new RuntimeException(e);
        } finally {
            reader.close();
            logWriter.close();
        }

        LOG.info("Structure import: processing rows completed at row " + reader.getCurrentRecord());
    }

    private Set<Privilege> getStructUnitAuthorityPrivileges(List<String> taskOwnerStructUnitAuthorityPrivileges) {
    	Set<Privilege> privileges = new HashSet<Privilege>();
    	if (taskOwnerStructUnitAuthorityPrivileges != null) {
    		for (String privilegeName: taskOwnerStructUnitAuthorityPrivileges) {
    			Privilege privilege = Privilege.getPrivilegeByName(privilegeName);
    			if (privilege != null) {
    				privileges.add(privilege);
    			}
    		}
    	}
    	if (!privileges.isEmpty()) {
    		return privileges;
    	} 
    	return null;
	}

	private static void validateStructureLine(CsvReader reader, ImportSettings settings) throws IOException, ImportValidationException {
        
        checkMandatory(reader, 8, 9, 10, 15, 16, 17, 19);
        if (StringUtils.isBlank(getString(reader, 11)) || StringUtils.isBlank(getString(reader, 12))) {
        	checkMandatory(reader, 13, 14);
        }
        if (StringUtils.isBlank(getString(reader, 13)) || StringUtils.isBlank(getString(reader, 14))) {
        	checkMandatory(reader, 11, 12);
        }
        checkInteger(reader, 8, 21);
        checkDate(reader, 19, 20);
        checkAnyOf(reader, VOLUME_TYPES, 15);
        checkAnyOf(reader, RESTRICTIONS, 22);
    }

    private NodeRef doFunction(CsvReader reader, ImportContext context, Map<Long, QName> propertyTypes) throws IOException {
    	String volumeStatus = (reader.getColumnCount() > 24)?getString(reader, 25):null;
    	Date volumeEndDate = getDate(reader, 20);
        
    	String mark = getString(reader, 9);
        String title = getString(reader, 10);

        NodeRef docsList = context.getDocumentListRef(volumeEndDate, volumeStatus);
        NodeRef function = context.getCachedFunction(docsList, mark + title);

        if (function == null) {
            for (ChildAssociationRef assoc : nodeService.getChildAssocs(docsList, FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION)) {
                if (mark.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.MARK))
                        && title.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.TITLE))) {
                    function = assoc.getChildRef();

                    Function f = functionsService.getFunction(function, propertyTypes);
                    if ("suletud".equals(f.getStatus())) {
                        functionsService.reopenFunction(f);

                        if (!context.getData().isVolumeOpen(volumeEndDate, volumeStatus)) {
                            context.closeFunction(function);
                        }
                    }

                    context.cacheFunction(mark + title, function);
                    break;
                }
            }
        }

        if (function == null) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(FunctionsModel.Props.ORDER, getInteger(reader, 8));
            props.put(FunctionsModel.Props.TITLE, title);
            props.put(FunctionsModel.Props.MARK, mark);
            props.put(FunctionsModel.Props.TYPE, mark.indexOf('.') > 0 ? "allfunktsioon" : "funktsioon");
            props.put(FunctionsModel.Props.STATUS, "avatud");

            function = nodeService.createNode(docsList, FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION, FunctionsModel.Types.FUNCTION, props)
                    .getChildRef();
            context.cacheFunction(mark + title, function);

            if (!context.getData().isVolumeOpen(volumeEndDate, volumeStatus)) {
                context.closeFunction(function);
            }
        }

        return function;
    }

    private NodeRef doSeries(CsvReader reader, final NodeRef function, ImportContext context) throws IOException, ImportValidationException {
        String volumeStatus = (reader.getColumnCount() > 24)?getString(reader, 25):null;
        String eventPlanName = (reader.getColumnCount() > 26)?getString(reader, 27):null;
        
    	Date volumeEndDate = getDate(reader, 20);
        String volumeType = VOLUME_TYPE_CODES[Arrays.binarySearch(VOLUME_TYPES, getString(reader, 15))];

        boolean seriesOpen = context.getData().isVolumeOpen(volumeEndDate, volumeStatus);
        boolean subseries = getString(reader, 13) != null;

        String seriesId = getString(reader, subseries ? 13 : 11);
        String title = getString(reader, subseries ? 14 : 12);

        NodeRef series = context.getCachedSeries(function, seriesId + title);

        if (series == null) {
            for (ChildAssociationRef assoc : nodeService.getChildAssocs(function, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES)) {
                if (seriesId.equals(nodeService.getProperty(assoc.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER))
                        && title.equals(nodeService.getProperty(assoc.getChildRef(), SeriesModel.Props.TITLE))) {
                    series = assoc.getChildRef();

                    context.cacheSeries(seriesId + title, series, function);
                    break;
                }
            }
        }

        NodeRef eventPlanRef = null;
        if (StringUtils.isNotBlank(eventPlanName)) {
        	eventPlanRef = getEventPlanRef(eventPlanName);
        	if (eventPlanRef == null) {
        		LOG.warn("event plan was not found; eventPlanName = " + eventPlanName + ", series.register = " + getRegisterId() + ", sereis.title = " + title); 
        	}
        }
        
        // ===============================
        // *** BEGIN check existing series
        if (series != null) {
            Series s = seriesService.getSeriesByNodeRef(series);
            if (seriesOpen && ("suletud".equals(s.getStatus()) || "hävitatud".equals(s.getStatus()))) {
                seriesService.openSeries(s);
            }

            NodeRef existingEventPlanRef = (NodeRef)nodeService.getProperty(series, SeriesModel.Props.EVENT_PLAN);
            if (existingEventPlanRef == null && eventPlanRef != null) {
        		nodeService.setProperty(series, SeriesModel.Props.EVENT_PLAN, eventPlanRef);
        	}
            
            List<String> volTypes = (List<String>) s.getNode().getProperties().get(SeriesModel.Props.VOL_TYPE);
            if (!volTypes.contains(volumeType)) {
                volTypes.add(volumeType);
                nodeService.setProperty(series, SeriesModel.Props.VOL_TYPE, (Serializable) volTypes);
            }

            Date restrictionBegin = getDate(reader, 19);
            Date nodeRestrictionBegin = (Date) nodeService.getProperty(series, SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
            if (restrictionBegin != null && (nodeRestrictionBegin == null || restrictionBegin.before(nodeRestrictionBegin))) {
                nodeService.setProperty(series, SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, restrictionBegin);
            }

            if (StringUtils.isNotEmpty(context.getTaskOwnerStructUnitAuthority()) && context.getTaskOwnerStructUnitAuthorityPrivileges() != null && !context.getTaskOwnerStructUnitAuthorityPrivileges().isEmpty()) {
                privilegeService.setPermissions(series, context.getTaskOwnerStructUnitAuthority(), context.getTaskOwnerStructUnitAuthorityPrivileges());
            }
        }

        // *** END check existing series
        // ===============================

        if (series == null) {
        	
            if (eventPlanRef == null) {
            	// for series from archived store check main store
            	NodeRef docsList = context.getDocumentListRef(volumeEndDate, volumeStatus);
            	if (docsList.equals(context.getArchDocsList())) {
            		eventPlanRef = getEventPlanRefFromGeneralStore(reader, context);
            	}
            }
        	
            boolean restrictionAK = false;

            if ("AK".equals(getString(reader, 22))) {
                checkMandatory(reader, 23);
                restrictionAK = true;
            }

            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(SeriesModel.Props.SERIES_IDENTIFIER, seriesId);
            props.put(SeriesModel.Props.TYPE, subseries ? "allsari" : "sari");
            props.put(SeriesModel.Props.TITLE, title);
            props.put(SeriesModel.Props.STATUS, seriesOpen ? "avatud" : "suletud");
            props.put(SeriesModel.Props.DOC_TYPE, (Serializable) context.getDocumentTypes());
            props.put(SeriesModel.Props.REGISTER, getRegisterId());
            props.put(SeriesModel.Props.DOC_NUMBER_PATTERN, "{S}/{DN}");
            props.put(SeriesModel.Props.VOL_TYPE, (Serializable) Collections.singletonList(volumeType));
            props.put(SeriesModel.Props.VOL_REGISTER, props.get(SeriesModel.Props.REGISTER));
            props.put(SeriesModel.Props.VOL_NUMBER_PATTERN, "{S}/{TN}");
            props.put(SeriesModel.Props.ACCESS_RESTRICTION, StringUtils.defaultIfEmpty(getString(reader, 22), "Avalik"));
            Integer retentionPeriod = getInteger(reader, 21);
            if (retentionPeriod != null && retentionPeriod > 0) {
                props.put(SeriesModel.Props.DESCRIPTION, "Säilitustähtaeg (migreeritud): " + retentionPeriod);
            }
            props.put(SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS, true);

            if (restrictionAK) {
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_REASON, getString(reader, 23));
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, getDate(reader, 19));
            }
            
        	if (eventPlanRef != null) {
        		props.put(SeriesModel.Props.EVENT_PLAN, eventPlanRef);
        	}

            series = nodeService.createNode(function, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES, props).getChildRef();
            seriesService.setSeriesDefaultPermissionsOnCreate(series);

            if (StringUtils.isNotEmpty(context.getTaskOwnerStructUnitAuthority())) {
                privilegeService.setPermissions(series, context.getTaskOwnerStructUnitAuthority(), Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
            }

            context.cacheSeries(seriesId + title, series, function);
        }

        return series;
    }
    
    private NodeRef getEventPlanRef(String name) {
    	ResultSet result = searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.generateTypeQuery(EventPlanModel.Types.EVENT_PLAN) 
                + " AND " 
                + SearchUtil.generatePropertyExactQuery(EventPlanModel.Props.NAME, name));
    	if (result != null && result.length() > 0) {
    		return result.getNodeRef(0);
    	}
    	
    	return null;
    }
    
    
    // gets eventPlan from the series with the same id and title but from general store (workspace space store)
    private NodeRef getEventPlanRefFromGeneralStore(CsvReader reader, ImportContext context) throws IOException {
    	String mark = getString(reader, 9);
        String title = getString(reader, 10);
        
        NodeRef docsList = context.getMainDocsList();
        NodeRef function = context.getCachedFunction(docsList, mark + title);
        if (function == null) {
            for (ChildAssociationRef assoc : nodeService.getChildAssocs(docsList, FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION)) {
                if (mark.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.MARK))
                        && title.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.TITLE))) {
                    
                	function = assoc.getChildRef();

                    context.cacheFunction(mark + title, function);
                    
                    break;
                }
            }
        }
        
        if (function != null) {
        	boolean subseries = getString(reader, 13) != null;
            String seriesId = getString(reader, subseries ? 13 : 11);
            String seriesTitle = getString(reader, subseries ? 14 : 12);

            NodeRef series = context.getCachedSeries(function, seriesId + seriesTitle);

            if (series == null) {
                for (ChildAssociationRef seriesAssoc : nodeService.getChildAssocs(function, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES)) {
                    if (seriesId.equals(nodeService.getProperty(seriesAssoc.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER))
                            && seriesTitle.equals(nodeService.getProperty(seriesAssoc.getChildRef(), SeriesModel.Props.TITLE))) {
                        series = seriesAssoc.getChildRef();

                        context.cacheSeries(seriesId + seriesTitle, series, function);
                        break;
                    }
                }
            }

            if (series != null) {
            	return (NodeRef) nodeService.getProperty(series, SeriesModel.Props.EVENT_PLAN);
            }
        }
        
        return null;

    }
    
    private String getStatus(String status, boolean isOpen) {
    	if (StringUtils.isNotBlank(status)) {
    		return status.toLowerCase();
    	}
    	return isOpen ? "avatud" : "suletud";
    }

    private NodeRef doVolume(CsvReader reader, final NodeRef series, ImportContext context) throws IOException {
    	String volumeStatus = (reader.getColumnCount() > 24)?getString(reader, 25):null;
        String volumeDescription = (reader.getColumnCount() > 25)?getString(reader, 26):null;
       
    	Date volumeEndDate = getDate(reader, 20);
        boolean volumeOpen = context.getData().isVolumeOpen(volumeEndDate, volumeStatus);

        String mark = getString(reader, 16);
        String title = getString(reader, 17);
        Date validFrom = getDate(reader, 19);
        NodeRef volume = null;

        List<ChildAssociationRef> existingVolumes = new ArrayList<ChildAssociationRef>();
        existingVolumes.addAll(nodeService.getChildAssocs(series, VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME));
        existingVolumes.addAll(nodeService.getChildAssocs(series, CaseFileModel.Assocs.CASE_FILE, CaseFileModel.Assocs.CASE_FILE));
        for (ChildAssociationRef assoc : existingVolumes) {
            if (mark.equals(nodeService.getProperty(assoc.getChildRef(), VolumeModel.Props.MARK))
                    && title.equals(nodeService.getProperty(assoc.getChildRef(), VolumeModel.Props.TITLE))
                    && validFrom.equals(nodeService.getProperty(assoc.getChildRef(), VolumeModel.Props.VALID_FROM))) {
                volume = assoc.getChildRef();
                break;
            }
        }

        if (volume == null) {
            String volumeType = VOLUME_TYPE_CODES[Arrays.binarySearch(VOLUME_TYPES, getString(reader, 15))];

            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.FUNCTION, nodeService.getPrimaryParent(series).getParentRef());
            props.put(DocumentCommonModel.Props.SERIES, series);
            props.put(VolumeModel.Props.MARK, mark);
            props.put(VolumeModel.Props.TITLE, title);
            props.put(VolumeModel.Props.VOLUME_TYPE, volumeType);
            props.put(VolumeModel.Props.VALID_FROM, validFrom);
            props.put(VolumeModel.Props.VALID_TO, volumeEndDate);
            props.put(VolumeModel.Props.STATUS, getStatus(volumeStatus, volumeOpen));
            props.put(VolumeModel.Props.CONTAINS_CASES, Boolean.FALSE);
            if (StringUtils.isNotBlank(volumeDescription)) {
            	props.put(VolumeModel.Props.DESCRIPTION, volumeDescription);
            }

            if (!VolumeType.CASE_FILE.name().equals(volumeType)) {

                if (volumeEndDate != null) {
                    Integer retentionPeriod = getInteger(reader, 21);
                    if (retentionPeriod == null) {
                        props.put(EventPlanModel.Props.RETAIN_UNTIL_DATE, volumeEndDate);
                    } else {
                        props.put(EventPlanModel.Props.RETAIN_UNTIL_DATE, DateUtils.addYears(volumeEndDate, retentionPeriod));
                    }
                }
                volume = nodeService.createNode(series, VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME, VolumeModel.Types.VOLUME, props).getChildRef();
            } else {
            	
            	String ownerId = findVolumeOwnerId(getString(reader, 24));
                if (ownerId == null) {
            		ownerId = context.getData().getDefaultOwnerId();
                }
                Map<QName, Serializable> userProps = userService.getUserProperties(ownerId);
                props.put(DocumentCommonModel.Props.OWNER_ID, ownerId);
                props.put(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(userProps));
                props.put(DocumentCommonModel.Props.OWNER_EMAIL, userProps.get(ContentModel.PROP_EMAIL));
                props.put(DocumentCommonModel.Props.OWNER_PHONE, userProps.get(ContentModel.PROP_TELEPHONE));
                props.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                props.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, userProps.get(ContentModel.PROP_ORGANIZATION_PATH));
                props.put(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, userProps.get(ContentModel.PROP_SERVICE_RANK));
                props.put(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, userProps.get(ContentModel.PROP_STREET_HOUSE));

                DocumentTypeVersion caseFileTypeVersion = context.getGeneralCaseFileTypeVersion();
                if (caseFileTypeVersion == null) {
                    CaseFileType caseFileType = documentAdminService.getCaseFileType("generalCaseFile",
                            DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
                    if (caseFileType != null) {
                        caseFileTypeVersion = caseFileType.getLatestDocumentTypeVersion();
                    }
                    if (caseFileTypeVersion == null) {
                        throw new RuntimeException(
                                "Could not retrieve latest version of case file type [generalCaseFile]. Please review the case file type name/settings before continuing.");
                    }
                    context.setGeneralCaseFileTypeVersion(caseFileTypeVersion);
                }

                CaseFile cf = caseFileService.createNewCaseFile(caseFileTypeVersion, series, false).getFirst();
                for (Entry<QName, Serializable> entry : props.entrySet()) {
                    cf.setProp(entry.getKey(), entry.getValue());
                }
                caseFileService.update(cf, null);
                volume = cf.getNodeRef();
            }

        }

        return volume;
    }

    private String findVolumeOwnerId(String goProUserName) {
    	String userName = null;
    	if (StringUtils.isNotBlank(goProUserName)) {
    		String userFullname = goProUserName;
    		if (goProUserName.contains("/")) {
    			userFullname = StringUtils.substringBefore(goProUserName, "/");
    		}
    		if (goProUserName.contains("(")) {
    			userFullname = StringUtils.substringBefore(goProUserName, "(").trim();
    		}
    		List<Node> userNodes = userService.searchUsers(userFullname, false, -1);
    		if (userNodes != null && userNodes.size() == 1) {
    			userName = (String) userNodes.get(0).getProperties().get(ContentModel.PROP_USERNAME);
    		}
    	}
		return userName;
	}

	private NodeRef doCase(CsvReader reader, final NodeRef volume) throws IOException {
        String title = getString(reader, 18);
        NodeRef caseRef = null;

        if (title != null && !VOLUME_TYPES[1].equals(getString(reader, 15))) {
            for (ChildAssociationRef assoc : nodeService.getChildAssocs(volume, CaseModel.Associations.CASE, CaseModel.Associations.CASE)) {
                if (title.equals(nodeService.getProperty(assoc.getChildRef(), CaseModel.Props.TITLE))) {
                    caseRef = assoc.getChildRef();
                    break;
                }
            }

            if (caseRef == null) {
                Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
                props.put(CaseModel.Props.TITLE, title);
                props.put(CaseModel.Props.STATUS, nodeService.getProperty(volume, VolumeModel.Props.STATUS));
                caseRef = nodeService.createNode(volume, CaseModel.Associations.CASE, CaseModel.Associations.CASE, CaseModel.Types.CASE, props).getChildRef();
            }
        }

        return caseRef;
    }

    private CsvWriter initLogWriter(File logFile) throws IOException {
        boolean fileNew = !logFile.exists();
        CsvWriter logWriter = ImportUtil.createLogWriter(logFile, true);

        if (fileNew) {
            logWriter.writeRecord(new String[] { "type", "volume_mark", "title", "old_url", "noderef", "valid_from", "valid_to", "function", "series", "seriesTitle" });
        }

        return logWriter;
    }

    private void logVolume(CsvWriter writer, NodeRef volume, String url, String functionRef, String seriesRef, String seriesTitle) throws IOException {
        if (CaseFileModel.Types.CASE_FILE.equals(nodeService.getType(volume))) {
            writer.write("caseFile");
        } else {
            writer.write("volume");
        }
        writer.write(nodeService.getProperty(volume, VolumeModel.Props.VOLUME_MARK).toString());
        writer.write(nodeService.getProperty(volume, VolumeModel.Props.TITLE).toString());
        writer.write(url);
        writer.write(volume.toString());
        writer.write(ImportUtil.formatDate((Date) nodeService.getProperty(volume, VolumeModel.Props.VALID_FROM)));
        writer.write(ImportUtil.formatDate((Date) nodeService.getProperty(volume, VolumeModel.Props.VALID_TO)));
        writer.write(functionRef);
        writer.write(seriesRef);
        writer.write(seriesTitle);
        writer.endRecord();
    }

    private void logCase(CsvWriter writer, NodeRef caseRef, String url, String functionRef, String seriesRef, String seriesTitle) throws IOException {
        NodeRef volume = nodeService.getPrimaryParent(caseRef).getParentRef();

        writer.write("case");
        writer.write("");
        writer.write(nodeService.getProperty(caseRef, CaseModel.Props.TITLE).toString());
        writer.write(url);
        writer.write(caseRef.toString());
        writer.write(ImportUtil.formatDate((Date) nodeService.getProperty(volume, VolumeModel.Props.VALID_FROM)));
        writer.write(ImportUtil.formatDate((Date) nodeService.getProperty(volume, VolumeModel.Props.VALID_TO)));
        writer.write(functionRef);
        writer.write(seriesRef);
        writer.write(seriesTitle);
        writer.endRecord();
    }

    private int getRegisterId() {
        if (registerId == null) {
            List<Register> registers = registerService.getRegisters();
            for (Register register : registers) {
                if (REGISTER_NAME.equals(register.getName())) {
                    registerId = register.getId();
                    break;
                }
            }
            if (registerId == null) {
                Node register = registerService.createRegister();
                Map<String, Object> props = register.getProperties();
                props.put(RegisterModel.Prop.NAME.toString(), REGISTER_NAME);
                props.put(RegisterModel.Prop.COUNTER.toString(), 0);
                props.put(RegisterModel.Prop.AUTO_RESET.toString(), Boolean.FALSE);
                registerService.updateProperties(register);
                registerId = (Integer) props.get(RegisterModel.Prop.ID.toString());
            }
        }
        return registerId;
    }

    private static class SeriesIdentifierComparator implements Comparator<String> {

        private static final SeriesIdentifierComparator INSTANCE = new SeriesIdentifierComparator();

        @Override
        public int compare(String id1, String id2) {
            if (id1 == null && id2 == null || id1 != null && id1.equals(id2)) {
                return 0;
            } else if (id1 == null || "".equals(id1)) {
                return 1;
            } else if (id2 == null || "".equals(id2)) {
                return -1;
            }

            int dash1 = id1.indexOf('-');
            int dash2 = id2.indexOf('-');

            String fn1 = dash1 < 1 ? id1 : id1.substring(0, dash1);
            String fn2 = dash2 < 1 ? id2 : id2.substring(0, dash2);

            if (dash1 == -1 || dash2 == -1 || !fn1.equals(fn2)) {
                return compareNums(fn1, fn2);
            }

            return compareNums(id1.substring(dash1 + 1), id2.substring(dash2 + 1));
        }

        private static int compareNums(String num1, String num2) {
            if (num1.equals(num2)) {
                return 0;
            } else if ("".equals(num1)) {
                return 1;
            } else if ("".equals(num2)) {
                return -1;
            }
            try {
                return Float.compare(Float.valueOf(num1), Float.valueOf(num2));
            } catch (NumberFormatException e) {
                return num1.compareTo(num2);
            }
        }
    }
}
