
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
 *         &lt;element name="msoOutput" type="{http://webmedia.ee/mso}msoOutput"/>
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
    "msoOutput"
})
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoOutput msoOutput;

    /**
     * Gets the value of the msoOutput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoOutput }
     *     
     */
    public MsoOutput getMsoOutput() {
        return msoOutput;
    }

    /**
     * Sets the value of the msoOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoOutput }
     *     
     */
    public void setMsoOutput(MsoOutput value) {
        this.msoOutput = value;
    }

}
