package ee.webmedia.alfresco.common.search;

import org.alfresco.web.bean.search.SearchContext;

public interface SearchExtensionService {

    String BEAN_NAME = "SearchExtensionService";

    void registerSearchQueryBuilder(SearchQueryBuilderFactory searchQueryBuilderFactory);

    SearchQueryBuilder getSearchQueryBuilder(SearchContext searchContext);

}
