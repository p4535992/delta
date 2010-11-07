package ee.webmedia.alfresco.search.service;

/*
import static ee.webmedia.alfresco.utils.SearchUtil.formatLuceneDate;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.isBlank;
import static ee.webmedia.alfresco.utils.SearchUtil.isDateProperty;
import static ee.webmedia.alfresco.utils.SearchUtil.isStringProperty;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.impl.SearchStatistics;
import org.alfresco.repo.search.impl.SearchStatistics.Data;
import org.alfresco.repo.search.impl.lucene.AnalysisMode;
import org.alfresco.repo.search.impl.lucene.LuceneAnalyser;
import org.alfresco.repo.search.impl.lucene.LuceneConfig;
import org.alfresco.repo.search.impl.lucene.analysis.MLTokenDuplicator;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.queryParser.QueryParser;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.SearchUtil;
*/

public abstract class AbstractSearchServiceImpl {
/*
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AbstractSearchServiceImpl.class);

    protected SearchService searchService;
    protected NodeService nodeService;
    protected DictionaryService dictionaryService;
    protected GeneralService generalService;
    protected ParametersService parametersService;
    protected LuceneConfig config;
    
    protected LuceneAnalyser luceneAnalyser;

    protected Pair<String, String> generateQuickSearchQuery(String searchString, List<String> typeParts, Set<QName> documentProps) {
        Pair<List<String>, String> pair = parseQuickSearchWords(searchString);
        List<String> searchWords = pair.getFirst();
        log.info("Quick search - words: " + searchWords.toString() + ", from string '" + searchString + "'");
        if (searchWords.isEmpty()) {
            return null;
        }
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(searchWords, documentProps), typeParts);
        return new Pair<String, String>(pair.getSecond(), query);
    }

    private Pair<List<String>, String> parseQuickSearchWords(String searchString) {
        List<String> searchWords = new ArrayList<String>();
        String searchWordsHumanReadable = "";
        if (StringUtils.isBlank(searchString)) {
            return new Pair<List<String>, String>(searchWords, searchWordsHumanReadable);
        }
        for (String searchWord : searchString.split("\\s")) {
            String searchWordStripped = SearchUtil.stripCustom(SearchUtil.replaceCustom(searchWord, ""));
            for (Token token : getTokens(searchWordStripped)) {
                String termText = token.term();
                if (termText.length() >= 3 && searchWords.size() < 3) {
                    String termTextHumanReadable = termText;
                    termText = QueryParser.escape(termText);
                    if (searchWord.startsWith("*")) {
                        termText = "*" + termText;
                        termTextHumanReadable = "*" + termTextHumanReadable;
                    }
                    if (searchWord.endsWith("*")) {
                        termText = termText + "*";
                        termTextHumanReadable = termTextHumanReadable + "*";
                    }

                    boolean exists = false;
                    for (String tmpWord : searchWords) {
                        exists |= tmpWord.equalsIgnoreCase(termText);
                    }
                    if (!exists) {
                        searchWords.add(termText);
                        if (searchWordsHumanReadable.length() == 0) {
                            searchWordsHumanReadable = termTextHumanReadable;
                        } else {
                            searchWordsHumanReadable += " " + termTextHumanReadable;
                        }
                    }
                }
            }
        }
        return new Pair<List<String>, String>(searchWords, searchWordsHumanReadable);
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
*/
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
/*
        // TODO: format should probably be read from bundle
        DateFormat userDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        userDateFormat.setLenient(false);

        List<String> wordQueries = new ArrayList<String>(10);
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                List<String> propQueries = new ArrayList<String>(searchProperties.size() + searchPropertiesDate.size());

                for (QName property : searchProperties) {
                    propQueries.add(generatePropertyExactQuery(property, searchWord, false));
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

    protected List<NodeRef> searchNodeRefs(String query, boolean limited) {
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

    protected Pair<List<WmNode>, Boolean> searchNodes(String query, boolean limited) {
        if (StringUtils.isEmpty(query)) {
            return new Pair<List<WmNode>,Boolean>(new ArrayList<WmNode>(),Boolean.FALSE);
        }
        boolean statisticsEnabled = SearchStatistics.isEnabled();
        long time0 = statisticsEnabled ? System.currentTimeMillis() : 0;
        long time1 = 0, time2 = 0, time3 = 0;
        List<WmNode> results = null;
        ResultSet resultSet = doSearch(query, limited);

        try {
            time1 = statisticsEnabled ? System.currentTimeMillis() : 0;
            Map<NodeRef, QName> types = new HashMap<NodeRef, QName>(resultSet.length());
            for (ResultSetRow row : resultSet) {
                NodeRef nodeRef = row.getNodeRef();
                try {
                    types.put(nodeRef, nodeService.getType(nodeRef));
                } catch (InvalidNodeRefException e) {
                    continue;
                }
            }
        
            time2 = statisticsEnabled ? System.currentTimeMillis() : 0;
            results = new ArrayList<WmNode>(resultSet.length());
            for (ResultSetRow row : resultSet) {
                NodeRef nodeRef = row.getNodeRef();
                QName type = types.get(nodeRef);
                if (type == null) {
                    continue;
                }

                Map<String, Serializable> props = row.getValues();
                // Getting node properties from lucene resultset is faster than from DB
                // Erko said; Aleksei also tested on 26.10.2010 6:28 - DB is 1.2-1.6 times slower

                WmNode node = new WmNode(nodeRef, type, props, null);
                node.setAspectsLazy();
                results.add(node);
            }
            time3 = statisticsEnabled ? System.currentTimeMillis() : 0;
            return new Pair<List<WmNode>, Boolean>(results,resultSet.hasMore());
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Do nothing
            }
            if (statisticsEnabled) {
                long time4 = System.currentTimeMillis();
                Data data = SearchStatistics.getData();
                data.resultsAfterAcl = results != null ? results.size() : -1;
                data.alfrescoSearchLayerOtherTime = time1 - time0 - data.luceneHitsTime - data.aclTime;
                data.nodeTypesTime = time2 - time1;
                data.nodePropsTime = time3 - time2;
                data.closeResultSetTime = time4 - time3;
                log.debug("Search results " + data.resultsBeforeAcl + " -> " + data.resultsAfterAcl + ", total time " + (time4 - time0)
                        + " ms\n  lucene query " + data.luceneHitsTime
                        + " ms\n  alfresco search layer other " + data.alfrescoSearchLayerOtherTime
                        + " ms\n  permissions filter " + data.aclTime
                        + " ms\n  get node types " + data.nodeTypesTime
                        + " ms\n  get node props " + data.nodePropsTime
                        + " ms\n  close resultset " + data.closeResultSetTime + " ms");
            }
        }
    }

*/
    /**
     * Sets up search parameters and queries
     * 
     * @param query
     * @param limited if true, only 100 first results are returned
     * @return query resultset
     */
/*
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

        ResultSet result = searchService.query(sp);
        return result;
    }

    public int getSearchLimit() {
        return parametersService.getParameter(Parameters.SEARCH_RESULTS_LIMIT, Integer.class);
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
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

    public void setLuceneConfig(LuceneConfig config) {
        this.config = config;
        luceneAnalyser = new LuceneAnalyser(dictionaryService, config.getDefaultMLSearchAnalysisMode());
    }

    public LuceneAnalyser getAnalyzer() {
        return luceneAnalyser;
    }

    protected List<Token> getTokens(String queryText) {
        String testText = queryText;
        String localeString = "";
        // String localeString = "et"; // Alfresco code calls getTokens twice, once with "", once with "et"
        AnalysisMode analysisMode = AnalysisMode.DEFAULT;
        boolean requiresMLTokenDuplication = true;
        String field = "@" + ContentModel.PROP_TITLE.toString();
        // String field = "@{http://www.alfresco.org/model/content/1.0}title";

        // Following copied from LuceneQueryParser#getFieldQueryImpl
        // ---------------------------------------------------------
        
        TokenStream source = getAnalyzer().tokenStream(field, new StringReader(queryText), analysisMode);

        ArrayList<org.apache.lucene.analysis.Token> list = new ArrayList<org.apache.lucene.analysis.Token>();
        org.apache.lucene.analysis.Token reusableToken = new org.apache.lucene.analysis.Token();
        org.apache.lucene.analysis.Token nextToken;
        int positionCount = 0;
        boolean severalTokensAtSamePosition = false;

        while (true)
        {
            try
            {
                nextToken = source.next(reusableToken);
            }
            catch (IOException e)
            {
                nextToken = null;
            }
            if (nextToken == null)
                break;
            list.add((org.apache.lucene.analysis.Token) nextToken.clone());
            if (nextToken.getPositionIncrement() != 0)
                positionCount += nextToken.getPositionIncrement();
            else
                severalTokensAtSamePosition = true;
        }
        try
        {
            source.close();
        }
        catch (IOException e)
        {
            // ignore
        }

        // add any alpha numeric wildcards that have been missed
        // Fixes most stop word and wild card issues

        for (int index = 0; index < testText.length(); index++)
        {
            char current = testText.charAt(index);
            if ((current == '*') || (current == '?'))
            {
                StringBuilder pre = new StringBuilder(10);
                if (index > 0)
                {
                    for (int i = index - 1; i >= 0; i--)
                    {
                        char c = testText.charAt(i);
                        if (Character.isLetterOrDigit(c))
                        {
                            boolean found = false;
                            for (int j = 0; j < list.size(); j++)
                            {
                                org.apache.lucene.analysis.Token test = list.get(j);
                                if ((test.startOffset() <= i) && (i <= test.endOffset()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            if (found)
                            {
                                break;
                            }
                            else
                            {
                                pre.insert(0, c);
                            }
                        }
                    }
                    if (pre.length() > 0)
                    {
                        // Add new token followed by * not given by the tokeniser
                        org.apache.lucene.analysis.Token newToken = new org.apache.lucene.analysis.Token(index - pre.length(), index);
                        newToken.setTermBuffer(pre.toString());
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication)
                        {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            MLAnalysisMode mlAnalysisMode = config.getDefaultMLSearchAnalysisMode();
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, mlAnalysisMode);
                            Iterator<org.apache.lucene.analysis.Token> it = duplicator.buildIterator(newToken);
                            if (it != null)
                            {
                                int count = 0;
                                while (it.hasNext())
                                {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1)
                                    {
                                        severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else
                        {
                            list.add(newToken);
                        }
                    }
                }

                StringBuilder post = new StringBuilder(10);
                if (index > 0)
                {
                    for (int i = index + 1; i < testText.length(); i++)
                    {
                        char c = testText.charAt(i);
                        if (Character.isLetterOrDigit(c))
                        {
                            boolean found = false;
                            for (int j = 0; j < list.size(); j++)
                            {
                                org.apache.lucene.analysis.Token test = list.get(j);
                                if ((test.startOffset() <= i) && (i <= test.endOffset()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            if (found)
                            {
                                break;
                            }
                            else
                            {
                                post.append(c);
                            }
                        }
                    }
                    if (post.length() > 0)
                    {
                        // Add new token followed by * not given by the tokeniser
                        org.apache.lucene.analysis.Token newToken = new org.apache.lucene.analysis.Token(index + 1, index + 1 + post.length());
                        newToken.setTermBuffer(post.toString());
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication)
                        {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            MLAnalysisMode mlAnalysisMode = config.getDefaultMLSearchAnalysisMode();
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, mlAnalysisMode);
                            Iterator<org.apache.lucene.analysis.Token> it = duplicator.buildIterator(newToken);
                            if (it != null)
                            {
                                int count = 0;
                                while (it.hasNext())
                                {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1)
                                    {
                                        severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else
                        {
                            list.add(newToken);
                        }
                    }
                }

            }
        }

        Collections.sort(list, new Comparator<org.apache.lucene.analysis.Token>()
        {

            public int compare(Token o1, Token o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                if (dif != 0)
                {
                    return dif;
                }
                else
                {
                    return o2.getPositionIncrement() - o1.getPositionIncrement();
                }
            }
        });

        // Combined * and ? based strings - should redo the tokeniser

        // Assume we only string together tokens for the same position

        int max = 0;
        int current = 0;
        for (org.apache.lucene.analysis.Token c : list)
        {
            if (c.getPositionIncrement() == 0)
            {
                current++;
            }
            else
            {
                if (current > max)
                {
                    max = current;
                }
                current = 0;
            }
        }
        if (current > max)
        {
            max = current;
        }

        ArrayList<org.apache.lucene.analysis.Token> fixed = new ArrayList<org.apache.lucene.analysis.Token>();
        for (int repeat = 0; repeat <= max; repeat++)
        {
            org.apache.lucene.analysis.Token replace = null;
            current = 0;
            for (org.apache.lucene.analysis.Token c : list)
            {
                if (c.getPositionIncrement() == 0)
                {
                    current++;
                }
                else
                {
                    current = 0;
                }

                if (current == repeat)
                {

                    if (replace == null)
                    {
                        StringBuilder prefix = new StringBuilder();
                        for (int i = c.startOffset() - 1; i >= 0; i--)
                        {
                            char test = testText.charAt(i);
                            if ((test == '*') || (test == '?'))
                            {
                                prefix.insert(0, test);
                            }
                            else
                            {
                                break;
                            }
                        }
                        String pre = prefix.toString();
                        if (requiresMLTokenDuplication)
                        {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            int position = termText.indexOf("}");
                            String language = termText.substring(0, position + 1);
                            String token = termText.substring(position + 1);
                            replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                            replace.setTermBuffer(language + pre + token);
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        }
                        else
                        {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                            replace.setTermBuffer(pre + termText);
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        }
                    }
                    else
                    {
                        StringBuilder prefix = new StringBuilder();
                        StringBuilder postfix = new StringBuilder();
                        StringBuilder builder = prefix;
                        for (int i = c.startOffset() - 1; i >= replace.endOffset(); i--)
                        {
                            char test = testText.charAt(i);
                            if ((test == '*') || (test == '?'))
                            {
                                builder.insert(0, test);
                            }
                            else
                            {
                                builder = postfix;
                                postfix.setLength(0);
                            }
                        }
                        String pre = prefix.toString();
                        String post = postfix.toString();

                        // Does it bridge?
                        if ((pre.length() > 0) && (replace.endOffset() + pre.length()) == c.startOffset())
                        {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            if (requiresMLTokenDuplication)
                            {
                                int position = termText.indexOf("}");
                                @SuppressWarnings("unused")
                                String language = termText.substring(0, position + 1);
                                String token = termText.substring(position + 1);
                                int oldPositionIncrement = replace.getPositionIncrement();
                                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                                replace = new org.apache.lucene.analysis.Token(replace.startOffset(), c.endOffset());
                                replace.setTermBuffer(replaceTermText + pre + token);
                                replace.setType(replace.type());
                                replace.setPositionIncrement(oldPositionIncrement);
                            }
                            else
                            {
                                int oldPositionIncrement = replace.getPositionIncrement();
                                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                                replace = new org.apache.lucene.analysis.Token(replace.startOffset(), c.endOffset());
                                replace.setTermBuffer(replaceTermText + pre + termText);
                                replace.setType(replace.type());
                                replace.setPositionIncrement(oldPositionIncrement);
                            }
                        }
                        else
                        {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            if (requiresMLTokenDuplication)
                            {
                                int position = termText.indexOf("}");
                                String language = termText.substring(0, position + 1);
                                String token = termText.substring(position + 1);
                                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                                org.apache.lucene.analysis.Token last = new org.apache.lucene.analysis.Token(replace.startOffset(), replace.endOffset() + post.length());
                                last.setTermBuffer(replaceTermText + post);
                                last.setType(replace.type());
                                last.setPositionIncrement(replace.getPositionIncrement());
                                fixed.add(last);
                                replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                                replace.setTermBuffer(language + pre + token);
                                replace.setType(c.type());
                                replace.setPositionIncrement(c.getPositionIncrement());
                            }
                            else
                            {
                                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                                org.apache.lucene.analysis.Token last = new org.apache.lucene.analysis.Token(replace.startOffset(), replace.endOffset() + post.length());
                                last.setTermBuffer(replaceTermText + post);
                                last.setType(replace.type());
                                last.setPositionIncrement(replace.getPositionIncrement());
                                fixed.add(last);
                                replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                                replace.setTermBuffer(pre + termText);
                                replace.setType(c.type());
                                replace.setPositionIncrement(c.getPositionIncrement());
                            }
                        }
                    }
                }
            }
            // finish last
            if (replace != null)
            {
                StringBuilder postfix = new StringBuilder();
                for (int i = replace.endOffset(); i < testText.length(); i++)
                {
                    char test = testText.charAt(i);
                    if ((test == '*') || (test == '?'))
                    {
                        postfix.append(test);
                    }
                    else
                    {
                        break;
                    }
                }
                String post = postfix.toString();
                int oldPositionIncrement = replace.getPositionIncrement();
                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                replace = new org.apache.lucene.analysis.Token(replace.startOffset(), replace.endOffset() + post.length());
                replace.setTermBuffer(replaceTermText + post);
                replace.setType(replace.type());
                replace.setPositionIncrement(oldPositionIncrement);
                fixed.add(replace);

            }
        }

        // Add in any missing words containsing * and ?

        // reorder by start position and increment

        Collections.sort(fixed, new Comparator<org.apache.lucene.analysis.Token>()
        {

            public int compare(Token o1, Token o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                if (dif != 0)
                {
                    return dif;
                }
                else
                {
                    return o2.getPositionIncrement() - o1.getPositionIncrement();
                }
            }
        });
        return fixed;
    }

*/
}
