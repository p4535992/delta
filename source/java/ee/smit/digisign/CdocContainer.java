package ee.smit.digisign;

import org.alfresco.util.Base64;

public class CdocContainer {
    private final String container;

    public CdocContainer(String container){
        this.container = container;
    }

    public String getContainerBase64(){
        return container;
    }

    public byte[] getContainer(){
        return Base64.decode(container);
    }
}
