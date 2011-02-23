package ee.webmedia.mso;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoDocumentAndPdfOutput", propOrder = { "pdfFile" })
public class MsoDocumentAndPdfOutput extends MsoDocumentOutput {

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler pdfFile;

    public DataHandler getPdfFile() {
        return pdfFile;
    }

    public void setPdfFile(DataHandler pdfFile) {
        this.pdfFile = pdfFile;
    }

}
