package ee.webmedia.mso;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoDocumentAndFormulasInput", propOrder = { "formula" })
public class MsoDocumentAndFormulasInput extends MsoDocumentInput {

    @XmlElement(required = true)
    protected List<Formula> formula;

    public List<Formula> getFormula() {
        return formula;
    }

    public void setFormula(List<Formula> formula) {
        this.formula = formula;
    }

}
