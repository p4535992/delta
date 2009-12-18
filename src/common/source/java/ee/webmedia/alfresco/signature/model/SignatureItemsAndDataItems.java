package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.List;

public class SignatureItemsAndDataItems implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<SignatureItem> signatureItems;
    private List<DataItem> dataItems;

    public List<SignatureItem> getSignatureItems() {
        return signatureItems;
    }

    public void setSignatureItems(List<SignatureItem> signatureItems) {
        this.signatureItems = signatureItems;
    }

    public List<DataItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<DataItem> dataItems) {
        this.dataItems = dataItems;
    }

}
