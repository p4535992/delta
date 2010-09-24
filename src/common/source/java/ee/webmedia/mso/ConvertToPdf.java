
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
 *         &lt;element name="msoInput" type="{http://webmedia.ee/mso}msoInput"/>
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
    "msoInput"
})
public class ConvertToPdf {

    @XmlElement(required = true)
    protected MsoInput msoInput;

    /**
     * Gets the value of the msoInput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoInput }
     *     
     */
    public MsoInput getMsoInput() {
        return msoInput;
    }

    /**
     * Sets the value of the msoInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoInput }
     *     
     */
    public void setMsoInput(MsoInput value) {
        this.msoInput = value;
    }

}
