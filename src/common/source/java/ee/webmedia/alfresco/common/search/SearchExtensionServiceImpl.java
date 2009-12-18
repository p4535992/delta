package ee.webmedia.alfresco.common.search;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.web.bean.search.SearchContext;

/**
 * @author Alar Kvell
 */
public class SearchExtensionServiceImpl implements SearchExtensionService {

    private List<SearchQueryBuilderFactory> extensionFactorys = new ArrayList<SearchQueryBuilderFactory>();

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
