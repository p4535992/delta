
package ee.webmedia.ocr;

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
 *         &lt;element name="ocrOutput" type="{http://webmedia.ee/ocr}ocrOutput"/>
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
    "ocrOutput"
})
public class ConvertToPdfResponse {

    @XmlElement(required = true)
    protected OcrOutput ocrOutput;

    /**
     * Gets the value of the ocrOutput property.
     * 
     * @return
     *     possible object is
     *     {@link OcrOutput }
     *     
     */
    public OcrOutput getOcrOutput() {
        return ocrOutput;
    }

    /**
     * Sets the value of the ocrOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link OcrOutput }
     *     
     */
    public void setOcrOutput(OcrOutput value) {
        this.ocrOutput = value;
    }

}
