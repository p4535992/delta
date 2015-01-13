package ee.webmedia.alfresco.common.search;

import org.alfresco.web.bean.search.SearchContext;

public interface SearchQueryBuilderFactory {

    SearchQueryBuilder createInstance(SearchContext searchContext);

}
