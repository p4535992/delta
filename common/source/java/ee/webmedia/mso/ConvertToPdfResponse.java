
package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for convertToPdfResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="convertToPdfResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msoPdfOutput" type="{http://webmedia.ee/mso}msoPdfOutput"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "convertToPdfResponse", propOrder = {
    "msoPdfOutput"
})
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoPdfOutput msoPdfOutput;

    /**
     * Gets the value of the msoPdfOutput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoPdfOutput }
     *     
     */
    public MsoPdfOutput getMsoPdfOutput() {
        return msoPdfOutput;
    }

    /**
     * Sets the value of the msoPdfOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoPdfOutput }
     *     
     */
    public void setMsoPdfOutput(MsoPdfOutput value) {
        this.msoPdfOutput = value;
    }

}
