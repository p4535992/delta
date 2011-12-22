package ee.webmedia.alfresco.adr.service;

import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithParentheses;
import static ee.webmedia.alfresco.utils.XmlUtil.getDate;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
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
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Dmitri Melnikov
 * @author Alar Kvell
 */
public class AdrServiceImpl extends BaseAdrServiceImpl {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AdrServiceImpl.class);

    public static FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");

    private DocumentSearchService documentSearchService;
    private FileService fileService;
    private DocumentTypeService documentTypeService;
    private DocumentService documentService;
    private NamespaceService namespaceService;
    private SimpleJdbcTemplate jdbcTemplate;

    // ========================================================================
    // =========================== REAL-TIME QUERYING =========================
    // ========================================================================

    @Override
    public List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik,
            String otsingusona) {
        long startTime = System.currentTimeMillis();

        Date regDateBegin = getDate(perioodiAlgusKuupaev);
        Date regDateEnd = getDate(perioodiLoppKuupaev);
        QName docType = getTypeQName(dokumendiLiik);

        List<Document> docs = documentSearchService.searchAdrDocuments(regDateBegin, regDateEnd, docType, otsingusona, documentTypeService.getPublicAdrDocumentTypeQNames());
        List<Dokument> list = new ArrayList<Dokument>(docs.size());
        for (Document doc : docs) {
            Dokument dokument = new Dokument();
            setDokumentProperties(dokument, doc, true);
            list.add(dokument);
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR otsiDokumendid finished, time " + (System.currentTimeMillis() - startTime) + " ms, results " + list.size()
                    + ", arguments:\n    perioodiAlgusKuupaev=" + perioodiAlgusKuupaev + "\n    perioodiLoppKuupaev=" + perioodiLoppKuupaev
                    + "\n    dokumendiLiik=" + dokumendiLiik + "\n    otsingusona=" + otsingusona);
        }
        return list;
    }

    @Override
    public DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg) {
        long startTime = System.currentTimeMillis();

        Set<QName> documentTypes = documentTypeService.getPublicAdrDocumentTypeQNames();
        Document doc = searchDocument(viit, registreerimiseAeg, documentTypes);
        DokumentDetailidega dokumentDetailidega;
        if (doc == null) {
            dokumentDetailidega = null;
        } else {
            dokumentDetailidega = buildDokumentDetailidega(doc, true, false, documentTypes);
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR dokumentDetailidega finished, time " + (System.currentTimeMillis() - startTime) + " ms, arguments:\n    viit=" + viit
                    + "\n    registreerimiseAeg=" + registreerimiseAeg);
        }
        return dokumentDetailidega;
    }

    @Override
    public Fail failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename) {
        long startTime = System.currentTimeMillis();

        Fail failSisuga;
        File file = searchFile(viit, registreerimiseAeg, filename);
        if (file == null) {
            failSisuga = null;
        } else {
            failSisuga = new Fail();
            setFailProperties(failSisuga, file, true);
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR failSisuga finished, time " + (System.currentTimeMillis() - startTime) + " ms, arguments:\n    viit=" + viit
                    + "\n    registreerimiseAeg=" + registreerimiseAeg + "\n    filename=" + filename);
        }
        return failSisuga;
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
        Document doc = documentService.getDocumentByNodeRef(documentRef);

        if (!documentTypeService.getPublicAdrDocumentTypeQNames().contains(doc.getType())) {
            return null;
        }

        // failiga seotud dokumendi docStatus = lõpetatud või incomingLetter & registreeritud
        boolean isFinished = doc.isDocStatus(DocumentStatus.FINISHED);
        boolean isRegisteredIncomingLetter = DocumentTypeHelper.isIncomingLetter(doc.getType())
                && StringUtils.isNotBlank((String) doc.getProperties().get(DocumentCommonModel.Props.REG_NUMBER));
        if (!isFinished && !isRegisteredIncomingLetter) {
            return null;
        }

        File file = searchFile(doc, filename);

        FailV2 failSisuga;
        if (file == null) {
            failSisuga = null;
        } else {
            failSisuga = new FailV2();
            setFailProperties(failSisuga, file, true);
            setFailV2Properties(failSisuga, file);
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR failSisugaV2 finished, time " + (System.currentTimeMillis() - startTime) + " ms, arguments:\n    documentRef=" + documentRef
                    + "\n    filename=" + filename);
        }
        return failSisuga;
    }

    @Override
    public List<Dokumendiliik> dokumendiliigid() {
        long startTime = System.currentTimeMillis();
        List<DocumentType> docTypes = documentTypeService.getPublicAdrDocumentTypes();
        List<Dokumendiliik> list = new ArrayList<Dokumendiliik>(docTypes.size());
        for (DocumentType docType : docTypes) {
            Dokumendiliik dokumendiliik = new Dokumendiliik();
            dokumendiliik.setNimi(docType.getName());
            list.add(dokumendiliik);
        }
        if (log.isDebugEnabled()) {
            log.debug("ADR dokumendiliigid finished, time " + (System.currentTimeMillis() - startTime) + " ms, results " + list.size());
        }
        return list;
    }

    private Document searchDocument(String viit, XMLGregorianCalendar registreerimiseAeg, Set<QName> documentTypes) {
        Date regDateTime = getDate(registreerimiseAeg);
        if (StringUtils.isBlank(viit) || regDateTime == null) {
            return null;
        }

        List<Document> docs = documentSearchService.searchAdrDocuments(viit, regDateTime, documentTypes);

        // loop üle docs'i ja võrrelda kas viit ja registreerimiseAeg vastavad TÄPSELT, kui mitte siis eemaldada
        for (Iterator<Document> i = docs.iterator(); i.hasNext();) {
            Document doc = i.next();
            if (!viit.equals(doc.getRegNumber()) || !regDateTime.equals(doc.getRegDateTime())) { // viit comparison is case sensitive - is it OK?
                i.remove();
            }
        }
        if (docs.size() == 0) {
            return null;
        }
        if (docs.size() > 1) {
            StringBuilder s = new StringBuilder("Multiple documents have same regNumber+regDateTime combination value, returning only the first, total=");
            s.append(docs.size());
            s.append(", regNr='").append(viit);
            s.append("', regDateTime=").append(regDateTime);
            for (Document doc : docs) {
                s.append("\n    nodeRef=" + doc.getNodeRef());
            }
            log.warn(s.toString());
        }
        return docs.get(0);
    }

    private File searchFile(String viit, XMLGregorianCalendar registreerimiseAeg, String filename) {
        if (StringUtils.isBlank(filename)) {
            return null;
        }

        // 5.1.2.1. failiga seotud dokumendi regDateTime = failRequest.registreerimiseAeg
        // 5.1.2.2. failiga seotud dokumendi regNumber = failRequest.viit
        // 5.1.2.4. failiga seotud dokumendi docStatus = lõpetatud
        Document doc = searchDocument(viit, registreerimiseAeg, documentTypeService.getPublicAdrDocumentTypeQNames());
        if (doc == null) {
            return null;
        }
        return searchFile(doc, filename);
    }

    private File searchFile(Document doc, String filename) {

        // Only include file list when document accessRestriction = Avalik
        if (!AccessRestriction.OPEN.equals(doc.getAccessRestriction())) {
            return null;
        }

        List<File> files = fileService.getAllActiveFiles(doc.getNodeRef());
        for (Iterator<File> i = files.iterator(); i.hasNext();) {
            File file = i.next();
            // 5.1.2.5. faili pealkiri = failRequest.failinimi
            if (!file.getName().equals(filename)) { // this should be the real file name, because ADR interface requires it to be unique under document
                i.remove();
            }
        }
        if (files.size() == 0) {
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
            log.warn(s.toString());
        }
        return files.get(0);
    }

    private QName getTypeQName(String dokumendiLiik) {
        if (StringUtils.isBlank(dokumendiLiik)) {
            return null;
        }
        List<DocumentType> allDocumentTypes = documentTypeService.getAllDocumentTypes();
        for (DocumentType type : allDocumentTypes) {
            if (type.getName().equals(dokumendiLiik)) {
                return type.getId();
            }
        }
        return null;
    }

    private static void setDokumentProperties(Dokument dokument, Document doc, boolean includeAdditionalProperties) {
        boolean isIncomingLetter = DocumentTypeHelper.isIncomingLetter(doc.getDocumentType().getId());

        dokument.setViit(getNullIfEmpty(doc.getRegNumber()));
        dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
        if (includeAdditionalProperties) {
            dokument.setDokumendiLiik(getNullIfEmpty(doc.getDocumentTypeName()));
            if (isIncomingLetter) {
                dokument.setSaatja(getNullIfEmpty(doc.getSender()));
                dokument.setSaaja(getNullIfEmpty(joinStringAndStringWithParentheses(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
            }
            dokument.setPealkiri(getNullIfEmpty(getDocNameAdr(doc)));
        }
    }

    private static String getDocNameAdr(Document doc) {
        String docName = (String) doc.getProperties().get(DocumentDynamicModel.Props.DOC_NAME_ADR);
        if (StringUtils.isBlank(docName)) {
            docName = doc.getDocName();
        }
        return docName;
    }

    private DokumentDetailidegaV2 buildDokumentDetailidegaV2(Document doc, boolean includeFileContent, Set<QName> documentTypes,
            Map<NodeRef, Map<QName, Serializable>> functionsCache, Map<NodeRef, Map<QName, Serializable>> seriesCache,
            Map<NodeRef, Map<QName, Serializable>> volumesCache) {

        DokumentDetailidegaV2 dokument = new DokumentDetailidegaV2();
        boolean isIncomingLetter = DocumentTypeHelper.isIncomingLetter(doc.getDocumentType().getId());

        // =======================================================
        // Copied from setDokumentProperties

        dokument.setViit(getNullIfEmpty(doc.getRegNumber()));
        dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
        if (isIncomingLetter) {
            dokument.setSaatja(getNullIfEmpty(doc.getSender()));
            dokument.setSaaja(getNullIfEmpty(joinStringAndStringWithParentheses(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        dokument.setPealkiri(getNullIfEmpty(getDocNameAdr(doc)));

        // =======================================================
        // Copied from setDokumentDetailidegaProperties

        dokument.setJuurdepaasuPiirang(getNullIfEmpty(doc.getAccessRestriction()));
        dokument.setJuurdepaasuPiiranguAlus(getNullIfEmpty(doc.getAccessRestrictionReason()));
        dokument.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionBeginDate()));
        dokument.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionEndDate()));
        dokument.setJuurdepaasuPiiranguLopp(getNullIfEmpty(doc.getAccessRestrictionEndDesc()));
        if (isIncomingLetter) {
            dokument.setVastamiseKuupaev(convertToXMLGergorianCalendar(doc.getComplienceDate()));
        } else if (DocumentSubtypeModel.Types.OUTGOING_LETTER_MV.equals(doc.getType())) {
            dokument.setVastamiseKuupaev(convertToXMLGergorianCalendar((Date) doc.getProperties().get(DocumentSpecificModel.Props.REPLY_DATE)));
        }
        dokument.setTahtaeg(convertToXMLGergorianCalendar(doc.getDueDate2()));
        if (!isIncomingLetter) {
            dokument.setKoostaja(getNullIfEmpty(joinStringAndStringWithParentheses(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        dokument.setAllkirjastaja(getNullIfEmpty(doc.getSignerName()));

        // =======================================================

        // Associated documents
        List<SeotudDokument> assocDocs = getSeotudDokumentList(doc.getNodeRef(), documentTypes);
        dokument.getSeotudDokument().addAll(assocDocs);

        // Only include file list when document accessRestriction = Avalik
        if (AccessRestriction.OPEN.equals(doc.getAccessRestriction())) {
            List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNodeRef());
            for (File file : allActiveFiles) {
                FailV2 fail = new FailV2();
                setFailProperties(fail, file, includeFileContent);
                setFailV2Properties(fail, file);
                dokument.getFail().add(fail);
            }
        }

        // =======================================================
        // New V2 specific fields

        dokument.setId(getNullIfEmpty(doc.getNodeRefAsString()));

        dokument.setLisad(getNullIfEmpty((String) doc.getProperties().get(DocumentSpecificModel.Props.ANNEX)));

        dokument.setSaatjaViit(getNullIfEmpty(doc.getSenderRegNumber()));

        String transmittalMode;
        if (isIncomingLetter) {
            transmittalMode = (String) doc.getProperties().get(DocumentSpecificModel.Props.TRANSMITTAL_MODE);
        } else {
            @SuppressWarnings("unchecked")
            List<String> sendModes = (List<String>) doc.getProperties().get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
            transmittalMode = TextUtil.joinUniqueStringsWithComma(sendModes);
        }
        dokument.setSaatmisviis(getNullIfEmpty(transmittalMode));

        // Tähtaja kirjeldus
        if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(doc.getType())) {
            dokument.setTahtaegKirjeldus(getNullIfEmpty((String) doc.getProperties().get(DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE_DESC)));
        } else if (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(doc.getType())) {
            dokument.setTahtaegKirjeldus(getNullIfEmpty((String) doc.getProperties().get(DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE_DESC)));
        }

        // Osapooled
        if (DocumentTypeHelper.isContract(doc.getType())) {
            List<Node> parties = null;
            String osapool = "";
            if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)) {
                osapool = TextUtil.joinNonBlankStringsWithComma(
                        Arrays.<String> asList((String) doc.getProperties().get(DocumentSpecificModel.Props.FIRST_PARTY_NAME),
                                (String) doc.getProperties().get(DocumentSpecificModel.Props.SECOND_PARTY_NAME),
                                (String) doc.getProperties().get(DocumentSpecificModel.Props.THIRD_PARTY_NAME)));

            } else if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                parties = doc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_PARTIES);

            } else if (doc.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_MV_DETAILS)) {
                parties = doc.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES);
            }

            if (parties != null) {
                List<String> names = new ArrayList<String>(parties.size());
                for (Node node : parties) {
                    names.add((String) node.getProperties().get(DocumentSpecificModel.Props.PARTY_NAME));
                }
                osapool = TextUtil.joinNonBlankStringsWithComma(names);
            }

            dokument.setOsapool(getNullIfEmpty(osapool));
        } else if (DocumentTypeHelper.isOutgoingLetter(doc.getType())) {
            dokument.setOsapool(getNullIfEmpty(doc.getRecipients()));
        }

        // =======================================================

        // Document type
        DokumendiliikV2 wsDocumentType = new DokumendiliikV2();
        wsDocumentType.setId(doc.getDocumentType().getId().getLocalName());
        wsDocumentType.setNimi(getNullIfEmpty(doc.getDocumentTypeName()));
        dokument.setDokumendiLiik(wsDocumentType);

        // Volume
        Map<QName, NodeRef> docParents = null;
        NodeRef volumeRef = (NodeRef) doc.getNode().getProperties().get(DocumentCommonModel.Props.VOLUME);
        Map<QName, Serializable> volumeProps = volumesCache.get(volumeRef);
        if (volumeProps == null) {
            if (volumeRef == null) {
                log.warn("Document property volume=null\n  nodeRef=" + doc.getNodeRefAsString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getAspects(), namespaceService));
                docParents = documentService.getDocumentParents(doc.getNodeRef());
                volumeRef = docParents.get(DocumentCommonModel.Props.VOLUME);
            }
            volumeProps = nodeService.getProperties(volumeRef);
            volumesCache.put(volumeRef, volumeProps);
        }
        Toimik wsVolume = new Toimik();
        wsVolume.setId(volumeRef.toString());
        wsVolume.setViit((String) volumeProps.get(VolumeModel.Props.MARK));
        wsVolume.setPealkiri((String) volumeProps.get(VolumeModel.Props.TITLE));
        wsVolume.setKehtivAlatesKuupaev(convertToXMLGergorianCalendar((Date) volumeProps.get(VolumeModel.Props.VALID_FROM)));
        wsVolume.setKehtivKuniKuupaev(convertToXMLGergorianCalendar((Date) volumeProps.get(VolumeModel.Props.VALID_TO)));
        dokument.setToimik(wsVolume);

        // Series
        NodeRef seriesRef = (NodeRef) doc.getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
        Map<QName, Serializable> seriesProps = seriesCache.get(seriesRef);
        if (seriesProps == null) {
            if (seriesRef == null) {
                log.warn("Document property series=null\n  nodeRef=" + doc.getNodeRefAsString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getAspects(), namespaceService));
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
            if (functionRef == null) {
                log.warn("Document property function=null\n  nodeRef=" + doc.getNodeRefAsString() + "\n  props="
                        + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService) + "\n  aspects="
                        + WmNode.toString(doc.getAspects(), namespaceService));
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

    private DokumentDetailidega buildDokumentDetailidega(Document doc, boolean includeSeotudDokumentAdditionalProperties, boolean includeFileContent, Set<QName> documentTypes) {
        DokumentDetailidega dokumentDetailidega = new DokumentDetailidega();
        setDokumentDetailidegaProperties(dokumentDetailidega, doc, includeSeotudDokumentAdditionalProperties, includeFileContent, documentTypes);
        return dokumentDetailidega;
    }

    private void setDokumentDetailidegaProperties(DokumentDetailidega dokumentDetailidega, Document doc
            , boolean includeSeotudDokumentAdditionalProperties, boolean includeFileContent, Set<QName> documentTypes) {
        setDokumentProperties(dokumentDetailidega, doc, true);
        boolean isIncomingLetter = DocumentTypeHelper.isIncomingLetter(doc.getDocumentType().getId());

        dokumentDetailidega.setJuurdepaasuPiirang(getNullIfEmpty(doc.getAccessRestriction()));
        dokumentDetailidega.setJuurdepaasuPiiranguAlus(getNullIfEmpty(doc.getAccessRestrictionReason()));
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionBeginDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionEndDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguLopp(getNullIfEmpty(doc.getAccessRestrictionEndDesc()));
        if (isIncomingLetter) {
            dokumentDetailidega.setVastamiseKuupaev(convertToXMLGergorianCalendar(doc.getComplienceDate()));
        } else if (DocumentSubtypeModel.Types.OUTGOING_LETTER_MV.equals(doc.getType())) {
            dokumentDetailidega.setVastamiseKuupaev(convertToXMLGergorianCalendar((Date) doc.getProperties().get(DocumentSpecificModel.Props.REPLY_DATE)));
        }
        dokumentDetailidega.setTahtaeg(convertToXMLGergorianCalendar(doc.getDueDate2()));
        if (!isIncomingLetter) {
            dokumentDetailidega.setKoostaja(getNullIfEmpty(joinStringAndStringWithParentheses(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        dokumentDetailidega.setAllkirjastaja(getNullIfEmpty(doc.getSignerName()));

        dokumentDetailidega.getSeotudDokument().addAll(otsiDokumendidSamasTeemas(doc.getNodeRef(), includeSeotudDokumentAdditionalProperties, documentTypes));

        // Only include file list when document accessRestriction = Avalik
        if (AccessRestriction.OPEN.equals(doc.getAccessRestriction())) {
            List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNodeRef());
            for (File file : allActiveFiles) {
                Fail fail = new Fail();
                setFailProperties(fail, file, includeFileContent);
                dokumentDetailidega.getFail().add(fail);
            }
        }
    }

    private void setFailProperties(Fail fail, File file, boolean includeContent) {
        fail.setFailinimi(getNullIfEmpty(file.getName())); // this should be the real file name, because ADR interface requires it to be unique under document
        fail.setSuurus((int) file.getSize());
        fail.setEncoding(getNullIfEmpty(file.getEncoding()));
        fail.setMimeType(getNullIfEmpty(file.getMimeType()));
        if (includeContent) {
            fail.setSisu(getFileDataHandler(file.getNodeRef(), file.getName()));
        }
    }

    private void setFailV2Properties(FailV2 failSisuga, File file) {
        failSisuga.setPealkiri(getNullIfEmpty(file.getDisplayName()));
        failSisuga.setMuutmiseAeg(convertToXMLGergorianCalendar(file.getModified()));
    }

    private List<Dokument> otsiDokumendidSamasTeemas(NodeRef document, boolean includeAdditionalProperties, Set<QName> documentTypes) {
        List<Document> docs = documentService.getReplyOrFollowUpDocuments(document);
        List<Dokument> list = new ArrayList<Dokument>(docs.size());
        for (Document doc : docs) {
            if ((doc.isDocStatus(DocumentStatus.FINISHED) || DocumentTypeHelper.isIncomingLetter(doc.getType()))
                    && (AccessRestriction.OPEN.equals(doc.getAccessRestriction()) || AccessRestriction.AK.equals(doc.getAccessRestriction()))
                    && StringUtils.isNotEmpty(doc.getRegNumber()) && doc.getRegDateTime() != null && documentTypes.contains(doc.getType())) {

                Dokument dokument = new Dokument();
                setDokumentProperties(dokument, doc, includeAdditionalProperties);
                list.add(dokument);
            }
        }
        return list;
    }

    private List<SeotudDokument> getSeotudDokumentList(NodeRef document, Set<QName> documentTypes) {
        List<SeotudDokument> list = new ArrayList<SeotudDokument>();
        for (AssociationRef targetAssocRef : nodeService.getTargetAssocs(document, RegexQNamePattern.MATCH_ALL)) {
            SeotudDokument seotudDokument = getSeotudDokument(targetAssocRef, false, documentTypes);
            if (seotudDokument != null) {
                list.add(seotudDokument);
            }
        }
        for (AssociationRef sourceAssocRef : nodeService.getSourceAssocs(document, RegexQNamePattern.MATCH_ALL)) {
            SeotudDokument seotudDokument = getSeotudDokument(sourceAssocRef, true, documentTypes);
            if (seotudDokument != null) {
                list.add(seotudDokument);
            }
        }
        return list;
    }

    public SeotudDokument getSeotudDokument(AssociationRef assocRef, boolean isSourceAssoc, Set<QName> documentTypes) {
        NodeRef otherDocument = isSourceAssoc ? assocRef.getSourceRef() : assocRef.getTargetRef();
        if (!nodeService.hasAspect(otherDocument, DocumentCommonModel.Aspects.SEARCHABLE)) {
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

        QName type = nodeService.getType(otherDocument);
        if (!documentTypes.contains(type)) {
            return null;
        }

        Map<QName, Serializable> props = nodeService.getProperties(otherDocument);
        String docStatus = (String) props.get(DocumentCommonModel.Props.DOC_STATUS);
        String accessRestriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if ((DocumentStatus.FINISHED.getValueName().equals(docStatus) || DocumentTypeHelper.isIncomingLetter(type))
                && (AccessRestriction.OPEN.equals(accessRestriction) || AccessRestriction.AK.equals(accessRestriction))
                && StringUtils.isNotEmpty(regNumber) && regDateTime != null) {

            seotudDokument.setId(otherDocument.toString());
            return seotudDokument;
        }
        return null;
    }

    // ========================================================================
    // ======================= PERIODIC SYNCHRONIZATION =======================
    // ========================================================================

    @Override
    public List<DokumentDetailidega> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        return koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDocumentCallback<DokumentDetailidega>() {
            @Override
            public DokumentDetailidega buildDocument(Document doc, Set<QName> documentTypes) {
                return buildDokumentDetailidega(doc, false, true, documentTypes);
            }
        }, 0, 0, false);
    }

    @Override
    public List<DokumentDetailidegaV2> koikDokumendidLisatudMuudetudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, int jataAlgusestVahele,
            int tulemustePiirang) {
        final Map<NodeRef, Map<QName, Serializable>> functionsCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> seriesCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> volumesCache = new HashMap<NodeRef, Map<QName, Serializable>>();

        return koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDocumentCallback<DokumentDetailidegaV2>() {
            @Override
            public DokumentDetailidegaV2 buildDocument(Document doc, Set<QName> documentTypes) {
                return buildDokumentDetailidegaV2(doc, false, documentTypes, functionsCache, seriesCache, volumesCache);
            }
        }, jataAlgusestVahele, tulemustePiirang, true);
    }

    private static interface BuildDocumentCallback<T> {
        T buildDocument(Document doc, Set<QName> documentTypes);
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

            Set<QName> publicAdrDocumentTypes = Collections.unmodifiableSet(documentTypeService.getPublicAdrDocumentTypeQNames());

            log.info("Executing lucene query to find all public ADR documents, modified between " + dateFormat.format(modifiedDateBegin) + " and "
                    + dateFormat.format(modifiedDateEnd) + " (inclusive)");
            List<NodeRef> docs1 = documentSearchService.searchAdrDocuments(modifiedDateBegin, modifiedDateEnd, publicAdrDocumentTypes);
            log.info("Found " + docs1.size() + " documents that were modified during specified period");
            Set<NodeRef> docs = new HashSet<NodeRef>(docs1);

            // ============= Search for document types that were changed to publicAdr=true during specified period
            // ============= and add ALL documents that belong to these types to results

            List<QName> addedDocumentTypes = documentSearchService.searchAdrAddedDocumentTypes(modifiedDateBegin, modifiedDateEnd);
            Set<QName> documentTypes = new HashSet<QName>(publicAdrDocumentTypes); // Currently allowed docTypes
            documentTypes.retainAll(addedDocumentTypes); // Result: docTypes that were added during this period AND are currently allowed
            if (documentTypes.size() > 0) {
                log.info("Executing lucene query to find all documents of the following document types whose publicAdr was changed during specified period: "
                        + WmNode.toString(documentTypes, namespaceService));
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
                Document doc = documentService.getDocumentByNodeRef(nodeRef);
                if (StringUtils.isEmpty(doc.getRegNumber()) || doc.getRegDateTime() == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + doc.getNodeRefAsString() + "\nproperties="
                            + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService));
                    continue; // should not happen!
                    // this may move the results according to skip, and the next request has some overlapping results;
                    // this should not be a problem, as long as there are less of these warning documents than limit
                }
                AdrDocument adrDocument = new AdrDocument(doc.getNodeRef(), doc.getRegNumber(), doc.getRegDateTime(), compareByNodeRef);
                if (compareByNodeRef || !results.containsKey(adrDocument)) {
                    log.debug("Constructing document " + (results.size() + 1));
                    results.put(adrDocument, buildDocumentCallback.buildDocument(doc, publicAdrDocumentTypes));
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

    private static interface BuildDeletedDocumentCallback<T> {
        T buildDocument(AdrDocument doc);
    }

    @Override
    public List<Dokument> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        return koikDokumendidKustutatud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDeletedDocumentCallback<Dokument>() {
            @Override
            public Dokument buildDocument(AdrDocument doc) {
                Dokument dokument = new Dokument();
                dokument.setViit(doc.regNumber);
                dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.regDateTime));
                return dokument;
            }
        }, false);
    }

    private <T> List<T> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev,
            BuildDeletedDocumentCallback<T> buildDeletedDocumentCallback, boolean compareByNodeRef) {

        long startTime = System.currentTimeMillis();

        Date deletedDateBegin = getDate(perioodiAlgusKuupaev);
        Date deletedDateEnd = getDate(perioodiLoppKuupaev);

        List<T> list;

        if (deletedDateBegin == null || deletedDateEnd == null) {
            list = Collections.emptyList();
        } else {

            // ============= Search for documents that were deleted during specified period

            List<NodeRef> existingDocs = documentSearchService.searchAdrDocuments(deletedDateBegin, deletedDateEnd, documentTypeService.getPublicAdrDocumentTypeQNames());
            Set<AdrDocument> existingDocsSet = new HashSet<AdrDocument>(existingDocs.size());
            for (NodeRef documentRef : existingDocs) {
                Document document = documentService.getDocumentByNodeRef(documentRef);
                if (StringUtils.isEmpty(document.getRegNumber()) || document.getRegDateTime() == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + document.getNodeRefAsString() + "\nproperties="
                            + WmNode.toString(RepoUtil.toQNameProperties(document.getProperties()), namespaceService));
                    continue; // should not happen!
                }
                existingDocsSet.add(new AdrDocument(document.getNodeRef(), document.getRegNumber(), document.getRegDateTime(), compareByNodeRef));
            }

            List<NodeRef> deletedDocs = documentSearchService.searchAdrDeletedDocuments(deletedDateBegin, deletedDateEnd);
            Set<AdrDocument> deletedDocsSet = new HashSet<AdrDocument>(deletedDocs.size());
            for (NodeRef deletedDoc : deletedDocs) {
                Map<QName, Serializable> props = nodeService.getProperties(deletedDoc);
                String regNumber = (String) props.get(AdrModel.Props.REG_NUMBER);
                Date regDateTime = (Date) props.get(AdrModel.Props.REG_DATE_TIME);
                if (StringUtils.isEmpty(regNumber) || regDateTime == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + deletedDoc + "\nproperties="
                            + WmNode.toString(props, namespaceService));
                    continue; // should not happen!
                }
                NodeRef nodeRef = (NodeRef) props.get(AdrModel.Props.NODEREF);
                if (compareByNodeRef && nodeRef == null) {
                    // Older data doesn't have nodeRef property
                    continue;
                }
                deletedDocsSet.add(new AdrDocument(nodeRef, regNumber, regDateTime, compareByNodeRef));
            }

            deletedDocsSet.removeAll(existingDocsSet);
            if (log.isDebugEnabled()) {
                log.debug("Found " + deletedDocsSet.size() + " documents that were deleted during specified period");
            }

            // ============= Search for document types that were changed to publicAdr=false during specified period
            // ============= and add ALL documents that belong to these types to results

            Set<QName> deletedDocumentTypes = new HashSet<QName>(documentSearchService.searchAdrDeletedDocumentTypes(deletedDateBegin, deletedDateEnd));
            // Result: docTypes that were deleted during this period AND are not currently allowed
            deletedDocumentTypes.removeAll(documentTypeService.getPublicAdrDocumentTypeQNames());

            List<NodeRef> docs = documentSearchService.searchAdrDocuments((Date) null, (Date) null, deletedDocumentTypes);
            if (log.isDebugEnabled()) {
                log.debug("Found " + docs.size() + " documents that belong to " + deletedDocumentTypes.size()
                        + " document types that were changed to publicAdr=FALSE during specified period");
            }
            for (NodeRef docRef : docs) {
                Document doc = documentService.getDocumentByNodeRef(docRef);
                if (StringUtils.isEmpty(doc.getRegNumber()) || doc.getRegDateTime() == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + doc.getNodeRefAsString() + "\nproperties="
                            + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService));
                    continue; // should not happen!
                }
                deletedDocsSet.add(new AdrDocument(doc.getNodeRef(), doc.getRegNumber(), doc.getRegDateTime(), compareByNodeRef));
            }

            // ============= Build results

            list = new ArrayList<T>(deletedDocsSet.size());
            for (AdrDocument doc : deletedDocsSet) {
                list.add(buildDeletedDocumentCallback.buildDocument(doc));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR koikDokumendidKustutatud finished, time " + (System.currentTimeMillis() - startTime)
                    + " ms, results " + list.size() + ", arguments:\n    perioodiAlgusKuupaev=" + perioodiAlgusKuupaev + "\n    perioodiLoppKuupaev="
                    + perioodiLoppKuupaev);
        }
        return list;
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
                NodeRef nodeRef = (NodeRef) props.get(AdrModel.Props.NODEREF);
                if (nodeRef == null) {
                    // Older data doesn't have nodeRef property
                    continue;
                }
                Date deletedDateTime = (Date) props.get(AdrModel.Props.DELETED_DATE_TIME);
                // Use regDateTime field to store deletedDateTime
                deletedDocs.add(new AdrDocument(nodeRef, "", deletedDateTime, true));
            }
            log.info("List contains " + deletedDocs.size() + " documents that were deleted during specified period");

            // ============= Search for documents that exist (were modified) during specified period

            log.info("Executing lucene query to find documents that were modified during specified period");
            List<NodeRef> existingDocRefs = documentSearchService.searchAdrDocuments(deletedDateBegin, deletedDateEnd, documentTypeService.getPublicAdrDocumentTypeQNames());
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

            Set<QName> deletedDocumentTypes = new HashSet<QName>(documentSearchService.searchAdrDeletedDocumentTypes(deletedDateBegin, deletedDateEnd));
            // Result: docTypes that were deleted during this period AND are not currently allowed
            deletedDocumentTypes.removeAll(documentTypeService.getPublicAdrDocumentTypeQNames());
            log.info("Found " + deletedDocumentTypes.size() + " document types, which were changed to publicAdr=false during specified period: "
                    + WmNode.toString(deletedDocumentTypes, namespaceService));
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
    public void addDeletedDocument(NodeRef document) {
        String regNumber = (String) nodeService.getProperty(document, DocumentCommonModel.Props.REG_NUMBER);
        Date regDateTime = (Date) nodeService.getProperty(document, DocumentCommonModel.Props.REG_DATE_TIME);
        addDeletedDocument(document, regNumber, regDateTime);
    }

    // START: getters / setters

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }
    // END: getters / setters
}
