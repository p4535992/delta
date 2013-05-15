package ee.webmedia.alfresco.adr.bootstrap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.adr.service.AdrServiceImpl;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidegaV2;
import ee.webmedia.alfresco.adr.ws.FailV2;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Alar Kvell
 */
public class AdrTestDataUpdater extends AbstractNodeUpdater {

    private DocumentAdminService documentAdminService;
    private DocumentSearchService documentSearchService;
    private DocumentDynamicService documentDynamicService;
    private AdrService adrService;
    private String dataFolder;

    private Set<String> publicAdrDocumentTypeIds;
    private Map<NodeRef, Map<QName, Serializable>> functionsCache; // functions.csv
    private Map<NodeRef, Map<QName, Serializable>> seriesCache; // series.csv
    private Map<NodeRef, Map<QName, Serializable>> volumesCache; // volumes.csv
    private Set<String> compilatorFirstNames; // users-firstnames.csv
    private Set<String> compilatorLastNames; // users-lastnames.csv
    private Set<String> compilatorOrgUnits; // orgunits.csv
    private Set<String> parties; // contacts.csv
    private Set<String> titles; // doctitles.csv
    private Set<String> regNumbers; // regnumbers.csv
    private Set<String> senderRegNumbers; // senderregnumbers.csv
    private Set<String> transmittalModes; // transmittalmodes.csv -- currently not used by TestDataService
    private Set<String> accessRestrictionReasons; // accessrestrictionreasons.csv
    private Set<String> accessRestrictionEndDescs; // accessrestrictionenddescs.csv
    private List<String[]> files; // files.csv

    @Override
    protected void executeUpdater() throws Exception {
        log.info("Checking that testdata files do not previously exist in folder: " + dataFolder);
        File functionsFile = checkFile("functions.csv");
        File seriesFile = checkFile("series.csv");
        File volumesFile = checkFile("volumes.csv");
        File usersFirstNamesFile = checkFile("users-firstnames.csv");
        File usersLastNamesFile = checkFile("users-lastnames.csv");
        File orgUnitsFile = checkFile("orgunits.csv");
        File contactsFile = checkFile("contacts.csv");
        File docTitlesFile = checkFile("doctitles.csv");
        File regNumbersFile = checkFile("regnumbers.csv");
        File senderRegNumbersFile = checkFile("senderregnumbers.csv");
        File transmittalModesFiles = checkFile("transmittalmodes.csv");
        File accessRestrictionReasonsFile = checkFile("accessrestrictionreasons.csv");
        File accessRestrictionEndDescsFile = checkFile("accessrestrictionenddescs.csv");
        File filesFile = checkFile("files.csv");

        publicAdrDocumentTypeIds = documentAdminService.getAdrDocumentTypeIds();
        functionsCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        seriesCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        volumesCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        compilatorFirstNames = new HashSet<String>();
        compilatorLastNames = new HashSet<String>();
        compilatorOrgUnits = new HashSet<String>();
        parties = new HashSet<String>();
        titles = new HashSet<String>();
        regNumbers = new HashSet<String>();
        senderRegNumbers = new HashSet<String>();
        transmittalModes = new HashSet<String>();
        accessRestrictionReasons = new HashSet<String>();
        accessRestrictionEndDescs = new HashSet<String>();
        files = new ArrayList<String[]>();

        super.executeUpdater();

        List<String[]> functions = new ArrayList<String[]>(functionsCache.size());
        for (Map<QName, Serializable> props : functionsCache.values()) {
            functions.add(new String[] {
                    (String) props.get(FunctionsModel.Props.MARK),
                    (String) props.get(FunctionsModel.Props.TITLE),
            });
        }
        List<String[]> series = new ArrayList<String[]>(seriesCache.size());
        for (Map<QName, Serializable> props : seriesCache.values()) {
            series.add(new String[] {
                    (String) props.get(SeriesModel.Props.SERIES_IDENTIFIER),
                    (String) props.get(SeriesModel.Props.TITLE),
            });
        }
        List<String[]> volumes = new ArrayList<String[]>(volumesCache.size());
        for (Map<QName, Serializable> props : volumesCache.values()) {
            volumes.add(new String[] {
                    (String) props.get(VolumeModel.Props.MARK),
                    (String) props.get(VolumeModel.Props.TITLE),
            });
        }

        log.info("Writing records to testdata files in folder: " + dataFolder);
        writeRecordsToCsvFile(functionsFile, functions);
        writeRecordsToCsvFile(seriesFile, series);
        writeRecordsToCsvFile(volumesFile, volumes);
        writeRecordsToCsvFile(usersFirstNamesFile, compilatorFirstNames);
        writeRecordsToCsvFile(usersLastNamesFile, compilatorLastNames);
        writeRecordsToCsvFile(orgUnitsFile, compilatorOrgUnits);
        writeRecordsToCsvFile(contactsFile, parties);
        writeRecordsToCsvFile(docTitlesFile, titles);
        writeRecordsToCsvFile(regNumbersFile, regNumbers);
        writeRecordsToCsvFile(senderRegNumbersFile, senderRegNumbers);
        writeRecordsToCsvFile(transmittalModesFiles, transmittalModes);
        writeRecordsToCsvFile(accessRestrictionReasonsFile, accessRestrictionReasons);
        writeRecordsToCsvFile(accessRestrictionEndDescsFile, accessRestrictionEndDescs);
        writeRecordsToCsvFile(filesFile, files);
        log.info("Writing testdata files complete");
    }

