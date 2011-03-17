
package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for replaceFormulas complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="replaceFormulas">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msoDocumentAndFormulasInput" type="{http://webmedia.ee/mso}msoDocumentAndFormulasInput"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulas", propOrder = {
    "msoDocumentAndFormulasInput"
})
public class ReplaceFormulas {

    @XmlElement(required = true)
    protected MsoDocumentAndFormulasInput msoDocumentAndFormulasInput;

    /**
     * Gets the value of the msoDocumentAndFormulasInput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoDocumentAndFormulasInput }
     *     
     */
    public MsoDocumentAndFormulasInput getMsoDocumentAndFormulasInput() {
        return msoDocumentAndFormulasInput;
    }

    /**
     * Sets the value of the msoDocumentAndFormulasInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoDocumentAndFormulasInput }
     *     
     */
    public void setMsoDocumentAndFormulasInput(MsoDocumentAndFormulasInput value) {
        this.msoDocumentAndFormulasInput = value;
    }

}
