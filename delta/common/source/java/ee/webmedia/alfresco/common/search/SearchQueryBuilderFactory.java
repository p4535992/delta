package ee.webmedia.alfresco.common.search;

import org.alfresco.web.bean.search.SearchContext;

/**
 * @author Alar Kvell
 */
public interface SearchQueryBuilderFactory {

    SearchQueryBuilder createInstance(SearchContext searchContext);

}
