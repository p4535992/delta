package ee.webmedia.alfresco.docdynamic.web;

import java.io.Serializable;

import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;

public interface DocumentDynamicBlock extends Serializable {

    /**
     * Reset all fields and components
     * 
     * @param provider may be {@code null}
     */
    void reset(DialogDataProvider provider);

}
