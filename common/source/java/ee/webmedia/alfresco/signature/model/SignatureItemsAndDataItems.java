package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.List;

public class SignatureItemsAndDataItems implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<SignatureItem> signatureItems;
    private final List<DataItem> dataItems;

    public SignatureItemsAndDataItems(List<SignatureItem> signatureItems, List<DataItem> dataItems) {
        this.signatureItems = signatureItems;
        this.dataItems = dataItems;
    }

    public List<SignatureItem> getSignatureItems() {
        return signatureItems;
    }

    public List<DataItem> getDataItems() {
        return dataItems;
    }

}
