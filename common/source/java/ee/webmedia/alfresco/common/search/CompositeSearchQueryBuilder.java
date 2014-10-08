package ee.webmedia.alfresco.common.search;

import java.util.Collection;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class CompositeSearchQueryBuilder implements SearchQueryBuilder {

    private final Collection<SearchQueryBuilder> builders;

    public CompositeSearchQueryBuilder(Collection<SearchQueryBuilder> builders) {
        this.builders = builders;
    }

    @Override
    public String getQuery() {
        StringBuilder s = new StringBuilder(builders.size() * 128);
        for (SearchQueryBuilder b : builders) {
            s.append(b.getQuery());
        }
        return s.toString();
    }

    @Override
    public void preProcessMultipleWordSearchAsIndividualSearchTerms() {
        for (SearchQueryBuilder b : builders) {
            b.preProcessMultipleWordSearchAsIndividualSearchTerms();
        }
    }

    @Override
    public void postProcessMultipleWordSearchAsIndividualSearchTerms() {
        for (SearchQueryBuilder b : builders) {
            b.postProcessMultipleWordSearchAsIndividualSearchTerms();
        }
    }

    @Override
    public void processMultipleWordSearchAsIndividualSearchTerms(String term, boolean operatorAND, boolean operatorNOT) {
        for (SearchQueryBuilder b : builders) {
            b.processMultipleWordSearchAsIndividualSearchTerms(term, operatorAND, operatorNOT);
        }
    }

    @Override
    public void processMultipleWordSearchAsSingleQuotedPhrase(String quotedSafeText) {
        for (SearchQueryBuilder b : builders) {
            b.processMultipleWordSearchAsSingleQuotedPhrase(quotedSafeText);
        }
    }

    @Override
    public void processSingleWordSearch(String text, boolean operatorNOT) {
        for (SearchQueryBuilder b : builders) {
            b.processSingleWordSearch(text, operatorNOT);
        }
    }

}
