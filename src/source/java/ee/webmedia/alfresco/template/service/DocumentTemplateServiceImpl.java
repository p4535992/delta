package ee.webmedia.alfresco.template.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;

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
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.util.Assert;

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
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateServiceImpl implements DocumentTemplateService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTemplateServiceImpl.class);
    
    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final String REGEXP_PATTERN = "\\{[^\\}]+\\}";

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
    public void updateGeneratedFilesOnRegistration(Node document) {
        List<FileInfo> files = fileFolderService.listFiles(document.getNodeRef());
        log.debug("Found " + files.size() + "files under document " + document.getNodeRefAsString());
        for(FileInfo file : files) {
            if(file.getProperties().get(ee.webmedia.alfresco.document.file.model.File.GENERATED) != null) {
                Map<QName, Serializable> docProp = nodeService.getProperties(document.getNodeRef());
                ContentReader templateReader = fileFolderService.getReader(file.getNodeRef());
                
                // Set document content's mimetype and encoding from template
                ContentWriter documentWriter = fileFolderService.getWriter(file.getNodeRef());
                documentWriter.setMimetype(templateReader.getMimetype());
                documentWriter.setEncoding(templateReader.getEncoding());
                
                try {
                    replace(templateReader, documentWriter, docProp);
                } catch (Exception e) {
                    log.error("Replacing failed!", e);
                    // Clean up and inform the dialog
                    throw new RuntimeException();
                }
                
            }
        }
    }

    @Override
    public DocumentTemplate getDocumentsTemplate(NodeRef document) {
        QName documentTypeId = nodeService.getType(document);
        // it's OK to pick first one
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOC_TYPE)) {
                QName docQName = QName.createQName(nodeService.getProperty(fi.getNodeRef(), DocumentTemplateModel.Prop.DOCTYPE_ID).toString());
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
        if (docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME) != null) {
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
        nodeService.setProperty(populatedTemplate.getNodeRef(), ee.webmedia.alfresco.document.file.model.File.GENERATED, true); // Set generated flag so we can process it during document registration
        log.debug("Created new node: " + populatedTemplate.getNodeRef() + "\nwith name: " + name);
        // Set document content's mimetype and encoding from template
        ContentWriter documentWriter = fileFolderService.getWriter(populatedTemplate.getNodeRef());
        documentWriter.setMimetype(templateReader.getMimetype());
        documentWriter.setEncoding(templateReader.getEncoding());

        try {
            replace(templateReader, documentWriter, docProp);
        } catch (Exception e) {
            log.error("Replacing failed!", e);
            
            // XXX TODO INVESTIGATE TRANSACTIONS: 
            // There might be some problems here. Exception should roll back the transaction and so file creations shouldn't be commited anyway.
            
            // Clean up and inform the dialog
            fileFolderService.delete(populatedTemplate.getNodeRef());
            throw new RuntimeException();
        }
    }

    public String getProcessedEmailTemplate(NodeRef document, NodeRef template) {
        StringBuffer result = new StringBuffer();
        Map<QName, Serializable> properties = nodeService.getProperties(document);
        ContentReader templateReader = fileFolderService.getReader(template);
        String templateTxt = templateReader.getContentString();
        Pattern pattern = Pattern.compile(REGEXP_PATTERN);
        Matcher matcher = pattern.matcher(templateTxt);
        while (matcher.find()) {
            String formula = matcher.group();
            String formulaResult = escapeXml(getReplaceString(formula, properties));
            matcher.appendReplacement(result, formulaResult);
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    private void replace(ContentReader reader, ContentWriter writer, Map<QName, Serializable> properties) throws Exception {

        // create temporary file to replace from
        File tempFromFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "." + mimetypeService.getExtension(reader.getMimetype()));
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
        xSearchDescriptor.setSearchString(REGEXP_PATTERN);
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
        File tempToFile = TempFileProvider.createTempFile("DTSP-" + java.util.Calendar.getInstance().getTimeInMillis(), "." + mimetypeService.getExtension(reader.getMimetype()));

        storable.storeToURL(toUrl(tempToFile), storeProps); // Second replacing run requires new URL
        log.debug("New URL is " + storable.getLocation() + "(old was " + url + ")");

        writer.putContent(tempToFile);
    }

    private String getReplaceString(String foundPattern, Map<QName, Serializable> properties) {
        String pattern = foundPattern.substring(1, foundPattern.length() - 1);
        for (QName key : properties.keySet()) {
            Serializable prop = properties.get(key);
            if (key.getLocalName().equals(pattern) && prop != null) {
                log.debug("Found property from document node: " + key);
                if (prop instanceof String && StringUtils.isNotBlank((String) prop))
                    return (String) prop;

                if (prop instanceof ArrayList<?>) {
                    List<?> list = (ArrayList<?>) prop;
                    String separator = ", ";

                    if (key.getLocalName().equals("recipientName"))
                        separator = "\r";

                    if(list.size() > 0 && list.get(0) != null && list.get(0) instanceof Date) {
                        List<String> dates = new ArrayList<String>(list.size());
                        for(int i = 0; i < list.size(); i++) {
                            dates.add(DateFormatUtils.format((Date) list.get(i), DATE_FORMAT));
                        }
                        return StringUtils.join(dates.iterator(), separator);
                    } else if (list.size() > 0 && list.get(0) instanceof String) {
                        if (StringUtils.isNotBlank(list.get(0).toString()))
                            return StringUtils.join(list.iterator(), separator);
                    }
                    
                }

                if (prop instanceof Date)
                    return DateFormatUtils.format((Date) prop, DATE_FORMAT);

                if (prop instanceof Double)
                    return ((Double) prop).toString();
                
                if (prop instanceof Integer)
                    return Integer.toString(Integer.parseInt(prop.toString()));
            }
        }
        return checkSpecificPattern(foundPattern, properties);
    }

    private String checkSpecificPattern(String foundPattern, Map<QName, Serializable> properties) {
        String pattern = foundPattern.substring(1, foundPattern.length() - 1);

        NodeRef document = new NodeRef(generalService.getStore(), properties.get(ContentModel.PROP_NODE_UUID).toString());

        if (pattern.equals("functionTitle"))
            return getAncestorProperty(foundPattern, document, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.TITLE);
        if (pattern.equals("functionMark"))
            return getAncestorProperty(foundPattern, document, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.MARK);
        if (pattern.equals("seriesTitle"))
            return getAncestorProperty(foundPattern, document, SeriesModel.Types.SERIES, SeriesModel.Props.TITLE);
        if (pattern.equals("seriesIdentifier"))
            return getAncestorProperty(foundPattern, document, SeriesModel.Types.SERIES, SeriesModel.Props.SERIES_IDENTIFIER);
        if (pattern.equals("volumeTitle"))
            return getAncestorProperty(foundPattern, document, VolumeModel.Types.VOLUME, VolumeModel.Props.TITLE);
        if (pattern.equals("volumeMark"))
            return getAncestorProperty(foundPattern, document, VolumeModel.Types.VOLUME, VolumeModel.Props.MARK);

        if (pattern.equals("recipientNameEmail")) {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) properties.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) properties.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            return generateNameAndEmail(names, emails); // No need to check against null, common property
        }

        if (pattern.equals("additionalRecipientNameEmail")) {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) properties.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) properties.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL);
            return generateNameAndEmail(names, emails);
        }

        if (pattern.equals("vacationOrderSubstitutionData")
                && (nodeService.hasAspect(document, DocumentSpecificModel.Aspects.VACATION_ORDER) || nodeService.hasAspect(document,
                DocumentSpecificModel.Aspects.VACATION_ORDER))) {
            return getVacationOrderSubstitutionData(properties);
        }

        return foundPattern;
    }

    /**
     * @param properties
     * @return
     */
    private String getVacationOrderSubstitutionData(Map<QName, Serializable> properties) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) properties.get(DocumentSpecificModel.Props.SUBSTITUTE_NAME);
        @SuppressWarnings("unchecked")
        List<Date> startDate = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE);
        @SuppressWarnings("unchecked")
        List<Date> endDate = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE);
        String until = MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_until");

        List<String> substitutes = new ArrayList<String>(names.size());
        for (int i = 0; i < names.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(names.get(i))
                    .append(" ")
                    .append(DateFormatUtils.format(startDate.get(i), DATE_FORMAT))
                    .append(" ")
                    .append(until)
                    .append(" ")
                    .append(DateFormatUtils.format(endDate.get(i), DATE_FORMAT));
            substitutes.add(sb.toString());
        }
        return StringUtils.join(substitutes.iterator(), "\r");
    }

    private String generateNameAndEmail(List<String> names, List<String> emails) {
        int size = 0;
        if (names != null) {
            size = names.size();
        } else if (emails != null) {
            size = emails.size();
        }
        List<String> rows = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            String name = null;
            if (names != null && i < names.size()) {
                name = names.get(i);
            }
            String email = null;
            if (emails != null && i < emails.size()) {
                email = emails.get(i);
            }
            String row = name;
            if (StringUtils.isNotBlank(email)) {
                row += " (" + email + ")";
            }
            if (!StringUtils.isBlank(row)) {
                rows.add(row);
            }
        }
        return StringUtils.join(rows.iterator(), "\r"); // New paragraph because justified text screws up the layout when \n is used.

    }

    /**
     * @param foundPattern
     * @param document
     * @return
     */
    private String getAncestorProperty(String foundPattern, NodeRef document, QName ancestorType, QName property) {
        Node parent = generalService.getAncestorWithType(document, ancestorType);
        return (parent != null) ? nodeService.getProperty(parent.getNodeRef(), property).toString() : foundPattern;
    }

    private XComponent loadComponent(String loadUrl) throws IOException, IllegalArgumentException {
        XComponentLoader desktop = openOfficeConnection.getDesktop();
        PropertyValue[] loadProps = new PropertyValue[1];
        loadProps[0] = new PropertyValue();
        loadProps[0].Name = "Hidden";
        loadProps[0].Value = Boolean.TRUE;
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
        for (FileInfo fi : templateFiles) {
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

    @Override
    public List<DocumentTemplate> getDocumentTemplates(QName docType) {
        Assert.notNull(docType, "Parameter docType is mandatory.");
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (docType.equals(template.getDocTypeId())) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<DocumentTemplate> getEmailTemplates() {
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (template.getDocTypeId() == null) {
                result.add(template);
            }
        }
        return result;
    }
    
    private String escapeXml(String replaceString) {
        replaceString = replaceString.replaceAll("&", "&amp;");
        replaceString = replaceString.replaceAll("\"", "&quot;");
        replaceString = replaceString.replaceAll("<", "&lt;");
        replaceString = replaceString.replaceAll(">", "&gt;");
        replaceString = replaceString.replaceAll("'", "&apos;");
        return replaceString;
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
