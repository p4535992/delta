package ee.webmedia.ocr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdfResponse", propOrder = { "ocrOutput" })
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected OcrOutput ocrOutput;

    public OcrOutput getOcrOutput() {
        return ocrOutput;
    }

    public void setOcrOutput(OcrOutput ocrOutput) {
        this.ocrOutput = ocrOutput;
    }

}
