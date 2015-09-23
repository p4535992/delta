package ee.webmedia.alfresco.docdynamic.web;

import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;

public interface DocumentDynamicBlock extends DialogBlockBean<DialogDataProvider> {

    /**
     * Reset all fields and components
     * 
     * @param provider may be {@code null}. When not null, then it should be interpreted as request for initializing block bean
     */
    @Override
    void resetOrInit(DialogDataProvider provider);

    void clean();

}
