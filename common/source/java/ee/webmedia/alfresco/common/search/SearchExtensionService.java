package ee.webmedia.alfresco.common.search;

import org.alfresco.web.bean.search.SearchContext;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public interface SearchExtensionService {

    String BEAN_NAME = "SearchExtensionService";

    void registerSearchQueryBuilder(SearchQueryBuilderFactory searchQueryBuilderFactory);

    SearchQueryBuilder getSearchQueryBuilder(SearchContext searchContext);

}
