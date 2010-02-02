package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.utils.SearchUtil.formatLuceneDate;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNotEmptyQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringWordsWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.isBlank;
import static ee.webmedia.alfresco.utils.SearchUtil.isDateProperty;
import static ee.webmedia.alfresco.utils.SearchUtil.isStringProperty;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;
import static ee.webmedia.alfresco.utils.SearchUtil.parseQuickSearchWords;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus;

/**
 * @author Alar Kvell
 * @author Erko Hansar
 */
public class DocumentSearchServiceImpl implements DocumentSearchService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchServiceImpl.class);

    private DocumentService documentService;
    private GeneralService generalService;
    private NodeService nodeService;
    private SearchService searchService;
    private DictionaryService dictionaryService;
    private SeriesService seriesService;

    @Override
    public List<Document> searchTodayRegisteredDocuments() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(1);
        queryParts.add(generatePropertyDateQuery(DocumentCommonModel.Props.REG_DATE_TIME, new Date()));
        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false);
        if (log.isDebugEnabled()) {
            log.debug("Today registered documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchUserWorkingDocuments() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateStringExactQuery(AuthenticationUtil.getRunAsUser(), DocumentCommonModel.Props.OWNER_ID));
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and WORKING documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchRecipientFinishedDocuments() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME));
        queryParts.add(generateStringExactQuery(DocumentStatus.FINISHED.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false);
        // Make sure the document does not have sendInfo children

        // XXX TODO FIXME is additional optimization needed? postprocessing could be eliminated and this condition added directly to lucene query
        for (Iterator<Document> it = results.iterator(); it.hasNext(); ) {
            Document doc = it.next();
            List<ChildAssociationRef> sendInfo = nodeService.getChildAssocs(doc.getNode().getNodeRef(), RegexQNamePattern.MATCH_ALL,
                    DocumentCommonModel.Assocs.SEND_INFO);
            if (sendInfo.size() > 0) {
                it.remove();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Series> searchSeriesUnit(String unit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(1);
        queryParts.add(generateStringExactQuery(unit, SeriesModel.Props.STRUCT_UNIT));
        String query = generateSeriesSearchQuery(queryParts);
        List<Series> results = searchSeriesImpl(query, false);

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchDocuments(Node filter) {
        long startTime = System.currentTimeMillis();
        String query = generateDocumentSearchQuery(filter);
        List<Document> results = searchDocumentsImpl(query, true);
        if (log.isDebugEnabled()) {
            log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    @Override
    public List<Document> searchDocumentsInOutbox() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, false);
    }

    @Override
    public Map<NodeRef /*sendInfo*/, String /*dvkId*/> searchOutboxDvkIds() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, false);
    }

    @Override
    public List<Document> searchDocumentsQuick(String searchString) {
        long startTime = System.currentTimeMillis();
        String query = generateQuickSearchQuery(searchString);
        List<Document> results = searchDocumentsImpl(query, true);
        if (log.isDebugEnabled()) {
            log.debug("Quick search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    private String getDvkOutboxQuery() {
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(generateStringExactQuery(SendOutService.SEND_MODE_DVK, DocumentCommonModel.Props.SEND_INFO_SEND_MODE));
        queryParts.add(generateStringExactQuery(SendStatus.SENT.toString(), DocumentCommonModel.Props.SEND_INFO_SEND_STATUS));
        String query = joinQueryPartsAnd(queryParts, false);
        return query;
    }

    private Map<NodeRef /* sendInfo */, String /* dvkId */> searchDhlIdsBySendInfoImpl(String query, boolean limited) {
        final HashMap<NodeRef, String> refsAndDvkIds = new HashMap<NodeRef, String>();
        searchGeneralImpl(query, limited, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                final NodeRef sendInfoRef = row.getNodeRef();
                refsAndDvkIds.put(sendInfoRef, (String) nodeService.getProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_DVK_ID));
                return null;
            }
        });
        return refsAndDvkIds;
    }

    private List<Document> searchDocumentsBySendInfoImpl(String query, boolean limited) {
        return searchGeneralImpl(query, limited, new SearchCallback<Document>() {
            private Set<String> nodeIds = new HashSet<String>();

            @Override
            public Document addResult(ResultSetRow row) {
                NodeRef docRef = row.getChildAssocRef().getParentRef();
                if (!nodeIds.contains(docRef.getId())) { // duplicate documents should be ignored
                    nodeIds.add(docRef.getId());
                    if (nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                        return documentService.getDocumentByNodeRef(docRef);
                    }
                }
                return null;
            }
        });
    }

    private String generateDocumentSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        Map<String, Object> props = filter.getProperties();

        @SuppressWarnings("unchecked")
        List<QName> documentTypes = (List<QName>) props.get(DocumentSearchModel.Props.DOCUMENT_TYPE);
        queryParts.add(generateTypeQuery(documentTypes));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(DocumentSearchModel.Props.FUNCTION), DocumentCommonModel.Props.FUNCTION));
        @SuppressWarnings("unchecked")
        List<NodeRef> series = (List<NodeRef>) props.get(DocumentSearchModel.Props.SERIES);
        queryParts.add(generateMultiNodeRefQuery(series, DocumentCommonModel.Props.SERIES));

        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.REG_DATE_TIME_BEGIN), (Date) props.get(DocumentSearchModel.Props.REG_DATE_TIME_END), DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.REG_NUMBER), DocumentCommonModel.Props.REG_NUMBER));
        @SuppressWarnings("unchecked")
        List<String> status = (List<String>) props.get(DocumentSearchModel.Props.DOC_STATUS);
        queryParts.add(generateMultiStringExactQuery(status, DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.RECIPIENT_NAME), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.DOC_NAME), DocumentCommonModel.Props.DOC_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SENDER_REG_NUMBER), DocumentSpecificModel.Props.SENDER_REG_NUMBER));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.SENDER_REG_DATE_BEGIN), (Date) props.get(DocumentSearchModel.Props.SENDER_REG_DATE_END), DocumentSpecificModel.Props.SENDER_REG_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.DUE_DATE_BEGIN), (Date) props.get(DocumentSearchModel.Props.DUE_DATE_END), DocumentSpecificModel.Props.DUE_DATE, DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE, DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.COMPLIENCE_DATE_BEGIN), (Date) props.get(DocumentSearchModel.Props.COMPLIENCE_DATE_END), DocumentSpecificModel.Props.COMPLIENCE_DATE));
        @SuppressWarnings("unchecked")
        List<String> accessRestriction = (List<String>) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION);
        queryParts.add(generateMultiStringExactQuery(accessRestriction, DocumentCommonModel.Props.ACCESS_RESTRICTION));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_REASON), DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_BEGIN_DATE_BEGIN), (Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_BEGIN_DATE_END), DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE));
        System.out.println("accessRestrictionEndDateBegin" + props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_BEGIN));
        System.out.println("accessRestrictionEndDateEnd" + props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_END));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_BEGIN), (Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_END), DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DESC), DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_NAME), DocumentCommonModel.Props.OWNER_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_ORG_STRUCT_UNIT), DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_JOB_TITLE), DocumentCommonModel.Props.OWNER_JOB_TITLE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SIGNER_NAME), DocumentCommonModel.Props.SIGNER_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SIGNER_JOB_TITLE), DocumentCommonModel.Props.SIGNER_JOB_TITLE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.KEYWORDS), DocumentCommonModel.Props.KEYWORDS));
        @SuppressWarnings("unchecked")
        List<String> storageType = (List<String>) props.get(DocumentSearchModel.Props.STORAGE_TYPE);
        queryParts.add(generateMultiStringExactQuery(storageType, DocumentCommonModel.Props.STORAGE_TYPE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.COST_MANAGER), DocumentSpecificModel.Props.COST_MANAGER));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.RESPONSIBLE_NAME), DocumentSpecificModel.Props.RESPONSIBLE_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.CO_RESPONSIBLES), DocumentSpecificModel.Props.CO_RESPONSIBLES));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.CONTACT_PERSON), DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON, DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON, DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));

        // Quick search
        List<String> searchWords = parseQuickSearchWords((String) props.get(DocumentSearchModel.Props.INPUT));
        if (!searchWords.isEmpty()) {
            queryParts.addAll(generateQuickSearchDocumentQuery(searchWords));
        }
        String query = generateDocumentSearchQuery(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Documents search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private String generateQuickSearchQuery(String searchString) {
        long startTime = System.currentTimeMillis();
        List<String> searchWords = parseQuickSearchWords(searchString);
        log.info("Quick search - words: " + searchWords.toString() + ", from string '" + searchString + "'");
        if (searchWords.isEmpty()) {
            return null;
        }
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(searchWords));
        if (log.isDebugEnabled()) {
            log.debug("Quick search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private List<String> generateQuickSearchDocumentQuery(List<String> searchWords) {
        // Fetch a list of all the properties from document type and it's subtypes.
        List<QName> searchProperties = new ArrayList<QName>();
        List<QName> searchPropertiesDate = new ArrayList<QName>();
        Collection<QName> docProperties = dictionaryService.getProperties(DocumentCommonModel.MODEL);
        docProperties.addAll(dictionaryService.getProperties(DocumentSpecificModel.MODEL));
        for (QName property : docProperties) {
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            QName type = propDef.getDataType().getName();
            if (isStringProperty(type)) {
                searchProperties.add(property);
            } else if (isDateProperty(type)) {
                searchPropertiesDate.add(property);
            }
        }

        /*
         * Construct a query with following structure:
         * ASPECT:searchable AND (
         * (TYPE:document AND (@prop1:"*word1*" OR @prop2:"*word1*") AND (@prop1:"*word2*" OR @prop2:"*word2*")) OR
         * (ASPECT:file AND (@name:"*word1*" OR @content:"*word1*") AND (@name:"*word2*" OR @content:"*word2*"))
         * )
         * Note: Property values must be wrapped with " symbols. Alfresco LuceneQueryParser somehow produces different
         * results (something to do with multi-language fields, et locale and ALL locales). It doesn't replace non latin-1
         * characters with latin-1 characters if the values are without wrapping " symbols and this breaks searches with estonian
         * special characters because Alfresco indexed them with ISOLatin1AccentFilter.
         */

        DateFormat userDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        userDateFormat.setLenient(false);

        List<String> wordQueries = new ArrayList<String>();
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                List<String> propQueries = new ArrayList<String>(searchProperties.size() + searchPropertiesDate.size());

                for (QName property : searchProperties) {
                    propQueries.add(generatePropertyWildcardQuery(property, searchWord, false));
                }
                Date date = null;
                try {
                    date = userDateFormat.parse(searchWord);
                } catch (ParseException e) {
                    // do nothing
                }
                // if it's a date match, then also add date properties
                if (date != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("getDocumentsQuickSearch - found date match: " + searchWord + " -> " + formatLuceneDate(date));
                    }
                    for (QName property : searchPropertiesDate) {
                        propQueries.add(generatePropertyDateQuery(property, date));
                    }
                }
                wordQueries.add(joinQueryPartsOr(propQueries, false));
            }
        }
        return wordQueries;
    }

    private static String generateSeriesSearchQuery(List<String> queryParts) {
        queryParts.add(0, generateTypeQuery(SeriesModel.Types.SERIES));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateDocumentSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        queryParts.add(1, generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        return joinQueryPartsAnd(queryParts);
    }

    private interface SearchCallback<E> {
        E addResult(ResultSetRow row);
    }

    private List<Document> searchDocumentsImpl(String query, boolean limited) {
        return searchGeneralImpl(query, limited, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                return documentService.getDocumentByNodeRef(row.getNodeRef());
            }
        });
    }

    private List<Series> searchSeriesImpl(String query, boolean limited) {
        return searchGeneralImpl(query, limited, new SearchCallback<Series>() {
            @Override
            public Series addResult(ResultSetRow row) {
                return seriesService.getSeriesByNodeRef(row.getNodeRef());
            }
        });
    }

    private <E extends Comparable<? super E>> List<E> searchGeneralImpl(String query, boolean limited, SearchCallback<E> callback) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        // build up the search parameters
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        sp.addStore(generalService.getStore());
        if (limited) {
            sp.setLimit(100);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
        } else {
            sp.setLimitBy(LimitBy.UNLIMITED);
        }

        List<E> result = new ArrayList<E>();
        long startTime = System.currentTimeMillis();
        ResultSet resultSet = searchService.query(sp);
        try {
            if (log.isDebugEnabled()) {
                log.debug("Lucene search time " + (System.currentTimeMillis() - startTime) + " ms, results: " + resultSet.length());
                startTime = System.currentTimeMillis();
            }

            for (ResultSetRow row : resultSet) {
                E item = callback.addResult(row);
                if (item != null) {
                    result.add(item);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Results construction time " + (System.currentTimeMillis() - startTime) + " ms, final results: " + result.size());
            }
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }
        Collections.sort(result);
        return result;
    }

    // START: getters / setters

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    // END: getters / setters

}
