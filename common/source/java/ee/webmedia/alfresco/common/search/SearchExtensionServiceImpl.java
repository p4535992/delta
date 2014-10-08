package ee.webmedia.alfresco.common.search;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.web.bean.search.SearchContext;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class SearchExtensionServiceImpl implements SearchExtensionService {

    private final List<SearchQueryBuilderFactory> extensionFactorys = new ArrayList<SearchQueryBuilderFactory>();

    @Override
    public void registerSearchQueryBuilder(SearchQueryBuilderFactory searchQueryBuilderFactory) {
        extensionFactorys.add(searchQueryBuilderFactory);
    }

    @Override
    public SearchQueryBuilder getSearchQueryBuilder(SearchContext searchContext) {
        List<SearchQueryBuilder> builders = new ArrayList<SearchQueryBuilder>(extensionFactorys.size());
        for (SearchQueryBuilderFactory factory : extensionFactorys) {
            builders.add(factory.createInstance(searchContext));
        }
        return new CompositeSearchQueryBuilder(builders);
    }

}
