
package ee.webmedia.mso;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for msoPdfOutput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="msoPdfOutput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pdfFile" type="{http://ws-i.org/profiles/basic/1.1/xsd}swaRef"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoPdfOutput", propOrder = {
    "pdfFile"
})
public class MsoPdfOutput {

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler pdfFile;

    /**
     * Gets the value of the pdfFile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public DataHandler getPdfFile() {
        return pdfFile;
    }

    /**
     * Sets the value of the pdfFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPdfFile(DataHandler value) {
        this.pdfFile = value;
    }

}
