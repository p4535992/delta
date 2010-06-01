
package ee.webmedia.ocr;

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
 *         &lt;element name="ocrInput" type="{http://webmedia.ee/ocr}ocrInput"/>
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
    "ocrInput"
})
public class ConvertToPdf {

    @XmlElement(required = true)
    protected OcrInput ocrInput;

    /**
     * Gets the value of the ocrInput property.
     * 
     * @return
     *     possible object is
     *     {@link OcrInput }
     *     
     */
    public OcrInput getOcrInput() {
        return ocrInput;
    }

    /**
     * Sets the value of the ocrInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link OcrInput }
     *     
     */
    public void setOcrInput(OcrInput value) {
        this.ocrInput = value;
    }

}
