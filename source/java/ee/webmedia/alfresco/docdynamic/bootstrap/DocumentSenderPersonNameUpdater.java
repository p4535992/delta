package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.SENDER_INITIALS_TO_ADR;
import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.SENDER_PERSON_NAME;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.SENDER_DETAILS_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class DocumentSenderPersonNameUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentSenderPersonNameUpdater.class);
    private static final Set<String> FIELD_NAMES = new HashSet<>(Arrays.asList(SENDER_DETAILS_NAME.getLocalName(), SENDER_PERSON_NAME.getLocalName()));

    private final Set<String> updatedDocumentTypeVersions = new HashSet<>();
    private FieldDefinition senderPersonNameField;

    private DocumentAdminService documentAdminService;
    private BaseService baseService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generatePropertyNotNullQuery(SENDER_DETAILS_NAME),
                SearchUtil.generatePropertyNotNullQuery(SENDER_INITIALS_TO_ADR)
                );
        List<ResultSet> resultSets = new ArrayList<>(2);
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        Map<QName, Serializable> docProps = nodeService.getProperties(docRef);
        String typeId = (String) docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        int versionNr = (int) docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR);

        String typeAndVersionStr = typeId + "-" + versionNr;
        if (!updatedDocumentTypeVersions.contains(typeAndVersionStr)) {
            updateDocumentTypeVersion(typeId, versionNr, typeAndVersionStr);
        }

        String result = "";
        String senderName = (String) docProps.get(SENDER_DETAILS_NAME);
        boolean sendInitialsToAdr = Boolean.TRUE.equals(docProps.get(SENDER_INITIALS_TO_ADR));
        if (sendInitialsToAdr) {
            if (StringUtils.isNotBlank(senderName)) {
                nodeService.setProperty(docRef, SENDER_DETAILS_NAME, "");
                nodeService.setProperty(docRef, SENDER_PERSON_NAME, senderName);
            }
            result += SENDER_DETAILS_NAME.getLocalName() + ": " + senderName + " -> '', " + SENDER_PERSON_NAME.getLocalName() + ": '' -> " + senderName;
        } else {
            nodeService.setProperty(docRef, SENDER_PERSON_NAME, "");
            result += SENDER_DETAILS_NAME.getLocalName() + ": " + senderName + " (uncahnged), " + SENDER_PERSON_NAME.getLocalName() + " to blank";
        }

        return new String[] { result };
    }

    private void updateDocumentTypeVersion(String typeId, int versionNr, String typeAndVersionStr) {
        Pair<DocumentType, DocumentTypeVersion> typeAndVersion = documentAdminService.getDocumentTypeAndVersion(typeId, versionNr, true);
        DocumentTypeVersion docVersion = typeAndVersion.getSecond();
        Collection<Field> fields = docVersion.getFieldsById(FIELD_NAMES);

        Field senderPersonName = getField(fields, SENDER_PERSON_NAME);
        if (senderPersonName != null) {
            LOG.info("Not updating " + typeId + " (ver: " + versionNr + ") because it already has " + SENDER_PERSON_NAME.getLocalName() + " field");
        } else {
            LOG.info("Updating " + typeId + " (ver: " + versionNr + ")");
            Field senderName = getField(fields, SENDER_DETAILS_NAME);
            Field f = new Field(senderName.getParent());
            documentAdminService.copyFieldProps(getSenderPersonNameField(), f);
            ChildrenList<MetadataItem> metadata = docVersion.getMetadata();
            int order = senderName.getOrder();
            f.setOrder(order);
            metadata.add(order, f);
            baseService.saveObject(docVersion);
        }
        updatedDocumentTypeVersions.add(typeAndVersionStr);
    }

    private FieldDefinition getSenderPersonNameField() {
        if (senderPersonNameField != null) {
            return senderPersonNameField;
        }
        List<FieldDefinition> fieldDefinitions = documentAdminService.getFieldDefinitions();
        for (FieldDefinition def : fieldDefinitions) {
            if (SENDER_PERSON_NAME.getLocalName().equals(def.getFieldId())) {
                senderPersonNameField = def;
                break;
            }
        }
        return senderPersonNameField;
    }

    private Field getField(Collection<Field> fields, QName fieldName) {
        for (Field field : fields) {
            if (fieldName.getLocalName().equals(field.getFieldId())) {
                return field;
            }
        }
        return null;
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        documentAdminService.clearDynamicTypesCache();
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }

}
