package ee.webmedia.alfresco.search.service;

import static ee.webmedia.alfresco.utils.SearchUtil.formatLuceneDate;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
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
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public abstract class AbstractSearchServiceImpl {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AbstractSearchServiceImpl.class);

    protected SearchService searchService;
    protected DictionaryService dictionaryService;
    protected GeneralService generalService;
    protected ParametersService parametersService;

    protected String generateQuickSearchQuery(String searchString, List<String> typeParts, Set<QName> documentProps) {
        long startTime = System.currentTimeMillis();
        List<String> searchWords = parseQuickSearchWords(searchString);
        log.info("Quick search - words: " + searchWords.toString() + ", from string '" + searchString + "'");
        if (searchWords.isEmpty()) {
            return null;
        }
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(searchWords, documentProps), typeParts);
        if (log.isDebugEnabled()) {
            log.debug("Quick search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    protected String generateDocumentSearchQuery(List<String> queryParts, List<String> typeParts) {
        if (isBlank(queryParts)) {
            return null;
        }

        typeParts.addAll(queryParts);
        return joinQueryPartsAnd(typeParts);
    }

    protected List<String> generateQuickSearchDocumentQuery(List<String> searchWords, Set<QName> documentProps) {
        // Fetch a list of all the properties from document type and it's subtypes.
        List<QName> searchProperties = new ArrayList<QName>(50);
        List<QName> searchPropertiesDate = new ArrayList<QName>();
        addDocumentProperties(documentProps, searchProperties, searchPropertiesDate);
        return generateQuery(searchWords, searchProperties, searchPropertiesDate);
    }

    protected List<String> generateQuery(List<String> searchWords, List<QName> searchProperties, List<QName> searchPropertiesDate) {
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
        // TODO: format should probably be read from bundle
        DateFormat userDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        userDateFormat.setLenient(false);

        List<String> wordQueries = new ArrayList<String>(10);
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

    protected void addDocumentProperties(Set<QName> props, List<QName> searchProperties, List<QName> searchPropertiesDate) {
        for (QName property : props) {
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            QName type = propDef.getDataType().getName();
            if (isStringProperty(type)) {
                searchProperties.add(property);
            } else if (isDateProperty(type)) {
                searchPropertiesDate.add(property);
            }
        }
    }

    protected abstract Set<QName> getDocumentPropertiesAll();

    protected List<NodeRef> searchNodes(String query, boolean limited) {
        ResultSet resultSet = doSearch(query, limited);
        try {
            return resultSet.getNodeRefs();
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    /**
     * Sets up search parameters and queries
     * 
     * @param query
     * @param limited if true, only 100 first results are returned
     * @return query resultset
     */
    protected ResultSet doSearch(String query, boolean limited) {
        return doSearch(query, limited, null);
    }

    protected ResultSet doSearch(String query, boolean limited, StoreRef storeRef) {
        // build up the search parameters
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        sp.addStore(storeRef == null ? generalService.getStore() : storeRef);
        if (limited) {
            sp.setLimit(getSearchLimit());
            sp.setLimitBy(LimitBy.FINAL_SIZE);
        } else {
            sp.setLimitBy(LimitBy.UNLIMITED);
        }

        return searchService.query(sp);
    }

    protected int getSearchLimit() {
        return parametersService.getParameter(Parameters.SEARCH_RESULTS_LIMIT, Integer.class);
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
