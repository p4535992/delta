package ee.webmedia.mso;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "modifiedFormulasOutput", propOrder = { "modifiedFormulas" })
public class ModifiedFormulasOutput {

    @XmlElement(required = true)
    protected Set<Formula> modifiedFormulas;

    public Set<Formula> getFormulas() {
        return modifiedFormulas;
    }

    public void setModifiedFormulas(Set<Formula> formulas) {
        modifiedFormulas = formulas;
    }

}
