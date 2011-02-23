
package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for replaceFormulasResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="replaceFormulasResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msoDocumentOutput" type="{http://webmedia.ee/mso}msoDocumentOutput"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulasResponse", propOrder = {
    "msoDocumentOutput"
})
public class ReplaceFormulasResponse {

    @XmlElement(required = true)
    protected MsoDocumentOutput msoDocumentOutput;

    /**
     * Gets the value of the msoDocumentOutput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoDocumentOutput }
     *     
     */
    public MsoDocumentOutput getMsoDocumentOutput() {
        return msoDocumentOutput;
    }

    /**
     * Sets the value of the msoDocumentOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoDocumentOutput }
     *     
     */
    public void setMsoDocumentOutput(MsoDocumentOutput value) {
        this.msoDocumentOutput = value;
    }

}
