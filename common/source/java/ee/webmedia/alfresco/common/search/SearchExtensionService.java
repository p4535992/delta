package ee.webmedia.alfresco.common.search;

import org.alfresco.web.bean.search.SearchContext;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface SearchExtensionService {

    String BEAN_NAME = "SearchExtensionService";

    void registerSearchQueryBuilder(SearchQueryBuilderFactory searchQueryBuilderFactory);

    SearchQueryBuilder getSearchQueryBuilder(SearchContext searchContext);

}
