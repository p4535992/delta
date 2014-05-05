package ee.webmedia.alfresco.sharepoint;

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
import static ee.webmedia.alfresco.sharepoint.ImportUtil.checkAnyOf;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.checkDate;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.checkInteger;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.checkMandatory;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.docsError;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.dropLogFile;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getDate;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getInteger;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getString;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.structError;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
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
 * Imports structure (functions, series, volumes and cases) from JuM Sharepoint.
 */
public class StructureImporter {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(StructureImporter.class);

    private static final String SOURCE_FILENAME = "struktuur.csv";
    static final String COMPLETED_FILENAME = "completed_volumes_cases.csv";
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

        List<Function> functions = functionsService.getAllFunctions();
        functions.addAll(archivalsService.getArchivedFunctions());

        for (Function f : functions) {
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

        if (logFile.exists()) {
            LOG.info(COMPLETED_FILENAME + " was found in work folder. Skipping structure import");
            return;
        }

        if (!sourceFile.exists()) {
            structError(errorsFile, "struktuur.csv cannot be found");
            return;
        }

        try {
            if (userService.getPerson(data.getDefaultOwnerId()) == null) {
                throw new ImportValidationException("Default user with id = " + data.getDefaultOwnerId() + " does not exist");
            }
        } catch (ImportValidationException e) {
            docsError(errorsFile, e);
            throw new RuntimeException(e);
        }

        NodeRef mainDocsList = generalService.getNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE, generalService.getStore());
        NodeRef archDocsList = generalService.getNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE, generalService.getArchivalsStoreRef());
        List<String> documentTypes = new ArrayList<String>(documentAdminService.getDocumentTypeNames(Boolean.TRUE).keySet());

        String taskOwnerStructUnitAuthority = null;
        if (StringUtils.isNotBlank(data.getTaskOwnerStructUnit())) {
            List<String> authorities = documentSearchService.searchAuthorityGroupsByExactName(data.getTaskOwnerStructUnit());
            if (authorities.size() != 1) {
                structError(errorsFile, "For authorityDisplayName '" + data.getTaskOwnerStructUnit() + "' found " + authorities.size() + " groups with authorityName values: "
                        + authorities);
                return;
            }
            taskOwnerStructUnitAuthority = authorities.get(0);
            LOG.info("For authorityDisplayName '" + data.getTaskOwnerStructUnit() + "' found group with authorityName '" + taskOwnerStructUnitAuthority + "'");
        }

        ImportContext context = new ImportContext(mainDocsList, archDocsList, documentTypes, data, taskOwnerStructUnitAuthority);

        CsvReader reader = ImportUtil.createDataReader(sourceFile);

        boolean canContinue = reader.readHeaders() && reader.readRecord();
        int colCount = data.isRiigikohusOrigin() ? 24 : 33;

        if (canContinue && reader.getColumnCount() != colCount) {
            structError(errorsFile, "Expected " + colCount + " columns, got " + reader.getColumnCount());
            reader.close();
            return;
        }

        LOG.info("Structure import: preparations done; starting to import...");

        CsvWriter logWriter = initLogWriter(logFile);

        try {
            while (canContinue) {
                if (reader.getCurrentRecord() % 500 == 0) {
                    LOG.info("Structure import reached row " + reader.getCurrentRecord());
                }

                status.incrCount();

                validateStructureLine(reader, data);

                NodeRef function = doFunction(reader, context);
                NodeRef series = doSeries(reader, function, context);
                NodeRef volume = doVolume(reader, series, context);
                NodeRef caseRef = doCase(reader, volume);

                // Record logging:

                String functionRef = (String) nodeService.getProperty(function, FunctionsModel.Props.MARK);
                String seriesRef = (String) nodeService.getProperty(series, SeriesModel.Props.SERIES_IDENTIFIER);
                String url = data.isRiigikohusOrigin() ? null : getString(reader, data.isSharepointOrigin() ? 33 : 1);

                if (caseRef == null) {
                    logVolume(logWriter, volume, url, functionRef, seriesRef);
                } else {
                    logCase(logWriter, caseRef, url, functionRef, seriesRef);
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

    private static void validateStructureLine(CsvReader reader, ImportSettings settings) throws IOException, ImportValidationException {
        if (settings.isSharepointOrigin()) {
            checkMandatory(reader, 33);
        } else if (settings.isAmphoraOrigin()) {
            checkMandatory(reader, 1);
        }
        checkMandatory(reader, 8, 9, 10, 11, 12, 15, 16, 17, 19, 21);
        checkInteger(reader, 8, 21);
        checkDate(reader, 19, 20);
        checkAnyOf(reader, VOLUME_TYPES, 15);
        checkAnyOf(reader, RESTRICTIONS, 22);
    }

    private NodeRef doFunction(CsvReader reader, ImportContext context) throws IOException {
        Date volumeEndDate = getDate(reader, 20);
        String mark = getString(reader, 9);
        String title = getString(reader, 10);

        NodeRef docsList = context.getDocumentListRef(volumeEndDate);
        NodeRef function = context.getCachedFunction(docsList, mark + title);

        if (function == null) {
            for (ChildAssociationRef assoc : nodeService.getChildAssocs(docsList, FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION)) {
                if (mark.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.MARK))
                        && title.equals(nodeService.getProperty(assoc.getChildRef(), FunctionsModel.Props.TITLE))) {
                    function = assoc.getChildRef();

                    Function f = functionsService.getFunctionByNodeRef(function);
                    if ("suletud".equals(f.getStatus())) {
                        functionsService.reopenFunction(f);

                        if (!context.getData().isVolumeOpen(volumeEndDate)) {
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

            if (!context.getData().isVolumeOpen(volumeEndDate)) {
                context.closeFunction(function);
            }
        }

        return function;
    }

    private NodeRef doSeries(CsvReader reader, final NodeRef function, ImportContext context) throws IOException, ImportValidationException {
        Date volumeEndDate = getDate(reader, 20);
        String volumeType = VOLUME_TYPE_CODES[Arrays.binarySearch(VOLUME_TYPES, getString(reader, 15))];

        boolean seriesOpen = context.getData().isVolumeOpen(volumeEndDate);
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

        // ===============================
        // *** BEGIN check existing series

        if (series != null) {
            Series s = seriesService.getSeriesByNodeRef(series);
            if (seriesOpen && "suletud".equals(s.getStatus())) {
                seriesService.openSeries(s);
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

            if (StringUtils.isNotEmpty(context.getTaskOwnerStructUnitAuthority())) {
                privilegeService.setPermissions(series, context.getTaskOwnerStructUnitAuthority(), Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
            }
        }

        // *** END check existing series
        // ===============================

        if (series == null) {
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

            series = nodeService.createNode(function, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES, props).getChildRef();
            seriesService.setSeriesDefaultPermissionsOnCreate(series);

            if (StringUtils.isNotEmpty(context.getTaskOwnerStructUnitAuthority())) {
                privilegeService.setPermissions(series, context.getTaskOwnerStructUnitAuthority(), Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
            }

            context.cacheSeries(seriesId + title, series, function);
        }

        return series;
    }

    private NodeRef doVolume(CsvReader reader, final NodeRef series, ImportContext context) throws IOException {
        Date volumeEndDate = getDate(reader, 20);
        boolean volumeOpen = context.getData().isVolumeOpen(volumeEndDate);

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
            props.put(VolumeModel.Props.STATUS, volumeOpen ? "avatud" : "suletud");
            props.put(VolumeModel.Props.CONTAINS_CASES, Boolean.FALSE);

            if (!VolumeType.CASE_FILE.name().equals(volumeType)) {
                props.put(VolumeModel.Props.CONTAINS_CASES, context.getData().isSharepointOrigin());
                props.put(VolumeModel.Props.CASES_CREATABLE_BY_USER, context.getData().isSharepointOrigin());

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
                String ownerId = context.getData().getDefaultOwnerId();
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
            logWriter.writeRecord(new String[] { "type", "volume_mark", "title", "old_url", "noderef", "valid_from", "valid_to", "function", "series" });
        }

        return logWriter;
    }

    private void logVolume(CsvWriter writer, NodeRef volume, String url, String functionRef, String seriesRef) throws IOException {
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
        writer.endRecord();
    }

    private void logCase(CsvWriter writer, NodeRef caseRef, String url, String functionRef, String seriesRef) throws IOException {
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
