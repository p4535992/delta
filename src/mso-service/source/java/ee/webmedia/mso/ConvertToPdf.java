package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdf", propOrder = { "msoDocumentInput" })
public class ConvertToPdf {

    @XmlElement(required = true)
    protected MsoDocumentInput msoDocumentInput;

    public MsoDocumentInput getMsoFile() {
        return msoDocumentInput;
    }

    public void setMsoFile(MsoDocumentInput msoDocumentInput) {
        this.msoDocumentInput = msoDocumentInput;
    }

}
