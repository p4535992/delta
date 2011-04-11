package ee.webmedia.alfresco.adr.service;

import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithParentheses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;

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

    private DocumentSearchService documentSearchService;
    private FileService fileService;
    private DocumentTypeService documentTypeService;
    private DocumentService documentService;
    private NamespaceService namespaceService;

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

        // 5.1.2.4. failiga seotud dokumendi docStatus = lõpetatud
        if (!DocumentStatus.FINISHED.equals(doc.getDocStatus())) {
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
            dokument.setPealkiri(getNullIfEmpty(doc.getDocName()));
        }
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
        dokument.setPealkiri(getNullIfEmpty(doc.getDocName()));

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

        String party = null;
        if (DocumentTypeHelper.isOutgoingLetter(doc.getType())) {
            // recipientName ja additionalRecipientName
            party = doc.getAllRecipients();
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(doc.getType()) || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(doc.getType())) {
            String secondPartyName = (String) doc.getProperties().get(DocumentSpecificModel.Props.SECOND_PARTY_NAME);
            String thirdPartyName = (String) doc.getProperties().get(DocumentSpecificModel.Props.THIRD_PARTY_NAME);
            party = TextUtil.joinStringAndStringWithComma(secondPartyName, thirdPartyName);
        } else if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(doc.getType())) {
            List<ChildAssociationRef> partyAssocs = nodeService.getChildAssocs(doc.getNodeRef(), DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES, RegexQNamePattern.MATCH_ALL);
            List<String> partyNames = new ArrayList<String>(partyAssocs.size());
            for (ChildAssociationRef partyRef : partyAssocs) {
                String partyName = (String) nodeService.getProperty(partyRef.getChildRef(), DocumentSpecificModel.Props.PARTY_NAME);
                partyNames.add(partyName);
            }
            party = TextUtil.joinNonBlankStringsWithComma(partyNames);
        }
        dokument.setOsapool(getNullIfEmpty(party));

        // TODO when new fields are added to document types:
        // dokument.setTahtaegKirjeldus(getNullIfEmpty(contractSimEndDesc / contractSmitEndDesc));

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
            if (DocumentStatus.FINISHED.equals(doc.getDocStatus())
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
        if (DocumentStatus.FINISHED.equals(docStatus)
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
        }, false);
    }

    @Override
    public List<DokumentDetailidegaV2> koikDokumendidLisatudMuudetudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        final Map<NodeRef, Map<QName, Serializable>> functionsCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> seriesCache = new HashMap<NodeRef, Map<QName, Serializable>>();
        final Map<NodeRef, Map<QName, Serializable>> volumesCache = new HashMap<NodeRef, Map<QName, Serializable>>();

        return koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDocumentCallback<DokumentDetailidegaV2>() {
            @Override
            public DokumentDetailidegaV2 buildDocument(Document doc, Set<QName> documentTypes) {
                return buildDokumentDetailidegaV2(doc, false, documentTypes, functionsCache, seriesCache, volumesCache);
            }
        }, true);
    }

    private static interface BuildDocumentCallback<T> {
        T buildDocument(Document doc, Set<QName> documentTypes);
    }

    private <T> List<T> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev
            , XMLGregorianCalendar perioodiLoppKuupaev, BuildDocumentCallback<T> buildDocumentCallback, boolean compareByNodeRef) {
        long startTime = System.currentTimeMillis();

        Date modifiedDateBegin = getDate(perioodiAlgusKuupaev);
        Date modifiedDateEnd = getDate(perioodiLoppKuupaev);

        List<T> list;

        if (modifiedDateBegin == null || modifiedDateEnd == null) {
            list = Collections.emptyList();
        } else {
            Map<AdrDocument, T> results = new HashMap<AdrDocument, T>();

            // ============= Search for documents that were modified during specified period

            Set<QName> publicAdrDocumentTypes = Collections.unmodifiableSet(documentTypeService.getPublicAdrDocumentTypeQNames());

            List<Document> docs = documentSearchService.searchAdrDocuments(modifiedDateBegin, modifiedDateEnd, publicAdrDocumentTypes);
            if (log.isDebugEnabled()) {
                log.debug("Found " + docs.size() + " documents that were modified during specified period");
            }
            for (Document doc : docs) {
                if (StringUtils.isEmpty(doc.getRegNumber()) || doc.getRegDateTime() == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + doc.getNodeRefAsString() + "\nproperties="
                            + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService));
                    continue; // should not happen!
                }
                AdrDocument adrDocument = new AdrDocument(doc.getNodeRef(), doc.getRegNumber(), doc.getRegDateTime(), compareByNodeRef);
                if (!results.containsKey(adrDocument)) {
                    log.debug("Constructing document " + (results.size() + 1));
                    results.put(adrDocument, buildDocumentCallback.buildDocument(doc, publicAdrDocumentTypes));
                }
            }

            // ============= Search for document types that were changed to publicAdr=true during specified period
            // ============= and add ALL documents that belong to these types to results

            List<QName> addedDocumentTypes = documentSearchService.searchAdrAddedDocumentTypes(modifiedDateBegin, modifiedDateEnd);
            Set<QName> documentTypes = new HashSet<QName>(publicAdrDocumentTypes); // Currently allowed docTypes
            documentTypes.retainAll(addedDocumentTypes); // Result: docTypes that were added during this period AND are currently allowed

            docs = documentSearchService.searchAdrDocuments((Date) null, (Date) null, documentTypes);
            if (log.isDebugEnabled()) {
                log.debug("Found " + docs.size() + " documents that belong to " + documentTypes.size()
                        + " document types that were changed to publicAdr=TRUE during specified period");
            }
            for (Document doc : docs) {
                if (StringUtils.isEmpty(doc.getRegNumber()) || doc.getRegDateTime() == null) {
                    log.warn("ADR document regNumber or regDateTime is missing: nodeRef=" + doc.getNodeRefAsString() + "\nproperties="
                            + WmNode.toString(RepoUtil.toQNameProperties(doc.getProperties()), namespaceService));
                    continue; // should not happen!
                }
                AdrDocument adrDocument = new AdrDocument(doc.getNodeRef(), doc.getRegNumber(), doc.getRegDateTime(), compareByNodeRef);
                if (!results.containsKey(adrDocument)) {
                    log.debug("Constructing document " + (results.size() + 1));
                    results.put(adrDocument, buildDocumentCallback.buildDocument(doc, publicAdrDocumentTypes));
                }
            }

            list = new ArrayList<T>(results.values());
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR koikDokumendidLisatudMuudetud finished, time " + (System.currentTimeMillis() - startTime)
                    + " ms, results " + list.size() + ", arguments:\n    perioodiAlgusKuupaev=" + perioodiAlgusKuupaev + "\n    perioodiLoppKuupaev="
                    + perioodiLoppKuupaev);
        }
        return list;
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

    @Override
    public List<DokumentId> koikDokumendidKustutatudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        return koikDokumendidKustutatud(perioodiAlgusKuupaev, perioodiLoppKuupaev, new BuildDeletedDocumentCallback<DokumentId>() {
            @Override
            public DokumentId buildDocument(AdrDocument doc) {
                DokumentId dokument = new DokumentId();
                dokument.setId(doc.nodeRef.toString());
                return dokument;
            }
        }, true);
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

            List<Document> existingDocs = documentSearchService.searchAdrDocuments(deletedDateBegin, deletedDateEnd, documentTypeService.getPublicAdrDocumentTypeQNames());
            Set<AdrDocument> existingDocsSet = new HashSet<AdrDocument>(existingDocs.size());
            for (Document document : existingDocs) {
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

            List<Document> docs = documentSearchService.searchAdrDocuments((Date) null, (Date) null, deletedDocumentTypes);
            if (log.isDebugEnabled()) {
                log.debug("Found " + docs.size() + " documents that belong to " + deletedDocumentTypes.size()
                        + " document types that were changed to publicAdr=FALSE during specified period");
            }
            for (Document doc : docs) {
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

    // END: getters / setters
}
