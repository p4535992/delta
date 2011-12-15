package ee.webmedia.alfresco.testdata;

import static ee.webmedia.alfresco.addressbook.model.AddressbookModel.URI;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getContentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileFolderService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getRegisterService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getTransactionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FILE_CONTENTS;

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
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
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
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Test data generator. Can't use any Lucene searches, because lucene indexing may be turned off during test data generation and then lucene search results would be out-of-date.
 * 
 * @author Alar Kvell
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
        log.info("Stop requested. Stopping after current batch.");
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
                        log.info("Thread started");
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
                        log.info("Thread stopped");
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
    private List<Integer> registerIds;
    private Set<NodeRef> functions;
    private List<NodeRef> functionsList;
    private Set<SerieVO> series;
    private List<SerieVO> seriesList;
    private Map<NodeRef, Integer> seriesMaxOrder;
    private Map<NodeRef, SerieVO> seriesByVolumeRef;
    private Set<NodeRef> volumes;
    private List<NodeRef> volumesWithCases;
    private Set<NodeRef> cases;
    private List<Node> contactNodes;
    private List<DocumentLocationVO> docLocations;
    private List<NodeRef> docs;
    private Map<String, DocumentTypeVersion> docVersions;
    private Map<String, List<ClassificatorValue>> classificators;
    private List<Pair<RootOrgUnit, List<OrgUnit>>> orgUnits;
    private List<OrganizationStructure> structUnits;
    private Map<Integer, OrganizationStructure> structUnitsByUnitId;

    private final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private int orgUnitsCount = 478;
    private int usersCount = 5900;
    private int contactsCount = 2000;
    private int registersCount = 200;
    private int functionsCount = 49;
    private int seriesCount = 503;
    private int volumesCount = 5000;
    private int casesCount = 500;
    private int documentsCount = 1714500;

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

        filterFiles();

        // FUTURE: document creation, registration, file creation and workflow times that vary in the past

        createOrgUnits(orgUnitsCount); // TODO fix orgUnits
        createUsers(usersCount);
        createSubstitutes();
        createContacts(contactsCount);
        createRegisters(registersCount, documentsCount, seriesCount);
        createFunctions(functionsCount);
        createSeries(seriesCount);
        createVolumes(volumesCount); // TODO et kõikide sarjade all oleks vähemalt üks toimik
        createCases(casesCount); // TODO et kõikide toimikute all oleks vähemalt üks asi
        createDocuments(documentsCount);

        // TODO progressi raporteerimine iga 50 ühiku järel - panna tsükli algusesse, siis on näha kohe
        // TODO statistika kirjutamine csv failidesse

        // TODO vaadata et nimekirjades Menetluses, Registreerimiseks, Saatmata, Saatmisel oleks sobiv arv dokumente

        // TODO kas arhiivi ka genereerida? kui jah, siis fn-sari-toimik-asi võib identsed või samade arvude alusel genereerida vist, aga lihtsalt suletud

        // ---------------
        // XXX koormustestimisega läbi mängida indekseerimise juhud: kui storeInIndex välja lülitada; kui propertite väärtused indexist küsida
        // XXX koormustestimisega läbi mängida store'de juhud: kui storeInIndex välja lülitada; kui propertite väärtused indexist küsida
        // XXX koormustestimisel uurida kustutamise aeglust ja workaroundi (öösel kustutamise) sobivust
        // XXX koormustestimisel jälgida gkrellm'iga ja jconsole'iga koormust; veel parem kui millegagi lindistada saaks graafikuid
    }

    private void filterFiles() {
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
            super(name, 0, 0, null);
            this.subUnitsCountGoal = subUnitsCountGoal;
            this.secondLevelNameCandidates = secondLevelNameCandidates;
        }

        public void setUnitId(int unitId) {
            Assert.isTrue(this.unitId == 0 && unitId > 0);
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
        protected int unitId;
        protected int level;
        protected String path;
        protected List<OrgUnit> subUnits = new ArrayList<OrgUnit>();

        public OrgUnit(String name, int unitId, int level, String parentPath) {
            this.name = name;
            this.unitId = unitId;
            this.level = level;
            path = (StringUtils.isNotBlank(parentPath) ? (parentPath + ", ") : "") + name;
        }

        public String getName() {
            return name;
        }

        public int getUnitId() {
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
        structUnitsByUnitId = new HashMap<Integer, OrganizationStructure>();
        // if (count < 5) {
        // return;
        // }
        // TODO disable orgUnits sync

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
            maxUnitId = Math.max(maxUnitId, structUnit.getUnitId());
            copy.remove(structUnit.getName());
            structUnitsByUnitId.put(structUnit.getUnitId(), structUnit);
        }

        for (Pair<RootOrgUnit, List<OrgUnit>> pair : orgUnits) {
            RootOrgUnit rootOrgUnit = pair.getFirst();
            for (OrganizationStructure structUnit : structUnits) {
                if (structUnit.getName().equals(rootOrgUnit.getName()) && structUnit.getSuperUnitId() == 0) {
                    rootOrgUnit.setUnitId(structUnit.getUnitId());
                    addSubUnits(rootOrgUnit, structUnits, rootOrgUnit.getSubUnits());
                    break;
                }
            }
            if (rootOrgUnit.getUnitId() < 1) {
                OrganizationStructure orgStruct = new OrganizationStructure();
                orgStruct.setUnitId(++maxUnitId);
                orgStruct.setSuperUnitId(0);
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
                orgStruct.setUnitId(++maxUnitId);
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
        // FIXME ALAR: Following parameter has been removed.
        //@formatter:off
        /*
        @SuppressWarnings("unchecked")
        Parameter<Long> employeeRegReceiveUsersPeriod = (Parameter<Long>) getParametersService().getParameter(Parameters.EMPLOYEE_REG_RECEIVE_USERS_PERIOD);
        employeeRegReceiveUsersPeriod.setParamValue(500000L);
        Collection<Parameter<? extends Serializable>> params = new ArrayList<Parameter<? extends Serializable>>();
        params.add(employeeRegReceiveUsersPeriod);
        getParametersService().updateParameters(params);
        log.info("Set parameter " + employeeRegReceiveUsersPeriod.getParamName() + " value to " + employeeRegReceiveUsersPeriod.getParamValue());
         */
        // @formatter:on

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

        File filename = new File(dataFolder, "users.csv");
        CsvWriter writer = new CsvWriter(new FileOutputStream(filename), ';', Charset.forName("UTF-8"));
        try {
            for (String userName : userNames) {
                writer.writeRecord(new String[] { userName });
            }
        } finally {
            writer.close();
        }
        log.info("Wrote " + userNames.size() + " usernames to file " + filename);
        userNamesList = new ArrayList<String>(userNames);
        Collections.shuffle(userNamesList);
    }

    private Map<String, Pair<NodeRef, Map<QName, Serializable>>> userDataByUserName;

    private void createSubstitutes() {
        userDataByUserName = new HashMap<String, Pair<NodeRef, Map<QName, Serializable>>>();
        if (userNamesList.size() < 2) {
            return;
        }
        Set<String> workingUsers = new HashSet<String>();
        Set<String> restingUsers = new HashSet<String>();
        for (String userName : userNames) {
            List<Substitute> subs = getSubstituteService().getSubstitutes(getUserData(userName).getFirst());
            for (Substitute sub : subs) {
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
            NodeRef userRef = getUserService().getPerson(userName);
            Map<QName, Serializable> userProps = getNodeService().getProperties(userRef);
            userData = Pair.newInstance(userRef, userProps);
            userDataByUserName.put(userName, userData);
        }
        return userData;
    }

    private void createUser(String userName, Set<String> userNames, Set<String> zones) {
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
        getPersonService().getPerson(userName); // to create home folder
        userNames.add(userName);
        log.info("Created user " + userName + " " + firstName + " " + lastName);
    }

    private String testEmail;

    public String getTestEmail() {
        return testEmail;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    private void createFunctions(int count) {
        List<Function> allFunctions = getFunctionsService().getAllFunctions();
        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(functionMarksAndTitles);
        int order = 1;
        functions = new HashSet<NodeRef>();
        for (Function function : allFunctions) {
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
            getFunctionsService().saveOrUpdate(function);
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

        List<DocumentType> documentTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
        for (Iterator<DocumentType> i = documentTypes.iterator(); i.hasNext();) {
            DocumentType docType = i.next();
            if (SystematicDocumentType.INCOMING_LETTER.isSameType(docType.getId()) || SystematicDocumentType.OUTGOING_LETTER.isSameType(docType.getId())) {
                i.remove();
            }
        }
        Random docTypesCountRandom = new Random();

        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(seriesMarksAndTitles);
        series = new HashSet<TestDataService.SerieVO>();
        seriesMaxOrder = new HashMap<NodeRef, Integer>();
        for (NodeRef functionRef : functions) {
            checkStop();
            List<Series> allSeries = getSeriesService().getAllSeriesByFunction(functionRef);
            for (Series serie : allSeries) {
                for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                    Pair<String, String> pair = i.next();
                    if (pair.getFirst().equals(stripAndTrim(serie.getSeriesIdentifier())) && pair.getSecond().equals(stripAndTrim(serie.getTitle()))) {
                        i.remove();
                    }
                }
                Integer order = seriesMaxOrder.get(serie.getFunctionNodeRef());
                if (order == null) {
                    order = 1;
                } else {
                    order = Math.max(order, serie.getOrder());
                }
                seriesMaxOrder.put(serie.getFunctionNodeRef(), order);
                series.add(new SerieVO(serie.getNode(), serie.getFunctionNodeRef()));
            }
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        while (i.hasNext() && series.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            createSerie(entry, count, documentTypes, docTypesCountRandom, functionsRandom, registersRandom);
        }
        while (series.size() < count) {
            checkStop();
            createSerie(getRandom(seriesMarksAndTitles), count, documentTypes, docTypesCountRandom, functionsRandom, registersRandom);
        }
        log.info("There are " + series.size() + " series; goal was " + count + " series");
        seriesList = new ArrayList<SerieVO>(series);
        Collections.shuffle(seriesList);
    }

    private void createSerie(Pair<String, String> entry, int count, List<DocumentType> documentTypes, Random docTypesCountRandom, Random functionsRandom, Random registersRandom) {
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
        double random = Math.random() * 3;
        if (random < 1.0d) {
            volType.add(VolumeType.ANNUAL_FILE.name());
            volType.add(VolumeType.SUBJECT_FILE.name());
        } else if (random < 2.0d) {
            volType.add(VolumeType.SUBJECT_FILE.name());
        } else {
            volType.add(VolumeType.ANNUAL_FILE.name());
        }
        props.put(SeriesModel.Props.VOL_TYPE.toString(), volType);
        Integer retentionPeriod = null;
        random = Math.random();
        if (random < 0.55d) {
            retentionPeriod = 5;
        } else if (random < 0.7d) {
            retentionPeriod = 7;
        } else if (random < 0.9d) {
            retentionPeriod = 75;
        }
        props.put(SeriesModel.Props.RETENTION_PERIOD.toString(), retentionPeriod);
        String accessRestriction;
        if (Math.random() < 0.9d) {
            accessRestriction = AccessRestriction.AK.getValueName();
            props.put(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString(), getRandom(accessRestrictionReasons));
            props.put(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), cal.getTime());
            if (Math.random() < 0.15d) {
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), getRandom(accessRestrictionEndDescs));
            } else if (retentionPeriod != null) {
                cal.add(Calendar.YEAR, retentionPeriod);
                props.put(SeriesModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), cal.getTime());
            }
        } else {
            accessRestriction = AccessRestriction.OPEN.getValueName();
        }
        props.put(SeriesModel.Props.ACCESS_RESTRICTION.toString(), accessRestriction);
        getSeriesService().saveOrUpdateWithoutReorder(serie);
        seriesMaxOrder.put(functionRef, order);

        series.add(new SerieVO(serie.getNode(), serie.getFunctionNodeRef()));
        log.info("Created series " + mark + " " + title);
    }

    private void createVolumes(int count) {
        Random seriesRandom = new Random();

        List<Pair<String, String>> copy = new ArrayList<Pair<String, String>>(volumesMarksAndTitles);
        volumes = new HashSet<NodeRef>();
        volumesWithCases = new ArrayList<NodeRef>();
        seriesByVolumeRef = new HashMap<NodeRef, SerieVO>();
        docLocations = new ArrayList<DocumentLocationVO>();
        for (SerieVO serie : series) {
            checkStop();
            List<Volume> allVolumes = getVolumeService().getAllVolumesBySeries(serie.getSeriesRef());
            for (Volume volume : allVolumes) {
                for (Iterator<Pair<String, String>> i = copy.iterator(); i.hasNext();) {
                    Pair<String, String> pair = i.next();
                    if (pair.getFirst().equals(stripAndTrim(volume.getVolumeMark())) && pair.getSecond().equals(stripAndTrim(volume.getTitle()))) {
                        i.remove();
                    }
                }
                NodeRef volumeRef = volume.getNode().getNodeRef();
                volumes.add(volumeRef);
                if (volume.isContainsCases()) {
                    volumesWithCases.add(volumeRef);
                } else {
                    docLocations.add(new DocumentLocationVO(serie, volumeRef));
                }
                seriesByVolumeRef.put(volumeRef, serie);
            }
        }
        Iterator<Pair<String, String>> i = copy.iterator();
        while (i.hasNext() && volumes.size() < count) {
            checkStop();
            Pair<String, String> entry = i.next();
            i.remove();
            createVolume(entry, count, seriesRandom);
        }
        while (volumes.size() < count) {
            checkStop();
            createVolume(getRandom(volumesMarksAndTitles), count, seriesRandom);
        }
        log.info("There are " + volumes.size() + " volumes; goal was " + count + " volumes");
        Collections.shuffle(volumesWithCases);
    }

    private void createVolume(Pair<String, String> entry, int count, Random seriesRandom) {
        String mark = entry.getFirst();
        String title = entry.getSecond();

        SerieVO serie = getRandomGaussian2(seriesList, seriesRandom);
        Volume volume = getVolumeService().createVolume(serie.getSeriesRef());
        Map<String, Object> props = volume.getNode().getProperties();
        String volumeType = getRandom(serie.getVolType());
        if (VolumeType.SUBJECT_FILE.equals(volumeType)) {
            mark = serie.getMark();
        }
        props.put(VolumeModel.Props.VOLUME_MARK.toString(), mark);
        props.put(VolumeModel.Props.TITLE.toString(), title);
        props.put(VolumeModel.Props.VOLUME_TYPE.toString(), volumeType);
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
        } else {
            docLocations.add(new DocumentLocationVO(serie, volumeRef));
        }
        seriesByVolumeRef.put(volumeRef, serie);
        log.info("Created volume " + mark + " " + title);
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
        for (NodeRef volume : volumesWithCases) {
            checkStop();
            List<Case> allCases = getCaseService().getAllCasesByVolume(volume);
            for (Case case1 : allCases) {
                for (Iterator<String> i = copy.iterator(); i.hasNext();) {
                    String title = i.next();
                    if (title.equals(stripAndTrim(case1.getTitle()))) {
                        i.remove();
                    }
                }
                NodeRef caseRef = case1.getNode().getNodeRef();
                cases.add(caseRef);
                NodeRef volumeRef = case1.getVolumeNodeRef();
                docLocations.add(new DocumentLocationVO(seriesByVolumeRef.get(volumeRef), volumeRef, caseRef));
            }
        }
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
        NodeRef contactRef = getNodeService().createNode(getAddressbookService().getAddressbookRoot(),
                assocType,
                randomqname,
                type,
                props).getChildRef();
        contactNodes.add(getAddressbookService().getNode(contactRef));
        log.info("Created contact " + contact);
    }

    private void createDocuments(int count) throws Exception {
        checkStop();
        docVersions = new HashMap<String, DocumentTypeVersion>();
        classificators = new HashMap<String, List<ClassificatorValue>>();
        Collections.shuffle(docLocations);
        Random docLocationsRandom = new Random();
        Random usersRandom = new Random();

        docs = new ArrayList<NodeRef>();
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

        try {
            while (docs.size() < documentsCount) {
                checkStop();
                final DocumentLocationVO docLocation = getRandomGaussian3(docLocations, docLocationsRandom);
                String userName = getRandomGaussian3(userNamesList, usersRandom);
                AuthenticationUtil.setFullyAuthenticatedUser(userName); // also sets runAsUser
                getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        createDocument(docLocation);
                        return null;
                    }
                }, false);
            }
        } finally {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());
            getFunctionsService().updateDocCounters();
        }
        log.info("There are " + docs.size() + " documents; goal was " + count + " documents");
    }

    private void createDocument(DocumentLocationVO docLocation) throws Exception {
        String docTypeId = getRandom(docLocation.getSeriesDocType());
        DocumentTypeVersion docVer = docVersions.get(docTypeId);
        if (docVer == null) {
            DocumentType docType = getDocumentAdminService().getDocumentType(docTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
            docVer = docType.getLatestDocumentTypeVersion();
            docVersions.put(docTypeId, docVer);
        }
        DocumentDynamic doc = getDocumentDynamicService().createNewDocument(docVer, docLocation.getDocumentParentRef()).getFirst();

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
            PropertyDefinition propDef = pair.getFirst();
            Field field = pair.getSecond();
            if (field == null) {
                continue;
            }
            QName propName = field.getQName();
            Serializable value = doc.getProp(propName);
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
            } else if (SystematicFieldGroupNames.CONTRACT_PARTIES.equals(systematicGroupName)) { // TODO subnode support!
                continue;
            }

            if (value == null
                    || (value instanceof String && StringUtils.isBlank((String) value))
                    || (value instanceof List && (((List<?>) value).size() == 0
                            || (((List<?>) value).size() == 1 && (((List<?>) value).get(0) == null
                                    || (((List<?>) value).get(0) instanceof String && StringUtils.isBlank((String) ((List<?>) value).get(0)))))))) {

                // Always fill empty fields that are mandatory; if not mandatory then fill only half of the fields
                if (!field.isMandatory() && Math.random() < 0.5d) {
                    continue;
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
                case INFORMATION_TEXT:
                    // do nothing
                    continue;
                }

                if (propDef.isMultiValued() && !(value instanceof List<?>)) {
                    ArrayList<Serializable> list = new ArrayList<Serializable>();
                    list.add(value);
                    value = list;
                }

                doc.setProp(propName, value);
            }

        }
        NodeRef docRef = doc.getNodeRef();

        // FILES
        int filesCount = 0;
        int r = (int) (Math.random() * 12);
        if (r > 10) {
            filesCount = (int) ((Math.random() * 30) + 10);
            if (filesCount > 35) {
                filesCount = (int) ((Math.random() * 30) + 36);
            }
        } else if (r > 1) {
            filesCount = r - 1;
        }
        ArrayList<String> fileTitles = new ArrayList<String>();
        Map<QName, Serializable> userProps = getUserData(getUserService().getCurrentUserName()).getSecond();
        ContentWriter allWriter = BeanHelper.getContentService().getWriter(docRef, FILE_CONTENTS, false);
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
        getNodeService().addAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
        doc.setProp(DocumentCommonModel.Props.FILE_NAMES, fileTitles);
        doc.setProp(DocumentCommonModel.Props.FILE_CONTENTS, allWriter.getContentData());
        doc.setProp(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, new ArrayList<String>());
        getDocumentDynamicService().updateDocument(doc, Arrays.asList("TestDataService"), false);

        // ASSOCS
        Random assocsRandom = new Random();
        int assocsCount = docs.isEmpty() ? 0 : Math.abs(getRandomGaussian2(assocsRandom, 16) - 8);
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

        createWorkflows(docRef, !isRegistered, docTypeId);

        docs.add(docRef);
        log.info("Created document " + docs.size() + " of " + documentsCount + " - " + docRef + " " + docTypeId + ", " + regNumber + ", " + filesCount + " files, "
                + assocOtherDocRefs.size() + " assocs");

        // FUTURE: õiguseid (nii dokumendi kui sarja omad) praegu ei tee

    }

    private void createWorkflows(NodeRef docRef, boolean inProgress, String docTypeId) {
        if (Math.random() < 0.05d) {
            return;
        }
        // 10% registreerimata, neist 95% omab töövoogu, seega kokku 9,5%, aga see on teostamisel
        // aga tahame et 1000 dokumenti oleksid registreerimiseks menüüpunktis
        // 1714500 dokist 162878 dokki on regamata, aga teostamisel töövooga, seega 0,61% neist tahame lõpetada
        if (inProgress && Math.random() < 0.0062d) {
            inProgress = false;
        }

        String creatorUserName = getRandom(userNamesList);
        Map<QName, Serializable> creatorProps = userDataByUserName.get(creatorUserName).getSecond();
        String creatorFullName = UserUtil.getPersonFullName1(creatorProps);
        String creatorEmail = (String) creatorProps.get(ContentModel.PROP_EMAIL);

        String cwfOwnerUserName = getRandom(userNamesList);
        String cwfOwnerFullName = UserUtil.getPersonFullName1(userDataByUserName.get(cwfOwnerUserName).getSecond());

        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.STATUS, inProgress ? Status.IN_PROGRESS.getName() : Status.FINISHED.getName());
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorFullName);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, ((int) (Math.random() * -900)) - 30);
        Date startedDateTime = cal.getTime();
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
        props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
        props.put(WorkflowCommonModel.Props.OWNER_ID, cwfOwnerUserName);
        props.put(WorkflowCommonModel.Props.OWNER_NAME, cwfOwnerFullName);
        NodeRef cwfRef = getNodeService().createNode(
                docRef,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Types.COMPOUND_WORKFLOW,
                props
                ).getChildRef();

        List<QName> wfTypes = Arrays.asList(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW, WorkflowSpecificModel.Types.OPINION_WORKFLOW,
                WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW);
        // Not used: REGISTRATION_WORKFLOW, EXTERNAL_REVIEW_WORKFLOW, DUE_DATE_EXTENSION_WORKFLOW -- too special
        // FUTURE: orderAssignmentWorkflow -- could be used, but don't remember exact rules right now (must have category...?)?
        Map<QName, WorkflowType> wfTypesByWf = getWorkflowService().getWorkflowTypes();

        int wfCount = ((int) (Math.random() * 6)) + 1; // 1 - 6
        for (int i = 0; i < wfCount; i++) {
            QName wfType;
            if (i == 0 && Math.random() < 0.5d) {
                wfType = WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW;
            } else {
                wfType = getRandom(wfTypes);
            }
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
            if (Math.random() < 0.8d) {
                taskCount = ((int) (Math.random() * 10)) + 1; // 1 - 10
            } else {
                taskCount = ((int) (Math.random() * 90)) + 11; // 11 - 100
            }
            for (int j = 0; j < taskCount; j++) {
                if (userNamesListCopy.isEmpty()) {
                    break;
                }
                String taskOwnerUserName = getRandom(userNamesListCopy);
                userNamesListCopy.remove(taskOwnerUserName);
                Map<QName, Serializable> taskOwnerProps = userDataByUserName.get(taskOwnerUserName).getSecond();
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
                props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) (taskOwnerStructUnit != null ? Collections.singleton(taskOwnerStructUnit.getName())
                        : null));

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
                NodeRef taskRef = getNodeService().createNode(
                        wfRef,
                        WorkflowCommonModel.Assocs.TASK,
                        WorkflowCommonModel.Assocs.TASK,
                        taskType,
                        props
                        ).getChildRef();
                getNodeService().addAspect(taskRef, WorkflowSpecificModel.Aspects.SEARCHABLE, null);

                // if assignmentWorkflow, then 1 task must have responsible active=true, (and 0-few can have responsible active=false)
                if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && j == 0) {
                    props = new HashMap<QName, Serializable>();
                    props.put(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
                    getNodeService().addAspect(taskRef, WorkflowSpecificModel.Aspects.RESPONSIBLE, props);
                }
            }
        }

        getNodeService().setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);
    }

    private void createFile(NodeRef docRef, ArrayList<String> fileTitles, Map<QName, Serializable> userProps, OutputStream allOutput, int i) throws Exception {
        List<String> file = getRandom(files);
        String fileUrl = file.get(0);
        String fileMimeType = file.get(1);
        String fileEncoding = file.get(2);
        String fileSize = file.get(3);
        String fileName = file.get(4);
        String fileTitle = file.get(5);
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
        if (!textFile.exists()) {
            textFile.createNewFile();
            boolean readerReady = true;
            ContentReader reader = getFileFolderService().getReader(fileRef);
            if (reader != null && reader.exists()) {
                if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN)
                        || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8")) {
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
        return StringUtils.trimToEmpty(StringUtils.stripToEmpty(value));
    }

    // SaveListener that sets draft=true on document

    @Override
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
        // do nothing
    }

    @Override
    public void save(DocumentDynamic document) {
        document.setDraft(true);
        document.setDraftOrImapOrDvk(true);
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

}
