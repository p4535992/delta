package ee.webmedia.alfresco.template.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XRefreshable;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateServiceImpl implements DocumentTemplateService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTemplateServiceImpl.class);
    
    private static String DATE_FORMAT = "dd.MM.yyyy";

    private GeneralService generalService;
    private NodeService nodeService;
    private FileService fileService;
    private MimetypeService mimetypeService;
    private FileFolderService fileFolderService;
    private OpenOfficeConnection openOfficeConnection;

    private static BeanPropertyMapper<DocumentTemplate> templateBeanPropertyMapper;
    static {
        templateBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentTemplate.class);
    }

    @Override
    public DocumentTemplate getDocumentsTemplate(NodeRef document) {
        QName documentTypeId = nodeService.getType(document);
        // it's OK to pick first one
        for(FileInfo fi : fileFolderService.listFiles(getRoot())){
            if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOC_TYPE)) {
                QName docQName = QName.createQName((String) nodeService.getProperty(fi.getNodeRef(), DocumentTemplateModel.Prop.DOCTYPE_ID));
                if (docQName.equals(documentTypeId)) {
                    return setupDocumentTemplate(fi);
                }
            }
        }
        return null;
    }

    /**
     * Sets the properties, adds download URL and NodeRef
     * @param fileInfo
     * @return
     */
    private DocumentTemplate setupDocumentTemplate(FileInfo fileInfo) {
        DocumentTemplate dt = templateBeanPropertyMapper.toObject(nodeService.getProperties(fileInfo.getNodeRef()), null);
        dt.setNodeRef(fileInfo.getNodeRef());
        dt.setDownloadUrl(fileService.generateURL(fileInfo.getNodeRef()));
        return dt;
    }

    @Override
    public void populateTemplate(NodeRef documentNodeRef) throws FileNotFoundException {

        log.debug("Processing document: " + documentNodeRef);
        Map<QName, Serializable> docProp = nodeService.getProperties(documentNodeRef);
        // Set document filename's extension from template
        String docName = FilenameUtil.stripForbiddenWindowsCharacters((String) docProp.get(DocumentCommonModel.Props.DOC_NAME));
        int i = 1;
        while (fileFolderService.searchSimple(documentNodeRef, docName + "." + mimetypeService.getExtension(MimetypeMap.MIMETYPE_WORD)) != null) {
            if (i > 1) {
                docName = docName.substring(0, docName.lastIndexOf("(") - 1);
            }
            docName = docName.concat(" (" + i + ")");
            i++;
        }
        String name = FilenameUtil.buildFileName(docName, mimetypeService.getExtension(MimetypeMap.MIMETYPE_WORD));

        String templName = "";
        if(docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME) != null) {
            templName = (String) docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME);
        }
        NodeRef nodeRef;
        if (StringUtils.isEmpty(templName)) {
            // No template specified, try to use default, if any
            // NOTE: we don't need to check for null, because in that case the button triggering this action isn't shown
            log.debug("Document template not specified, looking for default template! Document: " + documentNodeRef);
            nodeRef = getDocumentsTemplate(documentNodeRef).getNodeRef();
        } else {
            nodeRef = getTemplateByName(templName).getNodeRef();
        }
        log.debug("Using template: " + nodeRef);
        
        ContentReader templateReader = fileFolderService.getReader(nodeRef);
        
        ee.webmedia.alfresco.document.file.model.File populatedTemplate = new ee.webmedia.alfresco.document.file.model.File(fileFolderService.create(
                documentNodeRef, name, ContentModel.TYPE_CONTENT));
        log.debug("Created new node: " + populatedTemplate.getNodeRef() + "\nwith name: " + name);
        // Set document content's mimetype and encoding from template
        ContentWriter documentWriter = fileFolderService.getWriter(populatedTemplate.getNodeRef());
        documentWriter.setMimetype(templateReader.getMimetype());
        documentWriter.setEncoding(templateReader.getEncoding());

        try {
            replace(templateReader, documentWriter, docProp);
        } catch (Exception e) {
            log.error("Replacing failed!", e);
            // Clean up and inform the dialog
            fileFolderService.delete(populatedTemplate.getNodeRef());
            throw new RuntimeException();
        } 
    }

    private void replace(ContentReader reader, ContentWriter writer, Map<QName, Serializable> properties) throws Exception {

        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DocumentTemplateServicePopulator-", "." + mimetypeService.getExtension(reader.getMimetype()));
        // download the content from the source reader
        reader.getContent(tempFromFile);

        String url = toUrl(tempFromFile);
        log.debug("Loading data from URL: " + url);
        XComponent xComponent = loadComponent(url);

        XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);

        XSearchDescriptor xSearchDescriptor;
        XSearchable xSearchable = null;

        xSearchable = (XSearchable) UnoRuntime.queryInterface(XSearchable.class, xTextDocument);

        // You need a descriptor to set properies for Replace
        xSearchDescriptor = xSearchable.createSearchDescriptor();
        xSearchDescriptor.setPropertyValue("SearchRegularExpression", Boolean.TRUE);
        // Set the properties the replace method need
        xSearchDescriptor.setSearchString("\\{[^\\}]+\\}");
        XIndexAccess findAll = xSearchable.findAll(xSearchDescriptor);
        log.debug("Found " + findAll.getCount() + " pattern matches");
        for (int i = 0; i < findAll.getCount(); i++) {
            Object byIndex = findAll.getByIndex(i);
            XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, byIndex);
            xTextRange.setString(getReplaceString(xTextRange.getString(), properties));
        }

        XRefreshable refreshable = (XRefreshable) UnoRuntime.queryInterface(XRefreshable.class, xComponent);
        if (refreshable != null) {
            refreshable.refresh();
        }
        log.debug("Temporary file size is: " + tempFromFile.length());
        XStorable storable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xComponent);
        PropertyValue[] storeProps = new PropertyValue[0];
        storable.storeToURL(url, storeProps);
        log.debug("New URL is " + storable.getLocation() + "(old was " + url + ")");

        writer.putContent(tempFromFile);
    }

    private String getReplaceString(String foundPattern, Map<QName, Serializable> properties) {
        String pattern = foundPattern.substring(1, foundPattern.length() - 1);
        for (QName key : properties.keySet()) {
            Serializable prop = properties.get(key);
            if (key.getLocalName().equals(pattern) && prop != null) {
                log.debug("Found property from document node: " + key);
                if(prop instanceof String && StringUtils.isNotBlank((String) prop)) {
                    return (String) prop;
                }
                if(prop instanceof ArrayList<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> list = (ArrayList<String>) prop;
                    if(list.size() > 0 && StringUtils.isNotBlank(list.get(0))) {
                        StringBuilder sb = new StringBuilder();
                        for(String r : list) {
                            sb.append(r).append("\r"); // New paragraph because justified text screws up the layout when \n is used.
                        }
                        return sb.toString();
                    }
                }
                if(prop instanceof Date) {
                    return DateFormatUtils.format((Date) prop, DATE_FORMAT);
                }
                if(prop instanceof Double) {
                    return Double.toString((Double) prop);
                }
            }
        }
        return foundPattern;
    }

    private XComponent loadComponent(String loadUrl) throws IOException, IllegalArgumentException {
        XComponentLoader desktop = openOfficeConnection.getDesktop();
        PropertyValue[] loadProps = new PropertyValue[1];
        loadProps[0] = new PropertyValue();
        loadProps[0].Name = "Hidden";
        loadProps[0].Value = new Boolean(true);
        // load
        return desktop.loadComponentFromURL(loadUrl, "_blank", 0, loadProps);
    }

    private String toUrl(File file) {
        Object contentProvider = openOfficeConnection.getFileContentProvider();
        XFileIdentifierConverter fic = (XFileIdentifierConverter) UnoRuntime.queryInterface(XFileIdentifierConverter.class, contentProvider);
        return fic.getFileURLFromSystemPath("", file.getAbsolutePath());
    }

    public DocumentTemplate getTemplateByName(String name) throws FileNotFoundException {
        NodeRef nodeRef = fileFolderService.searchSimple(getRoot(), name);
        if (nodeRef == null) {
            throw new FileNotFoundException(name);
        }
        return setupDocumentTemplate(fileFolderService.getFileInfo(nodeRef));
    }

    @Override
    public List<DocumentTemplate> getTemplates() {
        List<FileInfo> templateFiles = fileFolderService.listFiles(getRoot());
        List<DocumentTemplate> templates = new ArrayList<DocumentTemplate>(templateFiles.size());
        for(FileInfo fi : templateFiles) {
            DocumentTemplate dt = setupDocumentTemplate(fi);
            if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOC_TYPE)) {
                NodeRef docType = generalService.getNodeRef(DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/" + dt.getDocTypeId());
                if (docType != null) {
                    dt.setDocTypeName((String) nodeService.getProperty(docType, DocumentTypeModel.Props.NAME));
                    templates.add(dt);
                }
            }
            /** Email templates do not have TEMPLATE_DOC_TYPE aspect */
            else {
                dt.setDocTypeName("");
                templates.add(dt);
            }
        }
        return templates;
    }

    @Override
    public NodeRef getRoot() {
        return generalService.getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setOpenOfficeConnection(OpenOfficeConnection openOfficeConnection) {
        this.openOfficeConnection = openOfficeConnection;
    }
    // END: getters / setters
}
