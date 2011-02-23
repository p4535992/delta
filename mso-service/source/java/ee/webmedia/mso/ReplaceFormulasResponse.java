package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulasResponse", propOrder = { "msoDocumentOutput" })
public class ReplaceFormulasResponse {

    @XmlElement(required = true)
    protected MsoDocumentOutput msoDocumentOutput;

    public MsoDocumentOutput getMsoDocumentOutput() {
        return msoDocumentOutput;
    }

    public void setMsoDocumentOutput(MsoDocumentOutput msoDocumentOutput) {
        this.msoDocumentOutput = msoDocumentOutput;
    }

}
