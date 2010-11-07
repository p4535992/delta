package ee.webmedia.mso;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoDocumentOutput", propOrder = { "documentFile" })
public class MsoDocumentOutput {

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler documentFile;

    public DataHandler getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(DataHandler documentFile) {
        this.documentFile = documentFile;
    }

}
