package ee.webmedia.alfresco.functions.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;

/**
 * Service for searching and listing functions.
 */
public interface FunctionsService {

    String BEAN_NAME = "FunctionsService";
    String NON_TX_BEAN_NAME = "functionsService";

    /**
     * Returns a list of all found functions.
     *
     * @return
     */
    List<UnmodifiableFunction> getAllFunctions();

    List<UnmodifiableFunction> getFunctions(NodeRef functionsRoot);

    List<UnmodifiableFunction> getAllFunctions(DocListUnitStatus... status);

    Function getFunction(NodeRef functionRef, Map<Long, QName> propertyTypes);

    /**
     * Updates or saves the function.
     *
     * @param fn
     */
    void saveOrUpdate(Function fn);

    Function saveOrUpdate(Function function, NodeRef functionsRoot);

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

    void delete(Function function);

    /**
     * Reopen the function by setting DocListUnitStatus.OPEN status
     *
     * @param function
     */
    void reopenFunction(Function function);

    NodeRef getFunctionsRoot();

    Location getDocumentListLocation();

    List<ChildAssociationRef> getFunctionAssocs(NodeRef functionsRoot);

    boolean isDraftsFunction(NodeRef functionRef);

    List<NodeRef> getAllLimitedActivityFunctions();

    String getFunctionLabel(NodeRef functionRef);

    UnmodifiableFunction getUnmodifiableFunction(NodeRef functionRef, Map<Long, QName> propertyTypes);

    void removeFromCache(NodeRef functionRef);

}
