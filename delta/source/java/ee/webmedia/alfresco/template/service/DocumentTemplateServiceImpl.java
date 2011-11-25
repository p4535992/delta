package ee.webmedia.alfresco.template.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.OpenOfficeService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
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
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.ISOLatin1Util;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

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

    private GeneralService generalService;
    private NodeService nodeService;
    private FileService fileService;
    private MimetypeService mimetypeService;
    private FileFolderService fileFolderService;
    private DocumentLogService documentLogService;
    private OpenOfficeService openOfficeService;
    private DictionaryService dictionaryService;
    private MsoService msoService;
    private ApplicationService applicationService;
    private ServletContext servletContext;
    private UserService userService;

    private static BeanPropertyMapper<DocumentTemplate> templateBeanPropertyMapper;
    static {
        templateBeanPropertyMapper = BeanPropertyMapper.newInstance(DocumentTemplate.class);
    }

    @Override
    public void updateGeneratedFiles(NodeRef docRef, boolean isRegistering) {
        if (!msoService.isAvailable()) {
            // Temporary safeguard until OO document templates & formulas are properly implemented
            return;
        }
        List<FileInfo> files = fileFolderService.listFiles(docRef);
        log.debug("Found " + files.size() + " files under document " + docRef);
        for (FileInfo file : files) {
            // This check ensures, that only proper DOC files are passed to Word
            // Unfortunately DOT files have mimetype application/octet-stream, and therefore MsoService must accept this mimetype also
            // So without this check, every binary file would be passed to word, which would be unnecessary and very time consuming
            Map<QName, Serializable> fileProps = file.getProperties();
            if ((fileProps.get(FileModel.Props.ACTIVE) == null || Boolean.TRUE.equals(fileProps.get(FileModel.Props.ACTIVE)))
                    && fileService.isFileGeneratedFromTemplate(file.getNodeRef())) {
                replaceFormulas(docRef, file.getNodeRef(), file.getNodeRef(), file.getName(), isRegistering);
            }
        }
    }

    @Override
    public void updateDocTemplate(Node docTemplNode) {
        generalService.setPropertiesIgnoringSystem(docTemplNode.getNodeRef(), docTemplNode.getProperties());
    }

    @Override
    public DocumentTemplate getDocumentsTemplate(NodeRef document) {
        String documentTypeId = (String) nodeService.getProperty(document, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        // it's OK to pick first one
        FileInfo word2003Template = null;
        for (FileInfo fi : fileFolderService.listFiles(getRoot())) {
            if (!nodeService.hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
                continue;
            }

            if (!documentTypeId.equals(fi.getProperties().get(DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                continue;
            }

            // If current file is 2007/2010 template, return the first one fount
            if (StringUtils.endsWithIgnoreCase(fi.getName(), ".dotx")) {
                return setupDocumentTemplate(fi);
            }

            // Otherwise mark it as a candidate if only 2003 binary template is present
            word2003Template = fi;
        }

        if (word2003Template != null) {
            return setupDocumentTemplate(word2003Template);
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
    public String populateTemplate(NodeRef documentNodeRef) throws FileNotFoundException {
        if (!msoService.isAvailable()) {
            // Temporary safeguard until OO document templates & formulas are properly implemented
            throw new UnableToPerformException("document_errorMsg_template_processsing_failed_mso_missing");
        }

        log.debug("Creating a file from template for document: " + documentNodeRef);
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
            nodeRef = getDocumentsTemplate(documentNodeRef).getNodeRef();
        } else {
            nodeRef = getTemplateByName(templName).getNodeRef();
        }
        String templateFilename = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        log.debug("Using template: " + templateFilename);

        String extension = mimetypeService.getExtension(MimetypeMap.MIMETYPE_WORD);
        if (StringUtils.endsWithIgnoreCase(templateFilename, ".dotx")) {
            extension += "x"; // Well, ain't this nice! :) (Filename must end with docx if 2003/2007 template is used)
        }
        String name = ((String) docProp.get(DocumentCommonModel.Props.DOC_NAME));
        String displayName = fileService.getUniqueFileDisplayName(documentNodeRef, name + "." + extension);
        name = FilenameUtil.replaceAmpersand(ISOLatin1Util.removeAccents(FilenameUtil.buildFileName(name,
                extension)));
        name = FilenameUtil.replaceNonAsciiCharacters(name);
        name = FilenameUtil.limitFileNameLength(name);
        name = generalService.getUniqueFileName(documentNodeRef, name);

        ee.webmedia.alfresco.document.file.model.File populatedTemplate = new ee.webmedia.alfresco.document.file.model.File(fileFolderService.create(
                documentNodeRef, name, ContentModel.TYPE_CONTENT));
        // Mark down the template that was used to generate the file
        nodeService.setProperty(populatedTemplate.getNodeRef(), FileModel.Props.GENERATED_FROM_TEMPLATE, templateFilename);
        nodeService.setProperty(populatedTemplate.getNodeRef(), FileModel.Props.GENERATION_TYPE, GeneratedFileType.WORD_TEMPLATE.name());

        // so we can process it during document registration
        nodeService.setProperty(populatedTemplate.getNodeRef(), FileModel.Props.DISPLAY_NAME, displayName);

        documentLogService.addDocumentLog(documentNodeRef, I18NUtil.getMessage("document_log_status_fileAdded", displayName));
        log.debug("Created new node: " + populatedTemplate.getNodeRef() + "\nwith name: " + name + "; displayName: " + displayName);
        // Set document content's mimetype and encoding from template

        replaceFormulas(documentNodeRef, nodeRef, populatedTemplate.getNodeRef(), templateFilename);
        return displayName;
    }

    private void replaceFormulas(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName) {
        replaceFormulas(document, sourceFile, destinationFile, sourceFileName, false);
    }

    private void replaceFormulas(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName, boolean finalize) {
        if (!StringUtils.endsWithIgnoreCase(sourceFileName, ".doc") && !StringUtils.endsWithIgnoreCase(sourceFileName, ".docx")
                && !StringUtils.endsWithIgnoreCase(sourceFileName, ".dot") && !StringUtils.endsWithIgnoreCase(sourceFileName, ".dotx")) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_invalid_file_extension");
        }

        Map<String, String> formulas = getFormulas(document);
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(formulas.entrySet()));
        }
        if (msoService.isAvailable()) {
            if (finalize) {
                formulas.put("FINALIZE", "1");
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
                throw new UnableToPerformException(MessageSeverity.ERROR, errorKey);
            } catch (Exception e) {
                throw new UnableToPerformException(MessageSeverity.ERROR, "template_replace_formulas_failed", e);
            }
        } else {
            replaceFormulasWithOpenOffice(document, sourceFile, destinationFile, sourceFileName);
        }
    }

    private void replaceFormulasWithOpenOffice(NodeRef document, NodeRef sourceFile, NodeRef destinationFile, String sourceFileName) {
        Map<String, String> formulas = getFormulas(document);
        if (log.isDebugEnabled()) {
            log.debug("Produced formulas " + WmNode.toString(formulas.entrySet()));
        }

        int retry = 3;
        do {
            ContentReader reader = fileFolderService.getReader(sourceFile);
            ContentWriter writer = fileFolderService.getWriter(destinationFile);
            writer.setMimetype(MimetypeMap.MIMETYPE_WORD);

            try {
                openOfficeService.replace(reader, writer, formulas);
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
                        .append(vol.getDispositionDate() == null ? "" : dateFormat.format(vol.getDispositionDate()))
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
        .append(doc.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID))
        .append("</td><td>")
        .append("<a href=\"").append(getDocumentUrl(doc.getNodeRef())).append(">").append(doc.getDocName()).append("</a>")
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
        ContentReader templateReader = fileFolderService.getReader(template);
        String templateTxt = templateReader.getContentString();
        if (dataNodeRefs.size() == 0) {
            return templateTxt;
        }

        Map<String, String> allFormulas = new LinkedHashMap<String, String>();
        for (Entry<String, NodeRef> entry : dataNodeRefs.entrySet()) {
            Map<String, String> formulas = getFormulas(entry.getValue());
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
                matcher.appendReplacement(result, escapeXml(subResult));
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
            matcher.appendReplacement(result, formulaResult);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Map<String, String> getFormulas(NodeRef objectRef) {
        Map<String, String> formulas = new LinkedHashMap<String, String>();

        // All properties
        Map<QName, Serializable> props = nodeService.getProperties(objectRef);
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
                if ("task.startedDateTime".equals(propName)) {
                    alternate = true;
                }

                formulas.put(propName, getTypeSpecificReplacement(propValue, alternate));
            }
        }

        // Specific formulas
        QName objectType = nodeService.getType(objectRef);
        if (dictionaryService.isSubClass(objectType, DocumentCommonModel.Types.DOCUMENT)) {
            formulas.put("functionTitle", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.TITLE));
            formulas.put("functionMark", getAncestorProperty(objectRef, FunctionsModel.Types.FUNCTION, FunctionsModel.Props.MARK));
            formulas.put("seriesTitle", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.TITLE));
            formulas.put("seriesIdentifier", getAncestorProperty(objectRef, SeriesModel.Types.SERIES, SeriesModel.Props.SERIES_IDENTIFIER));
            formulas.put("volumeTitle", getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.TITLE));
            formulas.put("volumeMark", getAncestorProperty(objectRef, VolumeModel.Types.VOLUME, VolumeModel.Props.MARK));
            String docUrl = getDocumentUrl(objectRef);
            formulas.put("docUrl", docUrl);
        }

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

        if (SystematicDocumentType.VACATION_APPLICATION.getId().equals(nodeService.getProperty(objectRef, DocumentAdminModel.Props.OBJECT_TYPE_ID))) {
            formulas.put("vacationOrderSubstitutionData", getVacationOrderSubstitutionData(props));
        }

        if (dictionaryService.isSubClass(objectType, WorkflowCommonModel.Types.TASK)) {
            if (nodeService.hasAspect(objectRef, WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                Serializable activeProp = props.get(WorkflowSpecificModel.Props.ACTIVE);
                if (activeProp != null) {
                    Boolean isActive = (Boolean) activeProp;
                    if (isActive) {
                        formulas.put("activeResponsible", isActive.toString());
                    } else {
                        formulas.put("unactiveResponsible", isActive.toString());
                    }
                }
            } else {
                formulas.put("coResponsible", Boolean.TRUE.toString());
            }
        }

        if (objectType.equals(ForumModel.TYPE_FORUM)) {
            Map<QName, Serializable> properties = nodeService.getProperties(objectRef);
            formulas.put("creatorName", userService.getUserFullName((String) properties.get(ContentModel.PROP_CREATOR)));
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

    @Override
    public String getDocumentUrl(NodeRef document) {
        return applicationService.getServerUrl() + servletContext.getContextPath() + "/n/document/" + document.getId();
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
        return (parent != null) ? nodeService.getProperty(parent.getNodeRef(), property).toString() : null;
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
    public NodeRef getSystemTemplateByName(String templateName) {
        if (StringUtils.isNotEmpty(templateName)) {
            NodeRef template = fileFolderService.searchSimple(getRoot(), templateName);
            if (template != null) {
                return template;
            }
            if (StringUtils.endsWith(templateName, ".html")) {
                // try alternative ".htm" that is default when adding system template through GUI
                templateName = templateName.substring(0, templateName.length() - 1);
                return getSystemTemplateByName(templateName);
            }
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

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
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

    // END: getters / setters

}
