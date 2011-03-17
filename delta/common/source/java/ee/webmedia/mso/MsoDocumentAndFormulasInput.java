
package ee.webmedia.mso;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for msoDocumentAndFormulasInput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="msoDocumentAndFormulasInput">
 *   &lt;complexContent>
 *     &lt;extension base="{http://webmedia.ee/mso}msoDocumentInput">
 *       &lt;sequence>
 *         &lt;element name="formula" type="{http://webmedia.ee/mso}formula" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoDocumentAndFormulasInput", propOrder = {
    "formula"
})
public class MsoDocumentAndFormulasInput
    extends MsoDocumentInput
{

    @XmlElement(required = true)
    protected List<Formula> formula;

    /**
     * Gets the value of the formula property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the formula property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFormula().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Formula }
     * 
     * 
     */
    public List<Formula> getFormula() {
        if (formula == null) {
            formula = new ArrayList<Formula>();
        }
        return this.formula;
    }

}
