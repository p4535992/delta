package ee.webmedia.alfresco.common.search;

public interface SearchQueryBuilder {

    void processSingleWordSearch(String text, boolean operatorNOT);

    void processMultipleWordSearchAsSingleQuotedPhrase(String quotedSafeText);

    void preProcessMultipleWordSearchAsIndividualSearchTerms();

    void processMultipleWordSearchAsIndividualSearchTerms(String term, boolean operatorAND, boolean operatorNOT);

    void postProcessMultipleWordSearchAsIndividualSearchTerms();

    String getQuery();

}
