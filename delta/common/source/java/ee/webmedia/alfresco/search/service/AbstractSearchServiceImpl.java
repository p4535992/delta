package ee.webmedia.alfresco.search.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateLuceneSearchParams;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.EmptyResultSet;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.impl.SearchStatistics;
import org.alfresco.repo.search.impl.lucene.AnalysisMode;
import org.alfresco.repo.search.impl.lucene.LuceneAnalyser;
import org.alfresco.repo.search.impl.lucene.LuceneConfig;
import org.alfresco.repo.search.impl.lucene.analysis.MLTokenDuplicator;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
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
import org.apache.lucene.search.BooleanQuery;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

public abstract class AbstractSearchServiceImpl {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AbstractSearchServiceImpl.class);

    protected DictionaryService dictionaryService;
    protected NodeService nodeService;
    protected GeneralService generalService;
    protected SearchService searchService;
    protected ParametersService parametersService;
    protected LuceneConfig config;

    protected LuceneAnalyser luceneAnalyser;

    /**
     * Default is without left wildcard and with right wildcard.
     */
    public String generateStringWordsWildcardQuery(String value, QName... documentPropNames) {
        return SearchUtil.generateStringWordsWildcardQuery(parseQuickSearchWords(value), false, true, documentPropNames);
    }

    public String generateStringWordsWildcardQuery(String value, boolean leftWildcard, boolean rightWildcard, QName... documentPropNames) {
        return SearchUtil.generateStringWordsWildcardQuery(parseQuickSearchWords(value), leftWildcard, rightWildcard, documentPropNames);
    }

    /**
     * Default is without left wildcard and with right wildcard.
     */
    public String generateMultiStringWordsWildcardQuery(List<String> values, QName... documentPropNames) {
        return generateMultiStringWordsWildcardQuery(values, false, true, documentPropNames);
    }

    public String generateMultiStringWordsWildcardQuery(List<String> values, boolean leftWildcard, boolean rightWildcard, QName... documentPropNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<String> queryParts = new ArrayList<String>(values.size());
        for (String value : values) {
            queryParts.add(generateStringWordsWildcardQuery(value, leftWildcard, rightWildcard, documentPropNames));
        }
        return SearchUtil.joinQueryPartsOr(queryParts);
    }

    /**
     * Escape symbols and use only 10 first unique words which contain at least 3 characters
     */
    public List<String> parseQuickSearchWords(String searchString) {
        return parseQuickSearchWords(searchString, 3);
    }

    protected List<String> parseQuickSearchWords(String searchString, int minTextLength) {
        List<String> searchWords = new ArrayList<String>();
        if (StringUtils.isBlank(searchString)) {
            return searchWords;
        }
        Long maxWords = BeanHelper.getParametersService().getLongParameter(Parameters.QUICK_SEARCH_WORDS_COUNT);
        if (maxWords == null || maxWords < 1 || maxWords > 10) {
            // TODO: is this valid default maximum value?
            maxWords = 10L;
        }
        for (String searchWord : searchString.split("\\s")) {

            // Extract dates first.
            SearchUtil.extractDates(searchWord, searchWords);

            // Tokenize the search word (even if contained only full date - textual content also needs to be searched):
            String searchWordStripped = SearchUtil.stripCustom(SearchUtil.replaceCustom(searchWord, ""));
            for (Token token : getTokens(searchWordStripped)) {
                String termText = token.term();
                if (termText.length() >= minTextLength && searchWords.size() < maxWords) {
                    termText = QueryParser.escape(termText);

                    boolean exists = false;
                    for (String tmpWord : searchWords) {
                        exists |= tmpWord.equalsIgnoreCase(termText);
                    }
                    if (!exists) {
                        searchWords.add(termText);
                    }
                }
            }
        }
        return searchWords;
    }

    protected int countResults(List<ResultSet> resultSets) {
        int count = 0;
        try {
            for (ResultSet resultSet : resultSets) {
                count += resultSet.length();
            }
        } finally {
            try {
                for (ResultSet resultSet : resultSets) {
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        return count;
    }

    protected Pair<List<NodeRef>, Boolean> searchNodes(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImplWithoutSort(query, limit, queryName, new SearchCallback<NodeRef>() {
            @Override
            public NodeRef addResult(ResultSetRow row) {
                return row.getNodeRef();
            }
        }, storeRefs);
    }

    protected interface SearchCallback<E> {
        E addResult(ResultSetRow row);
    }

    protected <E extends Comparable<? super E>> Pair<List<E>, Boolean> searchGeneralImpl(String query, int limit, String queryName, SearchCallback<E> callback) {
        return searchGeneralImpl(query, limit, queryName, callback, null);
    }

    protected <E extends Comparable<? super E>> Pair<List<E>, Boolean> searchGeneralImpl( //
            String query, int limit, String queryName, SearchCallback<E> callback, Collection<StoreRef> storeRefs) {
        final Pair<List<E>, Boolean> extractResults = searchGeneralImplWithoutSort(query, limit, queryName, callback, storeRefs);
        Collections.sort(extractResults.getFirst());
        return extractResults;
    }

    protected <E> Pair<List<E>, Boolean> searchGeneralImplWithoutSort( //
            String query, int limit, String queryName, SearchCallback<E> callback, Collection<StoreRef> storeRefs) {
        if (StringUtils.isBlank(query)) {
            return new Pair<List<E>, Boolean>(new ArrayList<E>(), false);
        }
        StoreRef singleStoreRef = null;
        if (storeRefs == null) {
            singleStoreRef = generalService.getStore();
        } else if (storeRefs.size() == 1) {
            singleStoreRef = storeRefs.iterator().next();
        }
        long startTime = System.currentTimeMillis();
        final List<ResultSet> resultSets;
        if (singleStoreRef != null) {
            resultSets = Arrays.asList(doSearch(query, limit, queryName, singleStoreRef));
        } else {
            resultSets = doSearches(query, limit, queryName, storeRefs);
        }
        final Pair<List<E>, Boolean> extractResults = extractResults(callback, startTime, resultSets, limit);
        return extractResults;
    }

    private <E> Pair<List<E>, Boolean> extractResults(SearchCallback<E> callback, long startTime, final List<ResultSet> resultSets, int limit) {
        try {
            List<E> result = new ArrayList<E>();
            boolean resultsGotLimited = false;
            if (log.isDebugEnabled()) {
                long resultsCount = 0;
                for (ResultSet resultSet : resultSets) {
                    resultsCount += resultSet.length();
                }
                log.debug("Lucene search time " + (System.currentTimeMillis() - startTime) + " ms, results: " + resultsCount);
                startTime = System.currentTimeMillis();
            }

            FILL_RESULT: for (ResultSet resultSet : resultSets) {
                for (ResultSetRow row : resultSet) {
                    if (!nodeService.exists(row.getNodeRef())) {
                        continue;
                    }
                    E item = callback.addResult(row);
                    if (item == null) {
                        continue;
                    }
                    if (limit > -1 && result.size() + 1 > limit) {
                        resultsGotLimited = true;
                        break FILL_RESULT;
                    }
                    result.add(item);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Results construction time " + (System.currentTimeMillis() - startTime) + " ms, final results: " + result.size());
            }
            return new Pair<List<E>, Boolean>(result, resultsGotLimited);
        } finally {
            try {
                for (ResultSet resultSet : resultSets) {
                    resultSet.close();
                }
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
    protected ResultSet doSearch(String query, int limit, String queryName, StoreRef storeRef) {
        final SearchParameters sp = generateLuceneSearchParams(query, storeRef == null ? generalService.getStore() : storeRef, limit);
        return doSearchQuery(sp, queryName);
    }

    protected List<ResultSet> doSearches(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        final SearchParameters sp = generateLuceneSearchParams(query, null, limit);
        if (storeRefs == null || storeRefs.size() == 0) {
            storeRefs = Arrays.asList(generalService.getStore());
        }
        final Collection<StoreRef> stores = (storeRefs == null || storeRefs.size() == 0) ? Arrays.asList(generalService.getStore()) : storeRefs;
        List<ResultSet> results = new ArrayList<ResultSet>(stores.size());
        for (StoreRef storeRef : stores) {
            sp.getStores().clear();
            sp.addStore(storeRef);
            results.add(doSearchQuery(sp, queryName));

            // Optimization: don't search from other stores if limit is reached
            if (limit > -1 && results.size() > limit) {
                break;
            }
        }
        return results;
    }

    private static AtomicLong parallelQueryCount = new AtomicLong(0L);
    private static AtomicLong parallelClausesCount = new AtomicLong(0L);

    protected ResultSet doSearchQuery(SearchParameters sp, String queryName) {
        if (StringUtils.isBlank(sp.getQuery())) {
            return new EmptyResultSet();
        }
        long startTime = System.nanoTime();
        try {
            int clauses = -1;
            long parallelQueryCountBefore = -1;
            long parallelQueryCountAfter = -1;
            long parallelClausesCountBefore = -1;
            long parallelClausesCountAfter = -1;
            SearchStatistics.Data data = null;

            if (log.isInfoEnabled()) {
                // This clauses counting is simplified but enough for most queries.
                clauses = StringUtils.countMatches(sp.getQuery(), " AND ") + StringUtils.countMatches(sp.getQuery(), " OR ") + 1;
                parallelQueryCountBefore = parallelQueryCount.incrementAndGet();
                parallelClausesCountBefore = parallelClausesCount.addAndGet(clauses);
                SearchStatistics.enable();
            }

            ResultSet resultSet;
            try {
                resultSet = searchService.query(sp);
            } finally {
                if (log.isInfoEnabled()) {
                    parallelQueryCountAfter = parallelQueryCount.getAndDecrement();
                    parallelClausesCountAfter = parallelClausesCount.getAndAdd(clauses * -1L);
                    data = SearchStatistics.getData();
                    SearchStatistics.disable();
                }
            }

            if (log.isInfoEnabled()) {
                long duration = CalendarUtil.duration(startTime);
                StringBuilder sb = new StringBuilder(100).append("PERFORMANCE: query ").append(queryName).append(" - ").append(duration).append(" ms");

                sb.append("|").append(clauses);
                sb.append("|").append(parallelClausesCountBefore);
                sb.append("|").append(parallelClausesCountAfter);
                sb.append("|").append(parallelQueryCountBefore);
                sb.append("|").append(parallelQueryCountAfter);
                if (data != null) {
                    sb.append("|").append(data.luceneHitsTime).append(" ms");
                    sb.append("|").append(data.originalQueryChars);
                    sb.append("|").append(data.rewrittenQueryChars);
                }

                // When more than 60 clauses were found in a query, log the query.
                if (clauses > 60) {
                    sb.append("\n    query ").append(queryName).append(": ").append(sp.getQuery());
                }
                log.info(sb.toString());
            }

            return resultSet;
        } catch (BooleanQuery.TooManyClauses e) {
            log.error("Search failed with TooManyClauses exception, query expanded over limit\n    queryName=" + queryName + "\n    store=" + sp.getStores()
                    + "\n    limit=" + sp.getLimit() + "\n    limitBy=" + sp.getLimitBy().toString() + "\n    exceptionMessage=" + e.getMessage());
            throw e;
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setLuceneConfig(LuceneConfig config) {
        this.config = config;
        luceneAnalyser = new LuceneAnalyser(dictionaryService, config.getDefaultMLSearchAnalysisMode());
    }

    public LuceneAnalyser getAnalyzer() {
        return luceneAnalyser;
    }

    private List<Token> getTokens(String queryText) {
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
        // boolean severalTokensAtSamePosition = false; // FIXME this is never read

        while (true) {
            try {
                nextToken = source.next(reusableToken);
            } catch (IOException e) {
                nextToken = null;
            }
            if (nextToken == null) {
                break;
            }
            list.add((org.apache.lucene.analysis.Token) nextToken.clone());
        }
        try {
            source.close();
        } catch (IOException e) {
            // ignore
        }

        // add any alpha numeric wildcards that have been missed
        // Fixes most stop word and wild card issues

        for (int index = 0; index < testText.length(); index++) {
            char current = testText.charAt(index);
            if ((current == '*') || (current == '?')) {
                StringBuilder pre = new StringBuilder(10);
                if (index > 0) {
                    for (int i = index - 1; i >= 0; i--) {
                        char c = testText.charAt(i);
                        if (Character.isLetterOrDigit(c)) {
                            boolean found = false;
                            for (int j = 0; j < list.size(); j++) {
                                org.apache.lucene.analysis.Token test = list.get(j);
                                if ((test.startOffset() <= i) && (i <= test.endOffset())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                break;
                            }
                            pre.insert(0, c);
                        }
                    }
                    if (pre.length() > 0) {
                        // Add new token followed by * not given by the tokeniser
                        org.apache.lucene.analysis.Token newToken = new org.apache.lucene.analysis.Token(index - pre.length(), index);
                        newToken.setTermBuffer(pre.toString());
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication) {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            MLAnalysisMode mlAnalysisMode = config.getDefaultMLSearchAnalysisMode();
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, mlAnalysisMode);
                            Iterator<org.apache.lucene.analysis.Token> it = duplicator.buildIterator(newToken);
                            if (it != null) {
                                int count = 0;
                                while (it.hasNext()) {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1) {
                                        // severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else {
                            list.add(newToken);
                        }
                    }
                }

                StringBuilder post = new StringBuilder(10);
                if (index > 0) {
                    for (int i = index + 1; i < testText.length(); i++) {
                        char c = testText.charAt(i);
                        if (Character.isLetterOrDigit(c)) {
                            boolean found = false;
                            for (int j = 0; j < list.size(); j++) {
                                org.apache.lucene.analysis.Token test = list.get(j);
                                if ((test.startOffset() <= i) && (i <= test.endOffset())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                break;
                            }
                            post.append(c);
                        }
                    }
                    if (post.length() > 0) {
                        // Add new token followed by * not given by the tokeniser
                        org.apache.lucene.analysis.Token newToken = new org.apache.lucene.analysis.Token(index + 1, index + 1 + post.length());
                        newToken.setTermBuffer(post.toString());
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication) {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            MLAnalysisMode mlAnalysisMode = config.getDefaultMLSearchAnalysisMode();
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, mlAnalysisMode);
                            Iterator<org.apache.lucene.analysis.Token> it = duplicator.buildIterator(newToken);
                            if (it != null) {
                                int count = 0;
                                while (it.hasNext()) {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1) {
                                        // severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else {
                            list.add(newToken);
                        }
                    }
                }

            }
        }

        Collections.sort(list, new Comparator<org.apache.lucene.analysis.Token>()
        {

            @Override
            public int compare(Token o1, Token o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                if (dif != 0)
                {
                    return dif;
                }
                return o2.getPositionIncrement() - o1.getPositionIncrement();
            }
        });

        // Combined * and ? based strings - should redo the tokeniser

        // Assume we only string together tokens for the same position

        int max = 0;
        int current = 0;
        for (org.apache.lucene.analysis.Token c : list) {
            if (c.getPositionIncrement() == 0) {
                current++;
            } else {
                if (current > max) {
                    max = current;
                }
                current = 0;
            }
        }
        if (current > max) {
            max = current;
        }

        ArrayList<org.apache.lucene.analysis.Token> fixed = new ArrayList<org.apache.lucene.analysis.Token>();
        for (int repeat = 0; repeat <= max; repeat++) {
            org.apache.lucene.analysis.Token replace = null;
            current = 0;
            for (org.apache.lucene.analysis.Token c : list) {
                if (c.getPositionIncrement() == 0) {
                    current++;
                } else {
                    current = 0;
                }

                if (current == repeat) {

                    if (replace == null) {
                        StringBuilder prefix = new StringBuilder();
                        for (int i = c.startOffset() - 1; i >= 0; i--) {
                            char test = testText.charAt(i);
                            if ((test == '*') || (test == '?')) {
                                prefix.insert(0, test);
                            } else {
                                break;
                            }
                        }
                        String pre = prefix.toString();
                        if (requiresMLTokenDuplication) {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            int position = termText.indexOf("}");
                            String language = termText.substring(0, position + 1);
                            String token = termText.substring(position + 1);
                            replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                            replace.setTermBuffer(language + pre + token);
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        } else {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            replace = new org.apache.lucene.analysis.Token(c.startOffset() - pre.length(), c.endOffset());
                            replace.setTermBuffer(pre + termText);
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        }
                    } else {
                        StringBuilder prefix = new StringBuilder();
                        StringBuilder postfix = new StringBuilder();
                        StringBuilder builder = prefix;
                        for (int i = c.startOffset() - 1; i >= replace.endOffset(); i--) {
                            char test = testText.charAt(i);
                            if ((test == '*') || (test == '?')) {
                                builder.insert(0, test);
                            } else {
                                builder = postfix;
                                postfix.setLength(0);
                            }
                        }
                        String pre = prefix.toString();
                        String post = postfix.toString();

                        // Does it bridge?
                        if ((pre.length() > 0) && (replace.endOffset() + pre.length()) == c.startOffset()) {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            if (requiresMLTokenDuplication) {
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
                            } else {
                                int oldPositionIncrement = replace.getPositionIncrement();
                                String replaceTermText = new String(replace.termBuffer(), 0, replace.termLength());
                                replace = new org.apache.lucene.analysis.Token(replace.startOffset(), c.endOffset());
                                replace.setTermBuffer(replaceTermText + pre + termText);
                                replace.setType(replace.type());
                                replace.setPositionIncrement(oldPositionIncrement);
                            }
                        } else {
                            String termText = new String(c.termBuffer(), 0, c.termLength());
                            if (requiresMLTokenDuplication) {
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
                            } else {
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
            if (replace != null) {
                StringBuilder postfix = new StringBuilder();
                for (int i = replace.endOffset(); i < testText.length(); i++) {
                    char test = testText.charAt(i);
                    if ((test == '*') || (test == '?')) {
                        postfix.append(test);
                    } else {
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

            @Override
            public int compare(Token o1, Token o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                if (dif != 0)
                {
                    return dif;
                }
                return o2.getPositionIncrement() - o1.getPositionIncrement();
            }
        });
        return fixed;
    }

}
