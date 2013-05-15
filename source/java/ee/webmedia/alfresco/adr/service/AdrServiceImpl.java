package ee.webmedia.alfresco.adr.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON;
import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithParentheses;
import static ee.webmedia.alfresco.utils.XmlUtil.getDate;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.sql.DataSource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.adr.ws.Dokumendiliik;
import ee.webmedia.alfresco.adr.ws.DokumendiliikV2;
import ee.webmedia.alfresco.adr.ws.Dokument;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidega;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidegaV2;
import ee.webmedia.alfresco.adr.ws.DokumentId;
import ee.webmedia.alfresco.adr.ws.Fail;
import ee.webmedia.alfresco.adr.ws.FailV2;
import ee.webmedia.alfresco.adr.ws.Funktsioon;
import ee.webmedia.alfresco.adr.ws.Sari;
import ee.webmedia.alfresco.adr.ws.SeotudDokument;
import ee.webmedia.alfresco.adr.ws.Toimik;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.ContentReaderDataSource;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Dmitri Melnikov
 * @author Alar Kvell
 */
public class AdrServiceImpl extends BaseAdrServiceImpl {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AdrServiceImpl.class);

    public static FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");

    private DocumentSearchService documentSearchService;
    private ClassificatorService classificatorService;
    private DocumentAdminService documentAdminService;
    private DocumentService documentService;
    private DocumentDynamicService documentDynamicService;
    private NamespaceService namespaceService;
    private SimpleJdbcTemplate jdbcTemplate;
    private boolean accessRestrictionChangeReasonEnabled;
    private boolean volumeTitleEnabled;

    // ========================================================================
    // =========================== REAL-TIME QUERYING =========================
    // ========================================================================

    @Override
    @Deprecated
    public List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik,
            String otsingusona) {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    @Override
    @Deprecated
    public DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg) {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    @Override
    @Deprecated
    public Fail failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename) {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    @Override
    public FailV2 failSisugaV2(NodeRef documentRef, String filename) {
        long startTime = System.currentTimeMillis();

        if (StringUtils.isBlank(filename)) {
            return null;
        }

        if (!nodeService.exists(documentRef)) {
            return null;
        }
        DocumentDynamic doc = documentDynamicService.getDocument(documentRef);

        // Let's be extra safe and check all conditions based on repository values, just in case lucene indexes are incorrect
        if (!isDocumentAllowedToAdr(doc, documentAdminService.getAdrDocumentTypeIds(), true)) {
            return null;
        }

        try {
            File file = searchFile(doc, filename);

            FailV2 failSisuga;
            if (file == null) {
                failSisuga = null;
            } else {
                failSisuga = new FailV2();
                setFailProperties(failSisuga, file, true);
            }

            if (log.isDebugEnabled()) {
                log.debug("ADR failSisugaV2 finished, time " + (System.currentTimeMillis() - startTime) + " ms, arguments:\n    documentRef=" + documentRef
                        + "\n    filename=" + filename
                        + "\n    failSisuga.suurus=" + (failSisuga == null ? -1 : failSisuga.getSuurus()));
            }

            return failSisuga;
        } finally {
            cleanTempFiles();
        }
    }

    @Override
    @Deprecated
    public List<Dokumendiliik> dokumendiliigid() {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    private File searchFile(DocumentDynamic doc, String filename) {
        log.debug("Searching for " + filename + " under " + doc.getDocName() + "(" + doc.getNodeRef() + ")");

        if (!isFileAllowedToAdr(doc)) {
            return null;
        }

        String publishToAdr = doc.getProp(DocumentDynamicModel.Props.PUBLISH_TO_ADR);
        if (publishToAdr != null && !PublishToAdr.TO_ADR.getValueName().equals(publishToAdr)) {
            log.debug("Publish to ADR is set with value: " + publishToAdr);
            return null;
        }

        List<File> files = fileService.getAllActiveFiles(doc.getNodeRef());
        log.debug("Found " + files.size() + " active file(s)");
        for (Iterator<File> i = files.iterator(); i.hasNext();) {
            File file = i.next();
            // 5.1.2.5. faili pealkiri = failRequest.failinimi
            String name = file.getName();
            if (!name.equals(filename)) { // this should be the real file name, because ADR interface requires it to be unique under document
                log.debug(name + " doesn't match. Removing file.");
                i.remove();
            }
        }
        if (files.size() == 0) {
            log.debug("All files were removed, returning null.");
            return null;
        }
        if (files.size() > 1) {
            StringBuilder s = new StringBuilder("Multiple files have same name under one document, returning only the first, total=");
            s.append(files.size());
            s.append(", filename='").append(filename);
            s.append("'");
            for (File file : files) {
                s.append("\n    nodeRef=" + file.getNodeRef());
            }
            log.debug(s.toString());
        }

        File file = files.get(0);
        log.debug("Returning file: " + file.getNodeRef());
        return file;
    }

    private static String getDocNameAdr(DocumentDynamic doc) {
        String docName = (String) doc.getProp(DocumentDynamicModel.Props.DOC_NAME_ADR);
        if (StringUtils.isBlank(docName)) {
            docName = doc.getDocName();
        }
        return docName;
    }

    @Override
    public DokumentDetailidegaV2 buildDokumentDetailidegaV2(DocumentDynamic doc, boolean includeFileContent, Set<String> documentTypeIds,
            Map<NodeRef, Map<QName, Serializable>> functionsCache, Map<NodeRef, Map<QName, Serializable>> seriesCache,
            Map<NodeRef, Map<QName, Serializable>> volumesCache, boolean testData) {

        DokumentDetailidegaV2 dokument = new DokumentDetailidegaV2();
        String documentTypeId = doc.getDocumentTypeId();
        boolean isIncomingLetter = SystematicDocumentType.INCOMING_LETTER.isSameType(documentTypeId);

        // =======================================================
        // Copied from setDokumentProperties

        dokument.setViit(getNullIfEmpty(doc.getRegNumber()));
        dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
        if (isIncomingLetter) {
            dokument.setSaatja(getNullIfEmpty(getInitialsIfNeeded((String) doc.getProp(DocumentSpecificModel.Props.SENDER_DETAILS_NAME), doc.getNodeRef())));
            dokument.setSaaja(getNullIfEmpty(getClassifiedOrgStructValueIfNeeded(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        dokument.setPealkiri(getNullIfEmpty(getDocNameAdr(doc)));

        // =======================================================
        // Copied from setDokumentDetailidegaProperties

        dokument.setJuurdepaasuPiirang(getNullIfEmpty((String) doc.getProp(ACCESS_RESTRICTION)));
        dokument.setJuurdepaasuPiiranguAlus(getNullIfEmpty((String) doc.getProp(ACCESS_RESTRICTION_REASON)));
        dokument.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(convertToXMLGergorianCalendar((Date) doc.getProp(ACCESS_RESTRICTION_BEGIN_DATE)));
        dokument.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(convertToXMLGergorianCalendar((Date) doc.getProp(ACCESS_RESTRICTION_END_DATE)));
        dokument.setJuurdepaasuPiiranguLopp(getNullIfEmpty((String) doc.getProp(ACCESS_RESTRICTION_END_DESC)));
        if (isIncomingLetter) {
            dokument.setVastamiseKuupaev(convertToXMLGergorianCalendar((Date) doc.getProp(DocumentSpecificModel.Props.COMPLIENCE_DATE)));
        } else if (SystematicDocumentType.OUTGOING_LETTER.isSameType(documentTypeId)) {
            dokument.setVastamiseKuupaev(convertToXMLGergorianCalendar((Date) doc.getProp(DocumentSpecificModel.Props.REPLY_DATE)));
        }
        dokument.setTahtaeg(convertToXMLGergorianCalendar((Date) doc.getProp(DocumentSpecificModel.Props.DUE_DATE)));
        if (!isIncomingLetter) {
            dokument.setKoostaja(getNullIfEmpty(getClassifiedOrgStructValueIfNeeded(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        @SuppressWarnings("unchecked")
        List<String> signerStructUnit = (List<String>) doc.getProp(DocumentCommonModel.Props.SIGNER_ORG_STRUCT_UNIT);
        dokument.setAllkirjastaja(getNullIfEmpty(getClassifiedOrgStructValueIfNeeded((String) doc.getProp(DocumentCommonModel.Props.SIGNER_NAME)
                , UserUtil.getDisplayUnit(signerStructUnit))));

        // =======================================================

        // Associated documents
        if (!testData) {
            List<SeotudDokument> assocDocs = getSeotudDokumentList(doc.getNodeRef(), documentTypeIds);
            dokument.getSeotudDokument().addAll(assocDocs);
        }

        if (isFileAllowedToAdr(doc)) {
            List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNodeRef());
            for (File file : allActiveFiles) {
                FailV2 fail = new FailV2();
                setFailProperties(fail, file, includeFileContent);
                if (testData) {
                    ContentData contentData = (ContentData) nodeService.getProperty(file.getNodeRef(), ContentModel.PROP_CONTENT);
                    fail.setId(contentData != null ? contentData.getContentUrl() : null);
                }
                dokument.getFail().add(fail);
            }
        }

        // =======================================================
        // New V2 specific fields

        dokument.setId(getNullIfEmpty(doc.getNodeRef().toString()));

        dokument.setLisad(getNullIfEmpty((String) doc.getProp(DocumentSpecificModel.Props.ANNEX)));

        dokument.setSaatjaViit(getNullIfEmpty((String) doc.getProp(DocumentSpecificModel.Props.SENDER_REG_NUMBER)));

        String transmittalMode;
        if (isIncomingLetter) {
            transmittalMode = (String) doc.getProp(DocumentSpecificModel.Props.TRANSMITTAL_MODE);
        } else {
            @SuppressWarnings("unchecked")
            List<String> sendModes = (List<String>) doc.getProp(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
            transmittalMode = TextUtil.joinUniqueStringsWithComma(sendModes);
        }
        dokument.setSaatmisviis(getNullIfEmpty(transmittalMode));

        // Tähtaja kirjeldus
        dokument.setTahtaegKirjeldus(getNullIfEmpty((String) doc.getProp(DocumentSpecificModel.Props.DUE_DATE_DESC)));

        // Osapooled
        String osapool = "";
        if (SystematicDocumentType.OUTGOING_LETTER.isSameType(documentTypeId)) {
            osapool = doc.getRecipients();
        } else {
            List<Node> parties = doc.getNode().getAllChildAssociations(DocumentChildModel.Assocs.CONTRACT_PARTY);
            if (parties != null) {
                List<String> names = new ArrayList<String>(parties.size());
                for (Node node : parties) {
                    names.add((String) node.getProperties().get(DocumentSpecificModel.Props.PARTY_NAME));
                }
                osapool = TextUtil.joinNonBlankStringsWithComma(names);
            }
        }

        dokument.setOsapool(getNullIfEmpty(osapool));

        // =======================================================

        if (AccessRestriction.OPEN.getValueName().equals(doc.getProp(ACCESS_RESTRICTION))
                && StringUtils.equals((String) doc.getProp(DocumentDynamicModel.Props.PUBLISH_TO_ADR), PublishToAdr.REQUEST_FOR_INFORMATION.getValueName())) {
            dokument.setAinultTeabenoudeKorras(Boolean.TRUE);
        } else {
            dokument.setAinultTeabenoudeKorras(Boolean.FALSE);
        }

        if (accessRestrictionChangeReasonEnabled && AccessRestriction.OPEN.getValueName().equals(dokument.getJuurdepaasuPiirang())) {
            dokument.setJuurdepaasuPiiranguMuutmisePohjus(getNullIfEmpty((String) doc.getProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON)));
        }

        // Document type
        DokumendiliikV2 wsDocumentType = new DokumendiliikV2();
        wsDocumentType.setId(documentTypeId);
        if (!testData) {
            wsDocumentType.setNimi(getNullIfEmpty(documentAdminService.getDocumentTypeName(documentTypeId)));
        }
        dokument.setDokumendiLiik(wsDocumentType);

        // Volume
        Map<QName, NodeRef> docParents = null;
        NodeRef volumeRef = (NodeRef) doc.getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
        Map<QName, Serializable> volumeProps = volumesCache.get(volumeRef);
        if (volumeProps == null) {
            if (volumeRef == null || !nodeService.exists(volumeRef)) {
                log.warn("Document property volume=null\n  nodeRef=" + doc.getNodeRef().toString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getNode().getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getNode().getAspects(), namespaceService));
                docParents = documentService.getDocumentParents(doc.getNodeRef());
                volumeRef = docParents.get(DocumentCommonModel.Props.VOLUME);
            }
            volumeProps = nodeService.getProperties(volumeRef);
            volumesCache.put(volumeRef, volumeProps);
        }
        Toimik wsVolume = new Toimik();
        wsVolume.setId(volumeRef.toString());
        wsVolume.setViit((String) volumeProps.get(VolumeModel.Props.MARK));
        wsVolume.setPealkiri(volumeTitleEnabled ? (String) volumeProps.get(VolumeModel.Props.TITLE) : "");
        wsVolume.setKehtivAlatesKuupaev(convertToXMLGergorianCalendar((Date) volumeProps.get(VolumeModel.Props.VALID_FROM)));
        wsVolume.setKehtivKuniKuupaev(convertToXMLGergorianCalendar((Date) volumeProps.get(VolumeModel.Props.VALID_TO)));
        dokument.setToimik(wsVolume);

        // Series
        NodeRef seriesRef = (NodeRef) doc.getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
        Map<QName, Serializable> seriesProps = seriesCache.get(seriesRef);
        if (seriesProps == null) {
            if (seriesRef == null || !nodeService.exists(seriesRef)) {
                log.warn("Document property series=null\n  nodeRef=" + doc.getNodeRef().toString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getNode().getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getNode().getAspects(), namespaceService));
                if (docParents == null) {
                    docParents = documentService.getDocumentParents(doc.getNodeRef());
                }
                seriesRef = docParents.get(DocumentCommonModel.Props.SERIES);
            }
            seriesProps = nodeService.getProperties(seriesRef);
            seriesCache.put(seriesRef, seriesProps);
        }
        Sari wsSeries = new Sari();
        wsSeries.setId(seriesRef.toString());
        wsSeries.setViit((String) seriesProps.get(SeriesModel.Props.SERIES_IDENTIFIER));
        wsSeries.setPealkiri((String) seriesProps.get(SeriesModel.Props.TITLE));
        wsSeries.setJarjekorraNumber((Integer) seriesProps.get(SeriesModel.Props.ORDER));
        dokument.setSari(wsSeries);

        // Function
        NodeRef functionRef = (NodeRef) doc.getNode().getProperties().get(DocumentCommonModel.Props.FUNCTION);
        Map<QName, Serializable> functionProps = functionsCache.get(functionRef);
        if (functionProps == null) {
            if (functionRef == null || !nodeService.exists(functionRef)) {
                log.warn("Document property function=null\n  nodeRef=" + doc.getNodeRef().toString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getNode().getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getNode().getAspects(), namespaceService));
                if (docParents == null) {
                    docParents = documentService.getDocumentParents(doc.getNodeRef());
                }
                functionRef = docParents.get(DocumentCommonModel.Props.FUNCTION);
            }
            functionProps = nodeService.getProperties(functionRef);
            functionsCache.put(functionRef, functionProps);
        }
        Funktsioon wsFunction = new Funktsioon();
        wsFunction.setId(functionRef.toString());
        wsFunction.setViit((String) functionProps.get(FunctionsModel.Props.MARK));
        wsFunction.setPealkiri((String) functionProps.get(FunctionsModel.Props.TITLE));
        wsFunction.setJarjekorraNumber((Integer) functionProps.get(FunctionsModel.Props.ORDER));
        dokument.setFunktsioon(wsFunction);

        return dokument;
    }

    /**
     * NB! Caller of this function must ensure that temporary files are deleted using the clearTempFiles method.
     */
    private void setFailProperties(FailV2 fail, File file, boolean includeContent) {
        String mimetype = getNullIfEmpty(file.getMimeType());
        String encoding = getNullIfEmpty(file.getEncoding());
        String title = getNullIfEmpty(file.getDisplayName());
        String name = getNullIfEmpty(file.getName());
        int size = (int) file.getSize();

        if (includeContent) {
            Pair<Boolean, DataHandler> fileDataHandler = getFileDataHandler(file.getNodeRef(), file.getName());
            fail.setSisu(fileDataHandler.getSecond());

            if (fileDataHandler.getFirst()) {
                ContentReaderDataSource contentReaderDataSource = (ContentReaderDataSource) fail.getSisu().getDataSource();
                mimetype = MimetypeMap.MIMETYPE_PDF;
                name = FilenameUtils.removeExtension(name) + ".pdf"; // replace the extension
                title = FilenameUtils.removeExtension(title) + ".pdf"; // replace the extension
                size = (int) contentReaderDataSource.getContentSize();
                encoding = contentReaderDataSource.getEncoding();
            }
        }

        fail.setFailinimi(name); // this should be the real file name, because ADR interface requires it to be unique under document
        fail.setPealkiri(title);
        fail.setMuutmiseAeg(convertToXMLGergorianCalendar(file.getModified()));
        fail.setSuurus(size);
        fail.setEncoding(encoding);
        fail.setMimeType(mimetype);
        fail.setId(file.getNodeRef().toString());
    }

    private List<SeotudDokument> getSeotudDokumentList(NodeRef document, Set<String> documentTypeIds) {
        List<SeotudDokument> list = new ArrayList<SeotudDokument>();
        for (AssociationRef targetAssocRef : nodeService.getTargetAssocs(document, RegexQNamePattern.MATCH_ALL)) {
            SeotudDokument seotudDokument = getSeotudDokument(targetAssocRef, false, documentTypeIds);
            if (seotudDokument != null) {
                list.add(seotudDokument);
            }
        }
        for (AssociationRef sourceAssocRef : nodeService.getSourceAssocs(document, RegexQNamePattern.MATCH_ALL)) {
            SeotudDokument seotudDokument = getSeotudDokument(sourceAssocRef, true, documentTypeIds);
            if (seotudDokument != null) {
                list.add(seotudDokument);
            }
        }
        return list;
    }

    public SeotudDokument getSeotudDokument(AssociationRef assocRef, boolean isSourceAssoc, Set<String> documentTypeIds) {
        NodeRef otherDocRef = isSourceAssoc ? assocRef.getSourceRef() : assocRef.getTargetRef();
        if (!DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(otherDocRef))) {
            return null;
        }
        DocumentDynamic otherDoc = documentDynamicService.getDocument(otherDocRef);
        if (!isDocumentAllowedToAdr(otherDoc, documentTypeIds, false)) {
            return null;
        }

        SeotudDokument seotudDokument = new SeotudDokument();
        if (DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocRef.getTypeQName())) {
            seotudDokument.setSeosLiik(AssocType.DEFAULT.getValueName());
        } else if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocRef.getTypeQName())) {
            if (isSourceAssoc) {
                seotudDokument.setSeosLiik(AssocType.FOLLOWUP.getValueName());
            } else {
                seotudDokument.setSeosLiik(AssocType.INITIAL.getValueName() + " (" + AssocType.FOLLOWUP.getValueName() + ")");
            }
        } else if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocRef.getTypeQName())) {
            if (isSourceAssoc) {
                seotudDokument.setSeosLiik(AssocType.REPLY.getValueName());
            } else {
                seotudDokument.setSeosLiik(AssocType.INITIAL.getValueName() + " (" + AssocType.REPLY.getValueName() + ")");
            }
        } else {
            log.warn("Unknown association type value, assocRef=" + assocRef);
            return null;
        }

        seotudDokument.setId(otherDocRef.toString());
        return seotudDokument;
    }

    // ========================================================================
    // ======================= PERIODIC SYNCHRONIZATION =======================
    // ========================================================================

    @Override
    @Deprecated
    public List<DokumentDetailidega> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    @Override
    public List<DokumentDetailidegaV2> koikDokumendidLisatudMuudetudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, int jataAlgusestVahele,
            int tulemustePiirang) {
        final Map<NodeRef, Map<QName, Serializable>> functionsCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> seriesCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> volumesCache = new HashMap<NodeRef, Map<QName, Serializable>>();

        try {
            return koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDocumentCallback<DokumentDetailidegaV2>() {
                @Override
                public DokumentDetailidegaV2 buildDocument(DocumentDynamic doc, Set<String> documentTypeIds) {
                    return buildDokumentDetailidegaV2(doc, false, documentTypeIds, functionsCache, seriesCache, volumesCache, false);
                }
            }, jataAlgusestVahele, tulemustePiirang, true);
        } finally {
            cleanTempFiles();
        }
    }

    private static interface BuildDocumentCallback<T> {
        T buildDocument(DocumentDynamic doc, Set<String> documentTypeIds);
    }

    private <T> List<T> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev,
            BuildDocumentCallback<T> buildDocumentCallback, int skip, int limit, boolean compareByNodeRef) {
        long startTime = System.currentTimeMillis();

        Date modifiedDateBegin = getDate(perioodiAlgusKuupaev);
        Date modifiedDateEnd = getDate(perioodiLoppKuupaev);
        log.info("Starting koikDokumendidLisatudMuudetud" + (compareByNodeRef ? "V2" : "") + ", arguments:"
                + "\n  modifiedDateBegin = " + dateFormat.format(modifiedDateBegin)
                + "\n  modifiedDateEnd = " + dateFormat.format(modifiedDateEnd)
                + "\n  skip = " + skip
                + "\n  limit = " + limit);

        List<T> list;

        if (modifiedDateBegin == null || modifiedDateEnd == null) {
            list = Collections.emptyList();
        } else {

            // ============= Get all documents from database sorted by modified

            List<NodeRef> documentsByModified = getDocumentsSortedByModified();

            // ============= Search for documents that were modified during specified period

            Set<String> publicAdrDocumentTypeIds = Collections.unmodifiableSet(documentAdminService.getAdrDocumentTypeIds());

            log.info("Executing lucene query to find all public ADR documents, modified between " + dateFormat.format(modifiedDateBegin) + " and "
                    + dateFormat.format(modifiedDateEnd) + " (inclusive)");
            List<NodeRef> docs1 = documentSearchService.searchAdrDocuments(modifiedDateBegin, modifiedDateEnd, publicAdrDocumentTypeIds);
            log.info("Found " + docs1.size() + " documents that were modified during specified period");
            Set<NodeRef> docs = new HashSet<NodeRef>(docs1);

            // ============= Search for document types that were changed to publicAdr=true during specified period
            // ============= and add ALL documents that belong to these types to results

            List<String> addedDocumentTypes = documentSearchService.searchAdrAddedDocumentTypes(modifiedDateBegin, modifiedDateEnd);
            Set<String> documentTypes = new HashSet<String>(publicAdrDocumentTypeIds); // Currently allowed docTypes
            documentTypes.retainAll(addedDocumentTypes); // Result: docTypes that were added during this period AND are currently allowed
            if (documentTypes.size() > 0) {
                log.info("Executing lucene query to find all documents of the following document types whose publicAdr was changed during specified period: "
                        + WmNode.toString(documentTypes));
                List<NodeRef> docs2 = documentSearchService.searchAdrDocuments((Date) null, (Date) null, documentTypes);
                log.info("Found " + docs2.size() + " documents that belong to " + documentTypes.size()
                        + " document types whose publicAdr was changed during specified period");
                docs.addAll(docs2);
            }

            log.info("Total found " + docs.size() + " documents");

            log.info("Starting document construction");
            int skipped = 0;
            Map<AdrDocument, T> results = new HashMap<AdrDocument, T>();
            for (NodeRef nodeRef : documentsByModified) {
                if (!docs.contains(nodeRef)) {
                    continue;
                }
                if (compareByNodeRef) { // skip
                    if (skip > 0 && skipped < skip) {
                        skipped++;
                        continue;
                    }
                }
                DocumentDynamic doc = documentDynamicService.getDocument(nodeRef);
                // Let's be extra safe and check all conditions based on repository values, just in case lucene indexes are incorrect
                if (!isDocumentAllowedToAdr(doc, publicAdrDocumentTypeIds, true)) {
                    continue;
                }
                AdrDocument adrDocument = new AdrDocument(doc.getNodeRef(), doc.getRegNumber(), doc.getRegDateTime(), compareByNodeRef);
                if (compareByNodeRef || !results.containsKey(adrDocument)) {
                    log.debug("Constructing document " + (results.size() + 1));
                    results.put(adrDocument, buildDocumentCallback.buildDocument(doc, publicAdrDocumentTypeIds));
                    if (limit > 0 && results.size() >= limit) {
                        log.info("Limit reached, breaking");
                        break;
                    }
                }
            }

            docs.removeAll(documentsByModified);
            log.info("There are " + docs.size() + " documents in the lucene response that are not in the sql response");

            list = new ArrayList<T>(results.values());
        }

        log.info("Finished koikDokumendidLisatudMuudetud" + (compareByNodeRef ? "V2" : "")
                + ", time " + (System.currentTimeMillis() - startTime) + " ms"
                + ", results " + list.size());
        return list;
    }

    public static boolean isDocumentAllowedToAdr(DocumentDynamic doc, Set<String> publicAdrDocumentTypeIds, boolean logInfo) {
        if (!publicAdrDocumentTypeIds.contains(doc.getDocumentTypeId())
                || StringUtils.isBlank(doc.getRegNumber())
                || doc.getRegDateTime() == null
                || (!doc.isDocStatus(DocumentStatus.FINISHED) && !SystematicDocumentType.INCOMING_LETTER.isSameType(doc.getDocumentTypeId()))
                || (!AccessRestriction.AK.getValueName().equals(doc.getAccessRestriction()) && !AccessRestriction.OPEN.getValueName().equals(doc.getAccessRestriction()))
                || (doc.getPublishToAdr() != null && !PublishToAdr.TO_ADR.equals(doc.getPublishToAdr()) && !PublishToAdr.REQUEST_FOR_INFORMATION.equals(doc.getPublishToAdr()))
                || !DocumentCommonModel.Types.DOCUMENT.equals(doc.getNode().getType())
                || !doc.getNode().hasAspect(DocumentCommonModel.Aspects.SEARCHABLE)) {
            if (logInfo) {
                log.info("Document does not meet ADR criteria, ignoring: " + doc.toString());
            }
            return false;
        }
        return true;
    }

    private static boolean isFileAllowedToAdr(DocumentDynamic doc) {
        // Only include file list when document accessRestriction = Avalik AND publishToAdr = Läheb ADR-i
        if (!AccessRestriction.OPEN.getValueName().equals(doc.getProp(ACCESS_RESTRICTION))) {
            return false;
        }
        String publishToAdr = doc.getProp(DocumentDynamicModel.Props.PUBLISH_TO_ADR);
        if (publishToAdr != null && !PublishToAdr.TO_ADR.getValueName().equals(publishToAdr)) {
            return false;
        }
        return true;
    }

    private List<NodeRef> getDocumentsSortedByModified() {
        StoreRef spacesStoreRef = generalService.getStore();
        StoreRef archivalsStoreRef = generalService.getArchivalsStoreRef();
        Long spacesStoreId = null;
        Long archivalsStoreId = null;
        List<Map<String, Object>> storeRowList = jdbcTemplate.queryForList("SELECT id, protocol, identifier FROM alf_store");
        for (Map<String, Object> storeRow : storeRowList) {
            if (matchesStore(spacesStoreRef, storeRow)) {
                spacesStoreId = (Long) storeRow.get("id");
            } else if (matchesStore(archivalsStoreRef, storeRow)) {
                archivalsStoreId = (Long) storeRow.get("id");
            }
        }
        Assert.notNull(spacesStoreId, "Store " + spacesStoreRef + " not found in alf_store");
        Assert.notNull(archivalsStoreId, "Store " + archivalsStoreRef + " not found in alf_store");
        Map<Long, StoreRef> storesById = new HashMap<Long, StoreRef>();
        storesById.put(spacesStoreId, spacesStoreRef);
        storesById.put(archivalsStoreId, archivalsStoreRef);

        // TODO document types
        log.info("Executing SQL query to find all public ADR documents, sorted by modified time");
        String query = "SELECT store_id, uuid FROM alf_node WHERE node_deleted = false AND store_id IN(?, ?) AND audit_modified IS NOT NULL ORDER BY audit_modified ASC, store_id ASC, uuid ASC";
        long queryStart = System.nanoTime();
        List<NodeRef> documentsByModified = jdbcTemplate.query(query, new NodeRefRowMapper(storesById), spacesStoreId, archivalsStoreId);
        long queryEnd = System.nanoTime();
        log.info("Executed SQL query, time " + ((queryEnd - queryStart) / 1000000L) + " ms, result " + documentsByModified.size() + " rows");
        return documentsByModified;
    }

    protected boolean matchesStore(StoreRef storeRef, Map<String, Object> storeRow) {
        return storeRow.get("protocol").equals(storeRef.getProtocol()) && storeRow.get("identifier").equals(storeRef.getIdentifier());
    }

    public static class NodeRefRowMapper implements ParameterizedRowMapper<NodeRef> {

        private final Map<Long, StoreRef> storesById;

        public NodeRefRowMapper(Map<Long, StoreRef> storesById) {
            this.storesById = storesById;
        }

        @Override
        public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long storeId = rs.getLong("store_id");
            String uuid = rs.getString("uuid");
            return new NodeRef(storesById.get(storeId), uuid);
        }

    }

    @Override
    @Deprecated
    public List<Dokument> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        throw new RuntimeException("Old queries are not supported! Please use newer V2 queries!");
    }

    @Override
    public List<DokumentId> koikDokumendidKustutatudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, int skip,
            int limit) {

        long startTime = System.currentTimeMillis();

        Date deletedDateBegin = getDate(perioodiAlgusKuupaev);
        Date deletedDateEnd = getDate(perioodiLoppKuupaev);
        log.info("Starting koikDokumendidKustutatudV2, arguments:"
                + "\n  deletedDateBegin = " + dateFormat.format(deletedDateBegin)
                + "\n  deletedDateEnd = " + dateFormat.format(deletedDateEnd)
                + "\n  skip = " + skip
                + "\n  limit = " + limit);

        List<DokumentId> list;

        if (deletedDateBegin == null || deletedDateEnd == null) {
            list = Collections.emptyList();
        } else {

            // ============= Search for documents that were deleted during specified period

            log.info("Executing lucene query to find documents that were deleted between " + dateFormat.format(deletedDateBegin) + " and " + dateFormat.format(deletedDateEnd)
                    + " (inclusive)");
            List<NodeRef> deletedDocRefs = documentSearchService.searchAdrDeletedDocuments(deletedDateBegin, deletedDateEnd);
            log.info("Found " + deletedDocRefs.size() + " documents that were deleted during specified period; loading properties ...");
            List<AdrDocument> deletedDocs = new ArrayList<AdrDocument>(deletedDocRefs.size());
            for (NodeRef deletedDoc : deletedDocRefs) {
                Map<QName, Serializable> props = nodeService.getProperties(deletedDoc);
                String nodeRef = (String) props.get(AdrModel.Props.NODEREF);
                if (nodeRef == null) {
                    // Older data doesn't have nodeRef property
                    continue;
                }
                Date deletedDateTime = (Date) props.get(AdrModel.Props.DELETED_DATE_TIME);
                // Use regDateTime field to store deletedDateTime
                deletedDocs.add(new AdrDocument(new NodeRef(nodeRef), "", deletedDateTime, true));
            }
            log.info("List contains " + deletedDocs.size() + " documents that were deleted during specified period");

            // ============= Search for documents that exist (were modified) during specified period

            log.info("Executing lucene query to find documents that were modified during specified period");
            List<NodeRef> existingDocRefs = documentSearchService.searchAdrDocuments(deletedDateBegin, deletedDateEnd, documentAdminService.getAdrDocumentTypeIds());
            log.info("Found " + existingDocRefs.size() + " documents that were modified during specified period");
            for (Iterator<AdrDocument> i = deletedDocs.iterator(); i.hasNext();) {
                AdrDocument deletedDoc = i.next();
                if (existingDocRefs.contains(deletedDoc.nodeRef)) {
                    i.remove();
                }
            }
            log.info("Removing existing docs, list now contains " + deletedDocs.size() + " documents that were deleted during specified period");

            Collections.sort(deletedDocs, ADR_DOCUMENT_BY_REG_DATE_TIME_COMPARATOR);
            deletedDocRefs = new ArrayList<NodeRef>(deletedDocs.size());
            for (AdrDocument deletedDoc : deletedDocs) {
                deletedDocRefs.add(deletedDoc.nodeRef);
            }

            // ============= Search for document types that were changed to publicAdr=false during specified period
            // ============= and add ALL documents that belong to these types to results

            Set<String> deletedDocumentTypes = new HashSet<String>(documentSearchService.searchAdrDeletedDocumentTypes(deletedDateBegin, deletedDateEnd));
            // Result: docTypes that were deleted during this period AND are not currently allowed
            deletedDocumentTypes.removeAll(documentAdminService.getAdrDocumentTypeIds());
            log.info("Found " + deletedDocumentTypes.size() + " document types, which were changed to publicAdr=false during specified period: "
                    + WmNode.toString(deletedDocumentTypes));
            if (deletedDocumentTypes.size() > 0) {
                log.info("Executing lucene query to find all documents that belong to " + deletedDocumentTypes.size()
                        + " document types which were changed to publicAdr=false during specified period");
                List<NodeRef> docs = documentSearchService.searchAdrDocuments((Date) null, (Date) null, deletedDocumentTypes);
                log.info("Found " + docs.size() + " documents that belong to " + deletedDocumentTypes.size()
                        + " document types which were changed to publicAdr=false during specified period");
                docs.removeAll(deletedDocRefs);

                // ============= Get all documents from database sorted by modified
                List<NodeRef> documentsByModified = getDocumentsSortedByModified();
                documentsByModified.retainAll(docs);

                deletedDocRefs.addAll(documentsByModified);

                docs.removeAll(documentsByModified);
                log.info("There are " + docs.size() + " documents in the lucene response that are not in the sql response");
            }

            // ============= Build results

            log.info("Total found " + deletedDocRefs.size() + " deleted documents");
            int skipped = 0;
            list = new ArrayList<DokumentId>(deletedDocRefs.size());
            for (NodeRef deletedDocRef : deletedDocRefs) {
                if (skip > 0 && skipped < skip) {
                    skipped++;
                    continue;
                }
                DokumentId dokument = new DokumentId();
                dokument.setId(deletedDocRef.toString());
                list.add(dokument);
                if (limit > 0 && list.size() >= limit) {
                    log.info("Limit reached, breaking");
                    break;
                }
            }
        }

        log.info("Finished koikDokumendidKustutatudV2"
                + ", time " + (System.currentTimeMillis() - startTime) + " ms"
                + ", results " + list.size());
        return list;
    }

    private static final Comparator<AdrDocument> ADR_DOCUMENT_BY_REG_DATE_TIME_COMPARATOR;
    static {
        // ComparatorChain is not thread-safe at construction time, but it is thread-safe to perform multiple comparisons after all the setup operations are complete.
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((AdrDocument) input).regDateTime;
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((AdrDocument) input).nodeRef;
            }
        }, new NullComparator()));
        @SuppressWarnings("unchecked")
        Comparator<AdrDocument> tmp = chain;
        ADR_DOCUMENT_BY_REG_DATE_TIME_COMPARATOR = tmp;
    }

    // ========================================================================
    // =============================== INTERNAL ===============================
    // ========================================================================

    @Override
    public NodeRef addDeletedDocument(NodeRef document) {
        String regNumber = (String) nodeService.getProperty(document, DocumentCommonModel.Props.REG_NUMBER);
        Date regDateTime = (Date) nodeService.getProperty(document, DocumentCommonModel.Props.REG_DATE_TIME);
        return addDeletedDocument(document, regNumber, regDateTime);
    }

    private String getInitialsIfNeeded(String name, NodeRef documentRef) {
        Boolean initialsToAdr = (Boolean) nodeService.getProperty(documentRef, DocumentDynamicModel.Props.SENDER_INITIALS_TO_ADR);
        return Boolean.TRUE.equals(initialsToAdr) ? UserUtil.getInitials(name) : name;
    }

    private String getClassifiedOrgStructValueIfNeeded(String ownerName, String ownerOrgStructUnit) {
        if (StringUtils.isBlank(ownerOrgStructUnit)) {
            return ownerName;
        }

        List<ClassificatorValue> allClassificatorValues = classificatorService.getAllClassificatorValues("classifiedOrgStructUnitsToAdr");
        for (ClassificatorValue classificatorValue : allClassificatorValues) {
            if (classificatorValue.getValueName().equals(ownerOrgStructUnit)) {
                return classificatorValue.getValueData();
            }
        }

        return joinStringAndStringWithParentheses(ownerName, ownerOrgStructUnit);
    }

    // START: getters / setters

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setAccessRestrictionChangeReasonEnabled(boolean accessRestrictionChangeReasonEnabled) {
        this.accessRestrictionChangeReasonEnabled = accessRestrictionChangeReasonEnabled;
    }

    public void setVolumeTitleEnabled(boolean volumeTitleEnabled) {
        this.volumeTitleEnabled = volumeTitleEnabled;
    }
    // END: getters / setters

}
