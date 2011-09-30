package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "modifiedFormulasResponse", propOrder = { "modifiedFormulasOutput" })
public class ModifiedFormulasResponse {

    @XmlElement(required = true)
    protected ModifiedFormulasOutput modifiedFormulasOutput;

    public ModifiedFormulasOutput getModifiedFormulasOutput() {
        return modifiedFormulasOutput;
    }

    public void setModifiedFormulasOutput(ModifiedFormulasOutput modifiedFormulasOutput) {
        this.modifiedFormulasOutput = modifiedFormulasOutput;
    }

}
