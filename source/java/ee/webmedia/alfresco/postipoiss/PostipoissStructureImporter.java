package ee.webmedia.alfresco.postipoiss;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import de.schlichtherle.io.FileInputStream;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Assocs;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.SeriesType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Imports structure (contacts, functions and volumes) from postipoiss.
<<<<<<< HEAD
 * 
 * @author Taimo Peelo
 * @author Aleksei Lissitsin
=======
>>>>>>> develop-5.1
 */
public class PostipoissStructureImporter {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PostipoissStructureImporter.class);

    private static final String CONTACT_GROUPS_FILENAME = "kontakt_grupid.csv";
    private static final String CONTACTS_FILENAME = "kontaktid.csv";
    private static final String COMPLETED_CONTACTS_FILENAME = "completed_kontaktid.csv";
    private static final String CONTACTS_IN_GROUPS_FILENAME = "kontakti_kuulumine_gruppi.csv";
    private static final String FUNCTIONS_FILENAME = "struktuur.csv";
    private static final String VOLUMES_FILENAME = "toimikud.csv";
    private static final String COMPLETED_VOLUMES_FILENAME = "completed_toimikud.csv";
    private static final String ATTR_MULTIPLE_YEARS = "mitu asjaajamisaastat";
    private static final String ATTR_SINGLE_YEAR = "üks asjaajamisaasta";

    final private static String REGISTER_NAME = "vana_register";

    private static final String INPUT_ENCODING = "ISO-8859-1";
    private static final String OUTPUT_ENCODING = "UTF-8";
    private static final char OUTPUT_SEPARATOR = ';';
    private static final String CREATOR_MODIFIER = "DELTA";

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    // ========= THINGS YOU MAY WANT TO CHANE =========

    private Date seriesValidFrom;

    // ================================================

    public PostipoissStructureImporter(PostipoissImporter postipoissImporter) {
        // <bean id="postipoissStructureImporter" class="ee.webmedia.alfresco.postipoiss.PostipoissStructureImporter">
        // <property name="generalService" ref="generalService" />
        // <property name="addressbookService" ref="AddressbookService" />
        // <property name="functionsService" ref="FunctionsService" />
        // <property name="seriesService" ref="SeriesService" />
        // <property name="volumeService" ref="VolumeService" />
        // <property name="documentTypeService" ref="DocumentTypeService" />
        // <property name="registerService" ref="RegisterService" />
        // <property name="transactionService" ref="TransactionService" />
        // <property name="nodeService" ref="NodeService" />
        // <property name="caseService" ref="CaseService" />
        // <property name="behaviourFilter" ref="policyBehaviourFilter" />
        // </bean>
        setGeneralService(BeanHelper.getGeneralService());
        setAddressbookService(BeanHelper.getAddressbookService());
        setFunctionsService(BeanHelper.getFunctionsService());
        setSeriesService(BeanHelper.getSeriesService());
        setVolumeService(BeanHelper.getVolumeService());
        setDocumentTypeService(BeanHelper.getDocumentTypeService());
        setRegisterService(BeanHelper.getRegisterService());
        setTransactionService(BeanHelper.getTransactionService());
        setNodeService(BeanHelper.getNodeService());
        setCaseService(BeanHelper.getCaseService());
        setBehaviourFilter(BeanHelper.getPolicyBehaviourFilter());
        setPostipoissImporter(postipoissImporter);
        seriesComparisonIncludesTitle = postipoissImporter.isSeriesComparisonIncludesTitle();

        dateFormat.setLenient(false);
        try {
            seriesValidFrom = dateFormat.parse("01.01.2010");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private File dataFolder;
    private File workFolder;
    private NodeRef archivalsRoot;
    private boolean openUnit;

    private AddressbookService addressbookService;
    private FunctionsService functionsService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private DocumentTypeService documentTypeService;
    private RegisterService registerService;
    private TransactionService transactionService;
    private GeneralService generalService;
    private NodeService nodeService;
    private CaseService caseService;
    private BehaviourFilter behaviourFilter;
    private PostipoissImporter postipoissImporter;

    private final boolean seriesComparisonIncludesTitle;
    private Map<String, NodeRef> contactCache;
    private Map<String, NodeRef> contactGroupCache;
    private Integer registerId;
    private Map<String, Funk> funks;
    private Map<String, NodeRef> functions;
    private Map<String, NodeRef> archivedFunctions;
    private Set<String> missingFunctions;
    private Map<String, Map<String, NodeRef>> seriesByIndex;
    private Map<String, Map<String, NodeRef>> archivedSeriesByIndex;
    private List<Toimik> toimikud;

    // private Map<Integer, Map<String, Toimik>> toimikMap;
    // private Map<String, Volume> preenteredVolumes;
    // private Map<String, Case> preenteredCases;

    // private List<QName> allDocumentTypes;

    // TODO kontaktgruppide impordi võib sisse kommenteerida ja kontrollida et kui faili pole olemas, siis skipiks
    // TODO funktsioonid võiks kohe arhiivi alla luua, dok.loetelu alla loomine ja liigutamine on mõttetu

    /**
     * Runs the contact/structure import process
     */
    public void runImport(File dataFolder, File workFolder, NodeRef archivalsRoot, boolean openUnit) throws Exception {
        this.dataFolder = dataFolder;
        this.workFolder = workFolder;
        this.archivalsRoot = archivalsRoot;
        this.openUnit = openUnit;
        init();

        // List<DocumentType> dts = documentTypeService.getAllDocumentTypes();
        // allDocumentTypes = new ArrayList<QName>();
        // for (DocumentType dt : dts) {
        // allDocumentTypes.add(dt.getId());
        // }

        log.info("Structure import is starting to run");

        RetryingTransactionHelper helper = getTransactionHelper();
        if (helper.doInTransaction(new RetryingTransactionCallback<Boolean>() {
            @Override
            public Boolean execute() throws Throwable {
                boolean createContacts = createContacts();
                if (createContacts) {
                    log.info("Commiting transaction...");
                }
                return createContacts;
            }
        })) {
            File contactsCompletedFile = new File(workFolder, COMPLETED_CONTACTS_FILENAME);
            FileUtils.writeStringToFile(contactsCompletedFile, "");
        }
        log.info("Contacts import completed");

        File outputFile = new File(workFolder, COMPLETED_VOLUMES_FILENAME);
        if (outputFile.exists()) {
            log.info("Structure results file '" + outputFile + "' exists, skipping structure import");
        } else {
            helper.doInTransaction(new RetryingTransactionCallback<Object>() {
                @Override
                public Object execute() throws Throwable {
                    behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
                    readFunks();
                    createSeries();
                    log.info("Commiting transaction...");
                    return null;
                }
            });
            writeToimikud();
        }
        log.info("Structure import is COMPLETED");
    }

    private void init() {
        contactCache = new LinkedHashMap<String, NodeRef>();
        contactGroupCache = new LinkedHashMap<String, NodeRef>();
        registerId = null;
        funks = new HashMap<String, Funk>();
        functions = new HashMap<String, NodeRef>();
        archivedFunctions = new HashMap<String, NodeRef>();
        missingFunctions = new HashSet<String>();
        seriesByIndex = new HashMap<String, Map<String, NodeRef>>();
        archivedSeriesByIndex = new HashMap<String, Map<String, NodeRef>>();
        toimikud = new ArrayList<Toimik>(6000);

        // toimikMap = new HashMap<Integer, Map<String, Toimik>>();
        // preenteredVolumes = new HashMap<String, Volume>();
        // preenteredCases = new HashMap<String, Case>();

        // loadPreenteredVolumes();
    }

    private int getRegisterId() {
        if (registerId == null) {
            List<Register> registers = registerService.getRegisters();
            for (Register register : registers) {
                if (REGISTER_NAME.equals(register.getName())) {
                    registerId = register.getId();
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

    // ///////////////////////////////////////////////////////////////////////////
    // CONTACTS
    // /////////////////////////////////////////////////////////////////////////
    protected boolean createContacts() throws Exception {
        {
            File contactsCompletedFile = new File(workFolder, COMPLETED_CONTACTS_FILENAME);
            if (contactsCompletedFile.exists()) {
                log.info("Contacts completed file '" + contactsCompletedFile + "' exists, skipping contacts and contact groups import");
                return false;
            }

            File contactsFile = new File(dataFolder, CONTACTS_FILENAME);
            if (!contactsFile.exists()) {
                log.info("Contacts file '" + contactsFile + "' does not exist, skipping contacts and contact groups import");
                return false;
            }

            log.info("Reading Postipoiss contacts file '" + contactsFile + "' with encoding " + INPUT_ENCODING);
            CsvReader contactReader = new CsvReader(new FileInputStream(contactsFile), ';', Charset.forName(INPUT_ENCODING));

            try {
                while (contactReader.readRecord()) {
                    try {
                        checkStop();
                        createContact(contactReader);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while importing contact from row index " + contactReader.getCurrentRecord() + " in file "
                                + contactsFile + ": " + e.getMessage(), e);
                    }
                }
            } finally {
                contactReader.close();
            }
        }

        //@formatter:off
        /* Contact groups import is not used in PPA
        {
            File contactgroupsFile = new File(dataFolder, CONTACT_GROUPS_FILENAME);
            log.info("Reading Postipoiss contact groups file '" + contactgroupsFile + "' with encoding " + INPUT_ENCODING);
            CsvReader contactGroupReader = new CsvReader(new FileInputStream(contactgroupsFile), ';', Charset.forName(INPUT_ENCODING));

            try {
                while (contactGroupReader.readRecord()) {
                    try {
                        checkStop();
                        createContactGroup(contactGroupReader);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while importing contactgroup from row index " + contactGroupReader.getCurrentRecord() + " in file "
                                + contactgroupsFile + ": " + e.getMessage(), e);
                    }
                }
            } finally {
                contactGroupReader.close();
            }
        }

        {
            File contactsingroupsFile = new File(dataFolder, CONTACTS_IN_GROUPS_FILENAME);
            log.info("Reading Postipoiss contacts-in-group file '" + contactsingroupsFile + "' with encoding " + INPUT_ENCODING);
            CsvReader contactsinGroupsReader = new CsvReader(new FileInputStream(contactsingroupsFile), ';', Charset.forName(INPUT_ENCODING));

            try {
                while (contactsinGroupsReader.readRecord()) {
                    try {
                        checkStop();
                        addContactsToGroups(contactsinGroupsReader);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while importing contacts in groups from row index " + contactsinGroupsReader.getCurrentRecord() + " in file "
                                + contactsingroupsFile + ": " + e.getMessage(), e);
                    }
                }
            } finally {
                contactsinGroupsReader.close();
            }
        }
        */
        //@formatter:on
        return true;
    }

    private void checkStop() {
        postipoissImporter.checkStop();
    }

    private void createContact(CsvReader reader) throws Exception {
        String orgId = reader.get(0);
        String orgName = reader.get(1);
        String registryCode = reader.get(2);
        String shortname = reader.get(3);
        String englishOrgName = reader.get(4);
        reader.get(5); // ppEmail
        String email = reader.get(6);
        String phone = reader.get(7);
        String fax = reader.get(8);
        String address = reader.get(9);
        String town = reader.get(10);
        String postalCode = reader.get(11);
        String country = reader.get(12);
        reader.get(13); // notes

        // ppEmail & notes will not be put into simDHS!
        NodeRef contactRef = createContactNode(orgId, orgName, registryCode, shortname, englishOrgName, email, phone, fax, address, town, postalCode, country);
        contactCache.put(orgId, contactRef);
        logAllCreation("contact '" + orgName + "'", contactRef);
    }

    private NodeRef createContactGroupNode(String groupName) {
        NodeRef abRoot = addressbookService.getAddressbookRoot();

        QName randomqname = QName.createQName(AddressbookModel.URI, GUID.generate());
        NodeRef result = nodeService.createNode(abRoot, AddressbookModel.Assocs.CONTACT_GROUPS, randomqname, AddressbookModel.Types.CONTACT_GROUP)
                .getChildRef();
        logAllCreation("contact group '" + groupName + "'", result);

        HashMap<QName, Serializable> map = new HashMap<QName, Serializable>();
        map.put(AddressbookModel.Props.GROUP_NAME, groupName);

        nodeService.setProperties(result, map);
        return result;
    }

    private void createContactGroup(CsvReader contactGroupReader) throws Exception {
        String groupId = null;
        String groupName = null;

        try {
            groupId = contactGroupReader.get(0);
            groupName = contactGroupReader.get(1);
        } catch (Exception e) {
            log.debug("error occurred CSVReader record #" + contactGroupReader.getCurrentRecord() + " obtained data follows: ");
            log.debug(" groupId = " + groupId);
            log.debug(" groupName = " + groupName);
            throw e;
        }

        NodeRef contactGroupRef = createContactGroupNode(groupName);
        contactGroupCache.put(groupId, contactGroupRef);
    }

    private NodeRef createContactNode(String orgId, String orgName, String registryCode, String shortname, String englishOrgName, String email,
            String phone, String fax, String address, String town, String postalCode, String country) {

        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(AddressbookModel.Props.ORGANIZATION_NAME, orgName);
        props.put(AddressbookModel.Props.ORGANIZATION_CODE, registryCode);
        props.put(AddressbookModel.Props.ORGANIZATION_ACRONYM, shortname);
        props.put(AddressbookModel.Props.ORGANIZATION_ALTERNATE_NAME, englishOrgName);
        props.put(AddressbookModel.Props.EMAIL, email);
        props.put(AddressbookModel.Props.PHONE, phone);
        props.put(AddressbookModel.Props.FAX, fax);
        props.put(AddressbookModel.Props.ADDRESS1, address);
        props.put(AddressbookModel.Props.CITY, town);
        props.put(AddressbookModel.Props.POSTAL, postalCode);
        props.put(AddressbookModel.Props.COUNTRY, country);
        props.put(AddressbookModel.Props.ACTIVESTATUS, Boolean.TRUE);

        QName randomqname = QName.createQName(AddressbookModel.URI, GUID.generate());
        NodeRef contactRef = nodeService.createNode(
                addressbookService.getAddressbookRoot(),
                AddressbookModel.Assocs.ORGANIZATIONS,
                randomqname,
                AddressbookModel.Types.ORGANIZATION,
                props).getChildRef();

        return contactRef;
    }

    private void addContactsToGroups(CsvReader contactsinGroupsReader) throws Exception {
        String groupId = null;
        String orgId = null;

        try {
            groupId = contactsinGroupsReader.get(0);
            orgId = contactsinGroupsReader.get(1);

            NodeRef groupNode = contactGroupCache.get(groupId);
            NodeRef orgNode = contactCache.get(orgId);

            if (orgNode == null || groupNode == null) {
                log.warn("could not add contact " + orgId + "  to group " + groupId + (orgNode == null ? "org is NULL" : "group is NULL") + " at row"
                        + contactsinGroupsReader.getCurrentRecord());
            } else {
                addContactToGroup(groupNode, orgNode);
                log.info("contact " + orgId + " added to group " + groupId);
            }

        } catch (Exception e) {
            log.debug("error occurred CSVReader record #" + contactsinGroupsReader.getCurrentRecord() + " obtained data follows: ");
            log.debug(" groupId = " + groupId);
            log.debug(" orgId = " + orgId);
            throw e;
        }
    }

    private void addContactToGroup(NodeRef groupNode, NodeRef orgNode) {
        nodeService.createAssociation(groupNode, orgNode, Assocs.CONTACT_ORGANIZATION);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Funks
    // /////////////////////////////////////////////////////////////////////////

    // private Date year2010;
    // {
    // try {
    // year2010 = dateFormat.parse("01.01.2010");
    // } catch (ParseException e) {
    // throw new RuntimeException(e);
    // }
    // }

    // private void loadPreenteredVolumes() {
    // List<Function> allFunctions = functionsService.getAllFunctions();
    // for (Function f : allFunctions) {
    // List<Series> allSeries = seriesService.getAllSeriesByFunction(f.getNodeRef());
    // for (Series s : allSeries) {
    // List<Volume> vols = volumeService.getAllVolumesBySeries(s.getNode().getNodeRef());
    // for (Volume v : vols) {
    // if (StringUtils.isNotEmpty(v.getVolumeMark()) && v.getValidFrom() != null && !v.getValidFrom().before(year2010)) {
    // if (v.isContainsCases()) {
    // List<Case> casesByVolume = caseService.getAllCasesByVolume(v.getNode().getNodeRef());
    // for (Case asi : casesByVolume) {
    // String title = asi.getTitle();
    // if (StringUtils.isNotBlank(title)) {
    // title = title.trim();
    // preenteredCases.put(title, asi);
    // }
    // }
    // }
    // preenteredVolumes.put(v.getVolumeMark(), v);
    // }
    // }
    // }
    // }
    // }

    static class Funk {
        String id;
        String title;
        String order;
        String parentId;

        public Funk(CsvReader r) throws IOException {
            id = r.get(0);
            title = r.get(1);
            order = r.get(2);
            parentId = r.get(3);
        }

        @Override
        public String toString() {
            return "Funk [id=" + id + ", title=" + title + ", order=" + order + ", parentId="
                    + parentId + "]";
        }
    }

    private void readFunks() throws Exception {
        final File inputFile = new File(dataFolder, FUNCTIONS_FILENAME);
        log.info("Reading Postipoiss functions file '" + inputFile + "' with encoding " + INPUT_ENCODING);
        CsvReader reader = new CsvReader(new FileInputStream(inputFile), ';', Charset.forName(INPUT_ENCODING));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                try {
                    Funk funk = new Funk(reader);
                    funks.put(funk.id, funk);
                } catch (Exception e) {
                    throw new RuntimeException("Error while reading function from row index " + reader.getCurrentRecord() + " in file " + inputFile + ": " + e.getMessage(), e);
                }
            }
        } finally {
            reader.close();
        }
    }

    private NodeRef getFunction(String functionId, boolean archived) {
        Map<String, NodeRef> map = archived ? archivedFunctions : functions;
        NodeRef funcRef = map.get(functionId);
        if (funcRef == null) {
            Funk funk = funks.get(functionId);
            if (funk == null) {
                return null;
            }
            funcRef = createFunction(funk, archived);
            map.put(functionId, funcRef);
        }
        return funcRef;
    }

    private NodeRef createFunction(Funk funk, boolean archived) {
        log.info(String.format("\n\nFUNCTION CREATION!!!!\n\n\nCreating%s function for funk: %s", (archived ? " archived" : ""), funk.toString()));
        NodeRef function = createFunction(funk);
        // XXX archivalsRoot already determines where the function is saved, so "archived" argument is not used
        // if (archived) {
        // function = archiveFunction(function);
        // }
        return function;
    }

    private NodeRef createFunction(Funk funk) {
        String trimmedTitle = funk.title; // A specific logic for SIM: trimPostipoissFunctionName(funk.title)
        String ppMark = getPostipoissMark(trimmedTitle);
        trimmedTitle = trimmedTitle.substring(ppMark.length());
        trimmedTitle = trimmedTitle.trim();
        log.info("IN CREATE FUNCTION: " + funk.id + " ---- " + trimmedTitle + " --- " + ppMark + " --- ");

        String functionType = ppMark.contains(".") ? "allfunktsioon" : "funktsioon";
        Function function = functionsService.createFunction();
        Map<String, Object> props = function.getNode().getProperties();
        props.put(FunctionsModel.Props.MARK.toString(), ppMark);
        props.put(FunctionsModel.Props.TITLE.toString(), trimmedTitle);
        props.put(FunctionsModel.Props.TYPE.toString(), functionType);
        props.put(FunctionsModel.Props.ORDER.toString(), funk.order);
        props.put(FunctionsModel.Props.STATUS.toString(), openUnit ? DocListUnitStatus.OPEN.getValueName() : DocListUnitStatus.CLOSED.getValueName());
        props.put(FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED.toString(), Boolean.FALSE);
        functionsService.saveOrUpdate(function, archivalsRoot);

        log.info(function.getNodeRef());

        logAllCreation("function '" + funk.id + "' " + funk.title, function.getNodeRef());
        return function.getNodeRef();
    }

    // ///////////////////////////////////////////////////////////////////////////
    // SERIES/VOLUMES
    // /////////////////////////////////////////////////////////////////////////

    // private NodeRef createNewCase(Volume v) {
    // Case asi = caseService.createCase(v.getNode().getNodeRef());
    // asi.setTitle(v.getTitle());
    // caseService.saveOrUpdate(asi, false);
    // return asi.getNode().getNodeRef();
    // }

    // private Toimik create2010Toimik(Toimik t) {
    // Volume v = preenteredVolumes.get(t.volumeMarkNormed);
    // if (v != null) {
    // NodeRef nodeRef = v.getNode().getNodeRef();
    // if (v.isContainsCases()) {
    // nodeRef = createNewCase(v);
    // }
    // t.nodeRef = nodeRef;
    // putToimik(t);
    // return t;
    // } else {
    // for (Entry<String, Case> entry : preenteredCases.entrySet()) {
    // if (entry.getKey().startsWith(t.volumeMarkNormed)) {
    // t.nodeRef = entry.getValue().getNode().getNodeRef();
    // putToimik(t);
    // return t;
    // }
    // }
    // }
    // return null;
    // }

    // private Toimik getToimik(Toimik t) {
    // Toimik r = findToimik(t);
    // if (r == null && isSeriesOpen(t)) {
    // r = create2010Toimik(t);
    // }
    // return r;
    // }

    // private Toimik findToimik(Toimik t) {
    // Map<String, Toimik> map = toimikMap.get(t.year());
    // if (map == null) {
    // return null;
    // }
    //
    // return map.get(t.volumeMarkNormed);
    // }

    // private void putToimik(Toimik t) {
    // Map<String, Toimik> map = toimikMap.get(t.year());
    // if (map == null) {
    // map = new HashMap<String, Toimik>();
    // toimikMap.put(t.year(), map);
    // }
    // map.put(t.volumeMarkNormed, t);
    // }

    class Toimik implements Comparable<Toimik> {
        String rowId;
        String functionId;
        String seriesIndex;
        String seriesTitle;
        String volumeMarkNormed;
        String volumeTitleNormed;
        String volumeMark;
        String volumeName;
        VolumeType volumeType;
        Date validFrom;
        Date validTo;
        int bestBefore;
        String seriesAccessRestriction;
        String seriesAccessRestrictionReason;
        private int year = 0;
        boolean archived = false;

        // FILLED AT IMPORT
        NodeRef nodeRef;

        Set<String> names;

        public Toimik(CsvReader r) throws IOException {
            rowId = r.get(0);
            functionId = r.get(1);
            seriesIndex = r.get(2);
            seriesTitle = r.get(3);
            volumeMarkNormed = r.get(4);
            volumeTitleNormed = r.get(5);
            volumeMark = r.get(6);
            volumeName = r.get(7);
            volumeType = toVolumeType(r.get(8));
            bestBefore = toBestBefore(r.get(9));
            validFrom = parseDate(r.get(10));
            Assert.notNull(validFrom, "validFrom is missing");
            validTo = parseDate(r.get(11));
            seriesAccessRestriction = r.get(12);
            seriesAccessRestrictionReason = r.get(13);

            archived = true; // Logic for SIM/MV was: isArchived(year(), validTo);
            prepareOrderArray();
        }

        int year() {
            if (year == 0) {
                year = PostipoissUtil.getYear(validFrom);
            }
            return year;
        }

        void add(String name) {
            if (names == null) {
                names = new HashSet<String>();
            }
            names.add(name);
        }

        @Override
        public String toString() {
            return "Toimik [rowId=" + rowId + ", functionId=" + functionId + ", seriesIndex=" + seriesIndex + ", seriesTitle="
                    + seriesTitle + ", volumeMarkNormed=" + volumeMarkNormed + ", volumeTitleNormed=" + volumeTitleNormed + ", validFrom=" + validFrom
                    + ", volumeType=" + volumeType + ", validTo=" + validTo + ", bestBefore=" + bestBefore + ", seriesAccessRestriction="
                    + seriesAccessRestriction + ", seriesAccessRestrictionReason=" + seriesAccessRestrictionReason + "]";
        }

        private Integer[] orderArray;

        private void prepareOrderArray() {
            List<Integer> ints = new ArrayList<Integer>();
            int curInt = 0;
            for (char c : seriesIndex.toCharArray()) {
                if (c == '.' || c == '-') {
                    ints.add(curInt);
                    curInt = 0;
                } else {
                    curInt = curInt * 10 + (c - '0');
                }
            }
            if (curInt != 0) {
                ints.add(curInt);
            }
            orderArray = ints.toArray(new Integer[0]);
        }

        @Override
        public int compareTo(Toimik o) {
            if (orderArray.length > o.orderArray.length) {
                return compare(orderArray, o.orderArray);
            } else {
                return -compare(o.orderArray, orderArray);
            }
        }

        private int compare(Integer[] longArray, Integer[] shortArray) {
            int length = shortArray.length;
            for (int i = 0; i < length; i++) {
                if (longArray[i] != shortArray[i]) {
                    return longArray[i] > shortArray[i] ? 1 : -1;
                }
            }
            if (longArray.length > length) {
                return 1;
            }
            return 0;
        }
    }

    private static int toBestBefore(String s) {
        int value = 0;
        if (StringUtils.isBlank(s)) {
            return value;
        }
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            log.warn("best before is not a number, ignoring: " + s);
        }
        return value;
    }

    private static VolumeType toVolumeType(String s) {
        if (ATTR_MULTIPLE_YEARS.equals(s)) {
            return VolumeType.SUBJECT_FILE;
        } else if (ATTR_SINGLE_YEAR.equals(s)) {
            return VolumeType.ANNUAL_FILE;
        } else {
            throw new RuntimeException("Invalid volumeType value: '" + s + "'");
        }
    }

    private Date parseDate(String s) {
        Date date = null;
        if (s == null) {
            return null;
        }
        try {
            date = dateFormat.parse(s);
        } catch (ParseException e) {
        }
        return date;
    }

    private NodeRef getSeries(Toimik t) {
        Map<String, Map<String, NodeRef>> bigMap = t.archived ? archivedSeriesByIndex : seriesByIndex;
        Map<String, NodeRef> map = bigMap.get(t.functionId);
        if (map != null) {
            return map.get(getSeriesMapKey(t));
        }
        return null;
    }

    private String getSeriesMapKey(Toimik t) {
        String key = t.seriesIndex;
        if (seriesComparisonIncludesTitle) {
            key += " " + StringUtils.deleteWhitespace(t.seriesTitle).toLowerCase();
        }
        return key;
    }

    private void putSeries(Toimik t, NodeRef series) {
        Map<String, Map<String, NodeRef>> bigMap = t.archived ? archivedSeriesByIndex : seriesByIndex;
        Map<String, NodeRef> map = bigMap.get(t.functionId);
        if (map == null) {
            map = new HashMap<String, NodeRef>();
            bigMap.put(t.functionId, map);
        }
        map.put(getSeriesMapKey(t), series);
    }

    private void importToimik(Toimik t) {
        // Toimik theOtherOne = getToimik(t);
        // if (theOtherOne != null) {
        // theOtherOne.add(t.volumeMark);
        // return;
        // }

        NodeRef series = getSeries(t);

        if (series == null) {
            series = createSeries(t);
            if (series != null) {
                putSeries(t, series);
            }
        }
        // This is the check for missing function
        if (series != null) {
            createVolume(series, t);
        }
    }

    private void createVolume(NodeRef series, Toimik t) {
        log.info("Creating volume for series = " + series + " and toimik = " + t);
        Volume volume = volumeService.createVolume(series);
        volume.setVolumeMark(t.volumeMarkNormed);
        volume.setTitle(t.volumeTitleNormed);
        volume.setVolumeTypeEnum(t.volumeType);
        // A specific logic for SIM
        // if (t.year() != 2010) {
        // volume.setContainsCases(false);
        // }
        volume.setContainsCases(false);

        volume.setValidFrom(t.validFrom);
        volume.setValidTo(t.validTo);
        volume.setStatus(openUnit ? DocListUnitStatus.OPEN.getValueName() : DocListUnitStatus.CLOSED.getValueName());

        if (t.validTo != null) {
            volume.setRetainUntilDate(DateUtils.addYears(t.validTo, t.bestBefore));
        }

        volumeService.saveOrUpdate(volume, false);
        t.nodeRef = volume.getNode().getNodeRef();

        // putToimik(t);
        t.add(t.volumeMark);
    }

    // A specific logic for SIM
    // private int getStructUnit() {
    // return 20201;
    // }

    private boolean sameOrderExists(int order, NodeRef fRef) {
        for (Series s : seriesService.getAllSeriesByFunction(fRef)) {
            if (order == s.getOrder()) {
                return true;
            }
        }
        return false;
    }

    private NodeRef createSeries(Toimik t) {
        log.info("Creating series for toimik = " + t);
        NodeRef fRef = getFunction(t.functionId, t.archived);
        if (fRef == null) {
            log.warn("cannot get function for id: " + t.functionId);
            missingFunctions.add(t.functionId);
            return null;
        }
        final Series series = seriesService.createSeries(fRef);
        Map<String, Object> props = series.getNode().getProperties();
        props.put(SeriesModel.Props.TYPE.toString(), SeriesType.SERIES.getValueName() /* toSeriesType(t) */);
        props.put(SeriesModel.Props.SERIES_IDENTIFIER.toString(), t.seriesIndex);
        props.put(SeriesModel.Props.TITLE.toString(), t.seriesTitle);
        props.put(SeriesModel.Props.STATUS.toString(), openUnit ? DocListUnitStatus.OPEN.getValueName() : DocListUnitStatus.CLOSED.getValueName());
        props.put(SeriesModel.Props.VALID_FROM_DATE.toString(), seriesValidFrom);
        try {
            int order = PostipoissUtil.inferLastNumber(t.seriesIndex);
            // If condition fails, the original order should be ok for insertion
            if (order != -1 && !sameOrderExists(order, fRef)) {
                props.put(SeriesModel.Props.ORDER.toString(), order);
            }
        } catch (Exception e) {
        }
        ArrayList<String> docType = new ArrayList<String>();
        docType.add(SystematicDocumentType.INCOMING_LETTER.getId());
        docType.add(SystematicDocumentType.OUTGOING_LETTER.getId());
        props.put(SeriesModel.Props.DOC_TYPE.toString(), docType); // docType is not important on closed series anyway
        props.put(SeriesModel.Props.REGISTER.toString(), getRegisterId());
        props.put(SeriesModel.Props.DOC_NUMBER_PATTERN.toString(), "{S}/{DN}");
        ArrayList<String> volType = new ArrayList<String>();
        volType.add(VolumeType.ANNUAL_FILE.name());
        volType.add(VolumeType.SUBJECT_FILE.name());
        props.put(SeriesModel.Props.VOL_TYPE.toString(), volType);
        if (t.bestBefore > 0) {
            props.put(SeriesModel.Props.DESCRIPTION.toString(), "Säilitustähtaeg (migreeritud): " + t.bestBefore);
        }
        // Default access restriction value is OPEN
        props.put(SeriesModel.Props.ACCESS_RESTRICTION.toString(), AccessRestriction.OPEN.getValueName());
        if (StringUtils.isNotEmpty(t.seriesAccessRestriction)) {
            props.put(SeriesModel.Props.ACCESS_RESTRICTION.toString(), t.seriesAccessRestriction);
        }
        if (StringUtils.isNotEmpty(t.seriesAccessRestrictionReason)) {
            props.put(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString(), t.seriesAccessRestrictionReason);
        }

        // A specific logic for SIM
        // if (t.year() != 2010) {
        // props.put(SeriesModel.Props.STRUCT_UNIT.toString(), getStructUnit());
        // props.put(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString(), false);
        // }

        // We check order ourselves above
        seriesService.saveOrUpdateWithoutReorder(series);

        return series.getNode().getNodeRef();
    }

    // A specific logic for SIM
    // private String toSeriesType(Toimik t) {
    // if ("11302".equals(t.functionId)) {
    // int year = t.year();
    // if ((year == 2007) || (year == 2008) || (year == 2009)) {
    // return VolumeType.OBJECT.getValueName();
    // }
    // }
    // return VolumeType.YEAR_BASED.getValueName();
    // }

    private void createSeries() throws Exception {
        final File inputFile = new File(dataFolder, VOLUMES_FILENAME);
        log.info("Reading Postipoiss volumes file '" + inputFile + "' with encoding " + INPUT_ENCODING);
        CsvReader reader = new CsvReader(new FileInputStream(inputFile), ';', Charset.forName(INPUT_ENCODING));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                try {
                    toimikud.add(new Toimik(reader));
                } catch (Exception e) {
                    throw new RuntimeException("Error while reading volume from row index " + reader.getCurrentRecord() + " in file " + inputFile + ": " + e.getMessage(), e);
                }
            }

            missingFunctions = new HashSet<String>();
            // toimikud = toimikud.subList(0, 20);
            Collections.sort(toimikud);
            for (Toimik t : toimikud) {
                checkStop();
                importToimik(t);
            }

            if (!missingFunctions.isEmpty()) {
                log.info("Functions which were referenced but not found: " + StringUtils.join(missingFunctions, ","));
            }

        } finally {
            reader.close();
        }
    }

    private void writeToimikud() {
        final File outputFile = new File(workFolder, COMPLETED_VOLUMES_FILENAME);
        CsvWriter writer = null;
        try {
            log.info("Writing results file '" + outputFile + "' with encoding " + OUTPUT_ENCODING);
            writer = getCsvWriter(outputFile);
            writer.writeRecord(new String[] { "year", "normedMark", "mark", "nodeRef" });
            for (Toimik t : toimikud) {
                if (t.nodeRef != null && t.names != null) {
                    for (String name : t.names) {
                        writer.writeRecord(new String[] {
                                String.valueOf(t.year() % 100), t.volumeMarkNormed, name, t.nodeRef.toString() });
                    }
                }
            }
            log.info("Finished writing results file " + outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Compares series indexes, sorting them according to their hierarchical numeric content.
     * <p>
     * For example:
     * </p>
     * 
     * <pre>
     * 12.2-3-9/13, 12.2-3-9/12, 7.2-13, 12.2-3-9/14, 9.1-4-4
     * </pre>
     * <p>
     * should be arranged as
     * </p>
     * 
     * <pre>
     * 7.2-13, 9.1-4-4, 12.2-3-9/12, 12.2-3-9/13, 12.2-3-9/14
     * </pre>
     */
    public static class SeriesIndexComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            int result = 0;
            List<Integer> numbers1 = extractNumbers(o1);
            List<Integer> numbers2 = extractNumbers(o2);

            List<Integer> shorter = numbers1.size() > numbers2.size() ? numbers2 : numbers1;
            for (int i = 0; i < shorter.size(); i++) {
                if (numbers1.get(i) > numbers2.get(i)) {
                    result = 1;
                    break;
                } else if (numbers1.get(i) < numbers2.get(i)) {
                    result = -1;
                    break;
                }
            }

            return result;
        }

        private List<Integer> extractNumbers(String s) {
            List<Integer> result = new ArrayList<Integer>();
            Scanner sc = new Scanner(s);
            sc.useDelimiter("[^\\d]");
            while (sc.hasNext()) {
                if (sc.hasNextInt()) {
                    result.add(sc.nextInt());
                } else {
                    sc.next();
                }
            }
            return result;
        }
    }

    // ------------------------------------------------------------------------------
    // Filename utility methods
    // ------------------------------------------------------------------------------
    // Returns the marker at the start of PP function name, consisting of numbers and dots
    private static String getPostipoissMark(String trimmedTitle) {
        if (!trimmedTitle.isEmpty() && CharUtils.isAsciiNumeric(trimmedTitle.charAt(0))) {
            int i = 1;
            for (; trimmedTitle.charAt(i) == '.' || CharUtils.isAsciiNumeric(trimmedTitle.charAt(i)); i++) {
                ;
            }
            return trimmedTitle.substring(0, i);
        }
        return "";
    }

    // A specific logic for SIM
    // private static String trimPostipoissFunctionName(String name) {
    // String result = name;
    // String validityPrefix = "Kehtib kuni 01.01.2010";
    // String endedPartialPrefix = "PETATUD";
    // if (name.startsWith(validityPrefix)) { // is PP 'function' that was open until 2010
    // result = name.substring(validityPrefix.length());
    // result = result.trim();
    // }
    // if (name.substring(2).startsWith(endedPartialPrefix)) { // if is ended PP 'function'
    // result = name.substring(endedPartialPrefix.length() + 2);
    // result = result.trim();
    // }
    // return result;
    // }

    private static void logAllCreation(String s, NodeRef node) {
        if (node == null) {
            log.warn("DHSPWarn: nodeRef NULL " + s);
            return;
        }
        log.info("DHSPPimport:" + s + " " + node.getId());
    }

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(transactionService);
        return helper;
    }

    protected CsvWriter getCsvWriter(File file) throws IOException {
        OutputStream outputStream = new FileOutputStream(file);

        // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
        outputStream.write("\ufeff".getBytes(OUTPUT_ENCODING));

        CsvWriter writer = new CsvWriter(outputStream, OUTPUT_SEPARATOR, Charset.forName(OUTPUT_ENCODING));
        return writer;
    }

    // INJECTORS
    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setPostipoissImporter(PostipoissImporter postipoissImporter) {
        this.postipoissImporter = postipoissImporter;
    }

}
