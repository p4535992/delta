package ee.webmedia.alfresco.testdata;

import static ee.webmedia.alfresco.addressbook.model.AddressbookModel.URI;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getContentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileFolderService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getMimetypeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getRegisterService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getTransactionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowConstantsBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.getPrivsWithDependencies;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.getRequiredPrivsForTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.management.subsystems.DefaultChildApplicationContextManager;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.UnmodifiableCase;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.UnmodifiableSubstitute;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * Test data generator. Can't use any Lucene searches, because lucene indexing may be turned off during test data generation and then lucene search results would be out-of-date.
 */
public class TestDataService implements SaveListener {
    protected final Log log = LogFactory.getLog(getClass());

    private final AtomicBoolean updaterRunning = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    private String dataFolder;

    public boolean isUpdaterRunning() {
        return updaterRunning.get();
    }

    public boolean isUpdaterStopping() {
        return isUpdaterRunning() && stopFlag.get();
    }

    /** @param event */
    public synchronized void stopUpdater(ActionEvent event) {
        stopUpdater();
    }

    public void stopUpdater() {
        stopFlag.set(true);
        log.info("Stop requested.");
    }

    /** @param event */
    public synchronized void executeUpdaterInBackground(ActionEvent event) {
        executeUpdaterInBackground();
    }

