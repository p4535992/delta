package ee.webmedia.alfresco.archivals.service;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import com.ociweb.xml.Version;
import com.ociweb.xml.WAX;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.archivals.model.ActivityFileType;
import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
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
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.DeletionType;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Romet Aidla
 */
public class ArchivalsServiceImpl implements ArchivalsService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArchivalsServiceImpl.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private CopyService copyService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private FunctionsService functionsService;
    private SearchService searchService;
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

    private StoreRef archivalsStore;
    private boolean simpleDestructionEnabled;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat DATE_SHORT_FORMAT = FastDateFormat.getInstance("yyyyMMdd");
    private static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");
    private static final FastDateFormat DATE_TIME_SHORT_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

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
                List<NodeRef> documents = documentService.getAllDocumentRefsByParentRef(volumeRef);
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
                || description.equals("Dokumendi importimine (DVK)")
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
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                is = new DigestInputStream(fileService.getFileContentInputStream(file.getNodeRef()), md5);
                x.start("Fail")
                        .child("FailIdent", file.getNodeRef().getId())
                        .child("FailNimi", file.getDisplayName())
                        .child("FailSuurus", Long.toString(file.getSize()))
                        .start("FailBase64");
                x.unescapedText("<![CDATA[");
                InputStreamReader reader = new InputStreamReader(new org.apache.commons.codec.binary.Base64InputStream(is, true, 0, null), "UTF-8");
                char[] buffer = new char[4096];
                while (-1 != reader.read(buffer)) {
                    x.unescapedText(new String(buffer));
                }
                x.unescapedText("]]>")
                        .end() // FailBase64
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
    public NodeRef archiveVolumeOrCaseFile(NodeRef volumeNodeRef) {
        return archiveVolumeOrCaseFile(volumeNodeRef, getVolumeArchiveNote());
    }

    private String getVolumeArchiveNote() {
        return String.format(MessageUtil.getMessage("caseFile_archiving_note"), DATE_TIME_SHORT_FORMAT.format(new Date()));
    }

    private NodeRef archiveVolumeOrCaseFile(NodeRef volumeNodeRef, String archivingNote) {
        Assert.notNull(volumeNodeRef, "Reference to volume node must be provided");
        Volume volume = volumeService.getVolumeByNodeRef(volumeNodeRef);
        Series series = seriesService.getSeriesByNodeRef(volume.getSeriesNodeRef());

        // Make a copy of function and series and then move them if necessary.
        // We can't copy them directly to different store (CopyService doesn't support this).
        NodeRef copiedFunNodeRef = copyService.copy(series.getFunctionNodeRef(), getTempArchivalRoot(),
                FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
        String functionMark = (String) nodeService.getProperty(copiedFunNodeRef, FunctionsModel.Props.MARK);
        NodeRef copiedSeriesNodeRef = copyService.copy(series.getNode().getNodeRef(), copiedFunNodeRef,
                SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
        String seriesIdentifier = (String) nodeService.getProperty(copiedSeriesNodeRef, SeriesModel.Props.SERIES_IDENTIFIER);

        // find if function with given mark from archival store
        NodeRef archivedFunRef = getArchivedFunctionByMark(functionMark);
        NodeRef archivedSeriesRef = null;
        if (archivedFunRef == null) { // function isn't archived before, let's do this now
            archivedFunRef = nodeService.moveNode(copiedFunNodeRef, getArchivalRoot(),
                    FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION).getChildRef();
            // with previous call, also series will be moved, find the series node ref
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunRef, seriesIdentifier);
        } else { // function is archived, let's check whether we need to archive series
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunRef, seriesIdentifier);
            if (archivedSeriesRef == null) { // series isn't archived before, let's do this now
                nodeService.setProperty(copiedSeriesNodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, 0); // reset value if it' first time
                archivedSeriesRef = nodeService.moveNode(copiedSeriesNodeRef, archivedFunRef,
                        SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES).getChildRef();
            }

            // let's delete our temporary copy
            nodeService.deleteNode(copiedFunNodeRef);
        }
        Assert.notNull(archivedSeriesRef, "Series was not archived");

        seriesService.updateContainingDocsCountByVolume(series.getNode().getNodeRef(), volumeNodeRef, false);
        // now move volume with all children

        QName volumeAssocQName = volume.isDynamic() ? CaseFileModel.Assocs.CASE_FILE : VolumeModel.Associations.VOLUME;
        NodeRef archivedVolumeNodeRef = nodeService.moveNode(volumeNodeRef, archivedSeriesRef,
                volumeAssocQName, volumeAssocQName).getChildRef();
        nodeService.setProperty(archivedVolumeNodeRef, EventPlanModel.Props.ARCHIVING_NOTE, archivingNote);
        seriesService.updateContainingDocsCountByVolume(archivedSeriesRef, archivedVolumeNodeRef, true);
        updateDocListUnitLocations(archivedFunRef, archivedSeriesRef, archivedVolumeNodeRef, volume.isContainsCases());
        return archivedVolumeNodeRef;
    }

    @Override
    public void archiveVolumesOrCaseFiles(final List<NodeRef> volumesToArchive) {
        generalService.runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
                for (final NodeRef nodeRef : volumesToArchive) {
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            archiveVolumeOrCaseFile(nodeRef, getVolumeArchiveNote());
                            return null;
                        }
                    }, false, true);
                }
                return null;
            }
        }, "archiveVolumes", false);
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
        final UserService userService = BeanHelper.getUserService();
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
        final UserService userService = BeanHelper.getUserService();
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
        final UserService userService = BeanHelper.getUserService();
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
    public void disposeVolumes(final List<NodeRef> volumes, final Date destructionStartDate, final String docDeletingComment, final NodeRef activityRef, final NodeRef templateRef,
            final String logMessage) {
        BeanHelper.getGeneralService().runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                disposeVolumes(volumes, destructionStartDate, docDeletingComment, logMessage);
                BeanHelper.getDocumentTemplateService().populateVolumeArchiveTemplate(activityRef, volumes, templateRef);
                setActivityStatusFinished(activityRef);
                return null;
            }

        }, "volumeDispose", true);
    }

    private void updateDocListUnitLocations(NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef, boolean containsCases) {
        updateDocListUnitLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, null, Collections.singletonList(archivedVolumeRef), false, false);
        if (containsCases) {
            List<NodeRef> caseRefs = caseService.getCaseRefsByVolume(archivedVolumeRef);
            updateDocListUnitLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, null, caseRefs, true, false);
            for (NodeRef caseRef : caseRefs) {
                List<NodeRef> documents = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseRef);
                updateDocListUnitLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, caseRef, documents, true, true);
            }
        }

        // Also process documents found directly under the volume
        List<NodeRef> documents = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(archivedVolumeRef);
        updateDocListUnitLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, null, documents, true, true);
    }

    private void updateDocListUnitLocationProps(NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef, NodeRef archivedCaseRef, List<NodeRef> docRefs,
            boolean addVolume, boolean addCase) {
        for (NodeRef docRef : docRefs) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.FUNCTION, archivedFunRef);
            props.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
            if (addVolume) {
                props.put(DocumentCommonModel.Props.VOLUME, archivedVolumeRef);
            }
            if (addCase) {
                props.put(DocumentCommonModel.Props.CASE, archivedCaseRef);
            }
            nodeService.addProperties(docRef, props);
        }
    }

    private void disposeVolumes(List<NodeRef> volumesToDestroy, final Date disposalDate, final String docDeletingComment, final String logMessage) {
        final UserService userService = BeanHelper.getUserService();
        RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        for (final NodeRef volumeNodeRef : volumesToDestroy) {
            // remove all childs
            deleteDocuments(docDeletingComment, retryingTransactionHelper, volumeNodeRef);

            for (NodeRef casRef : BeanHelper.getCaseService().getCaseRefsByVolume(volumeNodeRef)) {
                deleteDocuments(docDeletingComment, retryingTransactionHelper, casRef);
            }

            retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    seriesService.updateContainingDocsCountByVolume(volumeService.getVolumeByNodeRef(volumeNodeRef).getSeriesNodeRef(), volumeNodeRef, false);
                    HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(VolumeModel.Props.STATUS, DocListUnitStatus.DESTROYED.getValueName());
                    props.put(EventPlanModel.Props.DISPOSAL_DATE_TIME, disposalDate);
                    props.put(VolumeModel.Props.CONTAINING_DOCS_COUNT, 0);
                    if (nodeService.hasAspect(volumeNodeRef, DocumentCommonModel.Aspects.DOCUMENT_REG_NUMBERS_CONTAINER)) {
                        props.put(DocumentCommonModel.Props.DOCUMENT_REG_NUMBERS, null);
                    }
                    nodeService.addProperties(volumeNodeRef, props);
                    logService.addLogEntry(LogEntry.create(isCaseFile(volumeNodeRef) ? LogObject.CASE_FILE : LogObject.VOLUME, userService, volumeNodeRef, logMessage));
                    return null;
                }

            }, false, true);
        }

    }

    private void deleteDocuments(final String docDeletingComment, RetryingTransactionHelper retryingTransactionHelper, final NodeRef volumeNodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(volumeNodeRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            final NodeRef nodeRef = childAssoc.getChildRef();
            retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    if (!dictionaryService.isSubClass(nodeService.getType(nodeRef), DocumentCommonModel.Types.DOCUMENT)) {
                        return null;
                    }
                    adrService.addDeletedDocument(nodeRef);
                    // mark for permanent delete
                    nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.DELETE_PERMANENT, null);
                    documentService.deleteDocument(nodeRef, docDeletingComment, DeletionType.DISPOSITION);
                    return null;
                }

            }, false, true);
        }
    }

    @Override
    public List<Function> getArchivedFunctions() {
        return functionsService.getFunctions(getArchivalRoot());
    }

    @Override
    public NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus) {
        return addArchivalActivity(activityType, activityStatus, null, null);
    }

    @Override
    public NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus, List<NodeRef> volumeRefs, NodeRef templateRef) {
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ArchivalsModel.Props.ACTIVITY_TYPE, activityType.name());
        properties.put(ArchivalsModel.Props.STATUS, activityStatus.getValue());
        properties.put(ArchivalsModel.Props.CREATED, new Date());
        String username = AuthenticationUtil.getRunAsUser();
        properties.put(ArchivalsModel.Props.CREATOR_ID, username);
        properties.put(ArchivalsModel.Props.CREATOR_NAME, BeanHelper.getUserService().getUserFullName(username));
        ChildAssociationRef childAssoc = nodeService.createNode(getArchivalActivitiesRoot(), ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY, ArchivalsModel.Assocs.ARCHIVAL_ACTIVITY,
                ArchivalsModel.Types.ARCHIVAL_ACTIVITY, properties);
        NodeRef activityRef = childAssoc.getChildRef();
        if (templateRef != null) {
            documentTemplateService.populateVolumeArchiveTemplate(activityRef, volumeRefs, templateRef);
        }
        return activityRef;
    }

    @Override
    public ArchivalActivity getArchivalActivity(NodeRef archivalActivityRef) {
        WmNode wmNode = new WmNode(archivalActivityRef, ArchivalsModel.Types.ARCHIVAL_ACTIVITY);
        NodeRef docRef = getArchivalActivityDocument(archivalActivityRef);
        String docName = (String) (docRef != null ? nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_NAME) : null);
        List<File> files = getArchivalActivityFiles(archivalActivityRef);
        return new ArchivalActivity(wmNode, docName, docRef, files);
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

    private NodeRef getArchivalRoot() {
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

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
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

    public void setArchivalsStore(String archivalsStore) {
        this.archivalsStore = new StoreRef(archivalsStore);
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

    @Override
    public boolean isSimpleDestructionEnabled() {
        return simpleDestructionEnabled;
    }

    public void setSimpleDestructionEnabled(boolean simpleDestructionEnabled) {
        this.simpleDestructionEnabled = simpleDestructionEnabled;
    }

}
