package ee.webmedia.alfresco.adr.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.adr.Dokument;
import ee.webmedia.alfresco.adr.DokumentDetailidega;
import ee.webmedia.alfresco.adr.Fail;
import ee.webmedia.alfresco.adr.FailSisuga;
import ee.webmedia.alfresco.adr.util.ContentReaderDataSource;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;

public class AdrServiceImpl implements AdrService {
    
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AdrServiceImpl.class);

    private DocumentSearchService documentSearchService;
    private FileService fileService;
    private FileFolderService fileFolderService;
    private DocumentTypeService documentTypeService;
    private DocumentService documentService;
    
    private DatatypeFactory datatypeFactory;
    
    public AdrServiceImpl() {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik,
            String otsingusona) {
        if (log.isDebugEnabled()) {
            log.debug("ADR otsiDokumendid begin: dokumendiLiik = " + dokumendiLiik + ", otsingusona = " + otsingusona);
        }
        
        Date beginDate = null;
        if (perioodiAlgusKuupaev != null) {
            beginDate = perioodiAlgusKuupaev.toGregorianCalendar().getTime();
        }
        
        Date endDate = null;
        if (perioodiLoppKuupaev != null) {
            endDate = perioodiLoppKuupaev.toGregorianCalendar().getTime();
        }
        
        List<Document> searchDocumentsADR = new ArrayList<Document>(0);
        QName docType = getTypeQName(dokumendiLiik);
        if (docType != null || StringUtils.isEmpty(dokumendiLiik)) {
            // when dokumendiLiik is empty, return any type  
            searchDocumentsADR = documentSearchService.searchDocumentsADR(beginDate, endDate, docType, otsingusona, new AccessRestriction[] {
                    AccessRestriction.AK, AccessRestriction.OPEN });
        }
        
        List<Dokument> list = new ArrayList<Dokument>();
        for (Document doc : searchDocumentsADR) {
            boolean isIncomingLetter = DocumentSubtypeModel.Types.INCOMING_LETTER.equals(doc.getDocumentType().getId());
            
            Dokument dokument = new Dokument();
            dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
            dokument.setViit(doc.getRegNumber());
            dokument.setDokumendiLiik(doc.getDocumentTypeName());
            dokument.setSaatja(isIncomingLetter ? doc.getSender() : null);
            dokument.setSaaja(isIncomingLetter ? (doc.getOwnerName() + " (" + doc.getOwnerOrgStructUnit() + ")") : null);
            dokument.setPealkiri(doc.getDocName());
            
            list.add(dokument);
        }
        
        log.debug("ADR otsiDokumendid: finished");
        return list;
    }
    
    private QName getTypeQName(String dokumendiLiik) {
        List<DocumentType> allDocumentTypes = documentTypeService.getAllDocumentTypes();
        for (DocumentType type : allDocumentTypes) {
            if (type.getName().equals(dokumendiLiik)) {
                return type.getId();
            }
        }
        return null;
    }

    @Override
    public List<Dokument> otsiDokumendidSamasTeemas(String viit, XMLGregorianCalendar registreerimiseAeg) {
        if (log.isDebugEnabled()) {
            log.debug("ADR dokumentDetailidega begin: viit = " + viit);
        }
        
        Date regDate = null;
        if (registreerimiseAeg != null) {
            regDate = registreerimiseAeg.toGregorianCalendar().getTime();
        }
        
        List<Document> foundDocs = documentSearchService.searchDocumentDetailsADR(viit, regDate, new AccessRestriction[] {AccessRestriction.AK, AccessRestriction.OPEN});
        List<Dokument> list = new ArrayList<Dokument>();
        for (Document baseDoc : foundDocs) {
            List<Document> docs = documentService.getReplyOrFollowUpDocuments(baseDoc.getNode().getNodeRef());
            for (Document doc : docs) {
                if (DocumentStatus.FINISHED.equals(doc.getDocStatus()) &&
                    !AccessRestriction.INTERNAL.equals(doc.getAccessRestriction()) &&
                    StringUtils.isNotBlank(doc.getRegNumber())) {
                    
                    boolean isIncomingLetter = DocumentSubtypeModel.Types.INCOMING_LETTER.equals(doc.getDocumentType().getId());
                    Dokument dokument = new Dokument();
                    dokument.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
                    dokument.setViit(doc.getRegNumber());
                    dokument.setDokumendiLiik(doc.getDocumentTypeName());
                    dokument.setSaatja(isIncomingLetter ? doc.getSender() : null);
                    dokument.setSaaja(isIncomingLetter ? (doc.getOwnerName() + " (" + doc.getOwnerOrgStructUnit() + ")") : null);
                    dokument.setPealkiri(doc.getDocName());
                    
                    list.add(dokument);
                }
            }
        }
        
        log.debug("ADR otsiDokumendid: finished");
        return list;
    }
    
    @Override
    public DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg) {
        if (log.isDebugEnabled()) {
            log.debug("ADR dokumentDetailidega begin: viit = " + viit);
        }
        
        Date regDate = null;
        if (registreerimiseAeg != null) {
            regDate = registreerimiseAeg.toGregorianCalendar().getTime();
        }
        
        DokumentDetailidega dokumentDetailidega = new DokumentDetailidega();
        List<Document> docs = documentSearchService.searchDocumentDetailsADR(viit, regDate, new AccessRestriction[] {AccessRestriction.AK, AccessRestriction.OPEN});
        if (docs.size() == 0) {
            return dokumentDetailidega;
        }
        Document doc = docs.get(0);
        boolean isIncomingLetter = DocumentSubtypeModel.Types.INCOMING_LETTER.equals(doc.getDocumentType().getId());
        
        dokumentDetailidega.setViit(doc.getRegNumber());
        dokumentDetailidega.setRegistreerimiseAeg(convertToXMLGergorianCalendar(doc.getRegDateTime()));
        dokumentDetailidega.setDokumendiLiik(doc.getDocumentTypeName());
        dokumentDetailidega.setSaatja(isIncomingLetter ? doc.getSender() : null);
        dokumentDetailidega.setSaaja(isIncomingLetter ? (doc.getOwnerName() + " (" + doc.getOwnerOrgStructUnit() + ")") : null);
        dokumentDetailidega.setPealkiri(doc.getDocName());
        dokumentDetailidega.setJuurdepaasuPiirang(doc.getAccessRestriction());
        dokumentDetailidega.setJuurdepaasuPiiranguAlus(doc.getAccessRestrictionReason());
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionBeginDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(convertToXMLGergorianCalendar(doc.getAccessRestrictionEndDate()));
        dokumentDetailidega.setJuurdepaasuPiiranguLopp(doc.getAccessRestrictionEndDesc());
        if (isIncomingLetter) {
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
        } else {
            dokumentDetailidega.setTahtaeg(null);
        }
        dokumentDetailidega.setVastamiseKuupaev(isIncomingLetter ? convertToXMLGergorianCalendar(doc.getComplienceDate()) : null);
        dokumentDetailidega.setKoostaja(isIncomingLetter ? null : (doc.getOwnerName() + " (" + doc.getOwnerOrgStructUnit() + ")"));
        dokumentDetailidega.setAllkirjastaja(doc.getSignerName());
        
        List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNode().getNodeRef());
        for (File file : allActiveFiles) {
            Fail fail = new Fail();
            fail.setFailinimi(file.getName());
            fail.setSuurus((int) file.getSize());
            fail.setEncoding(file.getEncoding());
            fail.setMimeType(file.getMimeType());

            dokumentDetailidega.getFail().add(fail);
        }

        log.debug("ADR dokumentDetailidega: finished");
        return dokumentDetailidega;
    }
    
    @Override
    public FailSisuga failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename) {
        if (log.isDebugEnabled()) {
            log.debug("ADR failSisuga begin: viit = " + viit + ", filename = " + filename);
        }
        
        Date regDate = null;
        if (registreerimiseAeg != null) {
            regDate = registreerimiseAeg.toGregorianCalendar().getTime();
        }
        
        FailSisuga failSisuga = new FailSisuga();
        List<Document> docs = documentSearchService.searchDocumentDetailsADR(viit, regDate, new AccessRestriction[] {AccessRestriction.OPEN});
        if (docs.size() == 0) {
            return failSisuga;
        }
        
        Document doc = docs.get(0);
        List<File> allActiveFiles = fileService.getAllActiveFiles(doc.getNode().getNodeRef());
        for (File file : allActiveFiles) {
            if (file.getName().equals(filename)) {
                failSisuga.setFailinimi(file.getName());
                failSisuga.setSuurus((int) file.getSize());
                failSisuga.setEncoding(file.getEncoding());
                failSisuga.setMimeType(file.getMimeType());
                failSisuga.setSisu(getFileDataHandler(file.getNodeRef(), filename));
                break;
            }
        }
        
        log.debug("ADR failSisuga: finished");
        return failSisuga;
    }
    
    private DataHandler getFileDataHandler(NodeRef nodeRef, String filename) {
        ContentReader fileReader = fileFolderService.getReader(nodeRef);
        ContentReaderDataSource dataSource = new ContentReaderDataSource(fileReader, filename);
        return new DataHandler(dataSource);
    }
    
    private XMLGregorianCalendar convertToXMLGergorianCalendar(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return datatypeFactory.newXMLGregorianCalendar(cal);
    }
    
    // START: getters / setters

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }
    
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
