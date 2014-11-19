<<<<<<< HEAD
package ee.webmedia.alfresco.thesaurus.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;

/**
 * @author Kaarel Jõgeva
 *
 */
public interface ThesaurusService {

    String BEAN_NAME = "ThesaurusService";

    /**
     * Returns all thesauri from repository
     * 
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return list with all thesauri
     */
    List<Thesaurus> getThesauri(boolean fetchKeywords);

    /**
     * Returns thesaurus with given name. If a thesaurus with given name cannot be found, null is returned.
     * 
     * @param name name of the thesaurus
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return Thesaurus or null
     */
    Thesaurus getThesaurus(String name, boolean fetchKeywords);

    /**
     * Returns thesaurus with given NodeRef.
     * 
     * @param nodeRef nodeRef of the thesaurus
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return Thesaurus
     */
    Thesaurus getThesaurus(NodeRef nodeRef, boolean fetchKeywords);

    /**
     * Fetches a keyword from repository with given NodeRef.
     * 
     * @param keywordNodeRef NodeRef on the HierarchicalKeyword
     * @return keyword with given NodeRef
     */
    HierarchicalKeyword getKeyword(NodeRef keywordNodeRef);

    /**
     * Collects all keywords for a thesaurus with the given NodeRef
     * 
     * @param thesaurusNodeRef NodeRef of the thesaurus
     * @return List with keywords
     */
    List<HierarchicalKeyword> getThesaurusKeywords(NodeRef thesaurusNodeRef);

    /**
     * Create or update a thesaurus. Also stores changes to keywords.
     * 
     * @param thesaurus NodeRef of the thesaurus to update.
     * @return updated thesaurus that is in sync with repository
     */
    Thesaurus saveThesaurus(Thesaurus thesaurus);

    /**
     * Outputs all thesauri in XML format that can be use for importing
     * 
     * @param writer
     * @throws IOException
     */
    void exportThesauri(OutputStreamWriter writer) throws IOException;

    /**
     * Adds new thesauri and keywords to repository.
     * 
     * @param importedThesauri List of thesauri to be updated
     * @return true if any changes were made
     */
    boolean importThesauri(List<Thesaurus> importedThesauri);

    /**
     * Checks if thesaurus is bound to a fieldGroup
     * 
     * @param thesaurusName name of the thesaurus
     * @return true, if thesaurus is used in the system
     */
    boolean isThesaurusUsed(String thesaurusName);

    /**
     * Return the count of thesauri defined
     * 
     * @return number of thesauri found
     */
    int getThesauriCount();

}
=======
package ee.webmedia.alfresco.thesaurus.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;

/**
 *
 */
public interface ThesaurusService {

    String BEAN_NAME = "ThesaurusService";

    /**
     * Returns all thesauri from repository
     * 
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return list with all thesauri
     */
    List<Thesaurus> getThesauri(boolean fetchKeywords);

    /**
     * Returns thesaurus with given name. If a thesaurus with given name cannot be found, null is returned.
     * 
     * @param name name of the thesaurus
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return Thesaurus or null
     */
    Thesaurus getThesaurus(String name, boolean fetchKeywords);

    /**
     * Returns thesaurus with given NodeRef.
     * 
     * @param nodeRef nodeRef of the thesaurus
     * @param fetchKeywords if true, keywords are also fetched and added to the thesaurus
     * @return Thesaurus
     */
    Thesaurus getThesaurus(NodeRef nodeRef, boolean fetchKeywords);

    /**
     * Fetches a keyword from repository with given NodeRef.
     * 
     * @param keywordNodeRef NodeRef on the HierarchicalKeyword
     * @return keyword with given NodeRef
     */
    HierarchicalKeyword getKeyword(NodeRef keywordNodeRef);

    /**
     * Collects all keywords for a thesaurus with the given NodeRef
     * 
     * @param thesaurusNodeRef NodeRef of the thesaurus
     * @return List with keywords
     */
    List<HierarchicalKeyword> getThesaurusKeywords(NodeRef thesaurusNodeRef);

    /**
     * Create or update a thesaurus. Also stores changes to keywords.
     * 
     * @param thesaurus NodeRef of the thesaurus to update.
     * @return updated thesaurus that is in sync with repository
     */
    Thesaurus saveThesaurus(Thesaurus thesaurus);

    /**
     * Outputs all thesauri in XML format that can be use for importing
     * 
     * @param writer
     * @throws IOException
     */
    void exportThesauri(OutputStreamWriter writer) throws IOException;

    /**
     * Adds new thesauri and keywords to repository.
     * 
     * @param importedThesauri List of thesauri to be updated
     * @return true if any changes were made
     */
    boolean importThesauri(List<Thesaurus> importedThesauri);

    /**
     * Checks if thesaurus is bound to a fieldGroup
     * 
     * @param thesaurusName name of the thesaurus
     * @return true, if thesaurus is used in the system
     */
    boolean isThesaurusUsed(String thesaurusName);

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
