package ee.webmedia.ocr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ocrOutput", propOrder = { "log" })
public class OcrOutput extends OcrInput {

    @XmlElement(required = true)
    protected String log;

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

}
