package ee.webmedia.alfresco.register.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.register.model.Register;

/**
 * @author Kaarel Jõgeva
 */
public interface RegisterService {
    public static final String BEAN_NAME = "RegisterService";

    /**
     * Gets all registers
     * 
     * @return list with available registers
     */
    List<Register> getRegisters();

    /**
     * Gets the register node with provided id
     * 
     * @param id registers unique ID
     * @return register node with specified ID
     */
    Node getRegisterNode(int id);

    /**
     * @param registerId
     * @return instance of Register based on registerId
     * @see #getRegisterNode(int);
     */
    Register getRegister(Integer registerId);

    /**
     * Returns the registers root space NodeRef
     * 
     * @return Root NodeRef
     */
    NodeRef getRoot();

    /**
     * Returns the maximum id property from all registers
     * 
     * @return int maximum id
     */
    int getMaxRegisterId();

    /**
     * Creates new temporary Register node
     * 
     * @param prop
     * @return
     */
    Node createRegister();

    /**
     * Updates registers properties in repository
     * 
     * @param register node to update
     */
    void updateProperties(Node register);

    void updatePropertiesFromObject(Register register);

    /**
     * @param registerId
     * @return new value after incrementing
     */
    int increaseCount(int registerId);

    /**
     * reset register counter
     * 
     * @param register
     */
    void resetCounter(Node register);

    boolean isValueEditable();

}
