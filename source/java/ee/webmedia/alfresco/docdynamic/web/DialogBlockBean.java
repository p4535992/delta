package ee.webmedia.alfresco.docdynamic.web;

import java.io.Serializable;

/**
 * Base interface for Block beans of a dialog, that uses type <D> to reset each block
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public interface DialogBlockBean<D> extends Serializable {

    /**
     * Reset all fields and components
     * 
     * @param dataProvider may be {@code null}
     */
    void resetOrInit(D dataProvider);

}
