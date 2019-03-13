package ee.webmedia.alfresco.archivals.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.event.ActionEvent;

import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.node.integrity.IntegrityChecker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.util.Assert;

import com.ociweb.xml.Version;
import com.ociweb.xml.WAX;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.archivals.model.*;
import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.destruction.model.DestructionJobStatus;
import ee.webmedia.alfresco.destruction.model.DestructionModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.DeletionType;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class ArchivalsServiceImpl implements ArchivalsService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArchivalsServiceImpl.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private CopyService copyService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private FunctionsService functionsService;
    private DocumentSearchService documentSearchService;
    private DictionaryService dictionaryService;
    private AdrService adrService;
    private DocumentService documentService;
    private CaseService caseService;
    private DocumentAssociationsService documentAssociationsService;
    private FileService fileService;
    private LogService logService;
    private DocumentDynamicService documentDynamicService;
    private DocumentConfigService documentConfigService;
    private FileFolderService fileFolderService;
    private TransactionService transactionService;
    private DocumentTemplateService documentTemplateService;
    private UserService userService;
    private ContentService contentService;
    private ParametersService parametersService;
    private BulkLoadNodeService bulkLoadNodeService;

    private final AtomicBoolean progressTrigger = new AtomicBoolean(false);
    
    private final AtomicBoolean archivingInProgress = new  AtomicBoolean(false);
    private final AtomicBoolean archivingPaused = new AtomicBoolean(false);
    private final AtomicBoolean archivingContinuedManually = new AtomicBoolean(false);

    private final AtomicBoolean destructingInProgress = new  AtomicBoolean(false);
    private final AtomicBoolean destructingPaused = new AtomicBoolean(false);
    private final AtomicBoolean destructingContinuedManually = new AtomicBoolean(false);

    private boolean simpleDestructionEnabled;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat DATE_SHORT_FORMAT = FastDateFormat.getInstance("yyyyMMdd");
    private static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

    private static final Set<QName> CWF_PROPS_TO_LOAD = new HashSet<>(Arrays.asList(WorkflowCommonModel.Props.TYPE, WorkflowCommonModel.Props.MAIN_DOCUMENT));

    public void init() {
    	if (getDestructionsSpaceRef() == null) {
    		return;
    	}
    	
    	Boolean paused = (Boolean) nodeService.getProperty(getDestructionsSpaceRef(), DestructionModel.Props.DESTRUCTION_PAUSED);	   
    	if (paused != null && Boolean.TRUE.equals(paused)) {
    		destructingPaused.set(true);
    	}
    	else {
    		restorePausedStoppedActivitiesStatus();
    	}
    	
    }
    
    @Override
    public void exportToUam(final List<NodeRef> volumes, final Date exportStartDate, final NodeRef activityRef) {
        generalService.runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
                final ContentWriter writer = retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<ContentWriter>() {

                    @Override
                    public ContentWriter execute() throws Throwable {
                        return exportToUam(contentService.getWriter(null, null, false), new HashSet<NodeRef>(volumes));
                    }

                }, true);

                for (final NodeRef nodeRef : volumes) {
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                            props.put(EventPlanModel.Props.EXPORTED_FOR_UAM, Boolean.TRUE);
                            props.put(EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME, exportStartDate);
                            nodeService.addProperties(nodeRef, props);
                            logService.addLogEntry(LogEntry.create(isCaseFile(nodeRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, nodeRef,
                                    "applog_archivals_volume_exported_to_uam_xml"));
                            return null;
                        }

                    }, false, true);
                }

                retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {

                    @Override
                    public NodeRef execute() throws Throwable {
                        NodeRef fileRef = fileFolderService.create(activityRef, "UAM" + DATE_SHORT_FORMAT.format(exportStartDate) + ".xml", ContentModel.TYPE_CONTENT).getNodeRef();
                        Map<QName, Serializable> fileProps = new HashMap<QName, Serializable>(2);
                        fileProps.put(FileModel.Props.ACTIVITY_FILE_TYPE, ActivityFileType.UAM_XML.name());
                        fileProps.put(ContentModel.PROP_CONTENT, writer.getContentData());
                        fileProps.put(ContentModel.PROP_CONTENT_NOT_INDEXED, true);
                        nodeService.addProperties(fileRef, fileProps);
                        nodeService.setProperty(activityRef, ArchivalsModel.Props.STATUS, ActivityStatus.FINISHED.getValue());

                        return fileRef;
                    }

                }, false, true);

                return null;
            }
        }, "volumeExportToUam", false);
    }

    private boolean isCaseFile(final NodeRef nodeRef) {
        return CaseFileModel.Types.CASE_FILE.equals(nodeService.getType(nodeRef));
    }

    private ContentWriter exportToUam(ContentWriter writer, Set<NodeRef> volumeRefs) throws Exception {
        List<String> ignoredFields = Arrays.asList(
                DocumentCommonModel.Props.DOC_NAME.getLocalName(), DocumentCommonModel.Props.DOC_STATUS.getLocalName(), DocumentCommonModel.Props.REG_NUMBER.getLocalName(),
                DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(), DocumentCommonModel.Props.INDIVIDUAL_NUMBER.getLocalName(),
                DocumentCommonModel.Props.REG_DATE_TIME.getLocalName(), DocumentCommonModel.Props.FUNCTION.getLocalName(), DocumentCommonModel.Props.SERIES.getLocalName(),
                DocumentCommonModel.Props.VOLUME.getLocalName(), DocumentCommonModel.Props.ACCESS_RESTRICTION.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.getLocalName(), DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON.getLocalName(), DocumentCommonModel.Props.KEYWORDS.getLocalName(),
                DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName(),
                DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName(), DocumentCommonModel.Props.OWNER_ID.getLocalName(),
                DocumentCommonModel.Props.PREVIOUS_OWNER_ID.getLocalName(),
                DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(),
                VolumeModel.Props.VOLUME_MARK.getLocalName(), DocumentDynamicModel.Props.TITLE.getLocalName(), DocumentDynamicModel.Props.VALID_FROM.getLocalName(),
                DocumentDynamicModel.Props.VALID_TO.getLocalName(),
                DocumentDynamicModel.Props.STATUS.getLocalName(), DocumentCommonModel.Props.CASE.getLocalName());
        List<String> ignoredGroups = Arrays.asList(SystematicFieldGroupNames.ACCESS_RESTRICTION, SystematicFieldGroupNames.REGISTRATION_DATA,
                SystematicFieldGroupNames.CASE_VOLUME_VALID, SystematicFieldGroupNames.CASE_VOLUME_LOCATION);

        OutputStreamWriter out = null;
        writer.setMimetype(MimetypeMap.MIMETYPE_XML);
        writer.setEncoding("UTF-8");

        try {
            out = new OutputStreamWriter(writer.getContentOutputStream(), "UTF-8");
            WAX x = new WAX(out, Version.V1_0);
            x.start("UAM_import").defaultNamespace("http://www.ra.ee/schemas/EDHS/import_v0.1").namespace("dul", "http://www.nortal.com/schemas/delta/delta_uam_lisaandmed")
            .start("Arhiivikirjeldus");

            writeVolumes(x, volumeRefs, ignoredFields, ignoredGroups);

            x.end(); // Arhiivikirjeldus

            for (NodeRef volumeRef : volumeRefs) {
                // Export volume documents
                List<NodeRef> documents = documentSearchService.searchAllDocumentRefsByParentRef(volumeRef);
                writeDocuments(x, documents, ignoredFields, ignoredGroups);
            }
            x.close(); // UAM_import
        } finally {
            IOUtils.closeQuietly(out);
        }

        return writer;
    }

    private void writeVolumes(WAX x, Set<NodeRef> volumeRefs, List<String> ignoredFields, List<String> ignoredGroups) {
        Set<NodeRef> seriesToExport = new HashSet<NodeRef>();
        for (NodeRef volumeRef : volumeRefs) {
            Map<QName, Serializable> vol = nodeService.getProperties(volumeRef);

            NodeRef seriesRef = generalService.getAncestorNodeRefWithType(volumeRef, SeriesModel.Types.SERIES);
            Map<QName, Serializable> ser = nodeService.getProperties(seriesRef);
            seriesToExport.add(seriesRef);

            boolean isCaseFile = isCaseFile(volumeRef);

            Serializable volValidFrom = vol.get(VolumeModel.Props.VALID_FROM);
            Serializable volValidTo = vol.get(VolumeModel.Props.VALID_TO);
            x.start("Kirjeldusyksus")
            .child("KyTasand", "toimik")
            .start("KyIdentiteediala")
            .child("KyViit", (String) vol.get(VolumeModel.Props.VOLUME_MARK))
            .child("KyIdEDHS", volumeRef.getId())
            .child("KyVanemViit", (String) ser.get(SeriesModel.Props.SERIES_IDENTIFIER))
            .child("KyVanemIdEDHS", seriesRef.getId())
            .child("KyPealkiri", (String) vol.get(VolumeModel.Props.TITLE))
            .start("KyAeg")
            .start("Algus")
            .child("Tyyp", "kuupäev")
            .child("Tapsus", "true")
            .child("Vaartus", (volValidFrom == null ? "" : DATE_FORMAT.format(volValidFrom)))
            .end() // Algus
            .start("Lopp")
            .child("Tyyp", "kuupäev")
            .child("Tapsus", "true")
            .child("Vaartus", (volValidTo == null ? "" : DATE_FORMAT.format(volValidTo)))
            .end() // Lopp
            .end() // KyAeg
            .end() // KyIdentiteediala

            .start("KySisuStruktAla");
            if (isCaseFile) {
                writeKeywords(x, vol);
                writeAdditionalProperties(x, new Node(volumeRef), vol, ignoredFields, ignoredGroups);
            }
            x.end(); // KySisuStruktAla
            if (isCaseFile) {
                writeAccessRestriction(x, vol);
            }

            writeAssociations(x, new Node(volumeRef));

            LogFilter filter = new LogFilter();
            filter.setObjectId(Arrays.asList(volumeRef.toString()));
            List<LogEntry> logEntries = logService.getLogEntries(filter);
            for (LogEntry entry : logEntries) {
                if (!filterVolumeLog(entry)) {
                    continue;
                }
                writeLogEntry(x, entry);
            }

            x.end(); // Kirjeldusyksus
        }

        writeSeries(x, seriesToExport);
    }

    private boolean filterVolumeLog(LogEntry entry) {
        String description = entry.getEventDescription();
        if (StringUtils.isBlank(description)) {
            return false;
        }

        if (description.startsWith("Liigitusüksuse ") && description.endsWith(" loomine")
                || description.equals("Asjatoimiku loomine")
                || description.equals("Asjatoimiku sulgemine")
                || description.equals("Asjatoimiku taasavamine")
                || description.startsWith("Asjatoimiku asukohta on muudetud.")) {

            return true;
        }

        return false;
    }

    private void writeSeries(WAX x, Set<NodeRef> seriesRefs) {
        // Export parent series
        Set<NodeRef> functionRefs = new HashSet<NodeRef>();
        for (NodeRef seriesRef : seriesRefs) {
            Map<QName, Serializable> ser = nodeService.getProperties(seriesRef);
            NodeRef functionRef = generalService.getAncestorNodeRefWithType(seriesRef, FunctionsModel.Types.FUNCTION);
            functionRefs.add(functionRef);
            Map<QName, Serializable> fun = nodeService.getProperties(functionRef);

            Serializable serValidFrom = ser.get(SeriesModel.Props.VALID_FROM_DATE);
            x.start("Kirjeldusyksus").child("KyTasand", (String) ser.get(SeriesModel.Props.TYPE))
            .start("KyIdentiteediala")
            .child("KyViit", (String) ser.get(SeriesModel.Props.SERIES_IDENTIFIER))
            .child("KyIdEDHS", seriesRef.getId())
            .child("KyVanemViit", (String) fun.get(FunctionsModel.Props.MARK))
            .child("KyVanemIdEDHS", functionRef.getId())
            .child("KyPealkiri", (String) ser.get(SeriesModel.Props.TITLE))
            .start("KyAeg")
            .start("Algus")
            .child("Tyyp", "kuupäev")
            .child("Tapsus", "true")
            .child("Vaartus", (serValidFrom == null ? "" : DATE_FORMAT.format(serValidFrom)))
            .end(); // Algus
            Date end = (Date) ser.get(SeriesModel.Props.VALID_TO_DATE);
            if (end != null) {
                x.start("Lopp")
                .child("Tyyp", "kuupäev")
                .child("Tapsus", "true")
                .child("Vaartus", DATE_FORMAT.format(end))
                .end(); // Lopp
            }
            x.end() // KyAeg
            .end() // KyIdentiteediAla
            .start("KySisuStruktAla")
            .child("KyTaienemine", (DocListUnitStatus.OPEN.getValueName().equals(ser.get(SeriesModel.Props.STATUS)) ? "true" : "false"))
            .end(); // KySisuStruktAla

            LogFilter filter = new LogFilter();
            filter.setObjectId(Arrays.asList(seriesRef.toString()));
            List<LogEntry> logEntries = logService.getLogEntries(filter);
            for (LogEntry entry : logEntries) {
                if (!filterSeriesLog(entry)) {
                    continue;
                }
                writeLogEntry(x, entry);
            }
            x.end(); // Kirjeldusyksus
        }

        // Export parent function
        writeFunctions(x, functionRefs);

    }

    private boolean filterSeriesLog(LogEntry entry) {
        String description = entry.getEventDescription();
        if (StringUtils.isBlank(description)) {
            return false;
        }

        if (description.startsWith("Liigitusüksuse ") && description.endsWith(" loomine")) {
            return true;
        }

        return false;
    }

    private void writeLogEntry(WAX x, LogEntry entry) {
        x.start("KyTegevus");
        x.child("TegevusNimetus", entry.getEventDescription())
        .child("TeostajaNimi", entry.getCreatorName())
        .child("TegevusAeg", DATE_TIME_FORMAT.format(entry.getCreatedDateTime()));
        x.end(); // KyTegevus
    }

    private void writeFunctions(WAX x, Set<NodeRef> functionRefs) {
        for (NodeRef nodeRef : functionRefs) {
            Map<QName, Serializable> fun = nodeService.getProperties(nodeRef);
            x.start("Funktsioon")
            .child("FunktsioonNimi", (String) fun.get(FunctionsModel.Props.TITLE))
            .child("FunktsioonViit", (String) fun.get(FunctionsModel.Props.MARK))
            .child("FunktsioonIdEDHS", nodeRef.getId())
            .child("Kirjeldus", (String) fun.get(FunctionsModel.Props.DESCRIPTION))
            .child("Volitus")
            .end(); // Funktsioon
        }
    }

    private void writeDocuments(WAX x, List<NodeRef> documentRefs, List<String> ignoredFields, List<String> ignoredGroups) throws Exception {
        for (NodeRef docRef : documentRefs) {
            DocumentDynamic doc = documentDynamicService.getDocument(docRef);
            Map<QName, Serializable> docProps = RepoUtil.toQNameProperties(doc.getNode().getProperties());
            NodeRef volumeRef = generalService.getAncestorNodeRefWithType(doc.getNodeRef(), SeriesModel.Types.SERIES);

            x.start("Arhivaal")
            .start("KyMeta")

            .start("KyIdentiteediala")
            .child("KyViit", doc.getRegNumber())
            .child("KyIdEDHS", doc.getNodeRef().getId())
            .child("KyVanemViit", (String) nodeService.getProperty(volumeRef, VolumeModel.Props.VOLUME_MARK))
            .child("KyVanemIdEDHS", volumeRef.getId())
            .child("KyPealkiri", doc.getDocName())

            .start("KyAeg")
            .child("Tyyp", "kuupäev")
            .child("Tapsus", "true")
            .child("Vaartus", DATE_TIME_FORMAT.format((doc.getRegDateTime() != null ? doc.getRegDateTime() : doc.getProp(ContentModel.PROP_CREATED))))
            .end() // KyAeg

            .child("KyMuutmiseAeg", DATE_TIME_FORMAT.format(doc.getNode().getProperties().get(ContentModel.PROP_MODIFIED)))

            .end() // KyIdentiteediala

            .start("KySisuStruktAla")
            .child("KyLiik", documentDynamicService.getDocumentTypeName(doc.getNodeRef()));
            writeKeywords(x, docProps);
            x.end(); // KySisuStruktAla

            writeAccessRestriction(x, docProps);
            writeAssociations(x, doc.getNode());
            writeAdditionalProperties(x, doc.getNode(), docProps, ignoredFields, ignoredGroups);

            LogFilter filter = new LogFilter();
            filter.setObjectId(Arrays.asList(docRef.toString()));
            List<LogEntry> logEntries = logService.getLogEntries(filter);
            for (LogEntry entry : logEntries) {
                if (!filterDocumentLog(entry)) {
                    continue;
                }
                writeLogEntry(x, entry);
            }

            x.end(); // KyMeta

            writeFiles(x, doc.getNodeRef());

            x.end(); // Arhivaal
        }

    }

    private boolean filterDocumentLog(LogEntry entry) {
        String description = entry.getEventDescription();
        if (StringUtils.isBlank(description)) {
            return false;
        }

        if (description.equals("Dokumendi loomine")
                || description.startsWith("Dokumendi importimine (DVK)")
                || description.equals("Dokumendi importimine (IMAP)")
                || description.equals("Dokument on registreeritud")
                || description.startsWith("Dokumendi juurdepääsupiirangut on muudetud.")
                || description.startsWith("Dokumendi asukohta on muudetud.")
                || description.startsWith("Dokumendi vastuvõtmine")) {
            return true;
        }

        return false;
    }

    private void writeFiles(WAX x, NodeRef docRef) throws Exception {
        List<ee.webmedia.alfresco.document.file.model.File> activeFiles = fileService.getAllActiveFiles(docRef);
        for (ee.webmedia.alfresco.document.file.model.File file : activeFiles) {
            DigestInputStream is = null;
            InputStreamReader reader = null;
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                x.start("Fail")
                .child("FailIdent", file.getNodeRef().getId())
                .child("FailNimi", file.getDisplayName())
                .child("FailSuurus", Long.toString(file.getSize()))
                .start("FailBase64");
                is = new DigestInputStream(fileService.getFileContentInputStream(file.getNodeRef()), md5);
                x.unescapedText("<![CDATA[");
                reader = new InputStreamReader(new org.apache.commons.codec.binary.Base64InputStream(is, true, 0, null), "UTF-8");
                char[] buffer = new char[4096];
                int readResult = reader.read(buffer);
                while (-1 != readResult) {
                    x.unescapedText(new String(buffer));
                    readResult = reader.read(buffer);
                }
                x.unescapedText("]]>");
                x.end() // FailBase64
                .child("FailViide")
                .child("FailLoplik", "true")
                .child("FailOriginaal", "true")
                .child("FailArhiivivormingus", (file.isPdf() ? "true" : "false"))
                .child("FailKasutuskoopia", "false")
                .start("Rasi")
                .child("RasiVaartus", Hex.encodeHexString(md5.digest()))
                .child("RasiAlgoritm", md5.getAlgorithm())
                .child("RasiAeg", DATE_TIME_FORMAT.format(new Date()))
                .end().end(); // Rasi and Fail
            } catch (Exception e) {
                LOG.error("Error occurred when exporting file " + file.getNodeRef(), e);
                throw e;
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(is);
            }
        }
    }

    private void writeAdditionalProperties(WAX x, Node node, Map<QName, Serializable> props, List<String> ignoredFields, List<String> ignoredGroups) {
        x.start("KyMetaLiik");

        x.start("dul", "deltaLisaAndmed");

        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = documentConfigService.getPropertyDefinitions(node);
        Set<String> processedFields = new HashSet<String>();
        Set<String> processedGroups = new HashSet<String>();
        for (Pair<DynamicPropertyDefinition, Field> pair : propertyDefinitions.values()) {
            DynamicPropertyDefinition propDef = pair.getFirst();
            Field field = pair.getSecond();
            if (field == null || ignoredFields.contains(field.getFieldId()) || processedFields.contains(field.getFieldId())) {
                continue;
            }

            BaseObject parent = field.getParent();
            DataTypeDefinition dataType = propDef.getDataType();
            if (parent instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) parent;
                String name = group.getName();
                if (ignoredGroups.contains(name) || processedGroups.contains(name)) {
                    continue;
                }
                ChildrenList<Field> fields = group.getFields();
                x.start("dul", "andmevaljadeGrupp").attr("dul", "grNimetus", group.getName());
                for (Field f : fields) {
                    DynamicPropertyDefinition groupPropDefinition = propertyDefinitions.get(f.getFieldId()).getFirst();
                    writeField(x, props.get(f.getQName()), groupPropDefinition, f, groupPropDefinition.getDataType());
                }
                x.end(); // andmevaljadeGrupp
                // add to processed fields
                processedFields.add(field.getFieldId());
                processedGroups.add(name);
                continue;
            }

            writeField(x, props.get(field.getQName()), propDef, field, dataType);
        }
        x.end().end(); // deltaLisaAndmed and KyMetaliik
    }

    private void writeField(WAX x, Serializable serializableValue, PropertyDefinition propDef, Field field, DataTypeDefinition dataType) {
        DataTypeDefinition textData = dictionaryService.getDataType(DataTypeDefinition.TEXT);
        String avTyyp = null;
        QName dataTypeName = dataType.getName();
        if (DataTypeDefinition.DATE.equals(dataTypeName)) {
            avTyyp = "xs:date";
        } else if (DataTypeDefinition.BOOLEAN.equals(dataTypeName)) {
            avTyyp = "xs:boolean";
        } else if (DataTypeDefinition.DOUBLE.equals(dataTypeName)) {
            avTyyp = "xs:double";
        } else if (DataTypeDefinition.LONG.equals(dataTypeName)) {
            avTyyp = "xs:long";
        } else if (DataTypeDefinition.TEXT.equals(dataTypeName)) {
            avTyyp = "xs:string";
        }

        if (propDef.isMultiValued() && serializableValue instanceof List) {
            x.start("dul", "andmevaliMV").attr("dul", "avKood", field.getFieldId()).attr("dul", "avNimetus", field.getName()).attr("dul", "avTyyp", avTyyp);
            @SuppressWarnings("unchecked")
            List<Serializable> value = (List<Serializable>) serializableValue;
            for (Serializable val : value) {
                if (val != null && "date".equals(dataTypeName.getLocalName())) {
                    x.child("dul", "vaartus", DATE_FORMAT.format(val));
                } else {
                    x.child("dul", "vaartus", (String) DefaultTypeConverter.INSTANCE.convert(textData, val));
                }
            }
            x.end(); // andmevaliMV
        } else if (serializableValue instanceof List) {
            x.start("dul", "andmevali").attr("dul", "avKood", field.getFieldId()).attr("dul", "avNimetus", field.getName()).attr("dul", "avTyyp", avTyyp);
            @SuppressWarnings("unchecked")
            List<Serializable> value = (List<Serializable>) serializableValue;
            for (Serializable val : value) {
                if (val != null && "date".equals(dataTypeName.getLocalName())) {
                    x.child("dul", "vaartus", DATE_FORMAT.format(val));
                } else {
                    x.child("dul", "vaartus", (String) DefaultTypeConverter.INSTANCE.convert(textData, val));
                }
            }
            x.end(); // andmevali list
        } else if (serializableValue != null) {
            String val = "date".equals(dataTypeName.getLocalName()) ? DATE_FORMAT.format(serializableValue) : (String) DefaultTypeConverter.INSTANCE.convert(textData,
                    serializableValue);
            x.start("dul", "andmevali").attr("dul", "avKood", field.getFieldId()).attr("dul", "avNimetus", field.getName()).attr("dul", "avTyyp", avTyyp).text(val).end();
        }
    }

    private void writeAssociations(WAX wax, Node node) {
        wax.start("KySeonduvAines");
        List<DocAssocInfo> assocInfos = documentAssociationsService.getAssocInfos(node);
        for (DocAssocInfo assocInfo : assocInfos) {
            wax.child("SeotudOsaIdent", assocInfo.getOtherNodeRef().getId())
            .child("SeotudOsaSelgitus", assocInfo.getAssocType().getValueName());
        }
        wax.end(); // KySeonduvAines
    }

    private void writeAccessRestriction(WAX wax, Map<QName, Serializable> properties) {
        wax.start("KyJuurdepaasuala");
        String access = (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (access != null && !AccessRestriction.OPEN.getValueName().equals(access)) {
            Serializable restrictionBeginDate = properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
            wax.start("JuurdepaasPiirang")
            .child("Piirang", (AccessRestriction.AK.getValueName().equals(access) ? "AK" : "UleandjaPiirang"))
            .start("PiirangAeg")
            .start("Algus")
            .child("Tyyp", "kuupäev")
            .child("Tapsus", "true")
            .child("Vaartus", (restrictionBeginDate != null ? DATE_FORMAT.format(restrictionBeginDate) : null))
            .end(); // Algus
            Date end = (Date) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
            if (end != null) {
                wax.start("Lopp")
                .child("Tyyp", "kuupäev")
                .child("Tapsus", "true")
                .child("Vaartus", DATE_FORMAT.format(end))
                .end(); // Lopp
            }
            wax.end() // PiirangAeg
            .child("PiirangKestus", (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC))
            .child("PiirangAlus", (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON))
            .end(); // JuurdepaasPiirang
        }
        wax.end(); // KyJuurdepaasuala
    }

    private void writeKeywords(WAX wax, Map<QName, Serializable> properties) {
        // Single keyword
        String keywords = (String) properties.get(DocumentCommonModel.Props.KEYWORDS);
        if (StringUtils.isNotBlank(keywords)) {
            wax.child("KyMarksona", keywords);
        }

        // Hierarchical keywords
        @SuppressWarnings("unchecked")
        List<String> firstLevel = (List<String>) properties.get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL);
        @SuppressWarnings("unchecked")
        List<String> secondLevel = (List<String>) properties.get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL);
        if (firstLevel != null && !firstLevel.isEmpty()) {
            for (int i = 0; i < firstLevel.size(); i++) {
                wax.child("KyMarksona", TextUtil.joinStringAndStringWithComma(firstLevel.get(i), secondLevel.get(i)));
            }
        }
    }

    @Override
    public void archiveVolumeOrCaseFile(final NodeRef archivingJobRef, final boolean resumingPaused) {
        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        try {
            final ArchiveJobStatus status = archiveVolumeOrCaseFileImpl(archivingJobRef, resumingPaused);
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, status);
                    if (ArchiveJobStatus.FINISHED.equals(status)) {
                        props.put(ArchivalsModel.Props.ARCHIVING_END_TIME, new Date());
                        if (resumingPaused) {
                            nodeService.removeProperty(archivingJobRef, ArchivalsModel.Props.FAILED_NODE_COUNT);
                            nodeService.removeProperty(archivingJobRef, ArchivalsModel.Props.FAILED_DOCUMENTS_COUNT);
                            nodeService.removeProperty(archivingJobRef, ArchivalsModel.Props.TOTAL_ARCHIVED_DOCUMENTS_COUNT);
                            nodeService.removeProperty(archivingJobRef, ArchivalsModel.Props.ARCHIVED_NODE_COUNT);
                        }
                    }
                    nodeService.addProperties(archivingJobRef, props);
                    return null;
                }
            }, false, true);
        } catch (final Exception e) {
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.FAILED);
                    props.put(ArchivalsModel.Props.ARCHIVING_END_TIME, new Date());
                    props.put(ArchivalsModel.Props.ERROR_MESSAGE, e.getMessage() + "\n" + e);
                    nodeService.addProperties(archivingJobRef, props);
                    return null;
                }
            }, false, true);
        }
    }

    @Override
    public void addVolumeOrCaseToArchivingList(NodeRef volumeOrCaseRef) {
        NodeRef archivingJobRef = nodeService.createNode(getArchivalsSpaceRef(), ArchivalsModel.Assocs.ARCHIVING_JOB,
                ArchivalsModel.Assocs.ARCHIVING_JOB, ArchivalsModel.Types.ARCHIVING_JOB).getChildRef();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        props.put(ArchivalsModel.Props.VOLUME_REF, volumeOrCaseRef);
        props.put(ArchivalsModel.Props.ARCHIVE_NOTE, String.format(MessageUtil.getMessage("volume_archiving_note"), df.format(new Date())));
        props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.IN_QUEUE);
        nodeService.addProperties(archivingJobRef, props);
        setArchivingProperty(volumeOrCaseRef, Boolean.TRUE);
    }

    private void setArchivingProperty(NodeRef volumeRef, Boolean value) {
        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef, null);
        volume.setProperty(VolumeModel.Props.MARKED_FOR_ARCHIVING.toString(), value);
        getVolumeService().saveOrUpdate(volume);
    }

    private void setDestructingProperty(NodeRef volumeRef, Boolean value) {
         nodeService.setProperty(volumeRef, EventPlanModel.Props.MARKED_FOR_DESTRUCTION, value); 
    }
    
    @Override
    public List<NodeRef> getAllInQueueJobs() {
        List<NodeRef> volumeRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef ref : getArchivingJobChildAssocs()) {
            volumeRefs.add(ref.getChildRef());
        }
        return volumeRefs;
    }

    @Override
    public ArchiveJobStatus getArchivingStatus(NodeRef archivingJobNodeRef) {
        return archivingJobNodeRef == null ? null : ArchiveJobStatus.valueOf((String) nodeService.getProperty(archivingJobNodeRef, ArchivalsModel.Props.ARCHIVING_JOB_STATUS));
    }

    private NodeRef getArchivalsSpaceRef() {
        return generalService.getNodeRef(ArchivalsModel.Repo.ARCHIVALS_SPACE);
    }

    @Override
    public void markArchivingJobAsRunning(NodeRef archivingJobNodeRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.IN_PROGRESS);
        props.put(ArchivalsModel.Props.ARCHIVING_START_TIME, new Date());
        nodeService.addProperties(archivingJobNodeRef, props);
    }

    private List<ChildAssociationRef> getArchivingJobChildAssocs() {
        return nodeService.getChildAssocs(getArchivalsSpaceRef(), Collections.singleton(ArchivalsModel.Types.ARCHIVING_JOB));
    }

    private ArchiveJobStatus archiveVolumeOrCaseFileImpl(final NodeRef archivingJobRef, boolean resumingPaused) {
        final Map<QName, Serializable> jobProps = nodeService.getProperties(archivingJobRef);
        final NodeRef volumeNodeRef = (NodeRef) jobProps.get(ArchivalsModel.Props.VOLUME_REF);
        Assert.notNull(volumeNodeRef, "Reference to volume node must be provided");
        final String archivingNote = (String) jobProps.get(ArchivalsModel.Props.ARCHIVE_NOTE);

        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();

        final Volume volume = volumeService.getVolumeByNodeRef(volumeNodeRef, null);
        final Series series = seriesService.getSeriesByNodeRef(volume.getSeriesNodeRef());
        final NodeRef originalSeriesRef = series.getNode().getNodeRef();
        final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef = new HashMap<>();

        // do in separate transaction, must be visible in following transactions
        NodeRef[] archivedParentRefs = transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef[]>() {
            @Override
            public NodeRef[] execute() throws Throwable {
                return createAndRetrieveArchiveStructure(volumeNodeRef, originalSeriesRef, series.getFunctionNodeRef(), originalToArchivedCaseNodeRef, volume.isDynamic());
            }
        }, false, true);

        final NodeRef archivedFunctionRef = archivedParentRefs[0];
        final NodeRef archivedSeriesRef = archivedParentRefs[1];
        final NodeRef archivedVolumeRef = archivedParentRefs[2];
        final NodeRef copiedFunctionRef = archivedParentRefs[3];
        NodeRef copiedVolumeRef = archivedParentRefs[4];

        Assert.notNull(archivedSeriesRef, "Series was not archived");
        Assert.notNull(archivedVolumeRef, "Volume was not archived");

        Set<ChildAssociationRef> notCaseNodeRefs = new HashSet<>();
        final Set<NodeRef> caseNodeRefs = new HashSet<>();
        Map<NodeRef, Set<ChildAssociationRef>> archiveNodeRefs = new HashMap<>();
        collectNodeRefsToArchive(volumeNodeRef, notCaseNodeRefs, caseNodeRefs, archiveNodeRefs);

        int failedNodeCount = getCount(jobProps, ArchivalsModel.Props.FAILED_NODE_COUNT);
        int failedDocumentsCount = getCount(jobProps, ArchivalsModel.Props.FAILED_DOCUMENTS_COUNT);
        int totalArchivedDocumentsCount = getCount(jobProps, ArchivalsModel.Props.TOTAL_ARCHIVED_DOCUMENTS_COUNT);
        int archivedNodesCount = getCount(jobProps, ArchivalsModel.Props.ARCHIVED_NODE_COUNT);

        final boolean independentWorkflowEnabled = BeanHelper.getWorkflowConstantsBean().isIndependentWorkflowEnabled();

        final Map<NodeRef, Integer> caseDocsUpdated = new HashMap<>();
        long childCount = archivedNodesCount;
        for (Set<ChildAssociationRef> childNodes : archiveNodeRefs.values()) {
            childCount += childNodes.size();
        }
        if (resumingPaused) {
            LOG.info(String.format("Resuming paused archiving: %d nodes archived previously, starting to archive remaining %d nodes",
                    archivedNodesCount, (childCount - archivedNodesCount)));
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        ProgressTracker progress = new ProgressTracker(childCount, archivedNodesCount);
        int count = 0;
        for (Map.Entry<NodeRef, Set<ChildAssociationRef>> entry : archiveNodeRefs.entrySet()) {

            NodeRef originalParentRef = entry.getKey();
            NodeRef archivedParentRef = null;
            final boolean isInCase = !originalParentRef.equals(volumeNodeRef);
            if (!isInCase) {
                archivedParentRef = archivedVolumeRef;
            } else {
                archivedParentRef = getOrCreateArchiveCase(originalParentRef, archivedVolumeRef, copiedVolumeRef, originalToArchivedCaseNodeRef, transactionHelper,
                        archivedSeriesRef, archivedFunctionRef);
            }
            final NodeRef archivedParentRefFinal = archivedParentRef;
            for (final ChildAssociationRef childAssocRef : entry.getValue()) {
                final NodeRef childRef = childAssocRef.getChildRef();
                if (!nodeService.exists(childRef) || !originalParentRef.equals(nodeService.getPrimaryParent(childRef).getParentRef())) {
                    // node has been deleted or moved, skip it
                    LOG.info("Node not found in volume any more, skipping nodeRef=" + childRef);
                    continue;
                }
                final boolean isDocument = DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(childRef));
                try {
                    transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            Set<NodeRef> cwfRefs = null;
                            if (isDocument) {
                                String existingRegNr = (String) nodeService.getProperty(childRef, REG_NUMBER);
                                if (StringUtils.isNotBlank(existingRegNr)) {
                                    BeanHelper.getAdrService().addDeletedDocument(childRef);
                                }
                                // This approach would now work if institutions would change independent workflow enabled/disabled status
                                // but in practice institutions don't do that.
                                if (independentWorkflowEnabled) {
                                    cwfRefs = new HashSet<>();
                                    List<AssociationRef> assocRefs = nodeService.getTargetAssocs(childRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
                                    for (AssociationRef assocRef : assocRefs) {
                                        cwfRefs.add(assocRef.getTargetRef());
                                    }
                                }
                            }
                            NodeRef archivedNodeRef = nodeService.moveNode(childRef, archivedParentRefFinal, childAssocRef.getTypeQName(),
                                    childAssocRef.getQName()).getChildRef();
                            updateDocumentLocationProps(archivedFunctionRef, archivedSeriesRef, archivedVolumeRef, isInCase ? archivedParentRefFinal : null,
                                    archivedNodeRef);
                            if (isDocument && independentWorkflowEnabled) {
                                updateCompoundWorkflowProps(cwfRefs, childRef, archivedNodeRef);
                            }
                            if (isDocument) {
                            	logService.updateLogEntryObjectId(childRef.toString(), archivedNodeRef.toString());
                            }
                            
                            return null;
                        }
                    }, false, true);
                    if (isDocument) {
                        if (isInCase) {
                            Integer caseArchivedDocsCount = caseDocsUpdated.get(originalParentRef);
                            if (caseArchivedDocsCount == null) {
                                caseArchivedDocsCount = 0;
                            }
                            caseDocsUpdated.put(originalParentRef, ++caseArchivedDocsCount);
                        }
                        totalArchivedDocumentsCount++;
                    }
                    archivedNodesCount++;
                } catch (Exception e) {
                    failedNodeCount++;
                    failedDocumentsCount += isDocument ? 1 : 0;
                    LOG.error("Error archiving node in volume, volume original nodeRef=" + volumeNodeRef + ", archived volume nodeRef=" + archivedVolumeRef
                            + (isInCase ? ", original case nodeRef=" + originalParentRef + ", archived case nodeRef=" + archivedParentRef : "")
                            + ", child nodeRef=" + childAssocRef.getChildRef(), e);
                    // continue, try to archive as much documents as possible
                }
                if (++count >= 10) {
                    String info = progress.step(count);
                    count = 0;
                    if (info != null) {
                        LOG.info("Archiving volume: " + info);
                    }
                    if (isArchivingPaused() || !isArchivingAllowed(dateFormat)) {
                        final Map<QName, Serializable> props = new HashMap<>();
                        props.put(ArchivalsModel.Props.FAILED_NODE_COUNT, failedNodeCount);
                        props.put(ArchivalsModel.Props.FAILED_DOCUMENTS_COUNT, failedDocumentsCount);
                        props.put(ArchivalsModel.Props.TOTAL_ARCHIVED_DOCUMENTS_COUNT, totalArchivedDocumentsCount);
                        props.put(ArchivalsModel.Props.ARCHIVED_NODE_COUNT, archivedNodesCount);
                        LOG.info("Pausing archiving");
                        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                            @Override
                            public Void execute() throws Throwable {
                                nodeService.addProperties(archivingJobRef, props);
                                return null;
                            }
                        }, false, true);
                        updateCounters(volumeNodeRef, archivedVolumeRef, originalSeriesRef, archivedSeriesRef, originalToArchivedCaseNodeRef, totalArchivedDocumentsCount,
                                caseDocsUpdated, transactionHelper);
                        return ArchiveJobStatus.PAUSED;
                    }
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            LOG.info("Archiving volume: " + info);
        }
        // update archivingNote property
        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() {
                Map<QName, Serializable> props = new HashMap<>();
                props.put(EventPlanModel.Props.ARCHIVING_NOTE, archivingNote);
                props.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
                props.put(DocumentCommonModel.Props.FUNCTION, archivedFunctionRef);
                nodeService.addProperties(archivedVolumeRef, props);
                return null;
            }
        }, false, true);

        updateCounters(volumeNodeRef, archivedVolumeRef, originalSeriesRef, archivedSeriesRef, originalToArchivedCaseNodeRef, totalArchivedDocumentsCount, caseDocsUpdated,
                transactionHelper);

        LOG.info("Archived " + archivedNodesCount + " nodes from volume and contained cases,\n " +
                "   nodeRefs=" + archiveNodeRefs.keySet() + "\n" +
                "   of that " + totalArchivedDocumentsCount + " documents");

        if (failedNodeCount == 0) {
            deleteEmptyVolumeAndCases(volumeNodeRef, caseNodeRefs, transactionHelper);
        } else {
            LOG.info("Not deleting original volume, nodeRef=" + volumeNodeRef + ", because " + failedNodeCount + " errors occurred while archiving the volume, of that "
                    + failedDocumentsCount + " document archivation failures");
        }

        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
                nodeService.deleteNode(copiedFunctionRef);
                return null;
            }
        }, false, true);

        return ArchiveJobStatus.FINISHED;

    }

    private int getCount(final Map<QName, Serializable> jobProps, QName prop) {
        Integer count = (Integer) jobProps.get(prop);
        return (count != null) ? count : 0;
    }

    private boolean isArchivingAllowed(SimpleDateFormat dateFormat) {
        boolean allowedNow = isArchivingAllowedAtThisTime(dateFormat);

        if (allowedNow) {
            resetManualActions();
            return true;
        }

        if (!allowedNow && isArchivingContinuedManually()) {
            return true;
        }

        return allowedNow;
    }

    @Override
    public boolean isArchivingAllowed() {
        return isArchivingAllowed(new SimpleDateFormat("HH:mm"));
    }

    private boolean isArchivingAllowedAtThisTime(SimpleDateFormat dateFormat) {
        DateTime now = new DateTime();
        if (Boolean.valueOf(parametersService.getStringParameter(Parameters.CONTINUE_ARCIVING_OVER_WEEKEND))) {
            int weekDay = now.getDayOfWeek();
            if (DateTimeConstants.SATURDAY == weekDay || DateTimeConstants.SUNDAY == weekDay) {
                return true;
            }
        }
        String beginTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.ARCHIVING_BEGIN_TIME));
        String endTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.ARCHIVING_END_TIME));
        if (StringUtils.isBlank(beginTimeStr) || StringUtils.isBlank(endTimeStr)) {
            return true;
        }
        DateTime beginTime;
        DateTime endTime;
        try {
            beginTime = getDateTime(now, beginTimeStr, dateFormat);
            endTime = getDateTime(now, endTimeStr, dateFormat);
            if (beginTime.isAfter(endTime)) {
                endTime = endTime.plusDays(1);
            }
        } catch (ParseException e) {
            LOG.warn("Unable to parse " + Parameters.ARCHIVING_BEGIN_TIME.getParameterName() + " (value=" + beginTimeStr + ") or "
                    + Parameters.ARCHIVING_END_TIME.getParameterName() + " (value=" + endTimeStr + "), continuing archiving. " +
                    "Required format is " + dateFormat.toPattern());
            return true;
        }
        if (beginTime.isBefore(now) && endTime.isAfter(now)) {
            return true;
        }
        return false;
    }

    private DateTime getDateTime(DateTime now, String timeString, DateFormat dateFormat) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateFormat.parse(timeString));
        return new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0, 0);
    }

    private void deleteEmptyVolumeAndCases(final NodeRef volumeNodeRef, final Set<NodeRef> caseNodeRefs, final RetryingTransactionHelper transactionHelper) {
        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
                List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(volumeNodeRef);
                boolean deleteVolume = true;
                for (ChildAssociationRef childRef : childRefs) {
                    NodeRef childNodeRef = childRef.getChildRef();
                    if (caseNodeRefs.contains(childNodeRef)) {
                        if (nodeService.getChildAssocs(childNodeRef).size() == 0) {
                            // TODO: could be optimized, assuming that it is very likely that whole volume is going to be deleted,
                            // but as this runs in background and case has no children, deleting shouldn't take much time either
                            nodeService.deleteNode(childNodeRef);
                            LOG.info("Deleted empty case, nodeRef=" + childNodeRef);
                            continue;
                        }
                        deleteVolume = false;
                        LOG.info("case nodeRef=" + childNodeRef + " has not archived children, not deleting case.");
                        break;
                    }
                    deleteVolume = false;
                    LOG.info("Volume nodeRef=" + volumeNodeRef + " has not archived children created after archivation start, not deleting volume.");
                    break;
                }
                if (deleteVolume) {
                    nodeService.deleteNode(volumeNodeRef);
                    LOG.info("Deleted original volume nodeRef=" + volumeNodeRef);
                }
                return null;
            }
        }, false, true);
    }

    private NodeRef getOrCreateArchiveCase(final NodeRef originalCaseRef, final NodeRef archivedVolumeRef, final NodeRef copiedVolumeRef,
            final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef, final RetryingTransactionHelper transactionHelper, final NodeRef archivedSeriesRef,
            final NodeRef archivedFunctionRef) {
        NodeRef archivedCaseRef = originalToArchivedCaseNodeRef.get(originalCaseRef);
        if (archivedCaseRef == null) {
            final NodeRef originalCaseRefFinal = originalCaseRef;
            // create archive case
            archivedCaseRef = transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {
                @Override
                public NodeRef execute() {
                    NodeRef copiedCaseRef = copyService.copy(originalCaseRef, copiedVolumeRef, CaseModel.Associations.CASE, CaseModel.Associations.CASE);
                    NodeRef archCaseRef = nodeService.moveNode(copiedCaseRef, archivedVolumeRef, CaseModel.Associations.CASE, CaseModel.Associations.CASE).getChildRef();
                    Map<QName, Serializable> caseProps = new HashMap<QName, Serializable>();
                    caseProps.put(CaseModel.Props.CONTAINING_DOCS_COUNT, 0);
                    caseProps.put(CaseModel.Props.ORIGINAL_CASE, originalCaseRefFinal);
                    caseProps.put(DocumentCommonModel.Props.VOLUME, archivedVolumeRef);
                    caseProps.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
                    caseProps.put(DocumentCommonModel.Props.FUNCTION, archivedFunctionRef);
                    nodeService.addProperties(archCaseRef, caseProps);
                    return archCaseRef;
                }
            }, false, true);
            originalToArchivedCaseNodeRef.put(originalCaseRef, archivedCaseRef);
        }
        return archivedCaseRef;
    }

    private void updateCounters(final NodeRef originalVolumeRef, final NodeRef archivedVolumeRef, final NodeRef originalSeriesRef,
            final NodeRef archivedSeriesRef, final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef, final int archivedDocumentsCount,
            final Map<NodeRef, Integer> caseDocsUpdated, final RetryingTransactionHelper transactionHelper) {
        updateCounter(archivedVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocumentsCount, transactionHelper, false);
        volumeService.removeFromCache(archivedVolumeRef);
        updateCounter(originalVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocumentsCount, transactionHelper, true);
        volumeService.removeFromCache(originalVolumeRef);
        updateCounter(archivedSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocumentsCount, transactionHelper, false);
        seriesService.removeFromCache(archivedSeriesRef);
        updateCounter(originalSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocumentsCount, transactionHelper, true);
        seriesService.removeFromCache(originalSeriesRef);
        for (Map.Entry<NodeRef, Integer> entry : caseDocsUpdated.entrySet()) {
            NodeRef originalCaseRef = entry.getKey();
            NodeRef archivedCaseRef = originalToArchivedCaseNodeRef.get(originalCaseRef);
            int archivedDocCount = entry.getValue();
            updateCounter(archivedCaseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocCount, transactionHelper, false);
            caseService.removeFromCache(archivedCaseRef);
            updateCounter(originalCaseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocCount, transactionHelper, true);
            caseService.removeFromCache(originalCaseRef);
        }

    }

    private void updateCounter(final NodeRef nodeRefToUpdate, final QName docCountProp, final boolean added, final int docCount, final RetryingTransactionHelper transactionHelper,
            final boolean checkExisting) {
        try {
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    // node may be deleted while archiving, this is okey and shouldn't produce error
                    if (!checkExisting || nodeService.exists(nodeRefToUpdate)) {
                        generalService.updateParentContainingDocsCount(nodeRefToUpdate, docCountProp, added, docCount);
                    }
                    return null;
                }
            }, false, true);
        } catch (Exception e) {
            LOG.error("Error updating counters in archive volume process, nodeRef=" + nodeRefToUpdate, e);
            // continue updating other counters
        }
    }

    private void collectNodeRefsToArchive(final NodeRef volumeNodeRef, Set<ChildAssociationRef> notCaseNodeRefs, Set<NodeRef> caseNodeRefs,
            Map<NodeRef, Set<ChildAssociationRef>> archiveNodeRefs) {
        List<ChildAssociationRef> volumeChildAssocs = nodeService.getChildAssocs(volumeNodeRef);
        for (ChildAssociationRef childAssocRef : volumeChildAssocs) {
            NodeRef childNodeRef = childAssocRef.getChildRef();
            QName nodeType = nodeService.getType(childNodeRef);
            if (CaseModel.Types.CASE.equals(nodeType)) {
                caseNodeRefs.add(childNodeRef);
            } else {
                notCaseNodeRefs.add(childAssocRef);
            }
        }
        archiveNodeRefs.put(volumeNodeRef, notCaseNodeRefs);
        for (NodeRef caseNodeRef : caseNodeRefs) {
            archiveNodeRefs.put(caseNodeRef, new HashSet<>(nodeService.getChildAssocs(caseNodeRef)));
        }
    }

    private NodeRef[] createAndRetrieveArchiveStructure(final NodeRef volumeNodeRef, final NodeRef originalSeriesRef, final NodeRef originalFunctionRef,
            final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef, boolean dynamicVolume) {

        NodeRef copiedFunctionRef = copyService.copy(originalFunctionRef, getTempArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
        NodeRef copiedSeriesRef = copyService.copy(originalSeriesRef, copiedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
        QName volumeOrCaseFileAssoc = dynamicVolume ? CaseFileModel.Assocs.CASE_FILE : VolumeModel.Associations.VOLUME;
        NodeRef copiedVolumeRef = copyService.copy(volumeNodeRef, copiedSeriesRef, volumeOrCaseFileAssoc, volumeOrCaseFileAssoc);
        nodeService.setProperty(copiedSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, 0);
        nodeService.setProperty(copiedVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, 0);
        boolean movedHierarchy = true;

        NodeRef archivedFunctionRef = getArchivedFunctionByMark((String) nodeService.getProperty(originalFunctionRef, FunctionsModel.Props.MARK));
        NodeRef archivedSeriesRef = null;
        NodeRef archivedVolumeRef = null;
        QName volumeOrCaseFileType = dynamicVolume ? CaseFileModel.Types.CASE_FILE : VolumeModel.Types.VOLUME;
        if (archivedFunctionRef == null) {
            archivedFunctionRef = nodeService.moveNode(copiedFunctionRef, getArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION)
                    .getChildRef();
            archivedSeriesRef = nodeService.getChildAssocs(archivedFunctionRef, Collections.singleton(SeriesModel.Types.SERIES)).get(0).getChildRef();
            archivedVolumeRef = nodeService.getChildAssocs(archivedSeriesRef, Collections.singleton(volumeOrCaseFileType)).get(0).getChildRef();
        } else {
            String seriesIdentifier = (String) nodeService.getProperty(originalSeriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunctionRef, seriesIdentifier);
            if (archivedSeriesRef == null) {
                archivedSeriesRef = nodeService.moveNode(copiedSeriesRef, archivedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES).getChildRef();
                archivedVolumeRef = nodeService.getChildAssocs(archivedSeriesRef, Collections.singleton(volumeOrCaseFileType)).get(0).getChildRef();
            } else {
                archivedVolumeRef = volumeService.getArchivedVolumeByOriginalNodeRef(archivedSeriesRef, volumeNodeRef);
                if (archivedVolumeRef == null) {
                    archivedVolumeRef = nodeService.moveNode(copiedVolumeRef, archivedSeriesRef, volumeOrCaseFileAssoc, volumeOrCaseFileAssoc).getChildRef();
                } else {
                    movedHierarchy = false;
                    Map<QName, Serializable> originalVolumeProps = nodeService.getProperties(volumeNodeRef);
                    nodeService.addProperties(archivedVolumeRef, RepoUtil.getPropertiesIgnoringSystem(originalVolumeProps, dictionaryService));
                    List<ChildAssociationRef> caseChildAssocs = nodeService.getChildAssocs(archivedVolumeRef, Collections.singleton(CaseModel.Types.CASE));
                    for (ChildAssociationRef caseChildAssoc : caseChildAssocs) {
                        NodeRef archivedCaseRef = caseChildAssoc.getChildRef();
                        NodeRef originalCaseRef = (NodeRef) nodeService.getProperty(archivedCaseRef, CaseModel.Props.ORIGINAL_CASE);
                        if (originalCaseRef != null) {
                            originalToArchivedCaseNodeRef.put(originalCaseRef, archivedCaseRef);
                        }
                    }
                }
            }
        }
        if (movedHierarchy) {
            // if copied hierarchy was (partly) moved, delete remaining hierarchy and recreate new
            // so it can be used to copy cases during archivation process
            if (nodeService.exists(copiedFunctionRef)) {
                nodeService.deleteNode(copiedFunctionRef);
            }
            copiedFunctionRef = copyService.copy(originalFunctionRef, getTempArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
            copiedSeriesRef = copyService.copy(originalSeriesRef, copiedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
            copiedVolumeRef = copyService.copy(volumeNodeRef, copiedSeriesRef, volumeOrCaseFileAssoc, volumeOrCaseFileAssoc);
        }
        nodeService.setProperty(archivedVolumeRef, VolumeModel.Props.ORIGINAL_VOLUME, volumeNodeRef);
        return new NodeRef[] { archivedFunctionRef, archivedSeriesRef, archivedVolumeRef, copiedFunctionRef, copiedVolumeRef };
    }

    private void updateDocumentLocationProps(NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef, NodeRef archivedCaseRef, NodeRef docRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DocumentCommonModel.Props.FUNCTION, archivedFunRef);
        props.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
        props.put(DocumentCommonModel.Props.VOLUME, archivedVolumeRef);
        props.put(DocumentCommonModel.Props.CASE, archivedCaseRef);
        nodeService.addProperties(docRef, props);
    }

    private void updateCompoundWorkflowProps(Set<NodeRef> compoundWorkflowRefs, NodeRef mainDocRef, NodeRef archivedDocRef) {
        if (CollectionUtils.isEmpty(compoundWorkflowRefs)) {
            return;
        }
        Map<NodeRef, Node> nodes = bulkLoadNodeService.loadNodes(compoundWorkflowRefs, CWF_PROPS_TO_LOAD);
        for (Map.Entry<NodeRef, Node> entry : nodes.entrySet()) {
            Map<String, Object> cwfProps = entry.getValue().getProperties();
            CompoundWorkflowType type = CompoundWorkflowType.valueOf((String) cwfProps.get(WorkflowCommonModel.Props.TYPE));
            if (CompoundWorkflowType.INDEPENDENT_WORKFLOW.equals(type) && mainDocRef.equals(cwfProps.get(WorkflowCommonModel.Props.MAIN_DOCUMENT))) {
                nodeService.setProperty(entry.getKey(), WorkflowCommonModel.Props.MAIN_DOCUMENT, archivedDocRef);
            }
        }
    }

    @Override
    public void setNewReviewDate(final List<NodeRef> volumes, final Date reviewDate, final NodeRef activityRef) {
        setNextEvent(volumes, reviewDate, activityRef, FirstEvent.REVIEW, "volumeSetReviewDate", "applog_archivals_volume_new_review_date", true);
    }

    @Override
    public void setNextEventDestruction(final List<NodeRef> volumes, final Date newDate, final NodeRef activityRef) {
        setNextEvent(volumes, newDate, activityRef, FirstEvent.DESTRUCTION, "volumeSetNextEventDestruction", "applog_archivals_volume_next_event_destruction", false);
    }

    private void setNextEvent(final List<NodeRef> volumes, final Date newDate, final NodeRef activityRef, final FirstEvent nextEvent, String threadName, final String messageKey,
            final boolean logPreviousDate) {
        final FastDateFormat df = FastDateFormat.getInstance("dd.MM.yyyy");
        BeanHelper.getGeneralService().runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
                for (final NodeRef nodeRef : volumes) {
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            Date previousReviewDate = logPreviousDate ? (Date) nodeService.getProperty(nodeRef, EventPlanModel.Props.NEXT_EVENT_DATE) : null;
                            Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
                            newProps.put(EventPlanModel.Props.NEXT_EVENT, nextEvent.name());
                            newProps.put(EventPlanModel.Props.NEXT_EVENT_DATE, newDate);
                            nodeService.addProperties(nodeRef, newProps);

                            String previousDateStr = previousReviewDate != null ? df.format(previousReviewDate) : null;
                            String newDateStr = newDate != null ? df.format(newDate) : null;
                            Object[] messageValueHolders = logPreviousDate ? new Object[] { previousDateStr, newDateStr } : new Object[] { newDateStr };
                            logService.addLogEntry(LogEntry.create(isCaseFile(nodeRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, nodeRef, messageKey,
                                    messageValueHolders));
                            return null;
                        }
                    }, false, true);
                }
                setActivityStatusFinished(activityRef);
                return null;
            }

        }, threadName, false);

    }

    @Override
    public void markForTransfer(final List<NodeRef> volumes, final NodeRef activityRef) {
        setPropTrueAndFinish(volumes, activityRef, EventPlanModel.Props.MARKED_FOR_TRANSFER, "volumeMarkForTransfer", "applog_archivals_volume_marked_for_transfer");
    }

    @Override
    public void setDisposalActCreated(final List<NodeRef> volumes, final NodeRef activityRef) {
        setPropTrueAndFinish(volumes, activityRef, EventPlanModel.Props.DISPOSAL_ACT_CREATED, "volumeSetDisposalActCreated", null);
    }

    protected void setPropTrueAndFinish(final List<NodeRef> volumes, final NodeRef activityRef, final QName propQName, String threadName, final String logMessageKey) {
        BeanHelper.getGeneralService().runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
                for (final NodeRef nodeRef : volumes) {
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            nodeService.setProperty(nodeRef, propQName, Boolean.TRUE);
                            if (logMessageKey != null) {
                                logService.addLogEntry(LogEntry.create(isCaseFile(nodeRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, nodeRef, logMessageKey));
                            }
                            return null;
                        }
                    }, false, true);
                }
                setActivityStatusFinished(activityRef);
                return null;
            }

        }, threadName, false);
    }

    @Override
    public void confirmTransfer(final List<NodeRef> volumes, final Date confirmationDate, final NodeRef activityRef) {
        BeanHelper.getGeneralService().runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
                for (final NodeRef nodeRef : volumes) {
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                            props.put(EventPlanModel.Props.TRANSFER_CONFIRMED, Boolean.TRUE);
                            props.put(EventPlanModel.Props.TRANSFERED_DATE_TIME, confirmationDate);
                            nodeService.addProperties(nodeRef, props);
                            logService.addLogEntry(LogEntry.create(isCaseFile(nodeRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, nodeRef,
                                    "applog_archivals_volume_transfer_confirmed"));
                            return null;
                        }
                    }, false, true);
                }
                setActivityStatusFinished(activityRef);
                return null;
            }

        }, "volumeConfirmTransfer", false);
    }

    private void setActivityStatusFinished(final NodeRef activityRef) {
        nodeService.setProperty(activityRef, ArchivalsModel.Props.STATUS, ActivityStatus.FINISHED.getValue());
    }

    @Override
    public boolean disposeVolumes(final List<NodeRef> volumes, final Date destructionStartDate, final NodeRef activityRef, final NodeRef templateRef, final String logMessage) {
    	
    	ArchivalActivity activity = getArchivalActivity(activityRef);
        //final String executingUser = AuthenticationUtil.getFullyAuthenticatedUser();
    	//final String executingUser = activity.getCreatorName();
    	final String executingUser = activity.getCreatorId();	
    	
        if (!ActivityStatus.IN_PROGRESS.getValue().equals(nodeService.getProperty(activityRef, ArchivalsModel.Props.STATUS))) {
        	nodeService.setProperty(activityRef, ArchivalsModel.Props.STATUS, ActivityStatus.IN_PROGRESS.getValue());
        }
        
        String disposeSuccessMessage = null;
        String docDeletingComment = null;
        if (ActivityType.SIMPLE_DESTRUCTION.name().equals(nodeService.getProperty(activityRef, ArchivalsModel.Props.ACTIVITY_TYPE))) {
        	disposeSuccessMessage = "applog_archivals_volume_simple_disposed";
        	docDeletingComment = "archivals_volume_destruction_without_disposition_act";
        }
        else {
        	disposeSuccessMessage = "applog_archivals_volume_disposed";
        	docDeletingComment = "archivals_volume_destruction_with_disposition_act";
        }
        
		return disposeVolumes(volumes, destructionStartDate, docDeletingComment, disposeSuccessMessage, executingUser);
    }
    
    private boolean disposeVolumes(List<NodeRef> volumesToDestroy, final Date disposalDate, final String docDeletingComment, final String logMessage, String executingUser) {
        RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (final NodeRef volumeNodeRef : volumesToDestroy) {
            // remove all childs
            if (deleteDocuments(docDeletingComment, retryingTransactionHelper, volumeNodeRef, executingUser, true) == false) {
            	return false;
            }

            for (NodeRef casRef : BeanHelper.getCaseService().getCaseRefsByVolume(volumeNodeRef)) {
               if (deleteDocuments(docDeletingComment, retryingTransactionHelper, casRef, executingUser, true) == false) {
            	   return false;
               }
            	   
            }

            retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    seriesService.updateContainingDocsCountByVolume(volumeService.getVolumeByNodeRef(volumeNodeRef, propertyTypes).getSeriesNodeRef(), volumeNodeRef, false);
                    HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(VolumeModel.Props.STATUS, DocListUnitStatus.DESTROYED.getValueName());
                    props.put(EventPlanModel.Props.DISPOSAL_DATE_TIME, disposalDate);
                    props.put(VolumeModel.Props.CONTAINING_DOCS_COUNT, 0);
                    if (nodeService.hasAspect(volumeNodeRef, DocumentCommonModel.Aspects.DOCUMENT_REG_NUMBERS_CONTAINER)) {
                        props.put(DocumentCommonModel.Props.DOCUMENT_REG_NUMBERS, null);
                    }
                    nodeService.addProperties(volumeNodeRef, props);
                    volumeService.removeFromCache(volumeNodeRef);
                    logDestructionRelatedEvent(volumeNodeRef, logMessage);
                    return null;
                }

            }, false, true);
        }
        
        return true;
    }

    private boolean deleteDocuments(final String docDeletingComment, RetryingTransactionHelper retryingTransactionHelper, final NodeRef volumeNodeRef, final String executingUser, final boolean isDisposeVolume) {
//        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(volumeNodeRef, Collections.singleton(DocumentCommonModel.Types.DOCUMENT));
    	List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(volumeNodeRef, new QNamePattern() {
            @Override
            public boolean isMatch(QName qname) {
            	if (VolumeModel.Associations.DELETED_DOCUMENT.equals(qname)) {            	
            		return false;
            	}
            	else {
            		return true;
            	}
          
            }
        }, new QNamePattern() {
            @Override
            public boolean isMatch(QName qname) {
            	if (VolumeModel.Associations.DELETED_DOCUMENT.equals(qname)) {            	
            		return false;
            	}
            	else {
            		return true;
            	}
            }
        }); //QNamePattern.MATCH_ALL.);
        for (ChildAssociationRef childAssoc : childAssocs) {
        	
        	long start = System.currentTimeMillis();
        	
        	// put a check what validates what we are able to continue .
        	if (isDestructionPaused() || isDestructionAllowed() == false) {
        		LOG.info("Document destruction is not allowed or paused.");
        		return false;
        	}
        	
        	
            final NodeRef nodeRef = childAssoc.getChildRef();
            retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    if (!dictionaryService.isSubClass(nodeService.getType(nodeRef), DocumentCommonModel.Types.DOCUMENT)) {
                        return null;
                    }
                    if (isInvalidNode(nodeRef)) {
                        LOG.warn("Cannot delete node because provided nodeRef is invalid: " + nodeRef);
                        return null;
                    }
                    adrService.addDeletedDocument(nodeRef);
                    // mark for permanent delete
                    nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.DELETE_PERMANENT, null);
                    documentService.deleteDocument(nodeRef, docDeletingComment, DeletionType.DISPOSITION, executingUser,isDisposeVolume);
                    return null;
                }

            }, false, true);
            
            LOG.info("Delete single document time  is " + (System.currentTimeMillis() - start) + "ms");
        }
        
        return true;
    }

    private boolean isInvalidNode(NodeRef nodeRef) {
        return nodeRef == null || !nodeService.exists(nodeRef);
    }

    @Override
    public List<UnmodifiableFunction> getArchivedFunctions() {
        return functionsService.getFunctions(getArchivalRoot());
    }

    @Override
    public NodeRef addArchivalActivityExcel(ActivityType activityType, ActivityStatus activityStatus, List<NodeRef> volumeRefs, String templateCode) {
        NodeRef activityRef = createActivityReference(activityType, activityStatus);
        String templateName = MessageUtil.getMessage(templateCode);
        documentTemplateService.generateExcel(activityRef, volumeRefs, templateName);
        BeanHelper.getPrivilegeService().setPermissions(activityRef, "GROUP_ARCHIVISTS", Privilege.VIEW_DOCUMENT_FILES);
        return activityRef;
    }

    public NodeRef createActivityReference(ActivityType activityType, ActivityStatus activityStatus) {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ArchivalsModel.Props.ACTIVITY_TYPE, activityType.name());
        properties.put(ArchivalsModel.Props.STATUS, activityStatus.getValue());
        properties.put(ArchivalsModel.Props.CREATED, new Date());
        String username = AuthenticationUtil.getRunAsUser();
        properties.put(ArchivalsModel.Props.CREATOR_ID, username);
        properties.put(ArchivalsModel.Props.CREATOR_NAME, BeanHelper.getUserService().getUserFullName(username));
        ChildAssociationRef childAssoc = nodeService.createNode(getArchivalActivitiesRoot(),
                ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY, ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY,
                ArchivalsModel.Types.ARCHIVAL_ACTIVITY, properties);
        return childAssoc.getChildRef();
    }

    @Override
    public ArchivalActivity getArchivalActivity(NodeRef archivalActivityRef) {
        try {
            WmNode wmNode = new WmNode(archivalActivityRef, ArchivalsModel.Types.ARCHIVAL_ACTIVITY);
            NodeRef docRef = getArchivalActivityDocument(archivalActivityRef);
            String docName = (String) (docRef != null ? nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_NAME) : null);
            List<File> files = getArchivalActivityFiles(archivalActivityRef);
            return new ArchivalActivity(wmNode, docName, docRef, files);
        } catch (InvalidNodeRefException e) {
            return null;
        }
    }

    @Override
    public List<File> getArchivalActivityFiles(NodeRef archivalActivityRef) {
        return BeanHelper.getFileService().getAllFilesExcludingDigidocSubitems(archivalActivityRef);
    }

    private NodeRef getArchivalActivityDocument(NodeRef archivalActivityRef) {
        List<AssociationRef> assocRefs = nodeService.getTargetAssocs(archivalActivityRef, ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY_DOCUMENT);
        if (!assocRefs.isEmpty()) {
            return assocRefs.get(0).getTargetRef();
        }
        return null;
    }

    @Override
    public void addArchivalActivityDocument(NodeRef archivalActivityRef, NodeRef docRef) {
        nodeService.createAssociation(archivalActivityRef, docRef, ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY_DOCUMENT);
    }

    private NodeRef getArchivedFunctionByMark(String functionMark) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(getArchivalRoot(),
                RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);

        for (ChildAssociationRef childRef : childRefs) {
            String archivedFunMark = (String) nodeService.getProperty(childRef.getChildRef(), FunctionsModel.Props.MARK);
            if (functionMark.equals(archivedFunMark)) {
                return childRef.getChildRef(); // matching archived function found
            }
        }
        return null; // function is not archived before
    }

    private NodeRef getArchivedSeriesByIdentifies(NodeRef archivedFunRef, String identifier) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(archivedFunRef,
                RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        for (ChildAssociationRef childRef : childRefs) {
            String archivedSeriesId = (String) nodeService.getProperty(childRef.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER);
            if (identifier.equals(archivedSeriesId)) {
                return childRef.getChildRef(); // matching archived series found
            }
        }
        return null; // series is not archived before
    }

    @Override
    public boolean isVolumeInArchivingQueue(NodeRef volumeOrCaseFileRef) {
        List<NodeRef> archiveJobs = BeanHelper.getArchivalsService().getAllInQueueJobs();
        for (NodeRef jobRef : archiveJobs) {
            if (volumeOrCaseFileRef.equals(BeanHelper.getNodeService().getProperty(jobRef, ArchivalsModel.Props.VOLUME_REF))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NodeRef getArchivalRoot() {
        return generalService.getPrimaryArchivalsNodeRef();
    }

    private NodeRef getTempArchivalRoot() {
        return generalService.getNodeRef(ArchivalsModel.Repo.ARCHIVALS_TEMP_SPACE);
    }

    private NodeRef getArchivalActivitiesRoot() {
        return generalService.getNodeRef(ArchivalsModel.Repo.ARCHIVAL_ACTIVITIES_SPACE);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setAdrService(AdrService adrService) {
        this.adrService = adrService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setDocumentAssociationsService(DocumentAssociationsService documentAssociationsService) {
        this.documentAssociationsService = documentAssociationsService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    @Override
    public boolean isSimpleDestructionEnabled() {
        return simpleDestructionEnabled;
    }

    public void setSimpleDestructionEnabled(boolean simpleDestructionEnabled) {
        this.simpleDestructionEnabled = simpleDestructionEnabled;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @Override
    public void removeJobNodeFromArchivingList(NodeRef archivingJobRef) {
        if (archivingJobRef != null) {
            nodeService.deleteNode(archivingJobRef);
        }
    }

    @Override
    public void removeVolumeFromArchivingList(NodeRef volumeRef) {
        if (volumeRef == null) {
            return;
        }
        for (NodeRef archivingJobNodeRef : getAllInQueueJobs()) {
            if (volumeRef.equals(nodeService.getProperty(archivingJobNodeRef, ArchivalsModel.Props.VOLUME_REF))) {
                nodeService.deleteNode(archivingJobNodeRef);
                LOG.info("Volume with nodeRef=" + volumeRef + " was removed from archiving queue.");
                return;
            }
        }
    }

    @Override
    public void cancelAllArchivingJobs(ActionEvent event) {
        StringBuilder sb = new StringBuilder();
        for (NodeRef archivingJobNodeRef : getAllInQueueJobs()) {
            Map<QName, Serializable> props = nodeService.getProperties(archivingJobNodeRef);
            NodeRef volumeRef = (NodeRef) props.get(ArchivalsModel.Props.VOLUME_REF);
            String status = (String) props.get(ArchivalsModel.Props.ARCHIVING_JOB_STATUS);
            if (volumeRef != null && nodeService.exists(volumeRef)) {
                nodeService.setProperty(volumeRef, VolumeModel.Props.MARKED_FOR_ARCHIVING, Boolean.FALSE);
            }
            removeJobNodeFromArchivingList(archivingJobNodeRef);
            sb.append(volumeRef + "\t(status: " + status + ")+\n");
        }
        LOG.info("The archiving of the following volumes was cancelled: " + sb.toString());
    }

    @Override
    public void pauseArchiving(ActionEvent event) {
        archivingPaused.set(true);
        archivingContinuedManually.set(false);
        LOG.info("Volume archiving was paused");
    }

    @Override
    public void continueArchiving(ActionEvent event) {
        archivingPaused.set(false);
        archivingContinuedManually.set(true);
        LOG.info("Volume archiving was resumed");
    }

    @Override
    public boolean isArchivingPaused() {
        return archivingPaused.get();
    }

    @Override
    public boolean isArchivingContinuedManually() {
        return archivingContinuedManually.get();
    }

    @Override
    public void resetManualActions() {
        archivingContinuedManually.set(false);
    }

    @Override
    public void doPauseArchiving() {
        while (archivingPaused.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    // Destruction functions. 
    
	@Override
	public boolean isDestructionPaused() {
		return destructingPaused.get();
	}

	@Override
	public void pauseDestruction(ActionEvent event) {
        LOG.info("Volume desructing was paused");
		destructingPaused.set(true);
		destructingContinuedManually.set(false);
		
		nodeService.setProperty(getDestructionsSpaceRef(), DestructionModel.Props.DESTRUCTION_PAUSED, Boolean.TRUE);
		// pause all wait and in-progress activities until resume.
		setAllIncompleteActivitiesStatus(ActivityStatus.PAUSED);
	}

	@Override
	public boolean isDestructionAllowed() {
		return isDestructingAllowed(new SimpleDateFormat("HH:mm"));
	}
	
    private boolean isDestructingAllowed(SimpleDateFormat dateFormat) {
	    boolean allowedNow = isDestructingAllowedAtThisTime(dateFormat);
	
	    if (allowedNow) {
	        resetDestructionManualActions();
	        return true;
	    }
	
	    if (!allowedNow && isDestructingContinuedManually()) {
	        return true;
	    }
	
	    return allowedNow;
	}

	private boolean isDestructingContinuedManually() {
		return destructingContinuedManually.get();
	}

	@Override
	public void resetDestructionManualActions() {
		destructingContinuedManually.set(false);
	}

	private boolean isDestructingAllowedAtThisTime(SimpleDateFormat dateFormat) {
        DateTime now = new DateTime();
        
        if (Boolean.valueOf(parametersService.getStringParameter(Parameters.CONTINUE_DESTRUCTION_OVER_WEEKEND))) {
            int weekDay = now.getDayOfWeek();
            if (DateTimeConstants.SATURDAY == weekDay || DateTimeConstants.SUNDAY == weekDay) {
                return true;
            }
        }
        
        String beginTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.DESTRUCTION_BEGIN_TIME));
        String endTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.DESTRUCTION_END_TIME));
        String overweekendTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.CONTINUE_DESTRUCTION_OVER_WEEKEND));
        
        if (StringUtils.isBlank(beginTimeStr) || StringUtils.isBlank(endTimeStr) || StringUtils.isBlank(overweekendTimeStr)) {
        	LOG.warn("DestructionBeginTime or DestructionEndTime or ContinueDestructionOverWeekend is empty , destruction is put on hold!");
            return false;
        }
        
        DateTime beginTime;
        DateTime endTime;
        try {
            beginTime = getDateTime(now, beginTimeStr, dateFormat);
            endTime = getDateTime(now, endTimeStr, dateFormat);
            if (beginTime.isAfter(endTime)) {
                endTime = endTime.plusDays(1);
            }
        } catch (ParseException e) {
            LOG.warn("Unable to parse " + Parameters.DESTRUCTION_BEGIN_TIME.getParameterName() + " (value=" + beginTimeStr + ") or "
                    + Parameters.DESTRUCTION_END_TIME.getParameterName() + " (value=" + endTimeStr + "), continuing destructing. " +
                    "Required format is " + dateFormat.toPattern());
            return true;
        }
        
        if (beginTime.isBefore(now) && endTime.isAfter(now)) {
            return true;
        }
        return false;
	}

	private List<ChildAssociationRef> getDestructionJobChildAssocs() {
        return nodeService.getChildAssocs(getDestructionsSpaceRef(), Collections.singleton(DestructionModel.Types.DESTRUCTION_JOB));
    }

	@Override
	public List<NodeRef> getAllInQueueJobsForDesruction() {
        List<NodeRef> volumeRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef ref : getDestructionJobChildAssocs()) {
            volumeRefs.add(ref.getChildRef());
        }
        return volumeRefs;
	}

    @Override
    public void removeJobNodeFromDestructingList(final NodeRef destructingJobRef) {
        if (destructingJobRef != null) {
        
          final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();

          List<AssociationRef> activitiesList = nodeService.getSourceAssocs(destructingJobRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
          AssociationRef activityAssociationRef = activitiesList.get(0);
          
          final NodeRef activityRef = activityAssociationRef.getSourceRef();
                    
  		transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
		    @Override
		    public Void execute() throws Throwable {
	          nodeService.deleteNode(destructingJobRef);
	          
	          final List<AssociationRef> jobs = nodeService.getTargetAssocs(activityRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
	          
	          final int size = jobs.size();
	          if (size == 0) {
	        	  setActivityStatusFinished(activityRef);
	          }
		    	
		        return null;
		    }
		    }, false, true);
  		
        }
    }
    
    private NodeRef getDestructionsSpaceRef() {
        return generalService.getNodeRef(DestructionModel.Repo.DESTRUCTIONS_SPACE);
    }

    @Override
    public NodeRef addVolumeOrCaseToDestructingList(NodeRef volumeOrCaseRef, NodeRef activityRef) {
        NodeRef destructingJobRef = nodeService.createNode(getDestructionsSpaceRef(), DestructionModel.Assocs.DESTRUCTING_JOB,
        		DestructionModel.Assocs.DESTRUCTING_JOB, DestructionModel.Types.DESTRUCTION_JOB).getChildRef();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DestructionModel.Props.VOLUME_REF, volumeOrCaseRef); // this is property from Archive not from destruction how it's works? -rename
        props.put(DestructionModel.Props.DESCTRUCTING_ACTIVITY_REF, activityRef); // Need is questionable !? - remove
        props.put(DestructionModel.Props.DESTRUCING_JOB_STATUS, ArchiveJobStatus.IN_QUEUE);
        nodeService.addProperties(destructingJobRef, props);

        setDestructingProperty(volumeOrCaseRef, Boolean.TRUE);
        
        nodeService.createAssociation(activityRef, destructingJobRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);

        String activityType = getArchivalActivity(activityRef).getActivityNativeType();
        String logMessage = "";
        if (ActivityType.SIMPLE_DESTRUCTION.name().equals(activityType)) {
        	logMessage = "applog_archivals_volume_added_to_queue_to_be_simple_disposed";
        }
        else if (ActivityType.DESTRUCTION.name().equals(activityType)) {
        	logMessage = "applog_archivals_volume_added_to_queue_to_be_disposed";
        }        
        
        logDestructionRelatedEvent(volumeOrCaseRef, logMessage);
        
        return destructingJobRef;
    }

	@Override
	public void markDestructionJobFinished(final NodeRef destructingJobNodeRef) {
		final DestructionJobStatus status = DestructionJobStatus.FINISHED;
        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();

        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DestructionModel.Props.DESTRUCING_JOB_STATUS, status);
        props.put(DestructionModel.Props.DESTRUCTING_END_TIME, new Date());
        
        final NodeRef activity = getActivityForJob(destructingJobNodeRef);
                
        final Map<QName, Serializable> jobProps = nodeService.getProperties(destructingJobNodeRef);
        final NodeRef volumeNodeRef = (NodeRef) jobProps.get(DestructionModel.Props.VOLUME_REF);
        
		transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
		    @Override
		    public Void execute() throws Throwable {
		    	nodeService.addProperties(destructingJobNodeRef, props);
		    	documentTemplateService.updateExcel(activity, volumeNodeRef);
		        return null;
		    }
		    }, false, true);
	}

    @Override
    public void markDestructingJobAsRunning(NodeRef destructingJobNodeRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DestructionModel.Props.DESTRUCING_JOB_STATUS, ArchiveJobStatus.IN_PROGRESS);
        props.put(DestructionModel.Props.DESTRUCTING_START_TIME, new Date());
        nodeService.addProperties(destructingJobNodeRef, props);
    }

    private boolean safeTrigger(AtomicBoolean target, boolean needValue) {
    	if (needValue) {
    		if (progressTrigger.compareAndSet(false, true)) {
            	target.set(needValue);
            	progressTrigger.set(false);
            	return true;
    		}
    		else {
    			return false;
    		}
    	}
		else 
		{
			target.set(false);
		}

    	return true;
    }
    
    @Override
	public boolean setArchiveJobInProgress(boolean b) {
    	return safeTrigger(archivingInProgress, b);
    }
    
    @Override
    public boolean setDestructionJobInProgress(boolean b) {
    	return safeTrigger(destructingInProgress, b);
    }

	@Override
	public boolean isArchiveJobInProgress() {
		return archivingInProgress.get();
	}

	@Override
	public boolean isDestructionJobInProgress() {
		return destructingInProgress.get();
	}

	@Override
	public void stopDestructing(ActionEvent event) {
        LOG.info("Volume desructing was stopped until system restart");
		destructingPaused.set(true);
		destructingContinuedManually.set(false);
		
		// mark all activities which waiting or in progress as waiting till restart.
		setAllIncompleteActivitiesStatus(ActivityStatus.STOPPED);
	}

	@Override
	public void cancelAllDestructingJobs(ActionEvent event) {
		LOG.info("Canceling all destruction jobs");
		destructingPaused.set(true);
		LOG.info("Volume desructing is stopped"); 
		
        StringBuilder sb = new StringBuilder();
        
        for (final NodeRef destructingJobNodeRef : getAllInQueueJobsForDesruction()) {
            Map<QName, Serializable> props = nodeService.getProperties(destructingJobNodeRef);
            final NodeRef volumeRef = (NodeRef) props.get(DestructionModel.Props.VOLUME_REF);
            String status = (String) props.get(DestructionModel.Props.DESTRUCING_JOB_STATUS);
            
            if (!ArchiveJobStatus.IN_QUEUE.name().equals(status)) {
            	continue;
            }
            
            List<AssociationRef> activitiesList = nodeService.getSourceAssocs(destructingJobNodeRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
            
            if(activitiesList.size() == 0) {	// this is kind of unusual situation when we getting here so just remove job ! 
            	nodeService.deleteNode(destructingJobNodeRef);
            	LOG.error("The job is missed activity , removing it!");
            	continue;
            }
            
            AssociationRef activityAssociationRef = activitiesList.get(0);
            final NodeRef activityRef = activityAssociationRef.getSourceRef();
            String activityStatus = (String)nodeService.getProperty(activityRef, ArchivalsModel.Props.STATUS);
            
            final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
            
            if (!ActivityStatus.IN_PROGRESS.name().equals(activityStatus) && volumeRef != null && nodeService.exists(volumeRef)) {
            	
                transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                    @Override
                    public Void execute() throws Throwable {
            	
		            	setDestructingProperty(volumeRef, Boolean.FALSE); 
		            	nodeService.deleteNode(destructingJobNodeRef);
		            	
		            	if(getActivityJobsCount(activityRef) == 0) {
		            		nodeService.deleteNode(activityRef);
		            	}		            	
		                return null;
                    }
                    },false, true);
                
                sb.append(volumeRef + "\t(status: " + status + ")+\n");
                logDestructionRelatedEvent(volumeRef, "applog_archivals_volume_deleted_from_destruction_queue");
            }
        }
        LOG.info("The destructing of the following volumes was cancelled: " + sb.toString());
        destructingPaused.set(true);
	}

	@Override
	public void continueDestructing(ActionEvent event) {
		restorePausedStoppedActivitiesStatus();
		destructingPaused.set(false);
		destructingContinuedManually.set(true);
		nodeService.setProperty(getDestructionsSpaceRef(), DestructionModel.Props.DESTRUCTION_PAUSED, Boolean.FALSE);
		LOG.info("Volume destructing was resumed");
	}
	
	@Override
	public int getNonFinishedDestructionActivities() {
		int count = 0;
		List<ChildAssociationRef> activities = nodeService.getChildAssocs(getArchivalActivitiesRoot(), Collections.singleton(ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY));
		
		for (ChildAssociationRef a : activities) {
			ArchivalActivity aa = getArchivalActivity(a.getChildRef());
			
			if ((ActivityType.DESTRUCTION.name().equals(aa.getActivityNativeType()) ||
					ActivityType.SIMPLE_DESTRUCTION.name().equals(aa.getActivityNativeType())) && 
						!ActivityStatus.FINISHED.getValue().equals(aa.getStatus())) {
				count++;
			}
		}
		
		return count; 
	}
	
	@Override
	public NodeRef getDestructionJobArchivalActivity(NodeRef destructingJobRef) {
        List<AssociationRef> activitiesList = nodeService.getSourceAssocs(destructingJobRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
        AssociationRef activityAssociationRef = activitiesList.get(0);
        
        NodeRef activityRef = activityAssociationRef.getSourceRef();
        return activityRef;
	}
	
	private int getActivityJobsCount(NodeRef activityRef) {
        List<AssociationRef> jobs = nodeService.getTargetAssocs(activityRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
        return jobs.size();
	}
	
	private void setAllIncompleteActivitiesStatus(final ActivityStatus activityStatus) {
		List<ChildAssociationRef> activities = nodeService.getChildAssocs(getArchivalActivitiesRoot(), Collections.singleton(ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY));
        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
		
		for (ChildAssociationRef a : activities) {
			final ArchivalActivity aa = getArchivalActivity(a.getChildRef());
			
			if ((ActivityType.DESTRUCTION.name().equals(aa.getActivityNativeType()) ||
					ActivityType.SIMPLE_DESTRUCTION.name().equals(aa.getActivityNativeType())) && 
						!ActivityStatus.FINISHED.getValue().equals(aa.getStatus()) && 
						aa.getStatus() != activityStatus.getValue()) {
				
				// we are not allowing to stop previously paused activity or pause stopped you need to go
				// over pause/resume/stop or stop/pause/resume
				if (isActivityPausedOrStopped(aa.getStatus()) && isActivityPausedOrStopped(activityStatus.getValue())) {
					continue;
				}
				
                transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
        				nodeService.setProperty(aa.getNodeRef(), ArchivalsModel.Props.PREV_STATUS, aa.getStatus());
        				nodeService.setProperty(aa.getNodeRef(), ArchivalsModel.Props.STATUS, activityStatus.getValue());			
		                return null;
                    }
                    },false, true);
			}
		}
	}
	
	private void restorePausedStoppedActivitiesStatus() {
		
		List<ChildAssociationRef> activities = nodeService.getChildAssocs(getArchivalActivitiesRoot(), Collections.singleton(ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY));

        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();

		for (final ChildAssociationRef activityRef : activities) {
			String activtyNativeType = (String)nodeService.getProperty (activityRef.getChildRef(), ArchivalsModel.Props.ACTIVITY_TYPE);
			String activtyStatus = (String)nodeService.getProperty (activityRef.getChildRef(), ArchivalsModel.Props.STATUS);
			
			if ((ActivityType.DESTRUCTION.name().equals(activtyNativeType) ||
					ActivityType.SIMPLE_DESTRUCTION.name().equals(activtyNativeType)) && 
						(ActivityStatus.PAUSED.getValue().equals(activtyStatus) || 
								ActivityStatus.STOPPED.getValue().equals(activtyStatus))) {
				
				final String prevStatus = (String)nodeService.getProperty(activityRef.getChildRef(), ArchivalsModel.Props.PREV_STATUS);
				if (prevStatus != null) {
					
	                transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
	                    @Override
	                    public Void execute() throws Throwable {
	            	
	    					nodeService.setProperty(activityRef.getChildRef(), ArchivalsModel.Props.STATUS, prevStatus);
	    					nodeService.removeProperty(activityRef.getChildRef(), ArchivalsModel.Props.PREV_STATUS);
	    					
			                return null;
	                    }
	                    },false, true);
				}
				else {
					LOG.error("PREV_STATUS status for " + activtyStatus + " one of pause or stop doesn't exists ! , skipp restore prev status !" );
				}
			}
		}
	}

	private boolean isActivityPausedOrStopped(String state) {
		
		if(ActivityStatus.PAUSED.getValue().equals(state) || 
				ActivityStatus.STOPPED.getValue().equals(state)	) {
			return true;
		}
		
		return false;
	}
	
	private void logDestructionRelatedEvent(NodeRef volumeOrCaseRef, String logMessage) {
		
		String volumeMark = (String) nodeService.getProperty(volumeOrCaseRef, VolumeModel.Props.VOLUME_MARK);
		String title = (String) nodeService.getProperty(volumeOrCaseRef, VolumeModel.Props.TITLE);
		
		logService.addLogEntry(LogEntry.create(isCaseFile(volumeOrCaseRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, volumeOrCaseRef, logMessage, volumeMark, title));
	}

	@Override
	public void markDestructingJobAsPaused(NodeRef destructingJobNodeRef) {
		final DestructionJobStatus status = DestructionJobStatus.PAUSED;
		
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DestructionModel.Props.DESTRUCING_JOB_STATUS, status);
        nodeService.addProperties(destructingJobNodeRef, props);
	}
	
	private NodeRef getActivityForJob(NodeRef destructingJobRef) {
        List<AssociationRef> activitiesList = nodeService.getSourceAssocs(destructingJobRef, DestructionModel.Assocs.ACTIVITY_LINKED_JOBS);
        
        if(!activitiesList.isEmpty()) {
	        AssociationRef activityAssociationRef = activitiesList.get(0);
	        NodeRef activityRef = activityAssociationRef.getSourceRef();
	        
	        return activityRef;
        }
        
        return null;
	}
}
