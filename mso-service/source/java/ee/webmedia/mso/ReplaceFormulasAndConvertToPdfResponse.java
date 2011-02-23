package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulasAndConvertToPdfResponse", propOrder = { "msoDocumentAndPdfOutput" })
public class ReplaceFormulasAndConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoDocumentAndPdfOutput msoDocumentAndPdfOutput;

    public MsoDocumentAndPdfOutput getMsoDocumentAndPdfOutput() {
        return msoDocumentAndPdfOutput;
    }

    public void setMsoDocumentAndPdfOutput(MsoDocumentAndPdfOutput msoDocumentAndPdfOutput) {
        this.msoDocumentAndPdfOutput = msoDocumentAndPdfOutput;
    }

}
