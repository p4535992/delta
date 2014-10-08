<<<<<<< HEAD
package ee.webmedia.alfresco.template.service;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import ee.webmedia.alfresco.archivals.model.ActivityFileType;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.OpenOfficeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.mso.service.MsoService;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.template.exception.ExistingFileFromTemplateException;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.web.AddDocumentTemplateDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.ISOLatin1Util;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateServiceImpl implements DocumentTemplateService, ServletContextAware {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTemplateServiceImpl.class);

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");
    private static final String SEPARATOR = ".";
    private static final Pattern TEMPLATE_FORMULA_GROUP_PATTERN = Pattern.compile(OpenOfficeService.REGEXP_GROUP_PATTERN);
    private static final Pattern TEMPLATE_FORMULA_PATTERN = Pattern.compile(OpenOfficeService.REGEXP_PATTERN);
    public static final QName TEMP_PROP_FILE_NAME_BASE = RepoUtil.createTransientProp("fileNameBase");
    public static final QName TEMP_PROP_FILE_NAME_EXTENSION = RepoUtil.createTransientProp("fileNameExtension");

    private GeneralService generalService;
    private NodeService nodeService;
    private FileService fileService;
    private MimetypeService mimetypeService;
    private FileFolderService fileFolderService;
    private OpenOfficeService openOfficeService;
    private DictionaryService dictionaryService;
    private MsoService msoService;
    private ApplicationService applicationService;
    private ServletContext servletContext;
    private UserService userService;
    private DocumentConfigService documentConfigService;
    private DocumentAdminService documentAdminService;
    private VersionsService versionsService;
    private DocumentLogService documentLogService;
    private WorkflowService workflowService;

    private static BeanPropertyMapper<DocumentTemplate> templateBeanPropertyMapper;
    static {
        templateBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentTemplate.class);
    }

    @Override
    public void updateGeneratedFiles(NodeRef docRef, boolean isRegistering) {
        // NB! msoAvailable comes from application configuration, not from actual availability.
        // So if mso is configured to be available, but actually is not, then file generation will throw error.
        // If mso is configured as unavailable, mso file generation is skipped (up to admins to deal with these situations).
        // As oo service is used for file indexing, it must always be available, so it is not checked for availability here
        // and oo file generation may throw error if oo service is actually unavailable.
        boolean msoAvailable = msoService.isAvailable();

        // Shortcuts
        if (!isRegistering && !Boolean.TRUE.equals(nodeService.getProperty(docRef, DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES))) {
            return;
        }

        List<FileInfo> files = fileFolderService.listFiles(docRef);
        log.debug("Found " + files.size() + " files under document " + docRef);
        for (FileInfo file : files) {
            // If generator service isn't available, then skip this file
            String generationType = (String) file.getProperties().get(FileModel.Props.GENERATION_TYPE);
            if (GeneratedFileType.WORD_TEMPLATE.name().equals(generationType) && !msoAvailable) {
                continue;
            }
            if (StringUtils.isNotBlank((String) file.getProperties().get(FileModel.Props.GENERATED_FROM_TEMPLATE))
                    || Boolean.TRUE.equals(file.getProperties().get(FileModel.Props.UPDATE_METADATA_IN_FILES))) {

                replaceFormulas(docRef, file.getNodeRef(), file.getNodeRef(), file.getName(), isRegistering);
            }
        }
    }

    @Override
    public void updateDocTemplate(Node docTemplNode) {
        Map<String, Object> properties = docTemplNode.getProperties();
        String oldName = (String) properties.get(DocumentTemplateModel.Prop.NAME.toString());
        String newName = StringUtils.strip((String) properties.get(TEMP_PROP_FILE_NAME_BASE.toString())) + properties.get(TEMP_PROP_FILE_NAME_EXTENSION.toString());
        for (DocumentTemplate documentTemplate : getTemplates()) {
            String nameForCheck = documentTemplate.getName();
            if (!StringUtils.equals(nameForCheck, oldName) && StringUtils.equals(nameForCheck, newName)) {
                throw new UnableToPerformException(AddDocumentTemplateDialog.ERR_EXISTING_FILE, newName);
            }
        }
        properties.put(DocumentTemplateModel.Prop.NAME.toString(), newName);
        NodeRef docTemplateNodeRef = docTemplNode.getNodeRef();
        generalService.setPropertiesIgnoringSystem(docTemplateNodeRef, properties);
        nodeService.setProperty(docTemplateNodeRef, ContentModel.PROP_NAME, newName);
    }

    @Override
    public DocumentTemplate getDocumentsTemplate(NodeRef document) {
        String documentTypeId = (String) nodeService.getProperty(document, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        // it's OK to pick first one
        FileInfo word2003OrOOTemplate = null;
        boolean msoAvailable = msoService.isAvailable();
        boolean ooAvailable = openOfficeService.isAvailable();
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (!nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                continue;
            }

            if (!documentTypeId.equals(fi.getProperties().get(DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                continue;
            }

            // If current file is 2007/2010 template, return the first one found
            if (StringUtils.endsWithIgnoreCase(fi.getName(), ".dotx") && msoAvailable) {
                return setupDocumentTemplate(fi);
            }

            // Otherwise mark it as a candidate if only 2003 binary or OpenOffice template is present
            word2003OrOOTemplate = fi;
        }
        if (word2003OrOOTemplate != null) {
            String ext = getExtension(word2003OrOOTemplate.getName());
            if (equalsIgnoreCase("ott", ext) && ooAvailable || equalsIgnoreCase("dot", ext) && msoAvailable) {
                return setupDocumentTemplate(word2003OrOOTemplate);
            }
        }

        return null;
    }

    @Override
    public boolean hasDocumentsTemplate(NodeRef document) {
        String documentTypeId = (String) nodeService.getProperty(document, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (!nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                continue;
            }
            if (!documentTypeId.equals(fi.getProperties().get(DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                continue;
            }
            String fileNameExtension = FilenameUtils.getExtension((String) fi.getProperties().get(DocumentTemplateModel.Prop.NAME));
            Assert.isTrue(StringUtils.equals("dotx", fileNameExtension) || StringUtils.equals("dot", fileNameExtension) || StringUtils.equals("ott", fileNameExtension));
            return true;
        }
        return false;
    }

    /**
     * Sets the properties, adds download URL and NodeRef
     * 
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
    public Map<String, String> getDefaultFieldValues(NodeRef fileNodeRef) throws Exception {
        Assert.notNull(fileNodeRef, "NodeRef is mandatory");
        if (!openOfficeService.isAvailable()) {
            throw new UnableToPerformException("template_add_oo_service_unavailable");
        }
        NodeRef templateNodeRef = null;
        if (nodeService.hasAspect(fileNodeRef, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)
                || nodeService.hasAspect(fileNodeRef, DocumentTemplateModel.Aspects.TEMPLATE_ARCHIVAL_REPORT)) {
            templateNodeRef = fileNodeRef;
        } else {
            String genTemplName = (String) nodeService.getProperty(fileNodeRef, FileModel.Props.GENERATED_FROM_TEMPLATE);
            templateNodeRef = getTemplateByName(genTemplName).getNodeRef();
        }
        return openOfficeService.getUsedFormulasAndValues(fileFolderService.getReader(templateNodeRef));
    }

    @Override
    public String populateTemplate(NodeRef documentNodeRef, boolean overWritingGranted) throws FileNotFoundException {
        log.debug("Creating a file from template for document: " + documentNodeRef);
        boolean msoAvailable = msoService.isAvailable();
        boolean ooAvailable = openOfficeService.isAvailable();
        if (!msoAvailable && !ooAvailable) {
            throw new UnableToPerformException("document_errorMsg_template_processsing_failed_service_missing");
        }

        final Map<QName, Serializable> docProp = nodeService.getProperties(documentNodeRef);

        String templName = "";
        if (docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME) != null) {
            templName = (String) docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME);
        }
        NodeRef nodeRef;
        if (StringUtils.isEmpty(templName)) {
            // No template specified, try to use default, if any
            // NOTE: we don't need to check for null, because in that case the button triggering this action isn't shown
            log.debug("Document template not specified, looking for default template! Document: " + documentNodeRef);
            DocumentTemplate templ = getDocumentsTemplate(documentNodeRef);
            nodeRef = templ == null ? null : templ.getNodeRef();
        } else {
            nodeRef = getTemplateByName(templName).getNodeRef();
        }
        if (nodeRef == null) {
            throw new UnableToPerformException("document_errorMsg_template_not_found");
        }

        String templateFilename = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        boolean ooTemplate = "ott".equals(FilenameUtils.getExtension(templateFilename));
        if (ooTemplate && !ooAvailable || !ooTemplate && !msoAvailable) { // if it isn't OO template it must be Word template
            throw new UnableToPerformException("document_errorMsg_template_not_found_for_service");
        }

        log.debug("Using template: " + templateFilename);

        NodeRef existingGeneratedFile = null;
        String displayName = null;
        { // Check if we already have a file that is generated using this template and overwrite
            for (ChildAssociationRef caRef : nodeService.getChildAssocs(documentNodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL)) {
                NodeRef fileRef = caRef.getChildRef();
                String genTemplName = (String) nodeService.getProperty(fileRef, FileModel.Props.GENERATED_FROM_TEMPLATE);
                if (!StringUtils.equals(genTemplName, templateFilename)) {
                    continue;
                }

                if (!overWritingGranted) {
                    throw new ExistingFileFromTemplateException(templateFilename);
                }
                displayName = (String) nodeService.getProperty(fileRef, FileModel.Props.DISPLAY_NAME);
                existingGeneratedFile = fileRef;
                // Create new version for the file. Aspect must be added, so do this now (no point of checking if file already has it).
                versionsService.addVersionLockableAspect(existingGeneratedFile);
                versionsService.setVersionLockableAspect(existingGeneratedFile, false);
                versionsService.updateVersion(existingGeneratedFile, displayName, false);
                versionsService.updateVersionModifiedAspect(existingGeneratedFile);
                // Unlock the node here, since previous method locked it and there is no session (e.g. Word) that would unlock the file.
                versionsService.setVersionLockableAspect(existingGeneratedFile, false);

                break; // Assume there is only one file.
            }
        }

        if (existingGeneratedFile == null) { // Create the container for template content
            // XXX Alar: target file mimeType comes from MSO service, so it would be better to set file extension based on that mimeType
            String extension = mimetypeService.getExtension(templateFilename.endsWith(".ott") ? MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT : MimetypeMap.MIMETYPE_WORD);
            if (StringUtils.endsWithIgnoreCase(templateFilename, ".dotx")) {
                extension += "x"; // Well, ain't this nice! :) (Filename must end with docx if 2003/2007 template is used)
            }
            String name = ((String) docProp.get(DocumentCommonModel.Props.DOC_NAME));
            displayName = fileService.getUniqueFileDisplayName(documentNodeRef, name + "." + extension);
            name = FilenameUtil.replaceAmpersand(ISOLatin1Util.removeAccents(FilenameUtil.buildFileName(name, extension)));
            name = FilenameUtil.replaceNonAsciiCharacters(name);
            name = FilenameUtil.limitFileNameLength(name);
            name = generalService.getUniqueFileName(documentNodeRef, name);

            existingGeneratedFile = fileFolderService.create(documentNodeRef, name, ContentModel.TYPE_CONTENT).getNodeRef();

            Map<QName, Serializable> templateProps = new HashMap<QName, Serializable>(3);
            // Mark down the template that was used to generate the file
            templateProps.put(FileModel.Props.GENERATED_FROM_TEMPLATE, templateFilename);
            templateProps.put(FileModel.Props.GENERATION_TYPE, (ooTemplate ? GeneratedFileType.OPENOFFICE_TEMPLATE.name() : GeneratedFileType.WORD_TEMPLATE.name()));
            templateProps.put(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, Boolean.TRUE);
            // Set the display name so we can process it during document registration
            templateProps.put(FileModel.Props.DISPLAY_NAME, displayName);
            nodeService.addProperties(existingGeneratedFile, templateProps);
            versionsService.addVersionModifiedAspect(existingGeneratedFile);
            documentLogService.addDocumentLog(documentNodeRef, MessageUtil.getMessage("applog_doc_file_generated", displayName));
            log.debug("Created new node: " + existingGeneratedFile + "\nwith name: " + name + "; displayName: " + displayName);
        }

        // Set document content's MIME type and encoding from template
        replaceFormulas(documentNodeRef, nodeRef, existingGeneratedFile, templateFilename);
        return displayName;
    }

    @Override
    public void populateVolumeArchiveTemplate(NodeRef parentRef, List<NodeRef> volumeRefs, NodeRef templateRef) {
        if (!msoService.isAvailable()) {
            throw new UnableToPerformException("document_errorMsg_template_processsing_failed_service_missing");
        }
        // it is assumed that template name is fixed by application and thus it is not needed to check not allowed characters, length etc in filename
        String templateFilename = (String) nodeService.getProperty(templateRef, ContentModel.PROP_NAME);
        String filenameWithoutExtension = FilenameUtils.removeExtension(templateFilename);
        templateFilename = filenameWithoutExtension + " " + FastDateFormat.getInstance("yyyyMMdd").format(new Date()) + ".docx";
        NodeRef generatedFile = fileFolderService.create(parentRef, templateFilename, ContentModel.TYPE_CONTENT).getNodeRef();
        replaceFormulas(volumeRefs, templateRef, generatedFile, templateFilename, true);
        nodeService.setProperty(generatedFile, FileModel.Props.ACTIVITY_FILE_TYPE, ActivityFileType.GENERATED_DOCX.name());
    }

    private void replaceFormulas(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName) {
        replaceFormulas(document, sourceFile, destinationFile, sourceFileName, false);
    }

    @SuppressWarnings("unchecked")
    private void replaceFormulas(Object documentOrVolumeRefList, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName, boolean finalize) {
        Assert.isTrue(documentOrVolumeRefList instanceof NodeRef || documentOrVolumeRefList instanceof List);
        // Basically the same check is performed in MsoService#isFormulasReplaceable, but with MIME types
        boolean msoFile = StringUtils.endsWithIgnoreCase(sourceFileName, ".doc") || StringUtils.endsWithIgnoreCase(sourceFileName, ".docx")
                || StringUtils.endsWithIgnoreCase(sourceFileName, ".dot") || StringUtils.endsWithIgnoreCase(sourceFileName, ".dotx");
        boolean ooFile = StringUtils.endsWithIgnoreCase(sourceFileName, ".odt") || StringUtils.endsWithIgnoreCase(sourceFileName, ".ott");
        if (!msoFile && !ooFile) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_invalid_file_extension");
        }

        NodeRef document = null;
        Map<String, String> formulas = null;
        if (documentOrVolumeRefList instanceof NodeRef) {
            document = (NodeRef) documentOrVolumeRefList;
            formulas = getDocumentFormulas(document);
        } else {
            formulas = getArchiveFormulas((List<NodeRef>) documentOrVolumeRefList);
        }
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(formulas.entrySet()));
        }
        if (msoFile && msoService.isAvailable()) {
            if (finalize) {
                formulas.put("$FINALIZE", "1");
            }
            ContentReader documentReader = fileFolderService.getReader(sourceFile);
            ContentWriter documentWriter = fileFolderService.getWriter(destinationFile);
            try {
                msoService.replaceFormulas(formulas, documentReader, documentWriter);
            } catch (SOAPFaultException se) {
                String errorKey = "template_replace_formulas_failed";
                if (se.getMessage().contains("Err001")) {
                    errorKey += "_invalid_file_content";
                }
                throw new UnableToPerformException(MessageSeverity.ERROR, errorKey, se);
            } catch (Exception e) {
                throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_failed", e);
            }
        } else if (ooFile && openOfficeService.isAvailable()) {
            // generating document for volume list is not implemented for OpenOffice
            replaceFormulasWithOpenOffice(document, sourceFile, destinationFile, sourceFileName, formulas, finalize);
        }
    }

    private void replaceFormulasWithOpenOffice(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName, Map<String, String> formulas, boolean finalize) {
        int retry = 3;
        do {
            ContentReader reader = fileFolderService.getReader(sourceFile);
            ContentWriter writer = fileFolderService.getWriter(destinationFile);
            writer.setMimetype(MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT);

            try {
                openOfficeService.replace(reader, writer, formulas, finalize);
                retry = 0;
            } catch (OpenOfficeService.OpenOfficeReturnedNullInterfaceException e) {
                retry--;
                log.error("Replacing failed, OpenOffice error, retrying " + retry + " times more: " + e.getMessage() + "\n    fileName=" + sourceFileName
                        + "\n    reader=" + reader + "\n    writer=" + writer);
                if (retry <= 0) {
                    throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_failed", e);
                }
            } catch (Exception e) {
                log.error("Replacing failed!\n    fileName=" + sourceFileName + "\n    reader=" + reader + "\n    writer=" + writer, e);
                throw new RuntimeException(e);
            }
        } while (retry > 0);
    }

    @Override
    public String getProcessedVolumeDispositionTemplate(List<Volume> volumes, NodeRef template) {
        String templateText = fileFolderService.getReader(template).getContentString();
        StringBuilder sb = new StringBuilder();
        if (templateText.indexOf("{volumeDispositionDateNotificationData}") > -1) {
            for (Volume vol : volumes) {
                sb.append(vol.getVolumeMark())
                        .append(" ")
                        .append(vol.getTitle())
                        .append(" (")
                        .append(I18NUtil.getMessage("notification_dispostition_date"))
                        .append(": ")
                        .append(vol.getRetainUntilDate() == null ? "" : dateFormat.format(vol.getRetainUntilDate()))
                        .append(")")
                        .append("<br>\n");
            }
            if (sb.length() > 0) {
                templateText = templateText.replaceAll("\\{volumeDispositionDateNotificationData\\}", sb.toString());
            }
        }
        return templateText;
    }

    @Override
    public String getProcessedAccessRestrictionEndDateTemplate(List<Document> documents, NodeRef template) {
        String templateText = fileFolderService.getReader(template).getContentString();
        String endDateFormula = "{accessRestrEndDateNotificationData}";
        if (templateText.indexOf(endDateFormula) > -1) {
            StringBuilder debugInfo = new StringBuilder("AccessRestrictionEndDate date formatting:");
            debugInfo.append("\n  dateFormat.timeZone=").append(dateFormat.getTimeZone());
            debugInfo.append("\n  dateFormat.timeZoneOverridesCalendar=").append(dateFormat.getTimeZoneOverridesCalendar());
            debugInfo.append("\n  Locale.default=").append(Locale.getDefault());
            debugInfo.append("\n  TimeZone.default=").append(TimeZone.getDefault());
            StringBuilder sb = new StringBuilder();
            for (Document doc : documents) {
                Date endDate = doc.getAccessRestrictionEndDate();
                if (endDate != null) {
                    String formattedDate = dateFormat.format(endDate);
                    generateAccessRestrictionDocumentRow(sb, doc, formattedDate);

                    debugInfo.append("\n  * ").append(endDate.getTime()).append(" ").append(endDate.toString()).append(" -> ").append(formattedDate).append(" - ")
                            .append(doc.getNodeRef().toString());
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
                sb.append("</table>");
                templateText = templateText.replaceAll("\\{accessRestrEndDateNotificationData\\}", sb.toString());
            }
            log.info(debugInfo.toString());
        }

        String noEndDateFormula = "{accessRestrNoEndDateNotificationData}";
        if (templateText.indexOf(noEndDateFormula) > -1) {
            StringBuilder sb = new StringBuilder();
            for (Document doc : documents) {
                if (doc.getAccessRestrictionEndDate() == null) {
                    generateAccessRestrictionDocumentRow(sb, doc, doc.getAccessRestrictionEndDesc());
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
                sb.append("</table>");
                templateText = templateText.replaceAll("\\{accessRestrNoEndDateNotificationData\\}", sb.toString());
            }

        }

        return templateText;
    }

    private void generateAccessRestrictionDocumentRow(StringBuilder sb, Document doc, String lastColValue) {
        String regNr = doc.getRegNumber();
        if (regNr == null) {
            regNr = I18NUtil.getMessage("notification_document_not_registered", doc.getDocName());
        }

        sb.append("<tr><td>")
                .append(regNr)
                .append("</td><td>")
                .append(doc.getRegDateTime() == null ? "" : dateTimeFormat.format(doc.getRegDateTime()))
                .append("</td><td>")
                .append(documentAdminService.getDocumentTypeName((String) doc.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID)))
                .append("</td><td>")
                .append("<a href=\"").append(getDocumentUrl(doc.getNodeRef())).append("\">").append(doc.getDocName()).append("</a>")
                .append("</td><td>")
                .append(doc.getOwnerName())
                .append("</td><td>")
                .append(I18NUtil.getMessage("notification_access_restriction_end"))
                .append(": ")
                .append(lastColValue)
                .append("</td></tr>")
                .append("\n");
    }

    @Override
    public String getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template) {
        return getProcessedEmailTemplate(dataNodeRefs, template, null);
    }

    @Override
    public String getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template, Map<String, String> additionalFormulas) {
        ContentReader templateReader = fileFolderService.getReader(template);
        String templateTxt = templateReader.getContentString();
        if (dataNodeRefs.size() == 0) {
            return templateTxt;
        }

        // Use existing values as baseline if possible
        Map<String, String> allFormulas = (additionalFormulas == null) ? new LinkedHashMap<String, String>() : additionalFormulas;
        for (Entry<String, NodeRef> entry : dataNodeRefs.entrySet()) {
            Map<String, String> formulas = getEmailFormulas(entry.getValue());
            String keyPrefix = entry.getKey();
            if (StringUtils.isEmpty(keyPrefix)) {
                // Put these formulas without key prefix
                allFormulas.putAll(formulas);
            } else {
                // Put formulas with key prefix
                for (Entry<String, String> entry2 : formulas.entrySet()) {
                    allFormulas.put(keyPrefix + SEPARATOR + entry2.getKey(), entry2.getValue());
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(allFormulas.entrySet()));
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_FORMULA_GROUP_PATTERN.matcher(templateTxt);
        while (matcher.find()) {
            String group = matcher.group();
            String subResult = replaceCurlyBracesFormulas(group, allFormulas, false);

            if (group.equals(subResult)) { // If no replacement occurred then remove this group
                matcher.appendReplacement(result, "");
            } else { // remove group separators
                subResult = subResult.substring("/*".length(), subResult.length() - "*/".length());
                matcher.appendReplacement(result, Matcher.quoteReplacement(escapeXml(subResult)));
            }
        }
        matcher.appendTail(result);

        // Replace remaining curly braces formulae that weren't in a group
        return replaceCurlyBracesFormulas(result.toString(), allFormulas, true);
    }

    private String replaceCurlyBracesFormulas(String templateText, Map<String, String> formulas, boolean removeUnmatchedFormulas) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_FORMULA_PATTERN.matcher(templateText);
        while (matcher.find()) {
            String formulaKey = matcher.group().substring("{".length(), matcher.group().length() - "}".length());
            String formulaValue = formulas.get(formulaKey);
            /**
             * Spetsifikatsioon "Mallide loomine.docx"
             * 2.1.4. Kui valemi väärtus jääb täitmata, eemaldatakse valem malli tekstist; erandiks on valemid {regNumber} ja {regDateTime};
             * dokumendimallide korral toimub eemaldamine dokumendi registreerimisel.
             */
            if (formulaValue == null && (DocumentCommonModel.Props.REG_NUMBER.getLocalName().equals(formulaKey)
                    || DocumentCommonModel.Props.REG_DATE_TIME.getLocalName().equals(formulaKey) || !removeUnmatchedFormulas)) {
                formulaValue = matcher.group();
            } else if (formulaValue == null && removeUnmatchedFormulas) {
                formulaValue = "";
            }
            String formulaResult = escapeXml(formulaValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(formulaResult));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Map<String, String> getArchiveFormulas(List<NodeRef> volumeRefs) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();
        if (volumeRefs == null) {
            return formulas;
        }

        String currentUser = userService.getCurrentUserName();
        formulas.put("activityUserName", userService.getUserFullName(currentUser));
        formulas.put("activityUserId", currentUser);
        Date date = new Date();
        formulas.put("activityDate", dateFormat.format(date));
        formulas.put("activityDateTime", dateTimeFormat.format(date));
        formulas.put("volumesList", generateVolumeListOutput(volumeRefs));

        return formulas;
    }

    private String generateVolumeListOutput(List<NodeRef> volumeRefs) {
        String sep = "\t";
        StringBuffer sb = new StringBuffer(MessageUtil.getMessage("archivals_volume_word_file_heading_nr") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_mark") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_title") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_valid_from") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_valid_to") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_owner_name") + "\r\n");
        if (volumeRefs != null) {
            int volumeCount = 1;
            for (NodeRef volumeRef : volumeRefs) {
                Map<QName, Serializable> props = nodeService.getProperties(volumeRef);
                Date validFrom = (Date) props.get(VolumeModel.Props.VALID_FROM);
                Date validTo = (Date) props.get(VolumeModel.Props.VALID_TO);
                String ownerName = (String) props.get(DocumentDynamicModel.Props.OWNER_NAME);
                sb.append(volumeCount + sep
                        + props.get(VolumeModel.Props.MARK) + sep
                        + props.get(VolumeModel.Props.TITLE) + sep
                        + (validFrom != null ? dateFormat.format(validFrom) : "") + sep
                        + (validTo != null ? dateFormat.format(validTo) : "") + sep
                        + (StringUtils.isBlank(ownerName) ? "" : ownerName) + "\r\n");
                volumeCount++;
            }
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> getDocumentFormulas(NodeRef objectRef) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();
        if (objectRef == null) {
            return formulas;
        }

        WmNode documentNode = generalService.fetchObjectNode(objectRef, DocumentCommonModel.Types.DOCUMENT);
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = documentConfigService.getPropertyDefinitions(documentNode);

        Map<QName, Serializable> props = nodeService.getProperties(objectRef);
        for (Entry<String, Pair<DynamicPropertyDefinition, Field>> definition : propertyDefinitions.entrySet()) {
            Field field = definition.getValue().getSecond();
            if (field == null) {
                continue; // Hidden fields can be ignored
            }

            FieldType fieldType = field.getFieldTypeEnum();
            String fieldId = field.getFieldId();
            Serializable propValue = props.get(field.getQName());

            BaseObject parent = field.getParent();
            if (parent instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) parent;
                String name = group.getName();

                if (group.isSystematic()) {
                    if (isIgnoredFieldGroup(name)) {
                        continue;
                    }

                    if (SystematicFieldGroupNames.RECIPIENTS.equals(name) || SystematicFieldGroupNames.ADDITIONAL_RECIPIENTS.equals(name)) {
                        if (propValue != null) {
                            @SuppressWarnings("unchecked")
                            List<String> values = (List<String>) propValue;

                            for (int i = 1, j = 0; j < values.size(); i++, j++) {
                                formulas.put(fieldId + "." + i, values.get(j));
                            }
                        }
                        continue;
                    } else if (SystematicFieldGroupNames.THESAURI.equals(name)) {
                        String fieldId1 = DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName();
                        String fieldId2 = DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName();
                        List<String> firstLevelKeywords = new ArrayList<String>();
                        List<String> secondLevelKeywords = new ArrayList<String>();
                        for (Entry<String, Field> entry : group.getFieldsByOriginalId().entrySet()) {
                            if (fieldId1.equals(entry.getKey())) {
                                fieldId1 = entry.getValue().getFieldId();
                                firstLevelKeywords = (List<String>) props.get(entry.getValue().getQName());
                            } else if (fieldId2.equals(entry.getKey())) {
                                fieldId2 = entry.getValue().getFieldId();
                                secondLevelKeywords = (List<String>) props.get(entry.getValue().getQName());
                            }
                        }

                        // Check, if we have already added the keywords or there are no keywords present
                        if (formulas.containsKey(fieldId1) || firstLevelKeywords.isEmpty() || StringUtils.isEmpty(firstLevelKeywords.get(0))) {
                            continue;
                        }

                        String keywords = TextUtil.joinStringLists(firstLevelKeywords, secondLevelKeywords);
                        formulas.put(fieldId1, keywords);
                        formulas.put(fieldId2, keywords);
                        continue;
                    } else if (SystematicFieldGroupNames.SUBSTITUTE.equals(name)) {
                        if (formulas.containsKey("$vacationOrderSubstitutionData")) {
                            continue;
                        }
                        formulas.put("$vacationOrderSubstitutionData", getVacationOrderSubstitutionData(props));
                    }
                }
            }

            // Convert special cases
            boolean isList = propValue instanceof List;
            if (FieldType.DATE == fieldType) {
                if (isList && ((List<?>) propValue).size() == 1) {
                    propValue = (Serializable) ((List<?>) propValue).get(0);
                    isList = false;
                }

                if (propValue != null) {
                    String date = null;
                    if (isList) {
                        @SuppressWarnings("unchecked")
                        List<Date> dateList = (List<Date>) propValue;
                        List<String> dates = new ArrayList<String>();
                        for (Date d : dateList) {
                            dates.add(dateFormat.format(d));
                        }
                        date = StringUtils.join(dates, "; ");
                    } else {
                        date = dateFormat.format(propValue);
                    }
                    formulas.put(fieldId, date);
                    continue;
                }
            } else if (FieldType.LISTBOX == fieldType && isList) {
                @SuppressWarnings("unchecked")
                List<Serializable> listPropValue = (List<Serializable>) propValue;
                formulas.put(fieldId, StringUtils.join(listPropValue, "; "));
                continue;
            } else if (FieldType.CHECKBOX == fieldType && propValue instanceof Boolean) {
                String msgKey = (Boolean) propValue ? "yes" : "no";
                formulas.put(fieldId, MessageUtil.getMessage(msgKey));
                continue;
            } else if (FieldType.STRUCT_UNIT == fieldType && isList) {
                formulas.put(fieldId, UserUtil.getDisplayUnitText(propValue));
                continue;
            } else if (isList) {
                continue;
            }

            formulas.put(fieldId, propValue == null ? "" : propValue.toString());
        }

        formulas.put("$docType", documentAdminService.getDocumentTypeName(documentNode));
        getDocumentListStructureFormulae(objectRef, formulas);
        getContractPartyFormulae(objectRef, formulas);

        return formulas;
    }

    private void getContractPartyFormulae(NodeRef objectRef, Map<String, String> formulas) {
        // TODO from Alar: implement generic child-node support using propertyDefinition.getChildAssocTypeQNameHierarchy()
        List<ChildAssociationRef> contractPartyChildAssocs = nodeService.getChildAssocs(objectRef, DocumentChildModel.Assocs.CONTRACT_PARTY, RegexQNamePattern.MATCH_ALL);
        int index = 1;
        for (ChildAssociationRef childAssociationRef : contractPartyChildAssocs) {
            Map<QName, Serializable> properties = nodeService.getProperties(childAssociationRef.getChildRef());
            formulas.put(DocumentSpecificModel.Props.PARTY_NAME.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_NAME));
            formulas.put(DocumentSpecificModel.Props.PARTY_EMAIL.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_EMAIL));
            formulas.put(DocumentSpecificModel.Props.PARTY_SIGNER.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_SIGNER));
            formulas.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON));
            index++;
        }
    }

    private boolean isIgnoredFieldGroup(String fieldGroupName) {
        return SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(fieldGroupName)
                || SystematicFieldGroupNames.USERS_TABLE.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_REQUEST.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_CHANGE.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_CANCEL.equals(fieldGroupName)
                || SystematicFieldGroupNames.TRAINING_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT_SUMMARY.equals(fieldGroupName)
                || SystematicFieldGroupNames.DRIVE_COMPENSATION.equals(fieldGroupName);
    }

    private Map<String, String> getEmailFormulas(NodeRef objectRef) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();

        // All properties
        Map<QName, Serializable> props;
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        boolean isTask = workflowDbService.taskExists(objectRef);
        if (isTask) {
            props = RepoUtil
                    .toQNameProperties(workflowDbService.getTask(objectRef, BeanHelper.getWorkflowService().getTaskPrefixedQNames(), null, false).getNode().getProperties());
        } else {
            props = nodeService.getProperties(objectRef);
        }
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            String propName = entry.getKey().getLocalName();
            Serializable propValue = entry.getValue();
            if (propValue == null) {
                continue;
            }
            if (propValue instanceof List<?>) {
                List<?> list = (List<?>) propValue;
                String separator = ", ";
                if (propName.equals(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName())
                        || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName())
                        || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.getLocalName())) {
                    separator = "\n";
                }

                List<String> items = new ArrayList<String>(list.size());
                for (Object object : list) {
                    String itemValue = getTypeSpecificReplacement(object, false);
                    if (StringUtils.isNotBlank(itemValue)) {
                        items.add(itemValue);
                    }
                }
                formulas.put(propName, StringUtils.join(items.iterator(), separator));
            } else {
                boolean alternate = false;
                if (WorkflowCommonModel.Props.STARTED_DATE_TIME.getLocalName().equals(propName) && isTask) {
                    alternate = true;
                }

                formulas.put(propName, getTypeSpecificReplacement(propValue, alternate));
            }
        }

        // Specific formulas
        QName objectType = getDocumentListStructureFormulae(objectRef, formulas);

        {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            formulas.put("recipientNameEmail", generateNameAndEmail(names, emails));
        }

        {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL);
            formulas.put("additionalRecipientNameEmail", generateNameAndEmail(names, emails));
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.TASK)) {
            Serializable activeProp = workflowDbService.getTaskProperty(objectRef, WorkflowSpecificModel.Props.ACTIVE);
            if (activeProp != null) {
                Boolean isActive = (Boolean) activeProp;
                if (isActive) {
                    formulas.put("activeResponsible", isActive.toString());
                } else {
                    formulas.put("unactiveResponsible", isActive.toString());
                }
            } else {
                formulas.put("coResponsible", Boolean.TRUE.toString());
            }
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.WORKFLOW)) {
            formulas.put("type", MessageUtil.getMessage("workflow_" + objectType.getLocalName()));
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.COMPOUND_WORKFLOW)) {
            formulas.put("url", getCompoundWorkflowUrl(objectRef));
        } else if (dictionaryService.isSubClass(objectType, CaseFileModel.Types.CASE_FILE)) {
            formulas.put("url", getCaseFileUrl(objectRef));
        }

        if (objectType.equals(ForumModel.TYPE_FORUM)) {
            Map<QName, Serializable> properties = nodeService.getProperties(objectRef);
            formulas.put("creatorName", userService.getUserFullName((String) properties.get(ContentModel.PROP_CREATOR)));
        }

        if (objectType.equals(DocumentCommonModel.Types.DOCUMENT)) {
            formulas.putAll(getDocumentFormulas(objectRef));
        }

        /*
         * Spetsifikatsioon "Dokumendi ekraanivorm - Tegevused.docx" punkt 7.1.5.2
         * Kui vastav metaandme väli on täitmata, siis asendamist ei toimu.
         */

        // Remove formulas with empty values
        for (Iterator<Entry<String, String>> i = formulas.entrySet().iterator(); i.hasNext();) {
            Entry<String, String> entry = i.next();
            if (StringUtils.isBlank(entry.getValue())) {
                i.remove();
            }
        }
        return formulas;
    }

    private QName getDocumentListStructureFormulae(NodeRef objectRef, Map<String, String> formulas) {
        QName objectType = BeanHelper.getWorkflowService().getNodeRefType(objectRef);
        if (dictionaryService.isSubClass(objectType, DocumentCommonModel.Types.DOCUMENT)) {
            formulas.put("$functionTitle", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.TITLE));
            formulas.put("$functionMark", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.MARK));
            formulas.put("$seriesTitle", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.TITLE));
            formulas.put("$seriesIdentifier", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.SERIES_IDENTIFIER));
            formulas.put("$caseTitle", getAncestorProperty(objectRef, CaseModel.Types.CASE, CaseModel.Props.TITLE));
            formulas.put("$docUrl", getDocumentUrl(objectRef));

            String volTitle = getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.TITLE);
            formulas.put("$volumeTitle", StringUtils.isNotBlank(volTitle) ? volTitle : getAncestorProperty(objectRef, CaseFileModel.Types.CASE_FILE, VolumeModel.Props.TITLE));

            String volMark = getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.MARK);
            formulas.put("$volumeMark", StringUtils.isNotBlank(volMark) ? volMark : getAncestorProperty(objectRef, CaseFileModel.Types.CASE_FILE, VolumeModel.Props.MARK));
        }

        return objectType;
    }

    @Override
    public String getDocumentUrl(NodeRef document) {
        return getDocumentServerUrlPrefix() + document.getId();
    }

    @Override
    public String getCompoundWorkflowUrl(NodeRef compoundWorkflowRef) {
        if (compoundWorkflowRef != null && workflowService.isIndependentWorkflow(compoundWorkflowRef)) {
            return getCompoundWorkflowServerUrlPrefix() + compoundWorkflowRef.getId();
        }
        return null;
    }

    @Override
    public String getCaseFileUrl(NodeRef caseFileRef) {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_CASE_FILE + "/" + (caseFileRef != null ? caseFileRef.getId() : "");
    }

    @Override
    public String getVolumeUrl(NodeRef volumeRef) {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_VOLUME + "/" + volumeRef.getId();
    }

    @Override
    public String getCompoundWorkflowServerUrlPrefix() {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_NODEREF + "/";
    }

    @Override
    public String getDocumentServerUrlPrefix() {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_DOCUMENT + "/";
    }

    @Override
    public String getServerUrl() {
        return applicationService.getServerUrl() + servletContext.getContextPath();
    }

    private String getTypeSpecificReplacement(Object object, boolean alternate) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof Date) {
            return alternate ? dateTimeFormat.format((Date) object) : dateFormat.format((Date) object);
        }
        if (object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double || object instanceof Boolean) {
            return object.toString();
        }
        return null;
    }

    private String getVacationOrderSubstitutionData(Map<QName, Serializable> properties) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) properties.get(DocumentSpecificModel.Props.SUBSTITUTE_NAME);
        @SuppressWarnings("unchecked")
        List<Date> startDates = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE);
        @SuppressWarnings("unchecked")
        List<Date> endDates = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE);
        String until = I18NUtil.getMessage("template_until");

        int size = 0;
        if (names != null) {
            size = names.size();
        } else if (startDates != null) {
            size = startDates.size();
        } else if (endDates != null) {
            size = endDates.size();
        }
        List<String> substitutes = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            String name = names != null && i < names.size() ? names.get(i) : "";
            Date startDate = startDates != null && i < startDates.size() ? startDates.get(i) : null;
            Date endDate = endDates != null && i < endDates.size() ? endDates.get(i) : null;
            if (StringUtils.isEmpty(name) && startDate == null && endDate == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(name)
                    .append(" ")
                    .append(startDate == null ? "" : dateFormat.format(startDate))
                    .append(" ")
                    .append(until)
                    .append(" ")
                    .append(endDate == null ? "" : dateFormat.format(endDate));
            substitutes.add(sb.toString());
        }
        return StringUtils.join(substitutes.iterator(), "\n");
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
            String row = name == null ? "" : name;
            if (StringUtils.isNotBlank(email)) {
                row += " (" + email + ")";
            }
            if (!StringUtils.isBlank(row)) {
                rows.add(row);
            }
        }
        return StringUtils.join(rows.iterator(), "\n"); // XXX \r was used for OO

    }

    /**
     * @param foundPattern
     * @param document
     * @return
     */
    private String getAncestorProperty(NodeRef document, QName ancestorType, QName property) {
        Node parent = generalService.getAncestorWithType(document, ancestorType);
        return (parent != null) ? nodeService.getProperty(parent.getNodeRef(), property).toString() : "";
    }

    @Override
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
            if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                NodeRef docType = generalService.getNodeRef(DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE + "/" + DocumentAdminModel.PREFIX + dt.getDocTypeId());
                if (docType != null) {
                    dt.setDocTypeName((String) nodeService.getProperty(docType, DocumentAdminModel.Props.NAME));
                }
            } else if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
                dt.setDocTypeName("");
            } else {
                dt.setDocTypeName(dt.getDocTypeId());
            }
            templates.add(dt);
        }
        return templates;
    }

    @Override
    public NodeRef getRoot() {
        return generalService.getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
    }

    @Override
    public List<DocumentTemplate> getDocumentTemplates(String docTypeId) {
        Assert.notNull(docTypeId, "Parameter docTypeId is mandatory.");
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (docTypeId.equals(template.getDocTypeId())) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<DocumentTemplate> getEmailTemplates() {
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (nodeService.hasAspect(template.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<SelectItem> getReportTemplates(TemplateReportType typeId) {
        Assert.notNull(typeId, "Parameter typeId is mandatory.");
        List<SelectItem> result = new ArrayList<SelectItem>();
        String typeStr = typeId.toString();
        for (DocumentTemplate template : getTemplates()) {
            if (nodeService.hasAspect(template.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_REPORT) && typeStr.equals(template.getReportType())) {
                result.add(new SelectItem(template.getName()));
            }
        }
        return result;
    }

    private String escapeXml(String replaceString) {
        if (replaceString == null) {
            return "";
        }
        replaceString = replaceString.replaceAll("&", "&amp;");
        replaceString = replaceString.replaceAll("\"", "&quot;");
        replaceString = replaceString.replaceAll("<", "&lt;");
        replaceString = replaceString.replaceAll(">", "&gt;");
        replaceString = replaceString.replaceAll("'", "&apos;");
        return replaceString;
    }

    @Override
    public NodeRef getNotificationTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION;
        return getTemplateByName(templateName, templateAspect);
    }

    @Override
    public NodeRef getEmailTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_EMAIL;
        return getTemplateByName(templateName, templateAspect);
    }

    @Override
    public NodeRef getArchivalReportTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_ARCHIVAL_REPORT;
        return getTemplateByName(templateName, templateAspect);
    }

    private NodeRef getTemplateByName(String templateName, QName templateAspect) {
        if (StringUtils.isNotEmpty(templateName)) {
            NodeRef template = fileFolderService.searchSimple(getRoot(), templateName);
            if (template != null && nodeService.hasAspect(template, templateAspect)) {
                return template;
            }
            if (StringUtils.endsWith(templateName, ".html")) {
                // try alternative ".htm" that is default when adding system template through GUI
                templateName = templateName.substring(0, templateName.length() - 1);
                return getTemplateByName(templateName, templateAspect);
            }
        }
        return null;
    }

    @Override
    public NodeRef getReportTemplateByName(String templateName, TemplateReportType reportType) {
        Assert.notNull(reportType);
        if (StringUtils.isBlank(templateName)) {
            return null;
        }
        NodeRef template = fileFolderService.searchSimple(getRoot(), templateName);
        if (template != null && nodeService.hasAspect(template, DocumentTemplateModel.Aspects.TEMPLATE_REPORT)
                && reportType.toString().equals(nodeService.getProperty(template, DocumentTemplateModel.Prop.REPORT_TYPE))) {
            return template;
        }
        return null;
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

    public void setOpenOfficeService(OpenOfficeService openOfficeService) {
        this.openOfficeService = openOfficeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // END: getters / setters

=======
package ee.webmedia.alfresco.template.service;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import ee.webmedia.alfresco.archivals.model.ActivityFileType;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.OpenOfficeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.mso.service.MsoService;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.template.exception.ExistingFileFromTemplateException;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.model.ProcessedEmailTemplate;
import ee.webmedia.alfresco.template.web.AddDocumentTemplateDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.ISOLatin1Util;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DocumentTemplateServiceImpl implements DocumentTemplateService, ServletContextAware {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTemplateServiceImpl.class);

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");
    private static final String SEPARATOR = ".";
    private static final Pattern TEMPLATE_FORMULA_GROUP_PATTERN = Pattern.compile(OpenOfficeService.REGEXP_GROUP_PATTERN);
    private static final Pattern TEMPLATE_FORMULA_PATTERN = Pattern.compile(OpenOfficeService.REGEXP_PATTERN);
    public static final QName TEMP_PROP_FILE_NAME_BASE = RepoUtil.createTransientProp("fileNameBase");
    public static final QName TEMP_PROP_FILE_NAME_EXTENSION = RepoUtil.createTransientProp("fileNameExtension");

    private GeneralService generalService;
    private NodeService nodeService;
    private FileService _fileService;
    private MimetypeService mimetypeService;
    private FileFolderService fileFolderService;
    private OpenOfficeService openOfficeService;
    private DictionaryService dictionaryService;
    private MsoService msoService;
    private ApplicationService applicationService;
    private ServletContext servletContext;
    private UserService userService;
    private DocumentConfigService documentConfigService;
    private DocumentAdminService documentAdminService;
    private VersionsService versionsService;
    private DocumentLogService documentLogService;
    private WorkflowService workflowService;

    private static BeanPropertyMapper<DocumentTemplate> templateBeanPropertyMapper;
    static {
        templateBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentTemplate.class);
    }

    @Override
    public void updateGeneratedFiles(NodeRef docRef, boolean isRegistering) {
        // NB! msoAvailable comes from application configuration, not from actual availability.
        // So if mso is configured to be available, but actually is not, then file generation will throw error.
        // If mso is configured as unavailable, mso file generation is skipped (up to admins to deal with these situations).
        // As oo service is used for file indexing, it must always be available, so it is not checked for availability here
        // and oo file generation may throw error if oo service is actually unavailable.
        boolean msoAvailable = msoService.isAvailable();

        // Shortcuts
        if (!isRegistering && !Boolean.TRUE.equals(nodeService.getProperty(docRef, DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES))) {
            return;
        }

        List<FileInfo> files = fileFolderService.listFiles(docRef);
        log.debug("Found " + files.size() + " files under document " + docRef);
        for (FileInfo file : files) {
            // If generator service isn't available, then skip this file
            String generationType = (String) file.getProperties().get(FileModel.Props.GENERATION_TYPE);
            if (GeneratedFileType.WORD_TEMPLATE.name().equals(generationType) && !msoAvailable) {
                continue;
            }
            if (StringUtils.isNotBlank((String) file.getProperties().get(FileModel.Props.GENERATED_FROM_TEMPLATE))
                    || Boolean.TRUE.equals(file.getProperties().get(FileModel.Props.UPDATE_METADATA_IN_FILES))) {

                replaceFormulas(docRef, file.getNodeRef(), file.getNodeRef(), file.getName(), isRegistering, true);
            }
        }
    }

    @Override
    public void updateDocTemplate(Node docTemplNode) {
        Map<String, Object> properties = docTemplNode.getProperties();
        String oldName = (String) properties.get(DocumentTemplateModel.Prop.NAME.toString());
        String newName = StringUtils.strip((String) properties.get(TEMP_PROP_FILE_NAME_BASE.toString())) + properties.get(TEMP_PROP_FILE_NAME_EXTENSION.toString());
        for (DocumentTemplate documentTemplate : getTemplates()) {
            String nameForCheck = documentTemplate.getName();
            if (!StringUtils.equals(nameForCheck, oldName) && StringUtils.equals(nameForCheck, newName)) {
                throw new UnableToPerformException(AddDocumentTemplateDialog.ERR_EXISTING_FILE, newName);
            }
        }
        properties.put(DocumentTemplateModel.Prop.NAME.toString(), newName);
        NodeRef docTemplateNodeRef = docTemplNode.getNodeRef();
        generalService.setPropertiesIgnoringSystem(docTemplateNodeRef, properties);
        nodeService.setProperty(docTemplateNodeRef, ContentModel.PROP_NAME, newName);
    }

    @Override
    public DocumentTemplate getDocumentsTemplate(NodeRef document) {
        String documentTypeId = (String) nodeService.getProperty(document, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        // it's OK to pick first one
        FileInfo word2003OrOOTemplate = null;
        boolean msoAvailable = msoService.isAvailable();
        boolean ooAvailable = openOfficeService.isAvailable();
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (!nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                continue;
            }

            if (!documentTypeId.equals(fi.getProperties().get(DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                continue;
            }

            // If current file is 2007/2010 template, return the first one found
            if (StringUtils.endsWithIgnoreCase(fi.getName(), ".dotx") && msoAvailable) {
                return setupDocumentTemplate(fi);
            }

            // Otherwise mark it as a candidate if only 2003 binary or OpenOffice template is present
            word2003OrOOTemplate = fi;
        }
        if (word2003OrOOTemplate != null) {
            String ext = getExtension(word2003OrOOTemplate.getName());
            if (equalsIgnoreCase("ott", ext) && ooAvailable || equalsIgnoreCase("dot", ext) && msoAvailable) {
                return setupDocumentTemplate(word2003OrOOTemplate);
            }
        }

        return null;
    }

    @Override
    public boolean hasDocumentsTemplate(NodeRef document) {
        String documentTypeId = (String) nodeService.getProperty(document, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (!nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                continue;
            }
            if (!documentTypeId.equals(fi.getProperties().get(DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                continue;
            }
            String fileNameExtension = FilenameUtils.getExtension((String) fi.getProperties().get(DocumentTemplateModel.Prop.NAME));
            Assert.isTrue(StringUtils.equals("dotx", fileNameExtension) || StringUtils.equals("dot", fileNameExtension) || StringUtils.equals("ott", fileNameExtension));
            return true;
        }
        return false;
    }

    /**
     * Sets the properties, adds download URL and NodeRef
     * 
     * @param fileInfo
     * @return
     */
    private DocumentTemplate setupDocumentTemplate(FileInfo fileInfo) {
        DocumentTemplate dt = templateBeanPropertyMapper.toObject(nodeService.getProperties(fileInfo.getNodeRef()), null);
        dt.setNodeRef(fileInfo.getNodeRef());
        dt.setDownloadUrl(getFileService().generateURL(fileInfo.getNodeRef()));
        return dt;
    }

    @Override
    public String populateTemplate(NodeRef documentNodeRef, boolean overWritingGranted) throws FileNotFoundException {
        log.debug("Creating a file from template for document: " + documentNodeRef);
        boolean msoAvailable = msoService.isAvailable();
        boolean ooAvailable = openOfficeService.isAvailable();
        if (!msoAvailable && !ooAvailable) {
            throw new UnableToPerformException("document_errorMsg_template_processsing_failed_service_missing");
        }

        final Map<QName, Serializable> docProp = nodeService.getProperties(documentNodeRef);

        String templName = "";
        if (docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME) != null) {
            templName = (String) docProp.get(DocumentSpecificModel.Props.TEMPLATE_NAME);
        }
        NodeRef nodeRef;
        if (StringUtils.isEmpty(templName)) {
            // No template specified, try to use default, if any
            // NOTE: we don't need to check for null, because in that case the button triggering this action isn't shown
            log.debug("Document template not specified, looking for default template! Document: " + documentNodeRef);
            DocumentTemplate templ = getDocumentsTemplate(documentNodeRef);
            nodeRef = templ == null ? null : templ.getNodeRef();
        } else {
            nodeRef = getTemplateByName(templName).getNodeRef();
        }
        if (nodeRef == null) {
            throw new UnableToPerformException("document_errorMsg_template_not_found");
        }

        String templateFilename = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        boolean ooTemplate = "ott".equals(FilenameUtils.getExtension(templateFilename));
        if (ooTemplate && !ooAvailable || !ooTemplate && !msoAvailable) { // if it isn't OO template it must be Word template
            throw new UnableToPerformException("document_errorMsg_template_not_found_for_service");
        }

        log.debug("Using template: " + templateFilename);

        NodeRef existingGeneratedFile = null;
        String displayName = null;
        { // Check if we already have a file that is generated using this template and overwrite
            for (ChildAssociationRef caRef : nodeService.getChildAssocs(documentNodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL)) {
                NodeRef fileRef = caRef.getChildRef();
                String genTemplName = (String) nodeService.getProperty(fileRef, FileModel.Props.GENERATED_FROM_TEMPLATE);
                if (!StringUtils.equals(genTemplName, templateFilename)) {
                    continue;
                }

                if (!overWritingGranted) {
                    throw new ExistingFileFromTemplateException(templateFilename);
                }
                displayName = (String) nodeService.getProperty(fileRef, FileModel.Props.DISPLAY_NAME);
                existingGeneratedFile = fileRef;
                // Create new version for the file. Aspect must be added, so do this now (no point of checking if file already has it).
                versionsService.addVersionLockableAspect(existingGeneratedFile);
                versionsService.setVersionLockableAspect(existingGeneratedFile, false);
                versionsService.updateVersion(existingGeneratedFile, displayName, false);
                versionsService.updateVersionModifiedAspect(existingGeneratedFile);
                // Unlock the node here, since previous method locked it and there is no session (e.g. Word) that would unlock the file.
                versionsService.setVersionLockableAspect(existingGeneratedFile, false);

                break; // Assume there is only one file.
            }
        }

        if (existingGeneratedFile == null) { // Create the container for template content
            // XXX target file mimeType comes from MSO service, so it would be better to set file extension based on that mimeType
            String extension = mimetypeService.getExtension(templateFilename.endsWith(".ott") ? MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT : MimetypeMap.MIMETYPE_WORD);
            if (StringUtils.endsWithIgnoreCase(templateFilename, ".dotx")) {
                extension += "x"; // Well, ain't this nice! :) (Filename must end with docx if 2003/2007 template is used)
            }
            String name = ((String) docProp.get(DocumentCommonModel.Props.DOC_NAME));
            displayName = getFileService().getUniqueFileDisplayName(documentNodeRef, name + "." + extension);
            name = FilenameUtil.replaceAmpersand(ISOLatin1Util.removeAccents(FilenameUtil.buildFileName(name, extension)));
            name = FilenameUtil.replaceNonAsciiCharacters(name);
            name = FilenameUtil.limitFileNameLength(name);
            name = generalService.getUniqueFileName(documentNodeRef, name);

            existingGeneratedFile = fileFolderService.create(documentNodeRef, name, ContentModel.TYPE_CONTENT).getNodeRef();

            Map<QName, Serializable> templateProps = new HashMap<QName, Serializable>(3);
            // Mark down the template that was used to generate the file
            templateProps.put(FileModel.Props.GENERATED_FROM_TEMPLATE, templateFilename);
            templateProps.put(FileModel.Props.GENERATION_TYPE, (ooTemplate ? GeneratedFileType.OPENOFFICE_TEMPLATE.name() : GeneratedFileType.WORD_TEMPLATE.name()));
            templateProps.put(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, Boolean.TRUE);
            // Set the display name so we can process it during document registration
            templateProps.put(FileModel.Props.DISPLAY_NAME, displayName);
            nodeService.addProperties(existingGeneratedFile, templateProps);
            versionsService.addVersionModifiedAspect(existingGeneratedFile);
            documentLogService.addDocumentLog(documentNodeRef, MessageUtil.getMessage("applog_doc_file_generated", displayName));
            log.debug("Created new node: " + existingGeneratedFile + "\nwith name: " + name + "; displayName: " + displayName);
        }

        // Set document content's MIME type and encoding from template
        replaceFormulas(documentNodeRef, nodeRef, existingGeneratedFile, templateFilename);
        generalService.setModifiedToNow(documentNodeRef);
        return displayName;
    }

    @Override
    public void populateVolumeArchiveTemplate(NodeRef parentRef, List<NodeRef> volumeRefs, NodeRef templateRef) {
        if (!msoService.isAvailable()) {
            throw new UnableToPerformException("document_errorMsg_template_processsing_failed_service_missing");
        }
        // it is assumed that template name is fixed by application and thus it is not needed to check not allowed characters, length etc in filename
        String templateFilename = (String) nodeService.getProperty(templateRef, ContentModel.PROP_NAME);
        String filenameWithoutExtension = FilenameUtils.removeExtension(templateFilename);
        templateFilename = filenameWithoutExtension + " " + FastDateFormat.getInstance("yyyyMMdd").format(new Date()) + ".docx";
        NodeRef generatedFile = fileFolderService.create(parentRef, templateFilename, ContentModel.TYPE_CONTENT).getNodeRef();
        replaceFormulas(volumeRefs, templateRef, generatedFile, templateFilename, true, false);
        nodeService.setProperty(generatedFile, FileModel.Props.ACTIVITY_FILE_TYPE, ActivityFileType.GENERATED_DOCX.name());
    }

    private void replaceFormulas(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName) {
        replaceFormulas(document, sourceFile, destinationFile, sourceFileName, false, false);
    }

    @SuppressWarnings("unchecked")
    private void replaceFormulas(Object documentOrVolumeRefList, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName, boolean finalize, boolean dontSaveIfUnmodified) {
        Assert.isTrue(documentOrVolumeRefList instanceof NodeRef || documentOrVolumeRefList instanceof List);
        // Basically the same check is performed in MsoService#isFormulasReplaceable, but with MIME types
        boolean msoFile = StringUtils.endsWithIgnoreCase(sourceFileName, ".doc") || StringUtils.endsWithIgnoreCase(sourceFileName, ".docx")
                || StringUtils.endsWithIgnoreCase(sourceFileName, ".dot") || StringUtils.endsWithIgnoreCase(sourceFileName, ".dotx");
        boolean ooFile = StringUtils.endsWithIgnoreCase(sourceFileName, ".odt") || StringUtils.endsWithIgnoreCase(sourceFileName, ".ott");
        if (!msoFile && !ooFile) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_invalid_file_extension");
        }

        NodeRef document = null;
        Map<String, String> formulas = null;
        if (documentOrVolumeRefList instanceof NodeRef) {
            document = (NodeRef) documentOrVolumeRefList;
            formulas = getDocumentFormulas(document);
        } else {
            formulas = getArchiveFormulas((List<NodeRef>) documentOrVolumeRefList);
        }
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(formulas.entrySet()));
        }
        if (msoFile && msoService.isAvailable()) {
            if (finalize) {
                formulas.put("$FINALIZE", "1");
            }
            ContentReader documentReader = fileFolderService.getReader(sourceFile);
            // Disable automatic update if writing is not to be performed in case file is actually not changed by mso service
            ContentWriter documentWriter = fileFolderService.getWriter(destinationFile, !dontSaveIfUnmodified);
            try {
                boolean fileActuallyChanged = msoService.replaceFormulas(formulas, documentReader, documentWriter, dontSaveIfUnmodified);
                if (dontSaveIfUnmodified && fileActuallyChanged) {
                    // automatic saving has been turned off for checked saving, so save manually
                    nodeService.setProperty(destinationFile, ContentModel.PROP_CONTENT, documentWriter.getContentData());
                }
            } catch (SOAPFaultException se) {
                String errorKey = "template_replace_formulas_failed";
                if (se.getMessage().contains("Err001")) {
                    errorKey += "_invalid_file_content";
                }
                throw new UnableToPerformException(MessageSeverity.ERROR, errorKey, se);
            } catch (Exception e) {
                throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_failed", e);
            }
        } else if (ooFile && openOfficeService.isAvailable()) {
            // generating document for volume list is not implemented for OpenOffice
            replaceFormulasWithOpenOffice(document, sourceFile, destinationFile, sourceFileName, formulas, finalize, dontSaveIfUnmodified);
        }
    }

    private void replaceFormulasWithOpenOffice(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName, Map<String, String> formulas,
            boolean finalize, boolean dontSaveIfUnmodified) {
        int retry = 3;
        do {
            ContentReader reader = fileFolderService.getReader(sourceFile);
            // Disable automatic update if writing is not to be performed in case file is actually not changed by oo service
            ContentWriter writer = fileFolderService.getWriter(destinationFile, !dontSaveIfUnmodified);
            writer.setMimetype(MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT);

            try {
                boolean fileActuallyChanged = openOfficeService.replace(reader, writer, formulas, finalize);
                if (dontSaveIfUnmodified && fileActuallyChanged) {
                    // automatic saving has been turned off for checked saving, so save manually
                    nodeService.setProperty(destinationFile, ContentModel.PROP_CONTENT, writer.getContentData());
                }
                retry = 0;
            } catch (OpenOfficeService.OpenOfficeReturnedNullInterfaceException e) {
                retry--;
                log.error("Replacing failed, OpenOffice error, retrying " + retry + " times more: " + e.getMessage() + "\n    fileName=" + sourceFileName
                        + "\n    reader=" + reader + "\n    writer=" + writer);
                if (retry <= 0) {
                    throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_failed", e);
                }
            } catch (Exception e) {
                log.error("Replacing failed!\n    fileName=" + sourceFileName + "\n    reader=" + reader + "\n    writer=" + writer, e);
                throw new RuntimeException(e);
            }
        } while (retry > 0);
    }

    @Override
    public String getProcessedVolumeDispositionTemplate(List<Volume> volumes, NodeRef template) {
        String templateText = fileFolderService.getReader(template).getContentString();
        StringBuilder sb = new StringBuilder();
        if (templateText.indexOf("{volumeDispositionDateNotificationData}") > -1) {
            for (Volume vol : volumes) {
                sb.append(vol.getVolumeMark())
                        .append(" ")
                        .append(vol.getTitle())
                        .append(" (")
                        .append(I18NUtil.getMessage("notification_dispostition_date"))
                        .append(": ")
                        .append(vol.getRetainUntilDate() == null ? "" : dateFormat.format(vol.getRetainUntilDate()))
                        .append(")")
                        .append("<br>\n");
            }
            if (sb.length() > 0) {
                templateText = templateText.replaceAll("\\{volumeDispositionDateNotificationData\\}", sb.toString());
            }
        }
        return templateText;
    }

    @Override
    public String getProcessedAccessRestrictionEndDateTemplate(List<Document> documents, NodeRef template) {
        String templateText = fileFolderService.getReader(template).getContentString();
        String endDateFormula = "{accessRestrEndDateNotificationData}";
        if (templateText.indexOf(endDateFormula) > -1) {
            StringBuilder debugInfo = new StringBuilder("AccessRestrictionEndDate date formatting:");
            debugInfo.append("\n  dateFormat.timeZone=").append(dateFormat.getTimeZone());
            debugInfo.append("\n  dateFormat.timeZoneOverridesCalendar=").append(dateFormat.getTimeZoneOverridesCalendar());
            debugInfo.append("\n  Locale.default=").append(Locale.getDefault());
            debugInfo.append("\n  TimeZone.default=").append(TimeZone.getDefault());
            StringBuilder sb = new StringBuilder();
            for (Document doc : documents) {
                Date endDate = doc.getAccessRestrictionEndDate();
                if (endDate != null) {
                    String formattedDate = dateFormat.format(endDate);
                    generateAccessRestrictionDocumentRow(sb, doc, formattedDate);

                    debugInfo.append("\n  * ").append(endDate.getTime()).append(" ").append(endDate.toString()).append(" -> ").append(formattedDate).append(" - ")
                            .append(doc.getNodeRef().toString());
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "<table cellspacing=\"0\" cellpadding=\"5\" border=\"0\">");
                sb.append("</table>");
                templateText = templateText.replaceAll("\\{accessRestrEndDateNotificationData\\}", sb.toString());
            }
            log.info(debugInfo.toString());
        }

        String noEndDateFormula = "{accessRestrNoEndDateNotificationData}";
        if (templateText.indexOf(noEndDateFormula) > -1) {
            StringBuilder sb = new StringBuilder();
            for (Document doc : documents) {
                if (doc.getAccessRestrictionEndDate() == null) {
                    generateAccessRestrictionDocumentRow(sb, doc, doc.getAccessRestrictionEndDesc());
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "<table cellspacing=\"0\" cellpadding=\"5\" border=\"0\">");
                sb.append("</table>");
                templateText = templateText.replaceAll("\\{accessRestrNoEndDateNotificationData\\}", sb.toString());
            }

        }

        return templateText;
    }

    private void generateAccessRestrictionDocumentRow(StringBuilder sb, Document doc, String lastColValue) {
        String regNr = doc.getRegNumber();
        if (regNr == null) {
            regNr = I18NUtil.getMessage("notification_document_not_registered", doc.getDocName());
        }

        sb.append("<tr><td>")
                .append(regNr)
                .append("</td><td>")
                .append(doc.getRegDateTime() == null ? "" : dateTimeFormat.format(doc.getRegDateTime()))
                .append("</td><td>")
                .append(documentAdminService.getDocumentTypeName((String) doc.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID)))
                .append("</td><td>")
                .append("<a href=\"").append(getDocumentUrl(doc.getNodeRef())).append("\">").append(doc.getDocName()).append("</a>")
                .append("</td><td>")
                .append(doc.getOwnerName())
                .append("</td><td>")
                .append(I18NUtil.getMessage("notification_access_restriction_end"))
                .append(": ")
                .append(lastColValue)
                .append("</td></tr>")
                .append("\n");
    }

    @Override
    public ProcessedEmailTemplate getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template) {
        return getProcessedEmailTemplate(dataNodeRefs, template, null);
    }

    @Override
    public ProcessedEmailTemplate getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template, Map<String, String> additionalFormulas) {
        ContentReader templateReader = fileFolderService.getReader(template);
        String templateTxt = templateReader.getContentString();
        String subject = (String) nodeService.getProperty(template, DocumentTemplateModel.Prop.NOTIFICATION_SUBJECT);
        if (dataNodeRefs.size() == 0) {
            return new ProcessedEmailTemplate(subject, templateTxt);
        }

        // Use existing values as baseline if possible
        Map<String, String> allFormulas = (additionalFormulas == null) ? new LinkedHashMap<String, String>() : additionalFormulas;
        for (Entry<String, NodeRef> entry : dataNodeRefs.entrySet()) {
            Map<String, String> formulas = getEmailFormulas(entry.getValue());
            String keyPrefix = entry.getKey();
            if (StringUtils.isEmpty(keyPrefix)) {
                // Put these formulas without key prefix
                allFormulas.putAll(formulas);
            } else {
                // Put formulas with key prefix
                for (Entry<String, String> entry2 : formulas.entrySet()) {
                    allFormulas.put(keyPrefix + SEPARATOR + entry2.getKey(), entry2.getValue());
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(allFormulas.entrySet()));
        }
        if (subject != null) {
            subject = processEmailTemplate(subject, allFormulas);
        }
        String content = processEmailTemplate(templateTxt, allFormulas);
        return new ProcessedEmailTemplate(subject, content);
    }

    private String processEmailTemplate(String templateTxt, Map<String, String> formulas) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_FORMULA_GROUP_PATTERN.matcher(templateTxt);
        while (matcher.find()) {
            String group = matcher.group();
            String subResult = replaceCurlyBracesFormulas(group, formulas, false);

            if (group.equals(subResult)) { // If no replacement occurred then remove this group
                matcher.appendReplacement(result, "");
            } else { // remove group separators
                subResult = subResult.substring("/*".length(), subResult.length() - "*/".length());
                matcher.appendReplacement(result, Matcher.quoteReplacement(escapeXml(subResult)));
            }
        }
        matcher.appendTail(result);

        // Replace remaining curly braces formulae that weren't in a group
        return replaceCurlyBracesFormulas(result.toString(), formulas, true);
    }

    private String replaceCurlyBracesFormulas(String templateText, Map<String, String> formulas, boolean removeUnmatchedFormulas) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_FORMULA_PATTERN.matcher(templateText);
        while (matcher.find()) {
            String formulaKey = matcher.group().substring("{".length(), matcher.group().length() - "}".length());
            String formulaValue = formulas.get(formulaKey);
            /**
             * Spetsifikatsioon "Mallide loomine.docx"
             * 2.1.4. Kui valemi väärtus jääb täitmata, eemaldatakse valem malli tekstist; erandiks on valemid {regNumber} ja {regDateTime};
             * dokumendimallide korral toimub eemaldamine dokumendi registreerimisel.
             */
            if (formulaValue == null && (DocumentCommonModel.Props.REG_NUMBER.getLocalName().equals(formulaKey)
                    || DocumentCommonModel.Props.REG_DATE_TIME.getLocalName().equals(formulaKey) || !removeUnmatchedFormulas)) {
                formulaValue = matcher.group();
            } else if (formulaValue == null && removeUnmatchedFormulas) {
                formulaValue = "";
            }
            String formulaResult = escapeXml(formulaValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(formulaResult));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Map<String, String> getArchiveFormulas(List<NodeRef> volumeRefs) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();
        if (volumeRefs == null) {
            return formulas;
        }

        String currentUser = userService.getCurrentUserName();
        formulas.put("activityUserName", userService.getUserFullName(currentUser));
        formulas.put("activityUserId", currentUser);
        Date date = new Date();
        formulas.put("activityDate", dateFormat.format(date));
        formulas.put("activityDateTime", dateTimeFormat.format(date));
        formulas.put("volumesList", generateVolumeListOutput(volumeRefs));

        return formulas;
    }

    private String generateVolumeListOutput(List<NodeRef> volumeRefs) {
        String sep = "\t";
        StringBuffer sb = new StringBuffer(MessageUtil.getMessage("archivals_volume_word_file_heading_nr") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_mark") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_title") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_valid_from") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_valid_to") + sep
                + MessageUtil.getMessage("archivals_volume_word_file_heading_owner_name") + "\r\n");
        if (volumeRefs != null) {
            int volumeCount = 1;
            for (NodeRef volumeRef : volumeRefs) {
                Map<QName, Serializable> props = nodeService.getProperties(volumeRef);
                Date validFrom = (Date) props.get(VolumeModel.Props.VALID_FROM);
                Date validTo = (Date) props.get(VolumeModel.Props.VALID_TO);
                String ownerName = (String) props.get(DocumentDynamicModel.Props.OWNER_NAME);
                sb.append(volumeCount + sep
                        + props.get(VolumeModel.Props.MARK) + sep
                        + props.get(VolumeModel.Props.TITLE) + sep
                        + (validFrom != null ? dateFormat.format(validFrom) : "") + sep
                        + (validTo != null ? dateFormat.format(validTo) : "") + sep
                        + (StringUtils.isBlank(ownerName) ? "" : ownerName) + "\r\n");
                volumeCount++;
            }
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> getDocumentFormulas(NodeRef objectRef) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();
        if (objectRef == null) {
            return formulas;
        }

        WmNode documentNode = generalService.fetchObjectNode(objectRef, DocumentCommonModel.Types.DOCUMENT);
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = documentConfigService.getPropertyDefinitions(documentNode);

        Map<QName, Serializable> props = nodeService.getProperties(objectRef);
        for (Entry<String, Pair<DynamicPropertyDefinition, Field>> definition : propertyDefinitions.entrySet()) {
            Field field = definition.getValue().getSecond();
            if (field == null) {
                continue; // Hidden fields can be ignored
            }

            FieldType fieldType = field.getFieldTypeEnum();
            String fieldId = field.getFieldId();
            Serializable propValue = props.get(field.getQName());

            BaseObject parent = field.getParent();
            if (parent instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) parent;
                String name = group.getName();

                if (group.isSystematic()) {
                    if (isIgnoredFieldGroup(name)) {
                        continue;
                    }

                    if (SystematicFieldGroupNames.RECIPIENTS.equals(name) || SystematicFieldGroupNames.ADDITIONAL_RECIPIENTS.equals(name)) {
                        if (propValue != null) {
                            @SuppressWarnings("unchecked")
                            List<String> values = (List<String>) propValue;

                            for (int i = 1, j = 0; j < values.size(); i++, j++) {
                                formulas.put(fieldId + "." + i, values.get(j));
                            }
                        }
                        continue;
                    } else if (SystematicFieldGroupNames.THESAURI.equals(name)) {
                        String fieldId1 = DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName();
                        String fieldId2 = DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName();
                        List<String> firstLevelKeywords = new ArrayList<String>();
                        List<String> secondLevelKeywords = new ArrayList<String>();
                        for (Entry<String, Field> entry : group.getFieldsByOriginalId().entrySet()) {
                            if (fieldId1.equals(entry.getKey())) {
                                fieldId1 = entry.getValue().getFieldId();
                                firstLevelKeywords = (List<String>) props.get(entry.getValue().getQName());
                            } else if (fieldId2.equals(entry.getKey())) {
                                fieldId2 = entry.getValue().getFieldId();
                                secondLevelKeywords = (List<String>) props.get(entry.getValue().getQName());
                            }
                        }

                        // Check, if we have already added the keywords or there are no keywords present
                        if (formulas.containsKey(fieldId1) || firstLevelKeywords.isEmpty() || StringUtils.isEmpty(firstLevelKeywords.get(0))) {
                            continue;
                        }

                        String keywords = TextUtil.joinStringLists(firstLevelKeywords, secondLevelKeywords);
                        formulas.put(fieldId1, keywords);
                        formulas.put(fieldId2, keywords);
                        continue;
                    } else if (SystematicFieldGroupNames.SUBSTITUTE.equals(name)) {
                        if (formulas.containsKey("$vacationOrderSubstitutionData")) {
                            continue;
                        }
                        formulas.put("$vacationOrderSubstitutionData", getVacationOrderSubstitutionData(props));
                    }
                }
            }

            // Convert special cases
            boolean isList = propValue instanceof List;
            if (FieldType.DATE == fieldType) {
                if (isList && ((List<?>) propValue).size() == 1) {
                    propValue = (Serializable) ((List<?>) propValue).get(0);
                    isList = false;
                }

                if (propValue != null) {
                    String date = null;
                    if (isList) {
                        @SuppressWarnings("unchecked")
                        List<Date> dateList = (List<Date>) propValue;
                        List<String> dates = new ArrayList<String>();
                        for (Date d : dateList) {
                            dates.add(dateFormat.format(d));
                        }
                        date = StringUtils.join(dates, "; ");
                    } else {
                        date = dateFormat.format(propValue);
                    }
                    formulas.put(fieldId, date);
                    continue;
                }
            } else if (FieldType.LISTBOX == fieldType && isList) {
                @SuppressWarnings("unchecked")
                List<Serializable> listPropValue = (List<Serializable>) propValue;
                formulas.put(fieldId, StringUtils.join(listPropValue, "; "));
                continue;
            } else if (FieldType.CHECKBOX == fieldType && propValue instanceof Boolean) {
                String msgKey = (Boolean) propValue ? "yes" : "no";
                formulas.put(fieldId, MessageUtil.getMessage(msgKey));
                continue;
            } else if (FieldType.STRUCT_UNIT == fieldType && isList) {
                formulas.put(fieldId, UserUtil.getDisplayUnitText(propValue));
                continue;
            } else if (isList) {
                continue;
            }

            formulas.put(fieldId, propValue == null ? "" : propValue.toString());
        }

        formulas.put("$docType", documentAdminService.getDocumentTypeName(documentNode));
        getDocumentListStructureFormulae(objectRef, formulas);
        getContractPartyFormulae(objectRef, formulas);

        return formulas;
    }

    private void getContractPartyFormulae(NodeRef objectRef, Map<String, String> formulas) {
        // TODO from implement generic child-node support using propertyDefinition.getChildAssocTypeQNameHierarchy()
        List<ChildAssociationRef> contractPartyChildAssocs = nodeService.getChildAssocs(objectRef, DocumentChildModel.Assocs.CONTRACT_PARTY, RegexQNamePattern.MATCH_ALL);
        int index = 1;
        for (ChildAssociationRef childAssociationRef : contractPartyChildAssocs) {
            Map<QName, Serializable> properties = nodeService.getProperties(childAssociationRef.getChildRef());
            formulas.put(DocumentSpecificModel.Props.PARTY_NAME.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_NAME));
            formulas.put(DocumentSpecificModel.Props.PARTY_EMAIL.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_EMAIL));
            formulas.put(DocumentSpecificModel.Props.PARTY_SIGNER.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_SIGNER));
            formulas.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON.getLocalName() + "." + index, (String) properties.get(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON));
            index++;
        }
    }

    private boolean isIgnoredFieldGroup(String fieldGroupName) {
        return SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(fieldGroupName)
                || SystematicFieldGroupNames.USERS_TABLE.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_REQUEST.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_CHANGE.equals(fieldGroupName)
                || SystematicFieldGroupNames.LEAVE_CANCEL.equals(fieldGroupName)
                || SystematicFieldGroupNames.TRAINING_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT.equals(fieldGroupName)
                || SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT_SUMMARY.equals(fieldGroupName)
                || SystematicFieldGroupNames.DRIVE_COMPENSATION.equals(fieldGroupName);
    }

    private Map<String, String> getEmailFormulas(NodeRef objectRef) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();

        // All properties
        Map<QName, Serializable> props;
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        boolean isTask = workflowDbService.taskExists(objectRef);
        if (isTask) {
            props = RepoUtil
                    .toQNameProperties(workflowDbService.getTask(objectRef, BeanHelper.getWorkflowService().getTaskPrefixedQNames(), null, false).getNode().getProperties());
        } else {
            props = nodeService.getProperties(objectRef);
        }
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            String propName = entry.getKey().getLocalName();
            Serializable propValue = entry.getValue();
            if (propValue == null) {
                continue;
            }
            if (propValue instanceof List<?>) {
                List<?> list = (List<?>) propValue;
                String separator = ", ";
                if (propName.equals(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName())
                        || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName())
                        || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.getLocalName())) {
                    separator = "\n";
                }

                List<String> items = new ArrayList<String>(list.size());
                for (Object object : list) {
                    String itemValue = getTypeSpecificReplacement(object, false);
                    if (StringUtils.isNotBlank(itemValue)) {
                        items.add(itemValue);
                    }
                }
                formulas.put(propName, StringUtils.join(items.iterator(), separator));
            } else {
                boolean alternate = false;
                if (WorkflowCommonModel.Props.STARTED_DATE_TIME.getLocalName().equals(propName) && isTask) {
                    alternate = true;
                }

                formulas.put(propName, getTypeSpecificReplacement(propValue, alternate));
            }
        }

        // Specific formulas
        QName objectType = getDocumentListStructureFormulae(objectRef, formulas);

        {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            formulas.put("recipientNameEmail", generateNameAndEmail(names, emails));
        }

        {
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
            @SuppressWarnings("unchecked")
            List<String> emails = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL);
            formulas.put("additionalRecipientNameEmail", generateNameAndEmail(names, emails));
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.TASK)) {
            Serializable activeProp = workflowDbService.getTaskProperty(objectRef, WorkflowSpecificModel.Props.ACTIVE);
            if (activeProp != null) {
                Boolean isActive = (Boolean) activeProp;
                if (isActive) {
                    formulas.put("activeResponsible", isActive.toString());
                } else {
                    formulas.put("unactiveResponsible", isActive.toString());
                }
            } else {
                formulas.put("coResponsible", Boolean.TRUE.toString());
            }
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.WORKFLOW)) {
            formulas.put("type", MessageUtil.getMessage("workflow_" + objectType.getLocalName()));
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.COMPOUND_WORKFLOW)) {
            formulas.put("url", getCompoundWorkflowUrl(objectRef));
        } else if (dictionaryService.isSubClass(objectType, CaseFileModel.Types.CASE_FILE)) {
            formulas.put("url", getCaseFileUrl(objectRef));
        }

        if (objectType.equals(ForumModel.TYPE_FORUM)) {
            Map<QName, Serializable> properties = nodeService.getProperties(objectRef);
            formulas.put("creatorName", userService.getUserFullName((String) properties.get(ContentModel.PROP_CREATOR)));
        }

        if (objectType.equals(DocumentCommonModel.Types.DOCUMENT)) {
            formulas.putAll(getDocumentFormulas(objectRef));
        }

        /*
         * Spetsifikatsioon "Dokumendi ekraanivorm - Tegevused.docx" punkt 7.1.5.2
         * Kui vastav metaandme väli on täitmata, siis asendamist ei toimu.
         */

        // Remove formulas with empty values
        for (Iterator<Entry<String, String>> i = formulas.entrySet().iterator(); i.hasNext();) {
            Entry<String, String> entry = i.next();
            if (StringUtils.isBlank(entry.getValue())) {
                i.remove();
            }
        }
        return formulas;
    }

    private QName getDocumentListStructureFormulae(NodeRef objectRef, Map<String, String> formulas) {
        QName objectType = BeanHelper.getWorkflowService().getNodeRefType(objectRef);
        if (dictionaryService.isSubClass(objectType, DocumentCommonModel.Types.DOCUMENT)) {
            formulas.put("$functionTitle", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.TITLE));
            formulas.put("$functionMark", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.MARK));
            formulas.put("$seriesTitle", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.TITLE));
            formulas.put("$seriesIdentifier", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.SERIES_IDENTIFIER));
            formulas.put("$caseTitle", getAncestorProperty(objectRef, CaseModel.Types.CASE, CaseModel.Props.TITLE));
            formulas.put("$docUrl", getDocumentUrl(objectRef));

            String volTitle = getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.TITLE);
            formulas.put("$volumeTitle", StringUtils.isNotBlank(volTitle) ? volTitle : getAncestorProperty(objectRef, CaseFileModel.Types.CASE_FILE, VolumeModel.Props.TITLE));

            String volMark = getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.MARK);
            formulas.put("$volumeMark", StringUtils.isNotBlank(volMark) ? volMark : getAncestorProperty(objectRef, CaseFileModel.Types.CASE_FILE, VolumeModel.Props.MARK));
        }

        return objectType;
    }

    @Override
    public String getDocumentUrl(NodeRef document) {
        return getDocumentServerUrlPrefix() + document.getId();
    }

    @Override
    public String getCompoundWorkflowUrl(NodeRef compoundWorkflowRef) {
        if (compoundWorkflowRef != null && workflowService.isIndependentWorkflow(compoundWorkflowRef)) {
            return getCompoundWorkflowServerUrlPrefix() + compoundWorkflowRef.getId();
        }
        return null;
    }

    @Override
    public String getCaseFileUrl(NodeRef caseFileRef) {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_CASE_FILE + "/" + (caseFileRef != null ? caseFileRef.getId() : "");
    }

    @Override
    public String getVolumeUrl(NodeRef volumeRef) {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_VOLUME + "/" + volumeRef.getId();
    }

    @Override
    public String getCompoundWorkflowServerUrlPrefix() {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_NODEREF + "/";
    }

    @Override
    public String getDocumentServerUrlPrefix() {
        return getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_DOCUMENT + "/";
    }

    @Override
    public String getServerUrl() {
        return applicationService.getServerUrl() + servletContext.getContextPath();
    }

    private String getTypeSpecificReplacement(Object object, boolean alternate) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof Date) {
            return alternate ? dateTimeFormat.format((Date) object) : dateFormat.format((Date) object);
        }
        if (object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double || object instanceof Boolean) {
            return object.toString();
        }
        return null;
    }

    private String getVacationOrderSubstitutionData(Map<QName, Serializable> properties) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) properties.get(DocumentSpecificModel.Props.SUBSTITUTE_NAME);
        @SuppressWarnings("unchecked")
        List<Date> startDates = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE);
        @SuppressWarnings("unchecked")
        List<Date> endDates = (List<Date>) properties.get(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE);
        String until = I18NUtil.getMessage("template_until");

        int size = 0;
        if (names != null) {
            size = names.size();
        } else if (startDates != null) {
            size = startDates.size();
        } else if (endDates != null) {
            size = endDates.size();
        }
        List<String> substitutes = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            String name = names != null && i < names.size() ? names.get(i) : "";
            Date startDate = startDates != null && i < startDates.size() ? startDates.get(i) : null;
            Date endDate = endDates != null && i < endDates.size() ? endDates.get(i) : null;
            if (StringUtils.isEmpty(name) && startDate == null && endDate == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(name)
                    .append(" ")
                    .append(startDate == null ? "" : dateFormat.format(startDate))
                    .append(" ")
                    .append(until)
                    .append(" ")
                    .append(endDate == null ? "" : dateFormat.format(endDate));
            substitutes.add(sb.toString());
        }
        return StringUtils.join(substitutes.iterator(), "\n");
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
            String row = name == null ? "" : name;
            if (StringUtils.isNotBlank(email)) {
                row += " (" + email + ")";
            }
            if (!StringUtils.isBlank(row)) {
                rows.add(row);
            }
        }
        return StringUtils.join(rows.iterator(), "\n"); // XXX \r was used for OO

    }

    /**
     * @param foundPattern
     * @param document
     * @return
     */
    private String getAncestorProperty(NodeRef document, QName ancestorType, QName property) {
        Node parent = generalService.getAncestorWithType(document, ancestorType);
        return (parent != null) ? nodeService.getProperty(parent.getNodeRef(), property).toString() : "";
    }

    @Override
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
            if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                NodeRef docType = generalService.getNodeRef(DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE + "/" + DocumentAdminModel.PREFIX + dt.getDocTypeId());
                if (docType != null) {
                    dt.setDocTypeName((String) nodeService.getProperty(docType, DocumentAdminModel.Props.NAME));
                }
            } else if (nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
                dt.setDocTypeName("");
            } else {
                dt.setDocTypeName(dt.getDocTypeId());
            }
            templates.add(dt);
        }
        return templates;
    }

    @Override
    public NodeRef getRoot() {
        return generalService.getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
    }

    @Override
    public List<DocumentTemplate> getDocumentTemplates(String docTypeId) {
        Assert.notNull(docTypeId, "Parameter docTypeId is mandatory.");
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (docTypeId.equals(template.getDocTypeId())) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<DocumentTemplate> getEmailTemplates() {
        List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();
        for (DocumentTemplate template : getTemplates()) {
            if (nodeService.hasAspect(template.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<SelectItem> getReportTemplates(TemplateReportType typeId) {
        Assert.notNull(typeId, "Parameter typeId is mandatory.");
        List<SelectItem> result = new ArrayList<SelectItem>();
        String typeStr = typeId.toString();
        for (DocumentTemplate template : getTemplates()) {
            if (nodeService.hasAspect(template.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_REPORT) && typeStr.equals(template.getReportType())) {
                result.add(new SelectItem(template.getName()));
            }
        }
        return result;
    }

    @Override
    public List<Pair<SelectItem, String>> getReportTemplatesWithOutputTypes(TemplateReportType typeId) {
        Assert.notNull(typeId, "Parameter typeId is mandatory.");
        List<Pair<SelectItem, String>> result = new ArrayList<Pair<SelectItem, String>>();
        String typeStr = typeId.toString();
        for (DocumentTemplate template : getTemplates()) {
            if (nodeService.hasAspect(template.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_REPORT) && typeStr.equals(template.getReportType())) {
                Serializable outputType = BeanHelper.getNodeService().getProperty(template.getNodeRef(), DocumentTemplateModel.Prop.REPORT_OUTPUT_TYPE);
                if (outputType != null) {
                    String templateOutputType = outputType.toString();
                    SelectItem item = new SelectItem(template.getName());
                    result.add(new Pair<SelectItem, String>(item, templateOutputType));
                }
            }
        }
        return result;
    }

    private String escapeXml(String replaceString) {
        if (replaceString == null) {
            return "";
        }
        replaceString = replaceString.replaceAll("&", "&amp;");
        replaceString = replaceString.replaceAll("\"", "&quot;");
        replaceString = replaceString.replaceAll("<", "&lt;");
        replaceString = replaceString.replaceAll(">", "&gt;");
        replaceString = replaceString.replaceAll("'", "&apos;");
        return replaceString;
    }

    @Override
    public NodeRef getNotificationTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION;
        return getTemplateByName(templateName, templateAspect);
    }

    @Override
    public NodeRef getEmailTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_EMAIL;
        return getTemplateByName(templateName, templateAspect);
    }

    @Override
    public NodeRef getArchivalReportTemplateByName(String templateName) {
        QName templateAspect = DocumentTemplateModel.Aspects.TEMPLATE_ARCHIVAL_REPORT;
        return getTemplateByName(templateName, templateAspect);
    }

    private NodeRef getTemplateByName(String templateName, QName templateAspect) {
        if (StringUtils.isNotEmpty(templateName)) {
            NodeRef template = fileFolderService.searchSimple(getRoot(), templateName);
            if (template != null && nodeService.hasAspect(template, templateAspect)) {
                return template;
            }
            if (StringUtils.endsWith(templateName, ".html")) {
                // try alternative ".htm" that is default when adding system template through GUI
                templateName = templateName.substring(0, templateName.length() - 1);
                return getTemplateByName(templateName, templateAspect);
            }
        }
        return null;
    }

    @Override
    public NodeRef getReportTemplateByName(String templateName, TemplateReportType reportType) {
        Assert.notNull(reportType);
        if (StringUtils.isBlank(templateName)) {
            return null;
        }
        NodeRef template = fileFolderService.searchSimple(getRoot(), templateName);
        if (template != null && nodeService.hasAspect(template, DocumentTemplateModel.Aspects.TEMPLATE_REPORT)
                && reportType.toString().equals(nodeService.getProperty(template, DocumentTemplateModel.Prop.REPORT_TYPE))) {
            return template;
        }
        return null;
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setOpenOfficeService(OpenOfficeService openOfficeService) {
        this.openOfficeService = openOfficeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    private FileService getFileService() {
        if (_fileService == null) {
            _fileService = BeanHelper.getFileService();
        }
        return _fileService;
    }

    // END: getters / setters

>>>>>>> develop-5.1
}