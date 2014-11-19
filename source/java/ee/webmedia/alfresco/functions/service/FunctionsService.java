package ee.webmedia.alfresco.functions.service;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.Location;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;

/**
 * Service for searching and listing functions.
<<<<<<< HEAD
 * 
 * @author Dmitri Melnikov
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public interface FunctionsService {

    public static final String BEAN_NAME = "FunctionsService";

    /**
     * Returns a list of all found functions.
     * 
     * @return
     */
    List<Function> getAllFunctions();

    List<Function> getFunctions(NodeRef functionsRoot);

    List<Function> getAllFunctions(DocListUnitStatus status);

    /**
     * Returns a function by its string nodeRef;
     * 
     * @return
     */
    Function getFunctionByNodeRef(String nodeRef);

    /**
     * Returns a function by its nodeRef;
     * 
     * @return
     */
    Function getFunctionByNodeRef(NodeRef nodeRef);

    /**
     * Updates or saves the function.
     * 
     * @param fn
     */
    void saveOrUpdate(Function fn);

    void saveOrUpdate(Function function, NodeRef functionsRoot);

    /**
     * Created a new function.
     * 
     * @return
     */
    Function createFunction();

    /**
     * Closes the function.
     * False if there are unclosed series under the function, true otherwise.
     * 
     * @param function
     * @return
     */
    boolean closeFunction(Function function);

<<<<<<< HEAD
    void delete(Function function);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    /**
     * Reopen the function by setting DocListUnitStatus.OPEN status
     * 
     * @param function
     */
    void reopenFunction(Function function);

    NodeRef getFunctionsRoot();

    Location getDocumentListLocation();

    public List<ChildAssociationRef> getFunctionAssocs(NodeRef functionsRoot);

<<<<<<< HEAD
    boolean isDraftsFunction(NodeRef functionRef);

    List<NodeRef> getAllLimitedActivityFunctions();

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
