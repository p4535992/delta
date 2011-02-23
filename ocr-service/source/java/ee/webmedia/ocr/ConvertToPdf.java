package ee.webmedia.ocr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdf", propOrder = { "ocrInput" })
public class ConvertToPdf {

    @XmlElement(required = true)
    protected OcrInput ocrInput;

    public OcrInput getOcrInput() {
        return ocrInput;
    }

    public void setOcrInput(OcrInput ocrInput) {
        this.ocrInput = ocrInput;
    }

}
