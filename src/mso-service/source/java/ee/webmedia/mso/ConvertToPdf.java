package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdf", propOrder = { "msoInput" })
public class ConvertToPdf {

    @XmlElement(required = true)
    protected MsoInput msoInput;

    public MsoInput getMsoFile() {
        return msoInput;
    }

    public void setMsoFile(MsoInput msoInput) {
        this.msoInput = msoInput;
    }

}