    private File checkFile(String filename) {
        File file = new File(dataFolder, filename);
        Assert.isTrue(!file.exists(), "File exists: " + file.getAbsolutePath());
        return file;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = documentSearchService.generateAdrDocumentSearchQuery(new ArrayList<String>(), publicAdrDocumentTypeIds);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        DocumentDynamic documentDynamic = documentDynamicService.getDocument(nodeRef);
        // Let's be extra safe and check all conditions based on repository values, just in case lucene indexes are incorrect
        if (!AdrServiceImpl.isDocumentAllowedToAdr(documentDynamic, publicAdrDocumentTypeIds, false)) {
            return new String[] {
                    "didNotGoToAdr",
                    documentDynamic.getDocumentTypeId(),
                    documentDynamic.getDocStatus(),
                    documentDynamic.getRegDateTime() == null ? "" : dateFormat.format(documentDynamic.getRegDateTime()),
                    documentDynamic.getRegNumber(),
                    documentDynamic.getDocName(),
                    "" };
        }
        DokumentDetailidegaV2 doc = adrService.buildDokumentDetailidegaV2(documentDynamic, false, publicAdrDocumentTypeIds, functionsCache, seriesCache, volumesCache, true);
        String documentTypeId = doc.getDokumendiLiik().getId();
        boolean isLetter = "incomingLetter".equals(documentTypeId) || "incomingLetterMv".equals(documentTypeId) || "outgoingLetter".equals(documentTypeId)
                || "outgoingLetterMv".equals(documentTypeId);
        boolean isContract = "contractSim".equals(documentTypeId) || "contractSmit".equals(documentTypeId) || "contractMv".equals(documentTypeId) // Delta 2 documentTypeIds
                || "contract".equals(documentTypeId); // Delta 3 documentTypeId

        add(titles, doc.getPealkiri());
        add(regNumbers, doc.getViit());
        add(senderRegNumbers, isLetter ? doc.getSaatjaViit() : null);
        add(transmittalModes, doc.getSaatmisviis());
        add(accessRestrictionReasons, doc.getJuurdepaasuPiiranguAlus());
        add(accessRestrictionEndDescs, doc.getJuurdepaasuPiiranguLopp());

        // add(party, doc.getOsapool());
        String partyValue = null;
        if (isLetter || isContract) {
            if (StringUtils.isNotEmpty(doc.getSaatja()) && StringUtils.isNotEmpty(doc.getOsapool())) {
                partyValue = doc.getSaatja() + ", " + doc.getOsapool();
            } else if (StringUtils.isNotEmpty(doc.getSaatja())) {
                partyValue = doc.getSaatja();
            } else if (StringUtils.isNotEmpty(doc.getOsapool())) {
                partyValue = doc.getOsapool();
            }
        }
        add(parties, partyValue);

        String compilator = StringUtils.trimToNull(isLetter ? doc.getKoostaja() : null);
        if (compilator != null) {
            int i = compilator.indexOf('(');
            if (i >= 0 && compilator.endsWith(")")) {
                add(compilatorOrgUnits, compilator.substring(i + 1, compilator.length() - 1));
            }
            if (i >= 0) {
                StringUtils.trimToEmpty(compilator = compilator.substring(0, i));
            }
            i = compilator.lastIndexOf(' ');
            if (i >= 0) {
                add(compilatorFirstNames, compilator.substring(0, i));
                add(compilatorLastNames, compilator.substring(i + 1));
            }
        }

        int fileCount = 0;
        for (FailV2 failV2 : doc.getFail()) {
            if (StringUtils.isEmpty(failV2.getId())) {
                continue;
            }
            files.add(new String[] { failV2.getId(), failV2.getMimeType(), failV2.getEncoding(), Integer.toString(failV2.getSuurus()), failV2.getFailinimi(), failV2.getPealkiri() });
            fileCount++;
        }
        return new String[] {
                "wentToAdr",
                documentDynamic.getDocumentTypeId(),
                documentDynamic.getDocStatus(),
                documentDynamic.getRegDateTime() == null ? "" : dateFormat.format(documentDynamic.getRegDateTime()),
                documentDynamic.getRegNumber(),
                documentDynamic.getDocName(),
                Integer.toString(fileCount) };
    }

    private static void add(Set<String> set, String value) {
        value = StringUtils.trimToNull(value);
        if (value != null) {
            set.add(value);
        }
    }

    @Override
    public boolean isTransactionReadOnly() {
        return true;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setAdrService(AdrService adrService) {
        this.adrService = adrService;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

}
