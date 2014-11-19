package ee.webmedia.alfresco.common.search;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface SearchQueryBuilder {

    void processSingleWordSearch(String text, boolean operatorNOT);

    void processMultipleWordSearchAsSingleQuotedPhrase(String quotedSafeText);

    void preProcessMultipleWordSearchAsIndividualSearchTerms();

    void processMultipleWordSearchAsIndividualSearchTerms(String term, boolean operatorAND, boolean operatorNOT);

    void postProcessMultipleWordSearchAsIndividualSearchTerms();

    String getQuery();

}
