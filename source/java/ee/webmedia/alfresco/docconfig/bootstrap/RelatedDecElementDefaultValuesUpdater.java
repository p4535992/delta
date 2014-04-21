package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.dvk.service.DecContainerHandler;

/**
 * This class updates the default values of relatedOutgoingDecElement and relatedIncomingDecElement
 */
public class RelatedDecElementDefaultValuesUpdater extends AbstractNodeUpdater {

    /**
     * Specifies whether to update related<b>Outgoing</b>DecElement, related<b>Incoming</b>DecElement or <b>both</b> of them.
     */
    private enum Related {
        OUTGOING, INCOMING, BOTH;
    }

    private static Map<QName, Pair<Related, String>> mappedFields = new HashMap<QName, Pair<Related, String>>();
    private static Map<String, QName> localNameAndQName = new HashMap<String, QName>();

    static {
        mappedFields.put(DocumentCommonModel.Props.DOC_NAME, new Pair<Related, String>(Related.BOTH, "<RecordMetaData><RecordTitle>"));
        mappedFields.put(DocumentCommonModel.Props.COMMENT, new Pair<Related, String>(Related.INCOMING, "<Recipient><MessageForRecipient>"));
        mappedFields.put(DocumentSpecificModel.Props.DUE_DATE, new Pair<Related, String>(Related.BOTH, "<RecordMetaData><ReplyDueDate>"));
        mappedFields.put(DocumentCommonModel.Props.REG_NUMBER, new Pair<Related, String>(Related.OUTGOING, "<RecordMetaData><RecordOriginalIdentifier>"));
        mappedFields.put(DocumentCommonModel.Props.REG_DATE_TIME, new Pair<Related, String>(Related.OUTGOING, "<RecordMetaData><RecordDateRegistered>"));
        mappedFields.put(DocumentCommonModel.Props.OWNER_NAME, new Pair<Related, String>(Related.OUTGOING, "<RecordCreator><Person><Name>"));
        mappedFields.put(DocumentCommonModel.Props.OWNER_EMAIL, new Pair<Related, String>(Related.OUTGOING, "<RecordCreator><ContactData><Email>"));
        mappedFields.put(DocumentCommonModel.Props.OWNER_PHONE, new Pair<Related, String>(Related.OUTGOING, "<RecordCreator><ContactData><Phone>"));
        mappedFields.put(DocumentCommonModel.Props.SIGNER_NAME, new Pair<Related, String>(Related.OUTGOING, "<RecordSenderToDec><Person><Name>"));
        mappedFields.put(DocumentDynamicModel.Props.SIGNER_EMAIL, new Pair<Related, String>(Related.OUTGOING, "<RecordSenderToDec><ContactData><Email>"));
        mappedFields.put(DocumentDynamicModel.Props.SIGNER_PHONE, new Pair<Related, String>(Related.OUTGOING, "<RecordSenderToDec><ContactData><Phone>"));
        mappedFields.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><Organisation><Name>"));
        mappedFields.put(DocumentDynamicModel.Props.SENDER_PERSON_NAME, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><Person><Name>"));
        mappedFields.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><ContactData><Email>"));
        mappedFields
                .put(DocumentDynamicModel.Props.SENDER_STREET_HOUSE,
                        new Pair<Related, String>(
                                Related.INCOMING,
                                "<RecordSenderToDec><ContactData><PostalAddress><Street>, <RecordSenderToDec><ContactData><PostalAddress><HouseNumber>, <RecordSenderToDec><ContactData><PostalAddress><BuildingPartNumber>"));
        mappedFields
                .put(DocumentDynamicModel.Props.SENDER_POSTAL_CITY,
                        new Pair<Related, String>(
                                Related.INCOMING,
                                "<RecordSenderToDec><ContactData><PostalAddress><PostalCode>, <RecordSenderToDec><ContactData><PostalAddress><SmallPlace>, <RecordSenderToDec><ContactData><PostalAddress><AdministrativeUnit>"));
        mappedFields.put(DocumentSpecificModel.Props.SENDER_REG_NUMBER, new Pair<Related, String>(Related.INCOMING, "<RecordMetaData><RecordOriginalIdentifier>"));
        mappedFields.put(DocumentSpecificModel.Props.SENDER_REG_DATE, new Pair<Related, String>(Related.INCOMING, "<RecordMetaData><RecordDateRegistered>"));
        mappedFields.put(DocumentSpecificModel.Props.PARTY_NAME, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><Organisation><Name>"));
        mappedFields.put(DocumentSpecificModel.Props.PARTY_SIGNER, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><Person><Name>"));
        mappedFields.put(DocumentSpecificModel.Props.PARTY_EMAIL, new Pair<Related, String>(Related.INCOMING, "<RecordSenderToDec><ContactData><Email>"));
        mappedFields.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON, new Pair<Related, String>(Related.INCOMING, "<RecordCreator><Person><Name>"));

        for (QName qname : mappedFields.keySet()) {
            localNameAndQName.put(qname.getLocalName(), qname);
        }
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                joinQueryPartsOr(generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION), generateTypeQuery(DocumentAdminModel.Types.FIELD)),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.SYSTEMATIC, true),
                generatePropertyExactQuery(DocumentAdminModel.Props.FIELD_ID, localNameAndQName.keySet())
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String fieldNameStr = (String) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.FIELD_ID);
        QName fieldQName = localNameAndQName.get(fieldNameStr);
        Pair<Related, String> fieldAndValue = mappedFields.get(fieldQName);

        String newValueStr = fieldAndValue.getSecond();
        validate(newValueStr); // avoid typos etc.
        Serializable newval = (Serializable) Arrays.asList(newValueStr);

        switch (fieldAndValue.getFirst()) {
        case OUTGOING:
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT, newval);
            break;
        case INCOMING:
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_INCOMING_DEC_ELEMENT, newval);
            break;
        case BOTH:
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_INCOMING_DEC_ELEMENT, newval);
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT, newval);
            break;
        }
        return new String[] { "RelatedDecElements: " + fieldAndValue.getFirst().name(), newValueStr };
    }

    private void validate(String newValue) {
        StringTokenizer tokenizer = new StringTokenizer(newValue, ",");
        while (tokenizer.hasMoreTokens()) {
            String field = StringUtils.trim(tokenizer.nextToken());
            if (StringUtils.isNotBlank(field) && !DecContainerHandler.hasUserKey(field)) {
                throw new RuntimeException("Unable to find field for key " + field);
            }
        }
    }

}