    public static class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StopException() {
            //
        }

    }

    private void checkStop() {
        if (stopFlag.get()) {
            throw new StopException();
        }
    }

    public synchronized void executeUpdaterInBackground() {
        if (!isUpdaterRunning()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("Main thread started");
                        updaterRunning.set(true);
                        stopFlag.set(false);
                        AuthenticationUtil.runAs(new RunAsWork<Void>() {
                            @Override
                            public Void doWork() throws Exception {
                                try {
                                    executeUpdater();
                                    return null;
                                } catch (StopException e) {
                                    log.info("Stop requested");
                                } catch (Exception e) {
                                    log.error("Background updater error", e);
                                }
                                return null;
                            }
                        }, AuthenticationUtil.getSystemUserName());
                    } finally {
                        updaterRunning.set(false);
                        log.info("Main thread stopped");
                    }
                }
            }, "TestDataGeneratorThread").start();
        } else {
            log.warn("Updater is already running");
        }
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    private Set<String> userNames;
    private List<String> userNamesList;
    private List<String> usersFirstNames;
    private List<String> usersLastNames;
    private List<String> accessRestrictionReasons;
    private List<String> accessRestrictionEndDescs;
    private List<String> docTitles;
    private List<String> contacts;
    private List<String> orgUnitNames;
    private List<String> regNumbers;
    private List<String> senderRegNumbers;
    private List<List<String>> files;
    private List<Pair<String, String>> functionMarksAndTitles;
    private List<Pair<String, String>> seriesMarksAndTitles;
    private List<Pair<String, String>> volumesMarksAndTitles;
    private List<String> groupNames;
    private List<String> groupLevel1Names;
    private List<String> groupLevel2Names;
    private List<Integer> registerIds;
    private Set<NodeRef> functions;
    private List<NodeRef> functionsList;
    private Set<SerieVO> series;
    private List<SerieVO> seriesWithAnnualOrSubjectVolTypeList;
    private List<SerieVO> seriesWithCaseFilesList;
    private Map<NodeRef, Integer> seriesMaxOrder;
    private Map<NodeRef, SerieVO> seriesByVolumeRef;
    private Set<SerieVO> seriesWithCaseFiles;
    private Set<NodeRef> volumes;
    private List<NodeRef> volumesWithCases;
    private Set<NodeRef> caseFiles;
    private Set<NodeRef> cases;
    private List<Node> contactNodes;
    private List<DocumentLocationVO> docLocations;
    private List<NodeRef> docs;
    private List<NodeRef> independentCompoundWorkflows;
    private Map<String /* fileUrl */, Object /* lock object */> textFileLocks;
    private Object textFileLocksGlobalLock;
    private Random docLocationsRandom;
    private Random usersRandom;
    private Map<String, DocumentTypeVersion> docVersions;
    private Map<String, List<ClassificatorValue>> classificators;
    private List<Pair<RootOrgUnit, List<OrgUnit>>> orgUnits;
    private List<OrganizationStructure> structUnits;
    private Map<String, OrganizationStructure> structUnitsByUnitId;
    List<CaseFileType> caseFileTypes;

    private final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private int orgUnitsCount = 466;
    private int usersCount = 5905;
    private int contactsCount = 2057;
    private int registersCount = 203;
    private int functionsCount = 40;
    private int seriesCount = 370;
    private int volumesCount = 1436;
    private int caseFilesCount = 0;
    private int closedCaseFilesCount = 0;
    private int casesCount = 2057;
    private int documentsCount = 700000; // 1076040 would be max in SpacesStore in 2y2m
    private int independentWorkflowsCount = 0;
    private int finishedIndependentWorkflowsCount = 0;
    private int maxDocumentsInIndependentWorkflow = 0;
    private boolean documentWorkflowsEnabled = true;
    private boolean caseFileWorkflowsEnabled = false;
    private boolean filesEnabled = true;
    private int documentAndWorkflowGeneratorThreads = 1;

    private void executeUpdater() throws Exception {
        usersFirstNames = loadCsv("users-firstnames.csv");
        usersLastNames = loadCsv("users-lastnames.csv");
        accessRestrictionReasons = loadCsv("accessrestrictionreasons.csv");
        accessRestrictionEndDescs = loadCsv("accessrestrictionenddescs.csv");
        functionMarksAndTitles = loadCsvTwoCols("functions.csv");
        seriesMarksAndTitles = loadCsvTwoCols("series.csv");
        volumesMarksAndTitles = loadCsvTwoCols("volumes.csv");
        docTitles = loadCsv("doctitles.csv");
        contacts = loadCsv("contacts.csv");
        orgUnitNames = loadCsv("orgunits.csv");
        regNumbers = loadCsv("regnumbers.csv");
        senderRegNumbers = loadCsv("senderregnumbers.csv");
        files = loadCsvMultiCols("files.csv");
        // transmittalModes?

        // filterFiles();

        // FUTURE: document creation, registration, file creation and workflow times that vary in the past

        createOrgUnits(orgUnitsCount); // TODO fix orgUnits
        createUsers(usersCount);
        if (usersCount > 0) {
            createSubstitutes();
        }
        createGroups();
        createContacts(contactsCount);
        createRegisters(registersCount, documentsCount, seriesCount);
        createFunctions(functionsCount);
        createSeries(seriesCount);
        createVolumes(volumesCount); // TODO et kõikide sarjade all oleks vähemalt üks toimik
        createCaseFiles(caseFilesCount); // TODO et kõikide sarjade all oleks vähemalt üks toimik
        createCases(casesCount); // TODO et kõikide toimikute all oleks vähemalt üks asi
        createDocuments(documentsCount);
        createIndependentCompoundWorkflows(independentWorkflowsCount);

        // TODO arhiivi ka genereerida! fn-sari-toimik-asi võib identsed või samade arvude alusel genereerida vist, aga lihtsalt suletud
        // TODO: kas on vaja genereerida mustandite funktsioone (function.documentActivitiesAreLimited=true)?

        // TODO progressi raporteerimine iga 50 ühiku järel - panna tsükli algusesse, siis on näha kohe
        // TODO statistika kirjutamine csv failidesse

        // TODO vaadata et nimekirjades Menetluses, Registreerimiseks, Saatmata, Saatmisel oleks sobiv arv dokumente

        // ---------------
        // XXX koormustestimisega läbi mängida indekseerimise juhud: kui storeInIndex välja lülitada; kui propertite väärtused indexist küsida
        // XXX koormustestimisega läbi mängida store'de juhud: kas vähem või rohkem store'sid kasulikum
        // XXX koormustestimisel uurida kustutamise aeglust ja workaroundi (öösel kustutamise) sobivust
        // XXX koormustestimisel jälgida gkrellm'iga ja jconsole'iga koormust; veel parem kui millegagi lindistada saaks graafikuid
    }

    private void filterFiles() {
        if (!isFilesEnabled()) {
            return;
        }
        File contentStore = new File(dataFolder, "contentstore");
        File testfiles = new File(contentStore, "testfiles");
        Set<String> existingFiles = new HashSet<String>();
        for (File file : testfiles.listFiles()) {
            if (file.isDirectory()) {
                for (File file2 : file.listFiles()) {
                    if (file2.isFile() && file2.canRead()) {
                        existingFiles.add(file.getName() + "/" + file2.getName());
                    }
                }
            } else if (file.isFile() && file.canRead()) {
                existingFiles.add(file.getName());
            }
        }
        log.info("Found " + existingFiles.size() + " readable files in contentstore/testfiles");
        for (Iterator<List<String>> i = files.iterator(); i.hasNext();) {
            List<String> fileCols = i.next();
            if (!existingFiles.contains(fileCols.get(0))) {
                i.remove();
            }
        }
        log.info("There are " + files.size() + " usable files");
    }

    private static class SerieVO {
        private final NodeRef functionRef;
        private final NodeRef seriesRef;
        private final String mark;
        private final List<String> docType;
        private final List<String> volType;
        private final String accessRestriction;
        private final String accessRestrictionReason;
        private final Date accessRestrictionBeginDate;
        private final Date accessRestrictionEndDate;
        private final String accessRestrictionEndDesc;

        @SuppressWarnings("unchecked")
        public SerieVO(Node node, NodeRef functionRef) {
            this.functionRef = functionRef;
            seriesRef = node.getNodeRef();
            mark = (String) node.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER.toString());
            docType = (List<String>) node.getProperties().get(SeriesModel.Props.DOC_TYPE.toString());
            volType = (List<String>) node.getProperties().get(SeriesModel.Props.VOL_TYPE.toString());
            accessRestriction = (String) node.getProperties().get(SeriesModel.Props.ACCESS_RESTRICTION.toString());
            accessRestrictionReason = (String) node.getProperties().get(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString());
            accessRestrictionBeginDate = (Date) node.getProperties().get(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
            accessRestrictionEndDate = (Date) node.getProperties().get(SeriesModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
            accessRestrictionEndDesc = (String) node.getProperties().get(SeriesModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
        }

        public NodeRef getFunctionRef() {
            return functionRef;
        }

        public NodeRef getSeriesRef() {
            return seriesRef;
        }

        public String getMark() {
            return mark;
        }

        public List<String> getDocType() {
            return docType;
        }

        public List<String> getVolType() {
            return volType;
        }

        public String getAccessRestriction() {
            return accessRestriction;
        }

        public String getAccessRestrictionReason() {
            return accessRestrictionReason;
        }

        public Date getAccessRestrictionBeginDate() {
            return accessRestrictionBeginDate;
        }

        public Date getAccessRestrictionEndDate() {
            return accessRestrictionEndDate;
        }

        public String getAccessRestrictionEndDesc() {
            return accessRestrictionEndDesc;
        }

    }

    private static class DocumentLocationVO {
        private final NodeRef functionRef;
        private final NodeRef seriesRef;
        private final NodeRef volumeRef;
        private final NodeRef caseRef;
        private final List<String> seriesDocType;
        private final String seriesAccessRestriction;
        private final String seriesAccessRestrictionReason;
        private final Date seriesAccessRestrictionBeginDate;
        private final Date seriesAccessRestrictionEndDate;
        private final String seriesAccessRestrictionEndDesc;

        public DocumentLocationVO(SerieVO serie, NodeRef volumeRef) {
            this(serie, volumeRef, null);
        }

        public DocumentLocationVO(SerieVO serie, NodeRef volumeRef, NodeRef caseRef) {
            functionRef = serie.getFunctionRef();
            seriesRef = serie.getSeriesRef();
            this.volumeRef = volumeRef;
            this.caseRef = caseRef;
            seriesDocType = serie.getDocType();
            seriesAccessRestriction = serie.getAccessRestriction();
            seriesAccessRestrictionReason = serie.getAccessRestrictionReason();
            seriesAccessRestrictionBeginDate = serie.getAccessRestrictionBeginDate();
            seriesAccessRestrictionEndDate = serie.getAccessRestrictionEndDate();
            seriesAccessRestrictionEndDesc = serie.getAccessRestrictionEndDesc();
        }

        public NodeRef getFunctionRef() {
            return functionRef;
        }

        public NodeRef getSeriesRef() {
            return seriesRef;
        }

        public NodeRef getVolumeRef() {
            return volumeRef;
        }

        public NodeRef getCaseRef() {
            return caseRef;
        }

        public List<String> getSeriesDocType() {
            return seriesDocType;
        }

        public String getSeriesAccessRestriction() {
            return seriesAccessRestriction;
        }

        public String getSeriesAccessRestrictionReason() {
            return seriesAccessRestrictionReason;
        }

        public Date getSeriesAccessRestrictionBeginDate() {
            return seriesAccessRestrictionBeginDate;
        }

        public Date getSeriesAccessRestrictionEndDate() {
            return seriesAccessRestrictionEndDate;
        }

        public String getSeriesAccessRestrictionEndDesc() {
            return seriesAccessRestrictionEndDesc;
        }

        public NodeRef getDocumentParentRef() {
            return caseRef == null ? volumeRef : caseRef;
        }

    }

    private List<String> loadCsv(String fileName) throws FileNotFoundException, IOException {
        List<String> values = new ArrayList<String>();
        CsvReader reader = new CsvReader(new FileInputStream(new File(dataFolder, fileName)), ';', Charset.forName("UTF-8"));
        try {
            while (reader.readRecord()) {
                String value = stripAndTrim(reader.get(0));
                if (StringUtils.isNotBlank(value)) {
                    values.add(value);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + values.size() + " rows from " + fileName);
        return values;
    }

    private List<Pair<String, String>> loadCsvTwoCols(String fileName) throws FileNotFoundException, IOException {
        List<Pair<String, String>> values = new ArrayList<Pair<String, String>>();
        CsvReader reader = new CsvReader(new FileInputStream(new File(dataFolder, fileName)), ';', Charset.forName("UTF-8"));
        try {
            while (reader.readRecord()) {
                String value = stripAndTrim(reader.get(0));
                if (StringUtils.isNotBlank(value)) {
                    values.add(Pair.newInstance(value, stripAndTrim(reader.get(1))));
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + values.size() + " from " + fileName);
        return values;
    }

    private List<List<String>> loadCsvMultiCols(String fileName) throws FileNotFoundException, IOException {
        List<List<String>> values = new ArrayList<List<String>>();
        CsvReader reader = new CsvReader(new FileInputStream(new File(dataFolder, fileName)), ';', Charset.forName("UTF-8"));
        try {
            while (reader.readRecord()) {
                String value = stripAndTrim(reader.get(0));
                if (StringUtils.isNotBlank(value)) {
                    List<String> list = new ArrayList<String>();
                    for (int i = 0; i < reader.getColumnCount(); i++) {
                        list.add(stripAndTrim(reader.get(i)));
                    }
                    values.add(list);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + values.size() + " from " + fileName);
        return values;
    }

    public static class RootOrgUnit extends OrgUnit {
        protected final int subUnitsCountGoal;
        protected final List<String> secondLevelNameCandidates;

        public RootOrgUnit(String name, int subUnitsCountGoal, List<String> secondLevelNameCandidates) {
            super(name, null, 0, null);
            this.subUnitsCountGoal = subUnitsCountGoal;
            this.secondLevelNameCandidates = secondLevelNameCandidates;
        }

        public void setUnitId(String unitId) {
            Assert.isTrue(StringUtils.isBlank(this.unitId) && StringUtils.isNotBlank(unitId));
            this.unitId = unitId;
        }

        public int getSubUnitsCountGoal() {
            return subUnitsCountGoal;
        }

        public String getAndRemoveSecondLevelName() {
            String name = getRandom(secondLevelNameCandidates);
            secondLevelNameCandidates.remove(name);
            return name;
        }

        public List<OrgUnit> getSubUnits() {
            return subUnits;
        }
    }

    public static class OrgUnit {
        protected String name;
        protected String unitId;
        protected int level;
        protected String path;
        protected List<OrgUnit> subUnits = new ArrayList<OrgUnit>();

        public OrgUnit(String name, String unitId, int level, String parentPath) {
            this.name = name;
            this.unitId = unitId;
            this.level = level;
            path = (StringUtils.isNotBlank(parentPath) ? (parentPath + ", ") : "") + name;
        }

        public String getName() {
            return name;
        }

        public String getUnitId() {
            return unitId;
        }

        public int getLevel() {
            return level;
        }

        public String getPath() {
            return path;
        }

        public void addSubUnit(OrgUnit subUnit) {
            subUnits.add(subUnit);
        }

        public int getSubUnitsCount() {
            int sum = 0;
            for (OrgUnit orgUnit : subUnits) {
                sum += orgUnit.getSubUnitsCount() + 1;
            }
            return sum;
        }

    }

    private void createOrgUnits(int count) {
        structUnits = getOrganizationStructureService().getAllOrganizationStructures();
        structUnitsByUnitId = new HashMap<String, OrganizationStructure>();
        // if (count < 5) {
        // return;
        // }

        orgUnits = new ArrayList<Pair<RootOrgUnit, List<OrgUnit>>>();
        createRootOrgUnit("PPA", (int) (count / 3.19d));
        createRootOrgUnit("Põhja PREF", (int) (count / 5.14d));
        createRootOrgUnit("Lõuna PREF", (int) (count / 5.49d));
        createRootOrgUnit("Ida PREF", (int) (count / 7.6d));
        createRootOrgUnit("Lääne PREF", (int) (count / 5.63d));

        List<String> copy = new ArrayList<String>(contacts);
        for (Iterator<String> i = copy.iterator(); i.hasNext();) {
            String name = i.next();
            if (name.indexOf(",") >= 0) {
                i.remove();
            }
        }

        int maxUnitId = 1;
        for (OrganizationStructure structUnit : structUnits) {
            try {
                maxUnitId = Math.max(maxUnitId, Integer.parseInt(structUnit.getUnitId()));
            } catch (NumberFormatException e) {
                // Ignore
            }
            copy.remove(structUnit.getName());
            structUnitsByUnitId.put(structUnit.getUnitId(), structUnit);
        }

        for (Pair<RootOrgUnit, List<OrgUnit>> pair : orgUnits) {
            RootOrgUnit rootOrgUnit = pair.getFirst();
            for (OrganizationStructure structUnit : structUnits) {
                if (structUnit.getName().equals(rootOrgUnit.getName()) && StringUtils.isBlank(structUnit.getSuperUnitId())) {
                    rootOrgUnit.setUnitId(structUnit.getUnitId());
                    addSubUnits(rootOrgUnit, structUnits, rootOrgUnit.getSubUnits());
                    break;
                }
            }
            if (StringUtils.isBlank(rootOrgUnit.getUnitId())) {
                OrganizationStructure orgStruct = new OrganizationStructure();
                orgStruct.setUnitId(Integer.toString(++maxUnitId));
                orgStruct.setSuperUnitId(null);
                orgStruct.setName(rootOrgUnit.getName());
                orgStruct.setOrganizationPath(UserUtil.getPathHierarchy(rootOrgUnit.getPath(), ","));
                getOrganizationStructureService().createOrganisationStructure(orgStruct);
                structUnits.add(orgStruct);
                structUnitsByUnitId.put(orgStruct.getUnitId(), orgStruct);
                rootOrgUnit.setUnitId(orgStruct.getUnitId());
                log.info("Created orgUnit unitId=" + orgStruct.getUnitId() + " superUnitId=" + orgStruct.getSuperUnitId() + " name=" + orgStruct.getName() + " path="
                        + orgStruct.getOrganizationPath());
            }
            log.info("Found " + rootOrgUnit.getSubUnitsCount() + " subunits for " + rootOrgUnit.getName() + ", goal is " + rootOrgUnit.getSubUnitsCountGoal());
            while (rootOrgUnit.getSubUnitsCount() < rootOrgUnit.getSubUnitsCountGoal()) {
                OrganizationStructure orgStruct = new OrganizationStructure();
                orgStruct.setUnitId(Integer.toString(++maxUnitId));
                OrgUnit parentOrgUnit = getRandom(pair.getSecond());
                if (parentOrgUnit.getLevel() > 2) {
                    continue; // only 3 levels of orgUnits
                }
                orgStruct.setSuperUnitId(parentOrgUnit.getUnitId());
                String name;
                if (parentOrgUnit.getLevel() == 0) {
                    name = rootOrgUnit.getAndRemoveSecondLevelName();
                } else {
                    name = getRandom(copy);
                    copy.remove(name);
                }
                orgStruct.setName(name);
                OrgUnit subUnit = new OrgUnit(orgStruct.getName(), orgStruct.getUnitId(), parentOrgUnit.getLevel() + 1, parentOrgUnit.getPath());
                orgStruct.setOrganizationPath(UserUtil.getPathHierarchy(subUnit.getPath(), ","));
                getOrganizationStructureService().createOrganisationStructure(orgStruct);
                structUnits.add(orgStruct);
                structUnitsByUnitId.put(orgStruct.getUnitId(), orgStruct);
                parentOrgUnit.addSubUnit(subUnit);
                pair.getSecond().add(subUnit);
                log.info("Created orgUnit unitId=" + orgStruct.getUnitId() + " superUnitId=" + orgStruct.getSuperUnitId() + " name=" + orgStruct.getName() + " path="
                        + orgStruct.getOrganizationPath());
            }
        }
    }

    private void addSubUnits(OrgUnit parentOrgUnit, List<OrganizationStructure> structUnits, List<OrgUnit> subUnits) {
        for (OrganizationStructure structUnit : structUnits) {
            if (structUnit.getSuperUnitId() == parentOrgUnit.getUnitId()) {
                OrgUnit subUnit = new OrgUnit(structUnit.getName(), structUnit.getUnitId(), parentOrgUnit.getLevel() + 1, parentOrgUnit.getPath());
                parentOrgUnit.addSubUnit(subUnit);
                subUnits.add(subUnit);
                addSubUnits(subUnit, structUnits, subUnits);
            }
        }
    }

    private void createRootOrgUnit(String name, int subUnitsCountGoal) {
        RootOrgUnit rootOrgUnit = new RootOrgUnit(name, subUnitsCountGoal, new ArrayList<String>(orgUnitNames));
        List<OrgUnit> subUnits = new ArrayList<OrgUnit>();
        subUnits.add(rootOrgUnit);
        orgUnits.add(Pair.newInstance(rootOrgUnit, subUnits));
    }

    private void createUsers(int count) throws Exception {
        log.info("Creating users");
        userDataByUserName = new ConcurrentHashMap<String, Pair<NodeRef, Map<QName, Serializable>>>();
        DefaultChildApplicationContextManager authentication = BeanHelper.getSpringBean(DefaultChildApplicationContextManager.class, "Authentication");
        Collection<String> instanceIds = authentication.getInstanceIds();
        Set<String> zones = new HashSet<String>();
        zones.add(AuthorityService.ZONE_APP_DEFAULT);
        for (String instanceId : instanceIds) {
            zones.add(AuthorityService.ZONE_AUTH_EXT_PREFIX + instanceId);
        }
        log.info("instanceIds = " + instanceIds + "  zones = " + zones);

        userNames = BeanHelper.getAuthorityService().getAllAuthorities(AuthorityType.USER);
        userNames.remove(AuthenticationUtil.getSystemUserName());
        int i = 1;
        while (userNames.size() < count) {
            checkStop();
            String userName = "39900000000".substring(0, 11 - Integer.toString(i).length()) + Integer.toString(i);
            createUser(userName, userNames, zones);
            i++;
        }
        log.info("There are " + userNames.size() + " users; goal was " + count + " users");

        List<Map<QName, Serializable>> userProps = UserUtil.getFilteredTaskOwnerStructUnitUsersProps(userNames, BeanHelper.getNodeService(), BeanHelper.getParametersService(),
                BeanHelper.getDocumentSearchService(), BeanHelper.getUserService());
        File filename = new File(dataFolder, "users.csv");
        CsvWriter writer = new CsvWriter(new FileOutputStream(filename), ';', Charset.forName("UTF-8"));
        try {
            for (Map<QName, Serializable> userProp : userProps) {
                writer.writeRecord(new String[] { (String) userProp.get(ContentModel.PROP_USERNAME) });
            }
        } finally {
            writer.close();
        }
        log.info("Wrote " + userProps.size() + " usernames to file " + filename);

        filename = new File(dataFolder, "usersfirstlastnames.csv");
        writer = new CsvWriter(new FileOutputStream(filename), ';', Charset.forName("UTF-8"));
        try {
            for (Map<QName, Serializable> userProp : userProps) {
                writer.writeRecord(new String[] { (String) userProp.get(ContentModel.PROP_FIRSTNAME), (String) userProp.get(ContentModel.PROP_LASTNAME) });
            }
        } finally {
            writer.close();
        }
        log.info("Wrote " + userProps.size() + " user first names, last names to file " + filename);

        userNamesList = new ArrayList<String>(userNames);
        Collections.shuffle(userNamesList);
    }

    private Map<String, Pair<NodeRef, Map<QName, Serializable>>> userDataByUserName;

    private void createSubstitutes() {
        if (userNamesList.size() < 2) {
            return;
        }
        Set<String> workingUsers = new HashSet<String>();
        Set<String> restingUsers = new HashSet<String>();
        for (String userName : userNames) {
            List<UnmodifiableSubstitute> subs = getSubstituteService().getUnmodifiableSubstitutes(getUserData(userName).getFirst());
            for (UnmodifiableSubstitute sub : subs) {
                if (sub.isActive()) {
                    restingUsers.add(userName);
                    workingUsers.remove(userName);
                    if (!restingUsers.contains(sub.getSubstituteId())) {
                        workingUsers.add(sub.getSubstituteId());
                    }
                }
            }
        }
        HashSet<String> userNamesToProcess = new HashSet<String>(userNames);
        userNamesToProcess.removeAll(restingUsers);
        userNamesToProcess.removeAll(workingUsers);
        log.info("Found " + restingUsers.size() + " resting users, " + workingUsers.size() + " working users, " + userNamesToProcess.size() + " unknown users to process");
        int i = userNames.size() / 2;
        if (restingUsers.size() >= i) {
            log.info("There are " + restingUsers.size() + " resting users, goal was " + i + " resting users");
            return;
        }
        List<String> userNamesToProcessList = new ArrayList<String>(userNamesToProcess);
        Collections.shuffle(userNamesToProcessList);
        int usersNeededCount = i - restingUsers.size();
        if (userNamesToProcessList.size() < usersNeededCount) {
            usersNeededCount = userNamesToProcessList.size();
        }
        List<String> userNamesToAddToWorking = userNamesToProcessList.subList(usersNeededCount, userNamesToProcessList.size());
        List<String> userNamesToSendToRest = userNamesToProcessList.subList(0, usersNeededCount);
        log.info("Adding " + userNamesToAddToWorking.size() + " users to working list; sending " + userNamesToSendToRest.size() + " users to rest");
        workingUsers.addAll(userNamesToAddToWorking);
        List<String> workingUsersList = new ArrayList<String>(workingUsers);
        for (String restingUserName : userNamesToSendToRest) {
            String workingUserName = getRandom(workingUsersList);
            Pair<NodeRef, Map<QName, Serializable>> workingUserData = getUserData(workingUserName);
            Substitute sub = new Substitute();
            sub.setSubstituteId(workingUserName);
            String workingUserFullName = UserUtil.getPersonFullName1(workingUserData.getSecond());
            sub.setSubstituteName(workingUserFullName);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, ((int) (Math.random() * -60)) - 30);
            Date beginDate = cal.getTime();
            sub.setSubstitutionStartDate(beginDate);
            cal = Calendar.getInstance();
            cal.add(Calendar.DATE, ((int) (Math.random() * 60)) + 30);
            Date endDate = cal.getTime();
            sub.setSubstitutionEndDate(endDate);
            Pair<NodeRef, Map<QName, Serializable>> restingUserData = getUserData(restingUserName);
            getSubstituteService().addSubstitute(restingUserData.getFirst(), sub);
            String restingUserFullName = UserUtil.getPersonFullName1(restingUserData.getSecond());
            restingUsers.add(restingUserName);
            log.info("Created substitution: restingUser = " + restingUserFullName + ", workingUser = " + workingUserFullName + ", period = " + dateFormat.format(beginDate) + " - "
                    + dateFormat.format(endDate));
        }
        log.info("There are " + restingUsers.size() + " resting users, goal was " + i + " resting users");
    }

    private Pair<NodeRef, Map<QName, Serializable>> getUserData(String userName) {
        Pair<NodeRef, Map<QName, Serializable>> userData = userDataByUserName.get(userName);
        if (userData == null) {
            NodeRef userRef = getPerson(userName);
            Map<QName, Serializable> userProps = getNodeService().getProperties(userRef);
            userData = Pair.newInstance(userRef, userProps);
            userDataByUserName.put(userName, userData);
        }
        return userData;
    }

    private void createUser(final String userName, Set<String> userNames, Set<String> zones) {
        if (userNames.contains(userName)) {
            return;
        }
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        String firstName = getRandom(usersFirstNames);
        properties.put(ContentModel.PROP_FIRSTNAME, firstName);
        String lastName = getRandom(usersLastNames);
        properties.put(ContentModel.PROP_LASTNAME, lastName);
        properties.put(ContentModel.PROP_EMAIL, testEmail);
        OrganizationStructure structUnit = getRandom(structUnits);
        properties.put(ContentModel.PROP_ORGID, structUnit.getUnitId());
        properties.put(ContentModel.PROP_ORGANIZATION_PATH, (ArrayList<String>) structUnit.getOrganizationPath());

        // FUTURE: Jäävad praegu tühjaks, sest sisulist kasu ei ole
        // properties.put(ContentModel.PROP_TELEPHONE, reader.get(4));
        // properties.put(ContentModel.PROP_JOBTITLE, reader.get(5));
        // properties.put(ContentModel.PROP_COUNTY, reader.get(6));
        // properties.put(ContentModel.PROP_MUNICIPALITY, reader.get(7));
        // properties.put(ContentModel.PROP_VILLAGE, reader.get(8));
        // properties.put(ContentModel.PROP_STREET_HOUSE, reader.get(9));
        // properties.put(ContentModel.PROP_POSTAL_CODE, reader.get(10));
        // properties.put(ContentModel.PROP_SERVICE_RANK, reader.get(11));

        getPersonService().createPerson(properties, zones);
        getPerson(userName);
        userNames.add(userName);
        log.info("Created user " + userName + " " + firstName + " " + lastName);
    }

    private NodeRef getPerson(final String userName) {
        return BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {

            @Override
            public NodeRef execute() throws Throwable {
                return getPersonService().getPerson(userName); // to create home folder
            }

        }, false);
    }

    private String testEmail;

    public String getTestEmail() {
        return testEmail;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    private void createFunctions(int count) {
        // DOK.LOETELU
        NodeRef rootRef = getFunctionsService().getFunctionsRoot();

        // List<ArchivalsStoreVO> archivalsStoreVOs = new ArrayList<ArchivalsStoreVO>(getGeneralService().getArchivalsStoreVOs());
        // ARHIVAALIDE LOETELU
        // NodeRef rootRef = archivalsStoreVOs.get(0).getNodeRef();
        // 1. ARHIIVIMOODUSTAJA
        // NodeRef rootRef = archivalsStoreVOs.get(1).getNodeRef();
        // 2. ARHIIVIMOODUSTAJA
        // NodeRef rootRef = archivalsStoreVOs.get(2).getNodeRef();

        List<UnmodifiableFunction> allFunctions = getFunctionsService().getFunctions(rootRef);
        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(functionMarksAndTitles);
        int order = 1;
        functions = new HashSet<NodeRef>();
        for (UnmodifiableFunction function : allFunctions) {
            for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                Pair<String, String> pair = i.next();
                if (pair.getFirst().equals(stripAndTrim(function.getMark())) && pair.getSecond().equals(stripAndTrim(function.getTitle()))) {
                    i.remove();
                }
            }
            functions.add(function.getNodeRef());
            order = Math.max(function.getOrder(), order);
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        while (i.hasNext() && functions.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            Function function = getFunctionsService().createFunction();
            String mark = entry.getFirst();
            Map<String, Object> props = function.getNode().getProperties();
            props.put(FunctionsModel.Props.MARK.toString(), mark);
            String title = entry.getSecond();
            props.put(FunctionsModel.Props.TITLE.toString(), title);
            props.put(FunctionsModel.Props.TYPE.toString(), mark.indexOf(".") == -1 ? "funktsioon" : "allfunktsioon");
            props.put(FunctionsModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
            props.put(FunctionsModel.Props.ORDER.toString(), order + 1);
            props.put(FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED.toString(), false);
            getFunctionsService().saveOrUpdate(function, rootRef);
            functions.add(function.getNodeRef());
            order = Math.max(function.getOrder(), order);
            log.info("Created function " + mark + " " + title);
        }
        log.info("There are " + functions.size() + " functions; goal was " + count + " functions");
        Assert.isTrue(functions.size() >= count);
        functionsList = new ArrayList<NodeRef>(functions);
        Collections.shuffle(functionsList);
    }

    private void createRegisters(int count, int documentsCount, int seriesCount) {
        List<Register> registers = getRegisterService().getRegisters();
        registerIds = new ArrayList<Integer>();
        for (Register register : registers) {
            registerIds.add(register.getId());
        }
        while (registerIds.size() < count) {
            checkStop();
            Node reg = getRegisterService().createRegister();
            Map<String, Object> props = reg.getProperties();
            props.put(RegisterModel.Prop.NAME.toString(), getRandom(seriesMarksAndTitles).getSecond());
            props.put(RegisterModel.Prop.COUNTER.toString(), seriesCount == 0 ? 0 : (int) (Math.random() * (documentsCount / seriesCount)));
            props.put(RegisterModel.Prop.AUTO_RESET.toString(), Boolean.valueOf(Math.random() < 0.2d));
            getRegisterService().updateProperties(reg);
            registerIds.add((Integer) props.get(RegisterModel.Prop.ID.toString()));
        }
        log.info("There are " + registerIds.size() + " registers; goal was " + count + " registers");
        Collections.shuffle(registerIds);
    }

    private void createSeries(int count) {
        Random registersRandom = new Random();
        Random functionsRandom = new Random();
        Random groupLevel2Random = new Random();

        List<DocumentType> documentTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        for (Iterator<DocumentType> i = documentTypes.iterator(); i.hasNext();) {
            DocumentType docType = i.next();
            if (SystematicDocumentType.INCOMING_LETTER.isSameType(docType.getId()) || SystematicDocumentType.OUTGOING_LETTER.isSameType(docType.getId())) {
                i.remove();
            }
        }
        Random docTypesCountRandom = new Random();

        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(seriesMarksAndTitles);
        series = new HashSet<TestDataService.SerieVO>();
        seriesWithCaseFiles = new HashSet<TestDataService.SerieVO>();
        seriesWithAnnualOrSubjectVolTypeList = new ArrayList<SerieVO>();
        seriesMaxOrder = new HashMap<NodeRef, Integer>();
        SeriesService seriesService = getSeriesService();
        for (NodeRef functionRef : functions) {
            checkStop();
            List<NodeRef> allSeries = seriesService.getAllSeriesRefsByFunction(functionRef);
            for (NodeRef seriesRef : allSeries) {
                Series serie = seriesService.getSeriesByNodeRef(seriesRef);
                for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                    Pair<String, String> pair = i.next();
                    if (pair.getFirst().equals(stripAndTrim(serie.getSeriesIdentifier())) && pair.getSecond().equals(stripAndTrim(serie.getTitle()))) {
                        i.remove();
                    }
                }
                Integer order = seriesMaxOrder.get(functionRef);
                if (order == null) {
                    order = 1;
                } else {
                    order = Math.max(order, serie.getOrder());
                }
                seriesMaxOrder.put(serie.getFunctionNodeRef(), order);
                SerieVO serieVO = new SerieVO(serie.getNode(), serie.getFunctionNodeRef());
                series.add(serieVO);
                List<String> volTypes = serieVO.getVolType();
                if (volTypes.contains(VolumeType.CASE_FILE.name())) {
                    seriesWithCaseFiles.add(serieVO);
                }
                if (volTypes.contains(VolumeType.ANNUAL_FILE.name()) || volTypes.contains(VolumeType.SUBJECT_FILE.name())) {
                    seriesWithAnnualOrSubjectVolTypeList.add(serieVO);
                }
            }
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        while (i.hasNext() && series.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            createSerie(entry, count, documentTypes, docTypesCountRandom, functionsRandom, registersRandom, groupLevel2Random);
        }
        while (series.size() < count) {
            checkStop();
            createSerie(getRandom(seriesMarksAndTitles), count, documentTypes, docTypesCountRandom, functionsRandom, registersRandom, groupLevel2Random);
        }
        log.info("There are " + series.size() + " series; goal was " + count + " series");
        seriesWithCaseFilesList = new ArrayList<SerieVO>(seriesWithCaseFiles);
        Collections.shuffle(seriesWithAnnualOrSubjectVolTypeList);
    }

    private void createSerie(Pair<String, String> entry, int count, List<DocumentType> documentTypes, Random docTypesCountRandom, Random functionsRandom, Random registersRandom,
            Random groupLevel2Random) {
        String mark = entry.getFirst();
        String title = entry.getSecond();

        NodeRef functionRef = getRandomGaussian2(functionsList, functionsRandom);
        Series serie = getSeriesService().createSeries(functionRef);
        Map<String, Object> props = serie.getNode().getProperties();
        props.put(SeriesModel.Props.TYPE.toString(), mark.indexOf(".") == -1 ? "sari" : "allsari");
        props.put(SeriesModel.Props.SERIES_IDENTIFIER.toString(), mark);
        props.put(SeriesModel.Props.TITLE.toString(), title);
        props.put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, (int) (Math.random() * -2d * count)); // ~3 years back, when there are 500 series
        props.put(SeriesModel.Props.VALID_FROM_DATE.toString(), cal.getTime());
        Integer order = seriesMaxOrder.get(functionRef);
        if (order == null) {
            order = 1;
        } else {
            order = order + 1;
        }
        props.put(SeriesModel.Props.ORDER.toString(), order);
        ArrayList<String> docType = new ArrayList<String>();
        if (Math.random() < 0.6d) {
            docType.add(SystematicDocumentType.INCOMING_LETTER.getId());
        }
        if (Math.random() < 0.6d) {
            docType.add(SystematicDocumentType.OUTGOING_LETTER.getId());
        }
        int docTypesCount = getRandomGaussian3(docTypesCountRandom, documentTypes.size());
        for (int j = 0; j < docTypesCount; j++) {
            String docTypeId = getRandom(documentTypes).getId();
            if (docType.contains(docTypeId)) {
                continue;
            }
            docType.add(docTypeId);
        }
        props.put(SeriesModel.Props.DOC_TYPE.toString(), docType);
        props.put(SeriesModel.Props.REGISTER.toString(), getRandomGaussian3(registerIds, registersRandom));
        props.put(SeriesModel.Props.DOC_NUMBER_PATTERN.toString(), "{S}/{DN}");
        ArrayList<String> volType = new ArrayList<String>();
        if (caseFilesCount <= 0) {
            double random = Math.random() * 3;
            if (random < 1.0d) {
                volType.add(VolumeType.ANNUAL_FILE.name());
                volType.add(VolumeType.SUBJECT_FILE.name());
            } else if (random < 2.0d) {
                volType.add(VolumeType.SUBJECT_FILE.name());
            } else {
                volType.add(VolumeType.ANNUAL_FILE.name());
            }
        } else {
            double random = Math.random() * 7;
            if (random < 1.0d) {
                volType.add(VolumeType.ANNUAL_FILE.name());
                volType.add(VolumeType.SUBJECT_FILE.name());
                volType.add(VolumeType.CASE_FILE.name());
            } else if (random < 2.0d) {
                volType.add(VolumeType.SUBJECT_FILE.name());
                volType.add(VolumeType.CASE_FILE.name());
            } else if (random < 3.0d) {
                volType.add(VolumeType.ANNUAL_FILE.name());
                volType.add(VolumeType.CASE_FILE.name());
            } else if (random < 4.0d) {
                volType.add(VolumeType.ANNUAL_FILE.name());
                volType.add(VolumeType.SUBJECT_FILE.name());
            } else if (random < 5.0d) {
                volType.add(VolumeType.ANNUAL_FILE.name());
            } else if (random < 6.0d) {
                volType.add(VolumeType.SUBJECT_FILE.name());
            } else {
                volType.add(VolumeType.CASE_FILE.name());
            }
        }
        props.put(SeriesModel.Props.VOL_TYPE.toString(), volType);
        String accessRestriction;
        if (Math.random() < 0.9d) {
            accessRestriction = AccessRestriction.AK.getValueName();
            props.put(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString(), getRandom(accessRestrictionReasons));
            props.put(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), cal.getTime());
            if (Math.random() < 0.15d) {
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), getRandom(accessRestrictionEndDescs));
            } else {
                cal.add(Calendar.YEAR, (int) (Math.random() * 75));
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), cal.getTime());
            }
        } else {
            accessRestriction = AccessRestriction.OPEN.getValueName();
        }
        props.put(SeriesModel.Props.ACCESS_RESTRICTION.toString(), accessRestriction);
        getSeriesService().saveOrUpdateWithoutReorder(serie);
        seriesMaxOrder.put(functionRef, order);

        SerieVO newSerie = new SerieVO(serie.getNode(), functionRef);
        series.add(newSerie);
        if (volType.contains(VolumeType.CASE_FILE.name())) {
            seriesWithCaseFiles.add(newSerie);
        }
        if (volType.contains(VolumeType.ANNUAL_FILE.name()) || volType.contains(VolumeType.SUBJECT_FILE.name())) {
            seriesWithAnnualOrSubjectVolTypeList.add(newSerie);
        }

        // dok.halduritele lisada 2 priv alati
        // 20'st sarjast:
        // 11 sarjal * (3-4 II taseme gruppi; 2 priv oli niipaljudel gruppidel: 1,1,3,1,2,1,1,2,1,1,2)
        // 2 sarjal * (5-6 II taseme gruppi; 2 priv oli niipaljudel gruppidel: 1)
        // 1 sarjal * (4 I taseme gruppi, kõik 2 priv)
        // 6 sarjal * (4 I taseme gruppi, kõik 1 priv) + (10 II taseme gruppi, 20% 2 priv (niipaljudel gruppidel 2,2,2,2,2,1,3))

        NodeRef seriesRef = serie.getNode().getNodeRef();
        getPrivilegeService().setPermissions(seriesRef, getUserService().getDocumentManagersGroup(), priv2);

        double r = Math.random();
        if (r <= 0.55d) {
            int groupCount = ((int) (Math.random() * 2)) + 3; // 3 - 4
            int priv2Count;
            double r2 = Math.random();
            if (r2 < 0.64d) {
                priv2Count = 1;
            } else if (r2 < 0.90d) {
                priv2Count = 2;
            } else {
                priv2Count = 3;
            }
            for (int i = 0; i < groupCount; i++) {
                String auth = getRandomGaussian2(groupLevel2Names, groupLevel2Random);
                getPrivilegeService().setPermissions(seriesRef, auth, i + 1 <= priv2Count ? priv2 : priv1);
            }
        } else if (r <= 0.65d) {
            int groupCount = ((int) (Math.random() * 2)) + 5; // 5 - 6
            int priv2Count;
            double r2 = Math.random();
            if (r2 < 0.40d) {
                priv2Count = 1;
            } else if (r2 < 0.72d) {
                priv2Count = 2;
            } else if (r2 < 0.92d) {
                priv2Count = 2;
            } else {
                priv2Count = 3;
            }
            for (int i = 0; i < groupCount; i++) {
                String auth = getRandomGaussian2(groupLevel2Names, groupLevel2Random);
                getPrivilegeService().setPermissions(seriesRef, auth, i + 1 <= priv2Count ? priv2 : priv1);
            }
        } else if (r <= 0.70d) {
            for (int i = 0; i < Math.min(4, groupLevel1Names.size() - 1); i++) {
                getPrivilegeService().setPermissions(seriesRef, groupLevel1Names.get(i), priv2);
            }
        } else {
            List<String> groupLevel2NamesCopy = new ArrayList<String>(groupLevel2Names);
            for (int i = 0; i < Math.min(4, groupLevel1Names.size() - 1); i++) {
                getPrivilegeService().setPermissions(seriesRef, groupLevel1Names.get(i), priv1);
                for (Iterator<String> it = groupLevel2NamesCopy.iterator(); it.hasNext();) {
                    String groupLevel2Name = it.next();
                    if (groupLevel2Name.startsWith(groupLevel1Names.get(i) + ",")) {
                        it.remove();
                    }
                }
            }
            Collections.shuffle(groupLevel2NamesCopy);
            for (int i = 0; i < Math.min(10, groupLevel2NamesCopy.size()); i++) {
                getPrivilegeService().setPermissions(seriesRef, groupLevel2NamesCopy.get(i), Math.random() <= 0.2d ? priv2 : priv1);
            }
        }

        log.info("Created series " + mark + " " + title);
    }

    private final Set<Privilege> priv1 = new HashSet<Privilege>(Arrays.asList(Privilege.VIEW_DOCUMENT_FILES));
    private final Set<Privilege> priv2 = new HashSet<Privilege>(Arrays.asList(Privilege.VIEW_DOCUMENT_FILES, Privilege.EDIT_DOCUMENT));

    private void createVolumes(int count) {
        Random seriesRandom = new Random();
        Random volumePrivilegesRandom = new Random();

        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(volumesMarksAndTitles);
        volumes = new HashSet<NodeRef>();
        volumesWithCases = new ArrayList<NodeRef>();
        seriesByVolumeRef = new HashMap<NodeRef, SerieVO>();
        docLocations = new ArrayList<DocumentLocationVO>();
        for (SerieVO serie : series) {
            checkStop();
            List<UnmodifiableVolume> allVolumes = getVolumeService().getAllVolumesBySeries(serie.getSeriesRef());
            for (UnmodifiableVolume volume : allVolumes) {
                if (volume.isDynamic()) {
                    // case files are handeled in createCaseFiles
                    continue;
                }
                for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                    Pair<String, String> pair = i.next();
                    if (pair.getFirst().equals(stripAndTrim(volume.getVolumeMark())) && pair.getSecond().equals(stripAndTrim(volume.getTitle()))) {
                        i.remove();
                    }
                }
                NodeRef volumeRef = volume.getNodeRef();
                volumes.add(volumeRef);
                if (volume.isContainsCases()) {
                    volumesWithCases.add(volumeRef);
                }
                docLocations.add(new DocumentLocationVO(serie, volumeRef));
                seriesByVolumeRef.put(volumeRef, serie);
            }
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        while (i.hasNext() && volumes.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            createVolume(entry, count, seriesRandom, volumePrivilegesRandom.nextDouble() >= 0.77d);
        }
        while (volumes.size() < count) {
            checkStop();
            createVolume(getRandom(volumesMarksAndTitles), count, seriesRandom, volumePrivilegesRandom.nextDouble() >= 0.77d);
        }
        log.info("There are " + volumes.size() + " volumes; goal was " + count + " volumes");
        Collections.shuffle(volumesWithCases);
    }

    private void createCaseFiles(int count) {
        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(volumesMarksAndTitles);
        caseFileTypes = BeanHelper.getDocumentAdminService().getAllCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
        if (count > 0 && (caseFileTypes == null || caseFileTypes.isEmpty())) {
            log.error("There are no case file types defined; skipping case file generation.");
            return;
        }
        caseFiles = new HashSet<NodeRef>();
        // seriesByCaseFileRef = new HashMap<NodeRef, SerieVO>();
        for (SerieVO serie : series) {
            checkStop();
            List<UnmodifiableVolume> allVolumes = getVolumeService().getAllVolumesBySeries(serie.getSeriesRef());
            for (UnmodifiableVolume volume : allVolumes) {
                if (!volume.isDynamic()) {
                    // volumes (i.e. not case files) are handeled in createVolume
                    continue;
                }
                for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                    Pair<String, String> pair = i.next();
                    if (pair.getFirst().equals(stripAndTrim(volume.getVolumeMark())) && pair.getSecond().equals(stripAndTrim(volume.getTitle()))) {
                        i.remove();
                    }
                }
                NodeRef caseFileRef = volume.getNodeRef();
                caseFiles.add(caseFileRef);
                // seriesByCaseFileRef.put(caseFileRef, serie);
            }
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        Random caseFileLocationsRandom = new Random();
        double closedCaseFilesLikelyhood = caseFilesCount == 0 ? 0 : closedCaseFilesCount / caseFilesCount;
        Random workflowRandom = new Random();
        while (i.hasNext() && caseFiles.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            createCaseFile(entry, count, caseFileLocationsRandom, closedCaseFilesLikelyhood, workflowRandom);
        }
        while (caseFiles.size() < count) {
            checkStop();
            createCaseFile(getRandom(volumesMarksAndTitles), count, caseFileLocationsRandom, closedCaseFilesLikelyhood, workflowRandom);
        }
        log.info("There are " + caseFiles.size() + " case files; goal was " + count + " case files");
    }

    private void createVolume(Pair<String, String> entry, int count, Random seriesRandom, boolean addPrivileges) {
        String mark = entry.getFirst();
        String title = entry.getSecond();

        SerieVO serie = getRandomGaussian2(seriesWithAnnualOrSubjectVolTypeList, seriesRandom);
        Volume volume = getVolumeService().createVolume(serie.getSeriesRef());
        Map<String, Object> props = volume.getNode().getProperties();
        List<String> volumeTypes = new ArrayList<String>(serie.getVolType());
        volumeTypes.remove(VolumeType.CASE_FILE.name());
        VolumeType volumeType = VolumeType.valueOf(getRandom(volumeTypes));
        if (VolumeType.SUBJECT_FILE.equals(volumeType)) {
            mark = serie.getMark();
        }
        props.put(VolumeModel.Props.VOLUME_MARK.toString(), mark);
        props.put(VolumeModel.Props.TITLE.toString(), title);
        props.put(VolumeModel.Props.VOLUME_TYPE.toString(), volumeType.name());
        boolean isContainsCases = Math.random() < 0.5;
        props.put(VolumeModel.Props.CONTAINS_CASES.toString(), Boolean.valueOf(isContainsCases));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, (int) (Math.random() * (count / -5d))); // ~3 years back, when there are 5000 volumes
        props.put(VolumeModel.Props.VALID_FROM.toString(), cal.getTime());
        props.put(VolumeModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        getVolumeService().saveOrUpdate(volume);
        NodeRef volumeRef = volume.getNode().getNodeRef();
        volumes.add(volumeRef);
        if (isContainsCases) {
            volumesWithCases.add(volumeRef);
        }
        docLocations.add(new DocumentLocationVO(serie, volumeRef));
        seriesByVolumeRef.put(volumeRef, serie);

        if (addPrivileges) {
            List<String> groupLevel2NamesCopy = new ArrayList<String>(groupLevel2Names);
            for (AccessPermission permission : getPermissionService().getAllSetPermissions(serie.getSeriesRef())) {
                for (Iterator<String> it = groupLevel2NamesCopy.iterator(); it.hasNext();) {
                    String groupLevel2Name = it.next();
                    if (groupLevel2Name.startsWith(permission.getAuthority() + ",")) {
                        it.remove();
                    }
                }
            }
            int privCount = ((int) (Math.random() * 8)) + 3; // 3 - 10
            privCount = Math.min(privCount, groupLevel2NamesCopy.size());
            for (int i = 0; i < privCount; i++) {
                getPrivilegeService().setPermissions(volumeRef, getRandom(groupLevel2NamesCopy), Math.random() <= 0.2d ? priv2 : priv1);
            }
        }

        log.info("Created volume " + mark + " " + title);
    }

    private void createCaseFile(Pair<String, String> entry, int count, Random seriesRandom, double closedCaseFilesLikelyhood, Random workflowRandom) {
        String mark = entry.getFirst();
        String title = entry.getSecond();

        SerieVO serie = getRandomGaussian2(seriesWithCaseFilesList, seriesRandom);
        String typeId = getRandom(caseFileTypes).getId();
        CaseFile caseFile = BeanHelper.getCaseFileService().createNewCaseFile(typeId, serie.getSeriesRef()).getFirst();

        Map<String, Object> props = caseFile.getNode().getProperties();
        String volumeType = getRandom(serie.getVolType());
        props.put(VolumeModel.Props.VOLUME_MARK.toString(), mark);
        props.put(VolumeModel.Props.TITLE.toString(), title);

        Calendar cal = Calendar.getInstance();
        // TODO: check how this calculation should be changed
        cal.add(Calendar.DATE, (int) (Math.random() * (count / -5d)));
        props.put(DocumentDynamicModel.Props.VALID_FROM.toString(), cal.getTime());
        DocListUnitStatus status = DocListUnitStatus.OPEN;
        if (Math.random() < closedCaseFilesLikelyhood) {
            status = DocListUnitStatus.CLOSED;
        }
        props.put(DocumentDynamicModel.Props.STATUS.toString(), status.getValueName());

        caseFile.setFunction(serie.getFunctionRef());
        caseFile.setSeries(serie.getSeriesRef());

        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = getDocumentConfigService().getPropertyDefinitions(caseFile.getNode());
        for (Pair<DynamicPropertyDefinition, Field> pair : propDefs.values()) {
            DynamicPropertyDefinition propDef = pair.getFirst();
            Field field = pair.getSecond();
            if (field == null) {
                continue;
            }

            Node propNode = caseFile.getNode();
            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (hierarchy != null) {
                int i = 0;
                while (i < hierarchy.length) {
                    propNode = propNode.getAllChildAssociations(hierarchy[i]).get(0);
                    i++;
                }
            }

            QName propName = field.getQName();
            Serializable value = (Serializable) propNode.getProperties().get(propName.toString());
            String systematicGroupName = null;
            if (field.getParent() instanceof FieldGroup && ((FieldGroup) field.getParent()).isSystematic()) {
                systematicGroupName = ((FieldGroup) field.getParent()).getName();
            }

            if (SystematicFieldGroupNames.DOCUMENT_OWNER.equals(systematicGroupName)) {
                continue;
            } else if (SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(systematicGroupName)) {
                continue;
            }

            value = setPropValueIfNeeded(propDef, field, propNode, propName, value);

        }
        NodeRef caseFileRef = caseFile.getNodeRef();

        getNodeService().addAspect(caseFileRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
        getCaseFileService().update(caseFile, Arrays.asList("TestDataService"));

        int allTaskCount = 0;
        if (caseFileWorkflowsEnabled) {
            int compoundWorkflowCount = getCompoundWorkflowCount(workflowRandom);
            for (int i = 0; i < compoundWorkflowCount; i++) {
                Pair<NodeRef, Integer> pair = createWorkflows(caseFileRef, status == DocListUnitStatus.OPEN, typeId, CompoundWorkflowType.CASE_FILE_WORKFLOW);
                allTaskCount += pair.getSecond();
            }
        }

        // FUTURE: õiguseid (nii dokumendi kui sarja omad) praegu ei tee

        caseFiles.add(caseFileRef);
        seriesByVolumeRef.put(caseFileRef, serie);
        log.info("Created caseFile " + mark + " " + title + (caseFileWorkflowsEnabled ? ", " + allTaskCount + " tasks" : ""));
    }

    private void createCases(int count) {
        List<String> fnSerVolTitles = new ArrayList<String>();
        for (Pair<String, String> markAndTitle : functionMarksAndTitles) {
            fnSerVolTitles.add(markAndTitle.getSecond());
        }
        for (Pair<String, String> markAndTitle : seriesMarksAndTitles) {
            fnSerVolTitles.add(markAndTitle.getSecond());
        }
        for (Pair<String, String> markAndTitle : volumesMarksAndTitles) {
            fnSerVolTitles.add(markAndTitle.getSecond());
        }
        List<String> copy = new ArrayList<String>(fnSerVolTitles);

        Random volumesRandom = new Random();

        cases = new HashSet<NodeRef>();
        for (NodeRef volumeRef : volumesWithCases) {
            checkStop();
            List<UnmodifiableCase> allCases = getCaseService().getAllCasesByVolume(volumeRef);
            for (UnmodifiableCase case1 : allCases) {
                for (Iterator<String> i = copy.iterator(); i.hasNext();) {
                    String title = i.next();
                    if (title.equals(stripAndTrim(case1.getTitle()))) {
                        i.remove();
                    }
                }
                NodeRef caseRef = case1.getNodeRef();
                cases.add(caseRef);
                docLocations.add(new DocumentLocationVO(seriesByVolumeRef.get(volumeRef), volumeRef, caseRef));
            }
        }
        if (!volumesWithCases.isEmpty()) {
            Iterator<String> i = copy.iterator();
            while (i.hasNext() && cases.size() < count) {
                checkStop();
                String title = i.next();
                i.remove();
                createCase(title, volumesRandom);
            }
            while (cases.size() < count) {
                checkStop();
                createCase(getRandom(fnSerVolTitles), volumesRandom);
            }
        }
        log.info("There are " + cases.size() + " cases; goal was " + count + " cases");
    }

    private void createCase(String title, Random volumesRandom) {
        NodeRef volumeRef = getRandomGaussian2(volumesWithCases, volumesRandom);
        Case case1 = getCaseService().createCase(volumeRef);
        Map<String, Object> props = case1.getNode().getProperties();
        props.put(CaseModel.Props.TITLE.toString(), title);
        props.put(CaseModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        getCaseService().saveOrUpdate(case1);
        NodeRef caseRef = case1.getNode().getNodeRef();
        cases.add(caseRef);
        docLocations.add(new DocumentLocationVO(seriesByVolumeRef.get(volumeRef), volumeRef, caseRef));
        log.info("Created case " + title);
    }

    private void createGroups() {
        checkStop();
        log.info("Updating organization structure based groups");
        getOrganizationStructureService().updateOrganisationStructureBasedGroups();
        checkStop();
        log.info("Loading groups");
        Set<String> groups = getAuthorityService().getAllAuthorities(AuthorityType.GROUP);
        groups.removeAll(BeanHelper.getUserService().getSystematicGroups());
        groupNames = new ArrayList<String>(groups);
        groupLevel1Names = new ArrayList<String>();
        groupLevel2Names = new ArrayList<String>();
        for (String groupName : groupNames) {
            int groupLevel = StringUtils.split(groupName, ',').length;
            if (groupLevel == 1) {
                groupLevel1Names.add(groupName);
            } else if (groupLevel == 2) {
                groupLevel2Names.add(groupName);
            }
        }
        log.info("There are " + groupNames.size() + " groups (" + groupLevel1Names.size() + " first level groups, " + groupLevel2Names.size() + " second level groups)");
    }

    private void createContacts(int count) {
        contactNodes = new ArrayList<Node>();
        contactNodes.addAll(getAddressbookService().listOrganization());
        contactNodes.addAll(getAddressbookService().listPerson());
        while (contactNodes.size() < count) {
            checkStop();
            createContact();
        }
        log.info("There are " + contactNodes.size() + " contacts; goal was " + count + " contacts");

    }

    private void createContact() {
        String contact = getRandom(contacts);
        String[] split = StringUtils.split(contact, ',');
        contact = stripAndTrim(getRandom(Arrays.asList(split)));
        split = StringUtils.split(contact, ' ');
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AddressbookModel.Props.ACTIVESTATUS, Boolean.TRUE);
        QName type;
        QName assocType;
        if (split.length == 2 && StringUtils.isNotBlank(split[0]) && StringUtils.isNotBlank(split[1])) {
            props.put(AddressbookModel.Props.PERSON_FIRST_NAME, stripAndTrim(split[0]));
            props.put(AddressbookModel.Props.PERSON_LAST_NAME, stripAndTrim(split[1]));
            type = AddressbookModel.Types.PRIV_PERSON;
            assocType = AddressbookModel.Assocs.ABPEOPLE;
        } else {
            props.put(AddressbookModel.Props.ORGANIZATION_NAME, contact);
            type = AddressbookModel.Types.ORGANIZATION;
            assocType = AddressbookModel.Assocs.ORGANIZATIONS;
        }
        QName randomqname = QName.createQName(URI, GUID.generate());
        NodeRef contactRef = getNodeService().createNode(
                BeanHelper.getConstantNodeRefsBean().getAddressbookRoot(),
                assocType,
                randomqname,
                type,
                props).getChildRef();
        contactNodes.add(getAddressbookService().getNode(contactRef));
        log.info("Created contact " + contact);
    }

    private void createDocuments(int count) throws Exception {
        checkStop();

        docVersions = new ConcurrentHashMap<String, DocumentTypeVersion>();
        classificators = new ConcurrentHashMap<String, List<ClassificatorValue>>();
        textFileLocks = new ConcurrentHashMap<String, Object>();
        textFileLocksGlobalLock = new Object();
        docs = Collections.synchronizedList(new ArrayList<NodeRef>());
        docLocationsRandom = new Random();
        usersRandom = new Random();

        for (DocumentLocationVO docLocation : docLocations) {
            NodeRef parentRef = docLocation.getDocumentParentRef();
            List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(parentRef, DocumentCommonModel.Assocs.DOCUMENT, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef childAssocRef : childAssocs) {
                if (log.isTraceEnabled()) {
                    log.trace("assocType=" + childAssocRef.getTypeQName().toPrefixString(getNamespaceService()) + " assocName="
                            + childAssocRef.getQName().toPrefixString(getNamespaceService()));
                }
                docs.add(childAssocRef.getChildRef());
            }
        }
        Assert.isTrue(new HashSet<NodeRef>(docs).size() == docs.size());
        Collections.shuffle(docLocations);

        log.info("Starting " + documentAndWorkflowGeneratorThreads + " threads for generating documents");
        List<Thread> threads = new ArrayList<Thread>();
        try {
            for (int i = 0; i < documentAndWorkflowGeneratorThreads; i++) {
                String threadName = "DocumentGeneratorThread-" + i;
                log.info("Starting thread " + threadName);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        createDocumentsLoop();
                    }
                }, threadName);
                thread.start();
                threads.add(thread);
            }
            log.info("Document generator threads started. Waiting for all threads to complete");
        } finally {
            for (Thread thread : threads) {
                log.info("Waiting for " + thread.getName() + " to complete");
                thread.join();
            }
            log.info("All document generator threads completed.");
            log.info("There are " + docs.size() + " documents; goal was " + count + " documents");
            log.info("Now updating document counters...");
            getDocumentListService().updateDocCounters();
            log.info("Completed updating document counters");
        }
    }

    private void createDocumentsLoop() {
        try {
            log.info("Documents generator thread started");
            try {
                final Random workflowRandom = new Random();
                while (docs.size() < documentsCount) {
                    checkStop();
                    try {
                        final DocumentLocationVO docLocation = getRandomGaussian3(docLocations, docLocationsRandom);
                        String userName = getRandomGaussian3(userNamesList, usersRandom);
                        AuthenticationUtil.setFullyAuthenticatedUser(userName); // also sets runAsUser
                        Pair<NodeRef, String> pair = getTransactionService().getRetryingTransactionHelper().doInTransaction(
                                new RetryingTransactionCallback<Pair<NodeRef, String>>() {
                                    @Override
                                    public Pair<NodeRef, String> execute() throws Throwable {
                                        return createDocument(docLocation, workflowRandom);
                                    }
                                }, false);
                        NodeRef docRef = pair.getFirst();
                        docs.add(docRef);
                        log.info("Created document " + docs.size() + " of " + documentsCount + " - " + docRef + " " + pair.getSecond());
                    } catch (Exception e) {
                        log.error("Documents generator thread error", e);
                        Thread.sleep(Math.max(documentAndWorkflowGeneratorThreads * 1000, 1000));
                    }
                }
            } catch (StopException e) {
                log.info("Stop requested");
            } catch (Exception e) {
                log.error("Documents generator thread error", e);
            }
        } finally {
            log.info("Documents generator thread stopped");
        }
    }

    private Pair<NodeRef, String> createDocument(DocumentLocationVO docLocation, Random workflowRandom) throws Exception {
        String docTypeId = getRandom(docLocation.getSeriesDocType());
        DocumentTypeVersion docVer = docVersions.get(docTypeId);
        if (docVer == null) {
            DocumentType docType = getDocumentAdminService().getDocumentType(docTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
            docVer = docType.getLatestDocumentTypeVersion();
            docVersions.put(docTypeId, docVer);
        }
        DocumentDynamic doc = getDocumentDynamicService().createNewDocument(docVer, docLocation.getDocumentParentRef(), true).getFirst();

        doc.setProp(DocumentCommonModel.Props.FUNCTION, docLocation.getFunctionRef());
        doc.setProp(DocumentCommonModel.Props.SERIES, docLocation.getSeriesRef());
        doc.setProp(DocumentCommonModel.Props.VOLUME, docLocation.getVolumeRef());
        doc.setProp(DocumentCommonModel.Props.CASE, docLocation.getCaseRef());
        doc.setProp(DocumentCommonModel.Props.DOC_NAME, getRandom(docTitles));
        doc.setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION, docLocation.getSeriesAccessRestriction());
        doc.setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, docLocation.getSeriesAccessRestrictionReason());
        doc.setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, docLocation.getSeriesAccessRestrictionBeginDate());
        doc.setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, docLocation.getSeriesAccessRestrictionEndDate());
        doc.setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, docLocation.getSeriesAccessRestrictionEndDesc());

        String regNumber = "not registered";
        boolean isRegistered = false;
        if (Math.random() < 0.9d) { // 90% documents should be registered
            regNumber = getRandom(regNumbers);
            doc.setProp(DocumentCommonModel.Props.REG_NUMBER, regNumber);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, (int) (Math.random() * -1000)); // ~3 years back
            doc.setProp(DocumentCommonModel.Props.REG_DATE_TIME, cal.getTime());
            doc.setProp(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.FINISHED.getValueName());
            isRegistered = true;
        }

        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = getDocumentConfigService().getPropertyDefinitions(doc.getNode());
        for (Pair<DynamicPropertyDefinition, Field> pair : propDefs.values()) {
            DynamicPropertyDefinition propDef = pair.getFirst();
            Field field = pair.getSecond();
            if (field == null) {
                continue;
            }

            Node propNode = doc.getNode();
            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (hierarchy != null) {
                int i = 0;
                while (i < hierarchy.length) {
                    propNode = propNode.getAllChildAssociations(hierarchy[i]).get(0);
                    i++;
                }
            }

            QName propName = field.getQName();
            Serializable value = (Serializable) propNode.getProperties().get(propName.toString());
            String systematicGroupName = null;
            if (field.getParent() instanceof FieldGroup && ((FieldGroup) field.getParent()).isSystematic()) {
                systematicGroupName = ((FieldGroup) field.getParent()).getName();
            }

            if (SystematicFieldGroupNames.ACCESS_RESTRICTION.equals(systematicGroupName)) {
                continue;
            } else if (SystematicFieldGroupNames.DOCUMENT_OWNER.equals(systematicGroupName)) {
                continue;
            } else if (SystematicFieldGroupNames.REGISTRATION_DATA.equals(systematicGroupName)) {
                continue;
            } else if (SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(systematicGroupName)) {
                continue;
            }

            value = setPropValueIfNeeded(propDef, field, propNode, propName, value);

        }
        NodeRef docRef = doc.getNodeRef();

        // FILES
        ArrayList<String> fileTitles = new ArrayList<String>();
        ContentWriter allWriter = null;
        int filesCount = 0;
        double r = Math.random(); // 0×0,5+1×0,4+4,5×0,0995+45×0,0005 = 0,87
        if (isFilesEnabled() && r >= 0.5d) {
            if (r < 0.9d) {
                filesCount = 1;
            } else if (r < 0.9995) {
                filesCount = ((int) (Math.random() * 9)) + 2; // 2 - 10
            } else {
                filesCount = ((int) (Math.random() * 90)) + 11; // 11 - 100
            }
            Map<QName, Serializable> userProps = getUserData(getUserService().getCurrentUserName()).getSecond();
            allWriter = BeanHelper.getContentService().getWriter(docRef, FILE_CONTENTS, false);
            allWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
            allWriter.setEncoding("UTF-8");
            OutputStream allOutput = allWriter.getContentOutputStream();
            try {
                for (int i = 0; i < filesCount; i++) {
                    createFile(docRef, fileTitles, userProps, allOutput, i);
                }
            } finally {
                allOutput.close();
            }
        }
        getNodeService().addAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
        doc.setProp(DocumentCommonModel.Props.FILE_NAMES, fileTitles);
        doc.setProp(DocumentCommonModel.Props.FILE_CONTENTS, allWriter == null ? null : allWriter.getContentData());
        doc.setProp(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, new ArrayList<String>(0));
        getDocumentDynamicService().updateDocument(doc, Arrays.asList("TestDataService"), false, true);

        // ASSOCS
        int assocsCount = 0;
        if (!docs.isEmpty()) { // 0,001×45+0,01×4,5+0,25×1 = 0,34
            r = Math.random();
            if (r <= 0.25d) {
                assocsCount = 1;
            } else if (r <= 0.26d) {
                assocsCount = ((int) (Math.random() * 9)) + 2; // 2 - 10
            } else if (r <= 0.261d) {
                assocsCount = ((int) (Math.random() * 90)) + 11; // 11 - 100
            }
        }
        Set<NodeRef> assocOtherDocRefs = new HashSet<NodeRef>();
        for (int i = 0; i < assocsCount; i++) {
            QName assocQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
            if (i == 0 && Math.random() < 0.5d) {
                if (Math.random() < 0.5d) {
                    assocQName = DocumentCommonModel.Assocs.DOCUMENT_REPLY;
                } else {
                    assocQName = DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP;
                }
            }
            NodeRef otherDocRef = getRandom(docs);
            if (assocOtherDocRefs.contains(otherDocRef)) {
                continue; // just skip if it already exists; if there are too few documents in repo, then we can't fill assoc goal anyway
            }
            getNodeService().createAssociation(/* new */docRef, /* base */otherDocRef, assocQName);
            assocOtherDocRefs.add(otherDocRef);
        }

        int allTaskCount = 0;
        if (documentWorkflowsEnabled) {
            int compoundWorkflowCount = getCompoundWorkflowCount(workflowRandom);
            if (compoundWorkflowCount > 0) {
                boolean inProgress = !isRegistered;
                // 10% registreerimata, neist 95% omab töövoogu, seega kokku 9,5%, aga see on teostamisel
                // aga tahame et 1000 dokumenti oleksid registreerimiseks menüüpunktis
                // 1714500 dokist 162878 dokki on regamata, aga teostamisel töövooga, seega 0,61% neist tahame lõpetada
                if (inProgress && Math.random() < 0.0062d) {
                    inProgress = false;
                }

                for (int i = 0; i < compoundWorkflowCount; i++) {
                    allTaskCount += createWorkflows(docRef, inProgress, docTypeId, CompoundWorkflowType.DOCUMENT_WORKFLOW).getSecond();
                }
            }
        }

        // SENDINFOS
        // How many documents match recipientFinishedDocuments query (are finished && have recipientName/additionalRecipientName/partyName field, that is non-empty)
        // depends on documentTypes.xml that is used. In case of our sample documentTypes.xml, 34% of generated documents match
        // 34% of 1 700 000 is 578 000; 100 of 578 000 is 0.0002
        @SuppressWarnings("unchecked")
        List<String> recipientName = (List<String>) doc.getProp(DocumentCommonModel.Props.RECIPIENT_NAME);
        @SuppressWarnings("unchecked")
        List<String> additionalRecipientName = (List<String>) doc.getProp(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
        if (DocumentStatus.FINISHED.getValueName().equals(doc.getProp(DocumentCommonModel.Props.DOC_STATUS))
                && ((recipientName != null && !recipientName.isEmpty()) || (additionalRecipientName != null && !additionalRecipientName.isEmpty()))
                && Math.random() > 0.0002d) {

            Map<QName, Serializable> sendInfoProps = new HashMap<QName, Serializable>();
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_RESOLUTION, getRandom(docTitles));
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, getRandom(contacts));
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, SendMode.MAIL.getValueName());
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, SendStatus.RECEIVED.toString());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, (int) (Math.random() - 1d) * 2000);
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME, cal.getTime());

            getNodeService().createNode(docRef, //
                    DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, sendInfoProps).getChildRef();
            ArrayList<String> sendModes = new ArrayList<String>(1);
            sendModes.add(SendMode.MAIL.getValueName());
            getNodeService().setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
        }

        // FUTURE: õiguseid (nii dokumendi kui sarja omad) praegu ei tee

        return Pair.newInstance(docRef, docTypeId + ", " + regNumber + ", " + filesCount + " files, " + assocOtherDocRefs.size() + " assocs, " + allTaskCount + " tasks");
    }

    private int getCompoundWorkflowCount(Random compoundWorkflowRandom) {
        int compoundWorkflowCount;
        double workflowRandom = compoundWorkflowRandom.nextDouble();
        if (workflowRandom < 0.25d) {
            compoundWorkflowCount = 0;
        } else {
            compoundWorkflowCount = 1;
        }
        return compoundWorkflowCount;
    }

    private Serializable setPropValueIfNeeded(DynamicPropertyDefinition propDef, Field field, Node propNode, QName propName, Serializable value) {
        if (value == null
                || (value instanceof String && StringUtils.isBlank((String) value))
                || (value instanceof List && (((List<?>) value).size() == 0
                || (((List<?>) value).size() == 1 && (((List<?>) value).get(0) == null
                || (((List<?>) value).get(0) instanceof String && StringUtils.isBlank((String) ((List<?>) value).get(0)))))))) {

            // Always fill empty fields that are mandatory; if not mandatory then fill only half of the fields
            if (!field.isMandatory() && Math.random() < 0.5d) {

                // If we don't fill the value with data, then set it to something anyway, because we want the _amount_ of properties to be real also
                // And creating/editing documents through web interface sets non-filled properties to something also
                if (value == null) {
                    if (propDef.isMultiValued()) {
                        ArrayList<Serializable> list = new ArrayList<Serializable>();
                        value = list;
                    } else if (DataTypeDefinition.TEXT.equals(propDef.getDataTypeQName())) {
                        value = "";
                    }
                    propNode.getProperties().put(propName.toString(), value);
                }
                return value;
            }

            switch (field.getFieldTypeEnum()) {
            case CHECKBOX:
                value = Boolean.valueOf(Math.random() < 0.5d);
                break;
            case COMBOBOX:
            case COMBOBOX_EDITABLE:
            case COMBOBOX_AND_TEXT:
            case COMBOBOX_AND_TEXT_NOT_EDITABLE:
            case LISTBOX: // FUTURE IMPROVEMENT: multiple values can be selected
                value = getRandomClassificatorValue(field.getClassificator());
                if (value == null && field.getFieldTypeEnum() == FieldType.LISTBOX) {
                    value = new ArrayList<String>();
                }
                break;
            case TEXT_FIELD:
            case USER: // FUTURE IMPROVEMENT: users/contacts group mappings can be used
            case USERS:
            case USER_CONTACT:
            case USERS_CONTACTS:
            case CONTACT:
            case CONTACTS:
                if (field.getFieldId().toLowerCase().indexOf("regnum") >= 0) {
                    value = getRandom(senderRegNumbers);
                } else {
                    value = getRandom(contacts);
                }
                break;
            case DATE:
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, (int) (Math.random() - 0.5d) * 4000); // +- 6 years
                value = cal.getTime();
                break;
            case DOUBLE:
                value = (Math.random() - 0.5d) * 20000;
                break;
            case LONG:
                value = (long) ((Math.random() - 0.5d) * 20000);
                break;
            case STRUCT_UNIT:
                if (value == null) {
                    value = new ArrayList<String>();
                }
                break;
            case INFORMATION_TEXT:
                return value;
            }

            if (propDef.isMultiValued() && !(value instanceof List<?>)) {
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                list.add(value);
                value = list;
            }

            propNode.getProperties().put(propName.toString(), value);
        }
        return value;
    }

    private void createIndependentCompoundWorkflows(int count) throws Exception {
        checkStop();
        independentCompoundWorkflows = Collections.synchronizedList(new ArrayList<NodeRef>());

        NodeRef root = getWorkflowService().getIndependentWorkflowsRoot();
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(root, Collections.singleton(WorkflowCommonModel.Types.COMPOUND_WORKFLOW));
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            independentCompoundWorkflows.add(childAssociationRef.getChildRef());
        }

        if (independentWorkflowsCount <= 0) {
            return;
        }

        log.info("Starting " + documentAndWorkflowGeneratorThreads + " threads for generating independent compound workflows");
        List<Thread> threads = new ArrayList<Thread>();
        try {
            for (int i = 0; i < documentAndWorkflowGeneratorThreads; i++) {
                String threadName = "WorkflowGeneratorThread-" + i;
                log.info("Starting thread " + threadName);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        createIndependentCompoundWorkflowsLoop();
                    }
                }, threadName);
                thread.start();
                threads.add(thread);
            }
            log.info("Independent compound workflow generator threads started. Waiting for all threads to complete");
        } finally {
            for (Thread thread : threads) {
                log.info("Waiting for " + thread.getName() + " to complete");
                thread.join();
            }
            log.info("All independent compound workflow generator threads completed.");
            log.info("There are " + independentCompoundWorkflows.size() + " independent compound workflows; goal was " + count + " independent compound workflows");
        }
    }

    private void createIndependentCompoundWorkflowsLoop() {
        final NodeRef root = getWorkflowService().getIndependentWorkflowsRoot();
        final double finishedCompoundWorkflowsLikelihood = finishedIndependentWorkflowsCount / independentWorkflowsCount;
        try {
            log.info("Independent compound workflow generator thread started");
            try {
                while (independentCompoundWorkflows.size() < independentWorkflowsCount) {
                    checkStop();
                    try {
                        String userName = getRandomGaussian3(userNamesList, usersRandom);
                        AuthenticationUtil.setFullyAuthenticatedUser(userName); // also sets runAsUser
                        Pair<NodeRef, Integer> pair = getTransactionService().getRetryingTransactionHelper().doInTransaction(
                                new RetryingTransactionCallback<Pair<NodeRef, Integer>>() {
                                    @Override
                                    public Pair<NodeRef, Integer> execute() throws Throwable {
                                        return createWorkflows(root, Math.random() > finishedCompoundWorkflowsLikelihood, null, CompoundWorkflowType.INDEPENDENT_WORKFLOW);
                                    }
                                }, false);
                        NodeRef icwfRef = pair.getFirst();
                        independentCompoundWorkflows.add(icwfRef);
                        log.info("Created independent compound workflow " + independentCompoundWorkflows.size() + " of " + independentWorkflowsCount + " - " + icwfRef + " with "
                                + pair.getSecond() + " tasks");
                    } catch (Exception e) {
                        log.error("Independent compound workflow generator thread error", e);
                        Thread.sleep(Math.max(documentAndWorkflowGeneratorThreads * 1000, 1000));
                    }
                }
            } catch (StopException e) {
                log.info("Stop requested");
            } catch (Exception e) {
                log.error("Independent compound workflow generator thread error", e);
            }
        } finally {
            log.info("Independent compound workflow generator thread stopped");
        }

    }

    private Pair<NodeRef, Integer> createWorkflows(NodeRef docRef, boolean inProgress, String docTypeId, CompoundWorkflowType compoundWorkflowType) {
        int allTaskCount = 0;
        boolean isDocumentWorkflow = CompoundWorkflowType.DOCUMENT_WORKFLOW == compoundWorkflowType;

        String creatorUserName = getRandom(userNamesList);

        Map<QName, Serializable> creatorProps = getUserData(creatorUserName).getSecond();
        String creatorFullName = UserUtil.getPersonFullName1(creatorProps);
        String creatorEmail = (String) creatorProps.get(ContentModel.PROP_EMAIL);

        String cwfOwnerUserName = getRandom(userNamesList);
        String cwfOwnerFullName = UserUtil.getPersonFullName1(getUserData(cwfOwnerUserName).getSecond());

        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.STATUS, inProgress ? Status.IN_PROGRESS.getName() : Status.FINISHED.getName());
        props.put(WorkflowCommonModel.Props.TYPE, compoundWorkflowType.toString());
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorFullName);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, ((int) (Math.random() * -900)) - 30);
        Date startedDateTime = cal.getTime();
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
        props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
        props.put(WorkflowCommonModel.Props.OWNER_ID, cwfOwnerUserName);
        props.put(WorkflowCommonModel.Props.OWNER_NAME, cwfOwnerFullName);
        props.put(WorkflowCommonModel.Props.TITLE, getRandom(docTitles));
        NodeRef cwfRef = getNodeService().createNode(
                docRef,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Types.COMPOUND_WORKFLOW,
                props
                ).getChildRef();
        Map<QName, Serializable> taskSearchableProps = WorkflowUtil.getTaskSearchableProps(props);
        List<QName> wfTypes;
        if (CompoundWorkflowType.DOCUMENT_WORKFLOW == compoundWorkflowType || CompoundWorkflowType.INDEPENDENT_WORKFLOW == compoundWorkflowType) {
            wfTypes = Arrays.asList(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW, WorkflowSpecificModel.Types.OPINION_WORKFLOW,
                    WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW);
        } else {
            wfTypes = Arrays.asList(WorkflowSpecificModel.Types.OPINION_WORKFLOW,
                    WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW);
        }
        // Not used: REGISTRATION_WORKFLOW, EXTERNAL_REVIEW_WORKFLOW, DUE_DATE_EXTENSION_WORKFLOW -- too special
        // FUTURE: orderAssignmentWorkflow -- could be used, but don't remember exact rules right now (must have category...?)?
        Map<QName, WorkflowType> wfTypesByWf = getWorkflowConstantsBean().getWorkflowTypes();

        Map<String, Set<Privilege>> permissionsByTaskOwnerId = new HashMap<String, Set<Privilege>>();
        FileService fileService = BeanHelper.getFileService();

        boolean isIndependentWorkflow = CompoundWorkflowType.INDEPENDENT_WORKFLOW == compoundWorkflowType;
        boolean isCaseFileWorkflow = CompoundWorkflowType.CASE_FILE_WORKFLOW == compoundWorkflowType;

        int wfCount;
        if (Math.random() < 0.9d) {
            wfCount = 1;
        } else {
            wfCount = ((int) (Math.random() * 5)) + 2; // 2 - 6
        }
        boolean hasSignatureWorkflow = false;
        for (int i = 0; i < wfCount; i++) {
            QName wfType;
            if (i == 0 && Math.random() < 0.5d) {
                wfType = WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW;
            } else {
                wfType = getRandom(wfTypes);
            }
            hasSignatureWorkflow |= WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(wfType);
            props = new HashMap<QName, Serializable>();
            String wfStatus = inProgress ? (i == 0 ? Status.IN_PROGRESS.getName() : Status.NEW.getName()) : Status.FINISHED.getName();
            props.put(WorkflowCommonModel.Props.STATUS, wfStatus);
            props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorFullName);
            props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, Status.NEW.equals(wfStatus) ? null : startedDateTime);
            props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
            // description and resolution could be filled
            NodeRef wfRef = getNodeService().createNode(
                    cwfRef,
                    WorkflowCommonModel.Assocs.WORKFLOW,
                    WorkflowCommonModel.Assocs.WORKFLOW,
                    wfType,
                    props
                    ).getChildRef();
            Boolean isParallelTasks = (Boolean) getNodeService().getProperty(wfRef, WorkflowCommonModel.Props.PARALLEL_TASKS);

            cal.add(Calendar.DATE, ((int) (Math.random() * 30)) + 1);
            cal.add(Calendar.HOUR_OF_DAY, (int) (Math.random() * 24));
            Date dueDate = cal.getTime();
            List<String> userNamesListCopy = new ArrayList<String>(userNamesList);
            int taskCount;
            double r = Math.random();
            if (r < 0.985d) {
                taskCount = ((int) (Math.random() * 1.8d)) + 1; // 1 - 2
            } else if (r < 0.995d) {
                taskCount = ((int) (Math.random() * 8)) + 3; // 3 - 10
            } else {
                taskCount = ((int) (Math.random() * 90)) + 11; // 11 - 100
            }
            WorkflowType workflowType = getWorkflowConstantsBean().getWorkflowTypes().get(wfType);
            for (int j = 0; j < taskCount; j++) {
                if (userNamesListCopy.isEmpty()) {
                    break;
                }
                String taskOwnerUserName = getRandom(userNamesListCopy);
                userNamesListCopy.remove(taskOwnerUserName);
                Map<QName, Serializable> taskOwnerProps = getUserData(taskOwnerUserName).getSecond();
                String taskOwnerFullName = UserUtil.getPersonFullName1(taskOwnerProps);
                String taskOwnerEmail = (String) taskOwnerProps.get(ContentModel.PROP_EMAIL);
                String taskOwnerJobTitle = (String) taskOwnerProps.get(ContentModel.PROP_JOBTITLE);
                OrganizationStructure taskOwnerStructUnit = structUnitsByUnitId.get(taskOwnerProps.get(ContentModel.PROP_ORGID));

                QName taskType = wfTypesByWf.get(wfType).getTaskType();
                props = new HashMap<QName, Serializable>();
                String taskStatus = Status.IN_PROGRESS.equals(wfStatus) && !isParallelTasks && j > 0 ? Status.NEW.getName() : wfStatus;
                props.put(WorkflowCommonModel.Props.STATUS, taskStatus);
                props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorFullName);
                props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, Status.NEW.equals(taskStatus) ? null : startedDateTime);
                props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);

                props.put(WorkflowSpecificModel.Props.CREATOR_ID, creatorFullName);
                props.put(WorkflowSpecificModel.Props.CREATOR_EMAIL, creatorEmail);

                props.put(WorkflowCommonModel.Props.OWNER_ID, taskOwnerUserName);
                props.put(WorkflowCommonModel.Props.OWNER_NAME, taskOwnerFullName);
                props.put(WorkflowCommonModel.Props.OWNER_EMAIL, taskOwnerEmail);
                props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, taskOwnerJobTitle);
                props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, taskOwnerStructUnit != null ? new ArrayList<String>(Arrays.asList(taskOwnerStructUnit.getName()))
                        : null);

                props.put(WorkflowCommonModel.Props.DOCUMENT_TYPE, docTypeId);

                String outcome = null;
                Date completedDateTime = null;
                String comment = "";
                if (Status.FINISHED.equals(taskStatus)) {

                    int outcomeIndex = 0;
                    if (taskType.equals(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                        outcomeIndex = 1;
                    }
                    String outcomeLabelId = "task_outcome_" + taskType.getLocalName() + Integer.toString(outcomeIndex);
                    outcome = I18NUtil.getMessage(outcomeLabelId);
                    Assert.notNull(outcome);

                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTime(startedDateTime);
                    cal2.add(Calendar.HOUR_OF_DAY, ((int) (Math.random() * 48)) + 1);
                    completedDateTime = cal2.getTime();

                    if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
                            || taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)
                            || Math.random() < 0.4d) {
                        comment = getRandom(docTitles);
                    }
                }
                props.put(WorkflowCommonModel.Props.OUTCOME, outcome);
                props.put(WorkflowCommonModel.Props.COMPLETED_DATE_TIME, completedDateTime);
                props.put(WorkflowSpecificModel.Props.COMMENT, comment);
                // props.put(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, userFullName); <-- set to same as wfResolution

                props.put(WorkflowSpecificModel.Props.DUE_DATE, dueDate);
                props.put(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
                props.put(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, null);

                // for assignmentTask and confirmationTask, resolution could be filled
                props.putAll(taskSearchableProps);
                Task task = BeanHelper.getWorkflowService().createTaskInMemory(wfRef, workflowType, props);
                Set<QName> aspects = task.getNode().getAspects();
                aspects.add(WorkflowSpecificModel.Aspects.SEARCHABLE);

                // if assignmentWorkflow, then 1 task must have responsible active=true, (and 0-few can have responsible active=false)
                if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && j == 0) {
                    task.getNode().getProperties().put(WorkflowSpecificModel.Props.ACTIVE.toString(), Boolean.TRUE);
                    aspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                }

                task.setTaskIndexInWorkflow(j);
                BeanHelper.getWorkflowDbService().createTaskEntry(task, wfRef);

                if (!task.isStatus(Status.NEW)) {
                    String ownerId = task.getOwnerId();
                    if (StringUtils.isNotBlank(ownerId)) {
                        // document workflow
                        if (isDocumentWorkflow) {
                            Set<Privilege> requiredPrivileges = getRequiredPrivsForTask(task, docRef, fileService, false, false);
                            addOwnerPermissions(permissionsByTaskOwnerId, ownerId, requiredPrivileges);
                        }
                    } else if (isCaseFileWorkflow) {
                        // case file workflow
                        addOwnerPermissions(permissionsByTaskOwnerId, ownerId, getPrivsWithDependencies(getRequiredPrivsForTask(task, null, null, true, true)));
                    } else {
                        // independent workflow
                        Set<Privilege> privileges = WorkflowUtil.getIndependentWorkflowDefaultDocPermissions();
                        boolean addEditPrivilege = task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || WorkflowUtil.isFirstConfirmationTask(task);
                        Set<Privilege> documentPrivileges = new HashSet<Privilege>();
                        documentPrivileges.addAll(privileges);
                        if (addEditPrivilege) {
                            documentPrivileges.add(Privilege.EDIT_DOCUMENT);
                        }
                        addOwnerPermissions(permissionsByTaskOwnerId, ownerId, documentPrivileges);
                    }
                }

                allTaskCount++;
            }
        }

        if (!isIndependentWorkflow) {
            getNodeService().setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);
        }

        List<NodeRef> documentsToSign = new ArrayList<NodeRef>();
        if (isIndependentWorkflow && maxDocumentsInIndependentWorkflow > 0) {
            DocumentAssociationsService docAssocService = BeanHelper.getDocumentAssociationsService();
            int docAssocCount = (int) (Math.random() * maxDocumentsInIndependentWorkflow);
            Set<NodeRef> associatedDocRefs = new HashSet<NodeRef>();
            for (int i = 0; i < docAssocCount; i++) {
                NodeRef assocDocRef = getRandom(docs);
                if (associatedDocRefs.contains(assocDocRef)) {
                    // TODO: should we try to get anther doc if there is more than maxDocumentsInIndependentWorkflow dos available?
                    continue;
                }
                associatedDocRefs.add(assocDocRef);
                docAssocService.createWorkflowAssoc(assocDocRef, cwfRef, true, true);
                // assume that in signature workflow, most of documents are going to be signed
                if (hasSignatureWorkflow && Math.random() < 0.9d) {
                    documentsToSign.add(assocDocRef);
                }
            }
            if (!documentsToSign.isEmpty()) {
                BeanHelper.getNodeService().setProperty(cwfRef, WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, (Serializable) documentsToSign);
            }
        }

        PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
        if (isDocumentWorkflow) {
            for (Map.Entry<String, Set<Privilege>> entry : permissionsByTaskOwnerId.entrySet()) {
                privilegeService.setPermissions(docRef, entry.getKey(), entry.getValue());
            }
        } else if (isCaseFileWorkflow) {
            // and to documents under this case file
            for (NodeRef documentRef : BeanHelper.getDocumentService().getAllDocumentRefsByParentRefWithoutRestrictedAccess(docRef)) {
                for (Map.Entry<String, Set<Privilege>> entry : permissionsByTaskOwnerId.entrySet()) {
                    privilegeService.setPermissions(documentRef, entry.getKey(), entry.getValue());
                }
            }
        } else {
            List<Document> documents = BeanHelper.getWorkflowService().getCompoundWorkflowDocuments(cwfRef);
            for (Document document : documents) {
                for (Map.Entry<String, Set<Privilege>> entry : permissionsByTaskOwnerId.entrySet()) {
                    Set<Privilege> privileges = new HashSet<Privilege>(entry.getValue());
                    if (!document.isDocStatus(DocumentStatus.WORKING)) {
                        privileges.remove(Privilege.EDIT_DOCUMENT);
                    }
                    privilegeService.setPermissions(document.getNodeRef(), entry.getKey(), privileges);
                }
            }
        }

        return new Pair<NodeRef, Integer>(cwfRef, allTaskCount);
    }

    public void addOwnerPermissions(Map<String, Set<Privilege>> permissionsByTaskOwnerId, String ownerId, Set<Privilege> set) {
        if (!set.isEmpty()) {
            Set<Privilege> ownerPermissions = permissionsByTaskOwnerId.get(ownerId);
            if (ownerPermissions == null) {
                ownerPermissions = new HashSet<Privilege>();
                permissionsByTaskOwnerId.put(ownerId, ownerPermissions);
            }
            ownerPermissions.addAll(set);
        }
    }

    private void createFile(NodeRef docRef, ArrayList<String> fileTitles, Map<QName, Serializable> userProps, OutputStream allOutput, int i) throws Exception {
        List<String> file = getRandom(files);
        String fileUrl = file.get(0);
        String fileMimeType = file.get(1);
        String fileEncoding = file.get(2);
        String fileSize = file.get(3);
        String fileName = FilenameUtil.trimDotsAndSpaces(FilenameUtil.stripForbiddenWindowsCharacters(file.get(4)));
        String fileTitle = file.get(5).replace('+', '-');
        if (StringUtils.isBlank(fileMimeType)) {
            fileMimeType = getMimetypeService().guessMimetype(fileName);
        }
        if (StringUtils.isBlank(fileEncoding)) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(new File(dataFolder + "/contentstore/testfiles/" + fileUrl)));
                Charset charset = getMimetypeService().getContentCharsetFinder().getCharset(is, fileMimeType);
                fileEncoding = charset.name();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }
        if (StringUtils.isBlank(fileSize)) {
            fileSize = Long.toString(new File(dataFolder + "/contentstore/testfiles/" + fileUrl).length());
        }
        String contentUrl = FileContentStore.STORE_PROTOCOL + ContentStore.PROTOCOL_DELIMITER + "testfiles/" + fileUrl;
        ContentData contentData = new ContentData(contentUrl, fileMimeType, Long.parseLong(fileSize), fileEncoding, locale);

        Pair<String, String> fileNameAndTitle = FilenameUtil.getFilenameFromDisplayname(docRef, fileTitles, fileTitle, getGeneralService());
        fileName = fileNameAndTitle.getFirst();
        fileTitle = fileNameAndTitle.getSecond();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(VersionsModel.Props.VersionModified.FIRSTNAME, userProps.get(ContentModel.PROP_FIRSTNAME));
        props.put(VersionsModel.Props.VersionModified.LASTNAME, userProps.get(ContentModel.PROP_LASTNAME));
        props.put(VersionsModel.Props.VersionModified.MODIFIED, new Date());
        props.put(ContentModel.PROP_NAME, fileName);
        props.put(FileModel.Props.DISPLAY_NAME, fileTitle);
        props.put(FileModel.Props.ACTIVE, Boolean.valueOf(i == 0 || Math.random() < 0.8d));
        props.put(ContentModel.PROP_CONTENT, contentData);
        QName assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(fileName));
        NodeRef fileRef = getNodeService().createNode(
                docRef,
                ContentModel.ASSOC_CONTAINS,
                assocQName,
                ContentModel.TYPE_CONTENT,
                props
                ).getChildRef();
        fileTitles.add(fileTitle);

        File textFile = new File(dataFolder + "/contentstore/testfiles/" + fileUrl + ".txt");
        Object lockObject;
        synchronized (textFileLocksGlobalLock) {
            lockObject = textFileLocks.get(fileUrl);
            if (lockObject == null) {
                lockObject = new Object();
                textFileLocks.put(fileUrl, lockObject);
            }
        }
        synchronized (lockObject) {
            if (!textFile.exists()) {
                log.info("Transforming file contents: " + fileUrl + " - size=" + fileSize + " fromMimeType=" + fileMimeType + " fromEncoding=" + fileEncoding + " toMimeType="
                        + MimetypeMap.MIMETYPE_TEXT_PLAIN + " toEncoding=UTF-8");
                long startTime = System.currentTimeMillis();
                textFile.createNewFile();
                boolean readerReady = true;
                ContentReader reader = getFileFolderService().getReader(fileRef);
                if (reader != null && reader.exists()) {
                    if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN, true)
                            || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8", true)) {
                        final ContentTransformer transformer = getContentService().getTransformer(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
                        if (transformer == null) {
                            log.debug("No transformer found for " + reader.getMimetype());
                            readerReady = false;
                        } else {
                            final ContentWriter writer = getContentService().getTempWriter();
                            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            writer.setEncoding("UTF-8");
                            try {
                                transformer.transform(reader, writer);
                                reader = writer.getReader();
                                if (!reader.exists()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Transformation did not write any content, fileName '" + fileName + "', " + fileRef);
                                    }
                                    readerReady = false;
                                }
                            } catch (ContentIOException e) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Transformation failed, fileName '" + fileName + "', " + fileRef, e);
                                }
                                readerReady = false;
                            }
                        }
                    }
                } else {
                    readerReady = false;
                }
                if (readerReady) {
                    @SuppressWarnings("null")
                    InputStream input = reader.getContentInputStream();
                    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(textFile));
                    FileCopyUtils.copy(input, output);
                } else {
                    FileCopyUtils.copy(new byte[] {}, textFile);
                }
                long stopTime = System.currentTimeMillis();
                log.info("Completed transforming file contents: " + fileUrl + " - took " + (stopTime - startTime) + " ms");
            }
        }
        InputStream input = new BufferedInputStream(new FileInputStream(textFile));
        try {
            IOUtils.copyLarge(input, allOutput);
        } finally {
            input.close();
        }
        allOutput.write('\n');
    }

    private final Locale locale = new Locale("et", "EE");

    private String getRandomClassificatorValue(String classificatorName) {
        if (StringUtils.isBlank(classificatorName)) {
            return null;
        }
        List<ClassificatorValue> classificatorValues = classificators.get(classificatorName);
        if (classificatorValues == null) {
            classificatorValues = getClassificatorService().getAllClassificatorValues(classificatorName);
            classificators.put(classificatorName, classificatorValues);
        }
        if (classificatorValues.size() == 0) {
            return null;
        }
        ClassificatorValue classificatorValue = getRandom(classificatorValues);
        return classificatorValue.getValueName();
    }

    private static <V> V getRandom(List<V> list) {
        return list.get((int) (Math.random() * list.size()));
    }

    private static int getRandomGaussian2(Random r, int max) {
        return getRandomGaussian(r, max, 2);
    }

    private static int getRandomGaussian3(Random r, int max) {
        return getRandomGaussian(r, max, 3);
    }

    private static int getRandomGaussian(Random r, int max, int edge) {
        Assert.isTrue(max > 0);
        double multiplier = max / (edge * 2d);
        int v = (int) ((r.nextGaussian() + edge) * multiplier);
        if (v < 0) {
            return 0;
        } else if (v >= max) {
            return max - 1;
        }
        return v;
    }

    private static <V> V getRandomGaussian2(List<V> list, Random r) {
        return list.get(getRandomGaussian2(r, list.size()));
    }

    private static <V> V getRandomGaussian3(List<V> list, Random r) {
        return list.get(getRandomGaussian3(r, list.size()));
    }

    private String stripAndTrim(String value) {
        return StringUtils.trimToEmpty(StringUtils.stripToEmpty(StringUtils.defaultString(value).replaceAll("\\p{Cntrl}", " ").replaceAll(" {2,}", " ")));
    }

    // SaveListener that sets draft=true on document

    @Override
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        // do nothing
    }

    @Override
    public void save(DynamicBase document) {
        if (document instanceof DocumentDynamic) {
            ((DocumentDynamic) document).setDraft(true);
            ((DocumentDynamic) document).setDraftOrImapOrDvk(true);
        }
    }

    @Override
    public String getBeanName() {
        return null;
    }

    public int getOrgUnitsCount() {
        return orgUnitsCount;
    }

    public void setOrgUnitsCount(int orgUnitsCount) {
        this.orgUnitsCount = orgUnitsCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = usersCount;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public void setContactsCount(int contactsCount) {
        this.contactsCount = contactsCount;
    }

    public int getRegistersCount() {
        return registersCount;
    }

    public void setRegistersCount(int registersCount) {
        this.registersCount = registersCount;
    }

    public int getFunctionsCount() {
        return functionsCount;
    }

    public void setFunctionsCount(int functionsCount) {
        this.functionsCount = functionsCount;
    }

    public int getSeriesCount() {
        return seriesCount;
    }

    public void setSeriesCount(int seriesCount) {
        this.seriesCount = seriesCount;
    }

    public int getVolumesCount() {
        return volumesCount;
    }

    public void setVolumesCount(int volumesCount) {
        this.volumesCount = volumesCount;
    }

    public int getCaseFilesCount() {
        return caseFilesCount;
    }

    public void setCaseFilesCount(int caseFilesCount) {
        this.caseFilesCount = caseFilesCount;
    }

    public int getIndependentWorkflowsCount() {
        return independentWorkflowsCount;
    }

    public void setIndependentWorkflowsCount(int independentWorkflowsCount) {
        this.independentWorkflowsCount = independentWorkflowsCount;
    }

    public int getCasesCount() {
        return casesCount;
    }

    public void setCasesCount(int casesCount) {
        this.casesCount = casesCount;
    }

    public int getDocumentsCount() {
        return documentsCount;
    }

    public void setDocumentsCount(int documentsCount) {
        this.documentsCount = documentsCount;
    }

    public boolean isFilesEnabled() {
        return filesEnabled;
    }

    public void setFilesEnabled(boolean filesEnabled) {
        this.filesEnabled = filesEnabled;
    }

    public int getDocumentAndWorkflowGeneratorThreads() {
        return documentAndWorkflowGeneratorThreads;
    }

    public void setDocumentAndWorkflowGeneratorThreads(int documentAndWorkflowGeneratorThreads) {
        this.documentAndWorkflowGeneratorThreads = documentAndWorkflowGeneratorThreads;
    }

    public int getClosedCaseFilesCount() {
        return closedCaseFilesCount;
    }

    public void setClosedCaseFilesCount(int closedCaseFilesCount) {
        this.closedCaseFilesCount = closedCaseFilesCount;
    }

    public int getFinishedIndependentWorkflowsCount() {
        return finishedIndependentWorkflowsCount;
    }

    public void setFinishedIndependentWorkflowsCount(int finishedIndependentWorkflowsCount) {
        this.finishedIndependentWorkflowsCount = finishedIndependentWorkflowsCount;
    }

    public int getMaxDocumentsInIndependentWorkflow() {
        return maxDocumentsInIndependentWorkflow;
    }

    public void setMaxDocumentsInIndependentWorkflow(int maxDocumentsInIndependentWorkflow) {
        this.maxDocumentsInIndependentWorkflow = maxDocumentsInIndependentWorkflow;
    }

    public boolean isDocumentWorkflowsEnabled() {
        return documentWorkflowsEnabled;
    }

    public void setDocumentWorkflowsEnabled(boolean documentWorkflowsEnabled) {
        this.documentWorkflowsEnabled = documentWorkflowsEnabled;
    }

    public boolean isCaseFileWorkflowsEnabled() {
        return caseFileWorkflowsEnabled;
    }

    public void setCaseFileWorkflowsEnabled(boolean caseFileWorkflowsEnabled) {
        this.caseFileWorkflowsEnabled = caseFileWorkflowsEnabled;
    }

}
