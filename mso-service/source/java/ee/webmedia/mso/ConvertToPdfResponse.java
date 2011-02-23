package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdfResponse", propOrder = { "msoPdfOutput" })
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoPdfOutput msoPdfOutput;

    public MsoPdfOutput getMsoOutput() {
        return msoPdfOutput;
    }

    public void setMsoOutput(MsoPdfOutput msoPdfOutput) {
        this.msoPdfOutput = msoPdfOutput;
    }

}
