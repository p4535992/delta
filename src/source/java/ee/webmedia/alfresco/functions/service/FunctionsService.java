package ee.webmedia.alfresco.functions.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;

/**
 * Service for searching and listing functions.
 * 
 * @author Dmitri Melnikov
 */
public interface FunctionsService {
    
    public static final String BEAN_NAME = "FunctionsService";

    /**
     * Returns a list of all found functions.
     * @return
     */
    List<Function> getAllFunctions();

    List<Function> getAllFunctions(DocListUnitStatus status);

    /**
     * Returns a function by its string nodeRef;
     * @return
     */
    Function getFunctionByNodeRef(String nodeRef);
    
    /**
     * Returns a function by its nodeRef;
     * @return
     */
    Function getFunctionByNodeRef(NodeRef nodeRef);
    
    /**
     * Updates or saves the function.
     * @param fn
     */
    void saveOrUpdate(Function fn);
    
    /**
     * Created a new function.
     * @return
     */
    Function createFunction();
}
