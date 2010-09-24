package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdfResponse", propOrder = { "msoOutput" })
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoOutput msoOutput;

    public MsoOutput getMsoOutput() {
        return msoOutput;
    }

    public void setMsoOutput(MsoOutput msoOutput) {
        this.msoOutput = msoOutput;
    }

}
