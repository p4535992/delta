
package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for convertToPdf complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="convertToPdf">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msoDocumentInput" type="{http://webmedia.ee/mso}msoDocumentInput"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdf", propOrder = {
    "msoDocumentInput"
})
public class ConvertToPdf {

    @XmlElement(required = true)
    protected MsoDocumentInput msoDocumentInput;

    /**
     * Gets the value of the msoDocumentInput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoDocumentInput }
     *     
     */
    public MsoDocumentInput getMsoDocumentInput() {
        return msoDocumentInput;
    }

    /**
     * Sets the value of the msoDocumentInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoDocumentInput }
     *     
     */
    public void setMsoDocumentInput(MsoDocumentInput value) {
        this.msoDocumentInput = value;
    }

}
