package ee.webmedia.alfresco.adr.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.adr.Dokumendiliik;
import ee.webmedia.alfresco.adr.Dokument;
import ee.webmedia.alfresco.adr.DokumentDetailidega;
import ee.webmedia.alfresco.adr.Fail;
import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;

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
        
        List<Dokument> list;

        // If no parameters are given, then return nothing
        // Note: you can still get ALL documents very easily if you specify a very broad date range for example
        if (regDateBegin == null && regDateEnd == null && docType == null && StringUtils.isEmpty(otsingusona)) {
            list = Collections.emptyList();

        } else {
            List<Document> docs = documentSearchService.searchAdrDocuments(regDateBegin, regDateEnd, docType, otsingusona);

            list = new ArrayList<Dokument>(docs.size());
            for (Document doc : docs) {
                Dokument dokument = new Dokument();
                setDokumentProperties(dokument, doc, true);
                list.add(dokument);
            }
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

        DokumentDetailidega dokumentDetailidega;
        Document doc = searchDocument(viit, registreerimiseAeg);
        if (doc == null) {
            dokumentDetailidega = null;
        } else {
            dokumentDetailidega = buildDokumentDetailidega(doc, true, false);
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
            setFailProperties(failSisuga, file, false);
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR failSisuga finished, time " + (System.currentTimeMillis() - startTime) + " ms, arguments:\n    viit=" + viit
                    + "\n    registreerimiseAeg=" + registreerimiseAeg + "\n    filename=" + filename);
        }
        return failSisuga;
    }

    @Override
    public List<Dokumendiliik> dokumendiliigid() {
        long startTime = System.currentTimeMillis();
        List<DocumentType> docTypes = documentTypeService.getAllDocumentTypes();
        List<Dokumendiliik> list = new ArrayList<Dokumendiliik>(docTypes.size());
        for (DocumentType docType : docTypes) {
            // TODO only show "public" document types
            // TODO 2 does "public" have to be taken account on all service methods?

            Dokumendiliik dokumendiliik = new Dokumendiliik();
            dokumendiliik.setNimi(docType.getName());
            list.add(dokumendiliik);
        }
        if (log.isDebugEnabled()) {
            log.debug("ADR dokumendiliigid finished, time " + (System.currentTimeMillis() - startTime) + " ms, results " + list.size());
        }
        return list;
    }

    private Document searchDocument(String viit, XMLGregorianCalendar registreerimiseAeg) {
        Date regDateTime = getDate(registreerimiseAeg);
        if (StringUtils.isBlank(viit) || regDateTime == null) {
            return null;
        }

        List<Document> docs = documentSearchService.searchAdrDocuments(viit, regDateTime);

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
                s.append("\n    nodeRef=" + doc.getNode().getNodeRef());
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
        Document doc = searchDocument(viit, registreerimiseAeg);
        if (doc == null) {
            return null;
        }

        // 5.1.2.3. failiga seotud dokumendi accessRestriction = Avalik
        if (!AccessRestriction.OPEN.equals(doc.getAccessRestriction())) {
            return null;
        }

        List<File> files = fileService.getAllActiveFiles(doc.getNode().getNodeRef());
        for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
            File file = i.next();
            // 5.1.2.5. faili pealkiri = failRequest.failinimi
            if (!file.getName().equals(filename)) {
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
        boolean isIncomingLetter = DocumentSubtypeModel.Types.INCOMING_LETTER.equals(doc.getDocumentType().getId());

        dokument.setViit(getNullIfEmpty(doc.getRegNumber()));
        dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
        if (includeAdditionalProperties) {
            dokument.setDokumendiLiik(getNullIfEmpty(doc.getDocumentTypeName()));
            if (isIncomingLetter) {
                dokument.setSaatja(getNullIfEmpty(doc.getSender()));
                dokument.setSaaja(getNullIfEmpty(getWithParenthesis(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
            }
            dokument.setPealkiri(getNullIfEmpty(doc.getDocName()));
        }
    }

    private DokumentDetailidega buildDokumentDetailidega(Document doc, boolean includeSeotudDokumentAdditionalProperties, boolean includeFileContent) {
        DokumentDetailidega dokumentDetailidega = new DokumentDetailidega();
        setDokumentProperties(dokumentDetailidega, doc, true);
        boolean isIncomingLetter = DocumentSubtypeModel.Types.INCOMING_LETTER.equals(doc.getDocumentType().getId());

        dokumentDetailidega.setJuurdepaasuPiirang(getNullIfEmpty(doc.getAccessRestriction()));
        dokumentDetailidega.setJuurdepaasuPiiranguAlus(getNullIfEmpty(doc.getAccessRestrictionReason()));
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionBeginDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionEndDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguLopp(getNullIfEmpty(doc.getAccessRestrictionEndDesc()));
        if (isIncomingLetter) {
            dokumentDetailidega.setVastamiseKuupaev(isIncomingLetter ? convertToXMLGergorianCalendar(doc.getComplienceDate()) : null);
            dokumentDetailidega.setTahtaeg(convertToXMLGergorianCalendar(doc.getDueDate()));
        } else if (DocumentSubtypeModel.Types.MANAGEMENTS_ORDER.equals(doc.getDocumentType().getId())) {
            dokumentDetailidega.setTahtaeg(convertToXMLGergorianCalendar((Date) doc.getNode().getProperties().get(
                    DocumentSpecificModel.Props.MANAGEMENTS_ORDER_DUE_DATE)));
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(doc.getDocumentType().getId())) {
            dokumentDetailidega.setTahtaeg(convertToXMLGergorianCalendar((Date) doc.getNode().getProperties().get(
                    DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE)));
        } else if (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(doc.getDocumentType().getId())) {
            dokumentDetailidega.setTahtaeg(convertToXMLGergorianCalendar((Date) doc.getNode().getProperties().get(
                    DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE)));
        }
        if (!isIncomingLetter) {
            dokumentDetailidega.setKoostaja(getNullIfEmpty(getWithParenthesis(doc.getOwnerName(), doc.getOwnerOrgStructUnit())));
        }
        dokumentDetailidega.setAllkirjastaja(getNullIfEmpty(doc.getSignerName()));

        dokumentDetailidega.getSeotudDokument().addAll(otsiDokumendidSamasTeemas(doc.getNode().getNodeRef(), includeSeotudDokumentAdditionalProperties));

        // 5.1.2.3. failiga seotud dokumendi accessRestriction = Avalik
        if (!AccessRestriction.OPEN.equals(doc.getAccessRestriction())) {
            includeFileContent = false;
        }
        List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNode().getNodeRef());
        for (File file : allActiveFiles) {
            Fail fail = new Fail();
            setFailProperties(fail, file, includeFileContent);
            dokumentDetailidega.getFail().add(fail);
        }

        return dokumentDetailidega;
    }

    private void setFailProperties(Fail fail, File file, boolean includeContent) {
        fail.setFailinimi(getNullIfEmpty(file.getName()));
        fail.setSuurus((int) file.getSize());
        fail.setEncoding(getNullIfEmpty(file.getEncoding()));
        fail.setMimeType(getNullIfEmpty(file.getMimeType()));
        if (includeContent) {
            fail.setSisu(getFileDataHandler(file.getNodeRef(), file.getName()));
        }
    }

    private List<Dokument> otsiDokumendidSamasTeemas(NodeRef document, boolean includeAdditionalProperties) {
        List<Document> docs = documentService.getReplyOrFollowUpDocuments(document);
        List<Dokument> list = new ArrayList<Dokument>(docs.size());
        for (Document doc : docs) {
            if (DocumentStatus.FINISHED.equals(doc.getDocStatus()) &&
                    !AccessRestriction.INTERNAL.equals(doc.getAccessRestriction()) &&
                    StringUtils.isNotBlank(doc.getRegNumber())) {

                Dokument dokument = new Dokument();
                setDokumentProperties(dokument, doc, includeAdditionalProperties);
                list.add(dokument);
            }
        }
        return list;
    }

    // ========================================================================
    // ======================= PERIODIC SYNCHRONIZATION =======================
    // ========================================================================

    @Override
    public List<DokumentDetailidega> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        long startTime = System.currentTimeMillis();

        Date modifiedDateBegin = getDate(perioodiAlgusKuupaev);
        Date modifiedDateEnd = getDate(perioodiLoppKuupaev);

        List<DokumentDetailidega> list;

        if (modifiedDateBegin == null || modifiedDateEnd == null) {
            list = Collections.emptyList();
        } else {
            List<Document> docs = documentSearchService.searchAdrDocuments(modifiedDateBegin, modifiedDateEnd);
            list = new ArrayList<DokumentDetailidega>(docs.size());
            for (Document doc : docs) {
                DokumentDetailidega dokument = buildDokumentDetailidega(doc, false, true);
                if (StringUtils.isNotEmpty(dokument.getViit()) && dokument.getRegistreerimiseAeg() != null) {
                    list.add(dokument);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("ADR koikDokumendidLisatudMuudetud finished, time " + (System.currentTimeMillis() - startTime)
                    + " ms, results " + list.size() + ", arguments:\n    perioodiAlgusKuupaev=" + perioodiAlgusKuupaev + "\n    perioodiLoppKuupaev="
                    + perioodiLoppKuupaev);
        }
        return list;
    }

    @Override
    public List<Dokument> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev) {
        long startTime = System.currentTimeMillis();

        Date deletedDateBegin = getDate(perioodiAlgusKuupaev);
        Date deletedDateEnd = getDate(perioodiLoppKuupaev);

        List<Dokument> list;

        if (deletedDateBegin == null || deletedDateEnd == null) {
            list = Collections.emptyList();
        } else {
            List<Document> existingDocs = documentSearchService.searchAdrDocuments(deletedDateBegin, deletedDateEnd);
            Set<AdrDeletedDocument> existingDocsSet = new HashSet<AdrDeletedDocument>(existingDocs.size());
            for (Document document : existingDocs) {
                existingDocsSet.add(new AdrDeletedDocument(document.getRegNumber(), document.getRegDateTime()));
            }

            List<NodeRef> deletedDocs = documentSearchService.searchAdrDeletedDocuments(deletedDateBegin, deletedDateEnd);
            Set<AdrDeletedDocument> deletedDocsSet = new HashSet<AdrDeletedDocument>(deletedDocs.size());
            for (NodeRef deletedDoc : deletedDocs) {
                Map<QName, Serializable> props = nodeService.getProperties(deletedDoc);
                String regNumber = (String) props.get(AdrModel.Props.REG_NUMBER);
                Date regDateTime = (Date) props.get(AdrModel.Props.REG_DATE_TIME);
                if (StringUtils.isEmpty(regNumber) || regDateTime == null) {
                    continue; // should not happen!
                }
                deletedDocsSet.add(new AdrDeletedDocument(regNumber, regDateTime));
            }

            deletedDocsSet.removeAll(existingDocsSet);

            list = new ArrayList<Dokument>(deletedDocsSet.size());
            for (AdrDeletedDocument doc : deletedDocsSet) {
                Dokument dokument = new Dokument();
                dokument.setViit(doc.regNumber);
                dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.regDateTime));
                list.add(dokument);
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
        addDeletedDocument(regNumber, regDateTime);
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

    // END: getters / setters
}
