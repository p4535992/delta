/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package ee.webmedia.alfresco.webdav;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getMsoService;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Implements the WebDAV PUT method
 * 
 * @author Gavin Cornwell
 */
public class PutMethod extends WebDAVMethod {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PutMethod.class);

    // Request parameters
    private String m_strContentType = null;

    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        m_strContentType = m_request.getHeader(WebDAV.HEADER_CONTENT_TYPE);

        // Get the lock token, if any
        parseIfHeader();
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void parseRequestBody() throws WebDAVServerException {
        // Nothing to do in this method, the body contains
        // the content it will be dealt with later
    }

    /**
     * Exceute the WebDAV request
     * 
     * @exception WebDAVServerException
     */
    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        FileFolderService fileFolderService = getFileFolderService();

        // Get the status for the request path
        FileInfo contentNodeInfo = null;
        boolean created = false;
        try {
            contentNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
            // make sure that we are not trying to use a folder
            if (contentNodeInfo.isFolder()) {
                throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (FileNotFoundException e) {
            // create not allowed
            throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
        }
        NodeRef fileRef = contentNodeInfo.getNodeRef();
        WebDAVCustomHelper.checkDocumentFileWritePermission(fileRef);

        // Require the file to be locked for current user
        LockStatus lockStatus = getDocLockService().getLockStatus(fileRef);
        if (!LockStatus.LOCK_OWNER.equals(lockStatus)) {
            log.info("Not saving " + fileRef + ". LockStatus is " + lockStatus.name() + ", lock owner " + getDocLockService().getLockOwnerIfLocked(fileRef));
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED);
        }

        if (m_request.getContentLength() <= 0) {
            StringBuilder s = new StringBuilder("Client is trying to save zero-length content, ignoring and returning success; request headers:");
            for (Enumeration<?> e = m_request.getHeaderNames(); e.hasMoreElements();) {
                String headerName = (String) e.nextElement();
                s.append("\n  ").append(headerName).append(": ").append(m_request.getHeader(headerName));
            }
            log.warn(s.toString());
            // Set the response status, depending if the node existed or not
            m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK);

            m_response.setContentType("text/plain");
            m_response.setCharacterEncoding("UTF-8");
            PrintWriter writer = m_response.getWriter();
            try {
                writer.println("You are trying to save zero-length content, we are ignoring it and returning a successful result.");
                writer.println("This is probably a weird behaviour of the WebDAV client [described here http://java.net/jira/browse/JERSEY-154]:");
                writer.print("Some HTTP clients are sending empty bodies in PUTs. e. g. Microsoft's 'WebDAV-Mini-Redirector' does this: It first sends a PUT with Content-Length=0 ");
                writer.print("and an empty (zero bytes) body, and if that returns 200 OK it sends another PUT with 'correct' concent-length and full body; seems to be somekind of ");
                writer.println("safety or performance optimization.");
                writer.flush();
            } finally {
                writer.close();
            }
            return;
        }

        // Update the version if the node is unlocked
        boolean createdNewVersion = ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersion(fileRef, contentNodeInfo.getName(), true);

        // Access the content
        ContentWriter writer = fileFolderService.getWriter(fileRef);

        // Get the input stream from the request data
        InputStream is = m_request.getInputStream();

        // Do not allow to change mimeType or locale, use the same values as were set during file creation
        ContentData contentData = contentNodeInfo.getContentData();
        if (contentData == null) {
            log.warn("ContentData for node is null: " + fileRef);

            // set content properties
            String mimetype = getMimetypeService().guessMimetype(contentNodeInfo.getName());
            writer.setMimetype(mimetype);

            // Get the input stream from the request data
            is = is.markSupported() ? is : new BufferedInputStream(is);

            ContentCharsetFinder charsetFinder = getMimetypeService().getContentCharsetFinder();
            Charset encoding = charsetFinder.getCharset(is, mimetype);
            writer.setEncoding(encoding.name());

        } else {
            String mimetype = contentData.getMimetype();
            writer.setMimetype(mimetype);
            writer.setEncoding(contentData.getEncoding());
            if (m_strContentType != null && !mimetype.equalsIgnoreCase(m_strContentType)) {
                log.info("Client sent different mimetype '" + m_strContentType + "' when updating file with original mimetype '" + mimetype + "', ignoring");
            }
        }

        // Write the new data to the content node
        writer.putContent(is);

        if (writer.getSize() <= 0) {
            throw new RuntimeException("Saving zero-length content is not allowed" + ", is=" + is);
        }

        // add the user and date information to the custom aspect properties
        ((WebDAVCustomHelper) getDAVHelper()).getVersionsService().updateVersionModifiedAspect(fileRef);

        // Update document search info
        NodeRef document = getNodeService().getPrimaryParent(fileRef).getParentRef();
        ((WebDAVCustomHelper) getDAVHelper()).getDocumentService().updateSearchableFiles(document);

        // Update Document meta data and generated files
        updateDocumentAndGeneratedFiles(contentNodeInfo, document);

        // Set the response status, depending if the node existed or not
        m_response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
        logger.debug("saved file " + fileRef + ", " + (createdNewVersion ? "created" : "didn't crerate") + " new version");
    }

    private void updateDocumentAndGeneratedFiles(FileInfo contentNodeInfo, NodeRef document) {
        String generationType = (String) getNodeService().getProperty(contentNodeInfo.getNodeRef(), FileModel.Props.GENERATION_TYPE);
        if (!GeneratedFileType.WORD_TEMPLATE.name().equals(generationType)) {
            return;
        }

        if (!getMsoService().isAvailable()) {
            log.debug("MsoService is not available, skipping updating document");
            return;
        }
        Map<String, String> formulas;
        try {
            formulas = getMsoService().modifiedFormulas(getContentService().getReader(contentNodeInfo.getNodeRef(), ContentModel.PROP_CONTENT));
        } catch (Exception e) {
            throw new RuntimeException("Error getting formulas from MS Word file " + contentNodeInfo.getNodeRef() + " : " + e.getMessage(), e);
        }

        if (formulas == null || formulas.isEmpty()) {
            return;
        }

        DocumentDynamicService documentDynamicService = BeanHelper.getDocumentDynamicService();
        DocumentDynamic doc = documentDynamicService.getDocument(document);
        /*
         * TODO CL 179488, Mallide loomine.docx 3.1.8.1
         * if (!doc.isUpdateMetadataInFile()) {
         * return;
         * }
         */

        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = BeanHelper.getDocumentConfigService().getPropertyDefinitions(doc.getNode());

        List<String> updateDisabled = Arrays.asList(
                DocumentCommonModel.Props.OWNER_NAME.getLocalName(), DocumentCommonModel.Props.SIGNER_NAME.getLocalName()
                , DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName(), DocumentCommonModel.Props.OWNER_ID.getLocalName()
                , DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName()
                , DocumentCommonModel.Props.DOC_STATUS.getLocalName(), DocumentCommonModel.Props.REG_NUMBER.getLocalName()
                , DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(), DocumentCommonModel.Props.REG_DATE_TIME.getLocalName()
                , DocumentCommonModel.Props.INDIVIDUAL_NUMBER.getLocalName());
        List<FieldType> readOnlyFields = Arrays.asList(FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE, FieldType.LISTBOX, FieldType.CHECKBOX, FieldType.INFORMATION_TEXT);
        List<ContractPartyField> partyFields = new ArrayList<ContractPartyField>();
        ClassificatorService classificatorService = BeanHelper.getClassificatorService();

        for (Entry<String, String> entry : formulas.entrySet()) {
            String formulaKey = entry.getKey();
            String formulaValue = entry.getValue();

            // Check for special fields like recipients or contract parties
            int propIndex = -1;
            if (formulaKey.contains(".")) {
                String[] split = StringUtils.split(formulaKey, '.');
                formulaKey = split[0];
                propIndex = Integer.parseInt(split[1]) - 1; // Formula uses base 1 index

                // Since contract party is implemented using child nodes, we cannot check directly from document property definitions
                QName fieldQName = null;
                if (DocumentSpecificModel.Props.PARTY_NAME.getLocalName().equals(formulaKey)) {
                    fieldQName = DocumentSpecificModel.Props.PARTY_NAME;
                } else if (DocumentSpecificModel.Props.PARTY_EMAIL.getLocalName().equals(formulaKey)) {
                    fieldQName = DocumentSpecificModel.Props.PARTY_EMAIL;
                } else if (DocumentSpecificModel.Props.PARTY_SIGNER.getLocalName().equals(formulaKey)) {
                    fieldQName = DocumentSpecificModel.Props.PARTY_SIGNER;
                } else if (DocumentSpecificModel.Props.PARTY_CONTACT_PERSON.getLocalName().equals(formulaKey)) {
                    fieldQName = DocumentSpecificModel.Props.PARTY_CONTACT_PERSON;
                }
                if (fieldQName != null) {
                    partyFields.add(new ContractPartyField(propIndex, fieldQName, formulaValue));
                }
            }

            Pair<DynamicPropertyDefinition, Field> propDefAndField = propertyDefinitions.get(formulaKey);
            if (propDefAndField == null || propDefAndField.getSecond() == null) {
                continue;
            }

            PropertyDefinition propDef = propDefAndField.getFirst();
            Field field = propDefAndField.getSecond();

            // If field is not changeable, then don't allow it.
            if (isFieldUnchangeable(doc, updateDisabled, readOnlyFields, field, classificatorService, formulaValue)) {
                continue;
            }

            BaseObject parent = field.getParent();
            DataTypeDefinition dataType = propDef.getDataType();
            if (parent instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) parent;
                String name = group.getName();

                if (group.isSystematic() && (SystematicFieldGroupNames.RECIPIENTS.equals(name) || SystematicFieldGroupNames.ADDITIONAL_RECIPIENTS.equals(name))) {
                    Serializable propValue = doc.getProp(field.getQName());
                    if (propDef.isMultiValued()) {
                        if (propValue != null) {
                            @SuppressWarnings("unchecked")
                            List<Serializable> values = (List<Serializable>) propValue;
                            if (propIndex > -1 && propIndex < values.size()) {
                                values.set(propIndex, (Serializable) DefaultTypeConverter.INSTANCE.convert(dataType, formulaValue));
                            }
                            propValue = (Serializable) values;
                        }
                    }
                    doc.setPropIgnoringEmpty(field.getQName(), propValue);
                    continue;
                }
            }

            if (Arrays.asList(FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS).contains(field.getFieldTypeEnum())) {
                continue;
            }

            Serializable value;
            // Handle dates separately
            if ("date".equals(dataType.getName().getLocalName())) {
                if (StringUtils.isBlank(formulaValue)) {
                    value = null;
                } else {
                    try {
                        value = new SimpleDateFormat("dd.MM.yyyy").parse(formulaValue);
                    } catch (ParseException e) {
                        throw new RuntimeException("Unable to parse date value from field '" + formulaKey + "': " + e.getMessage(), e);
                    }
                }
            } else {
                value = (Serializable) DefaultTypeConverter.INSTANCE.convert(dataType, formulaValue);
            }
            if (propDef.isMultiValued()) {
                value = (Serializable) Collections.singletonList(value); // is this correct?
            }
            doc.setPropIgnoringEmpty(field.getQName(), value);
        }

        // Update sub-nodes
        // TODO from implement generic child-node support using propertyDefinition.getChildAssocTypeQNameHierarchy()
        if (!partyFields.isEmpty()) {
            NodeService nodeService = getNodeService();
            List<ChildAssociationRef> contractPartyChildAssocs = nodeService.getChildAssocs(document, DocumentChildModel.Assocs.CONTRACT_PARTY, RegexQNamePattern.MATCH_ALL);
            for (ContractPartyField field : partyFields) {
                if (field.getIndex() < contractPartyChildAssocs.size()) {
                    NodeRef childNodeRef = contractPartyChildAssocs.get(field.getIndex()).getChildRef();
                    Serializable propValue = field.getValue();
                    Serializable origPropValue = nodeService.getProperty(childNodeRef, field.getField());
                    if ((propValue == null || ((propValue instanceof String) && ((String) propValue).isEmpty()))
                            && (origPropValue == null || ((origPropValue instanceof String) && ((String) origPropValue).isEmpty()))) {
                        continue;
                    }
                    nodeService.setProperty(childNodeRef, field.getField(), propValue);
                }
            }
        }

        documentDynamicService.updateDocument(doc, null, false); // This also updates generated files
    }

    private boolean isFieldUnchangeable(DocumentDynamic doc, List<String> updateDisabled, List<FieldType> readOnlyFields, Field field, ClassificatorService classificatorService,
            String formulaValue) {
        return FieldChangeableIf.ALWAYS_NOT_CHANGEABLE == field.getChangeableIfEnum()
                || FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC == field.getChangeableIfEnum()
                && !DocumentStatus.WORKING.getValueName().equals(doc.getProp(DocumentCommonModel.Props.DOC_STATUS))
                || updateDisabled.contains(field.getFieldId()) || readOnlyFields.contains(field.getFieldTypeEnum())
                || DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName().equals(field.getOriginalFieldId())
                || DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName().equals(field.getOriginalFieldId())
                || FieldType.STRUCT_UNIT == field.getFieldTypeEnum()
                || FieldType.COMBOBOX == field.getFieldTypeEnum() && field.isComboboxNotRelatedToClassificator()
                || FieldType.COMBOBOX == field.getFieldTypeEnum() && !classificatorService.hasClassificatorValueName(field.getClassificator(), formulaValue);
    }

    private class ContractPartyField implements Serializable {
        private static final long serialVersionUID = 1L;

        final private int index;
        final private QName field;
        final private Serializable value;

        public ContractPartyField(int index, QName field, Serializable value) {
            this.index = index;
            this.field = field;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public QName getField() {
            return field;
        }

        public Serializable getValue() {
            return value;
        }
    }
}
