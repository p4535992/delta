
package ee.webmedia.mso;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for msoDocumentOutput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="msoDocumentOutput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="documentFile" type="{http://ws-i.org/profiles/basic/1.1/xsd}swaRef"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoDocumentOutput", propOrder = {
    "documentFile"
})
@XmlSeeAlso({
    MsoDocumentAndPdfOutput.class
})
public class MsoDocumentOutput {

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler documentFile;

    /**
     * Gets the value of the documentFile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public DataHandler getDocumentFile() {
        return documentFile;
    }

    /**
     * Sets the value of the documentFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentFile(DataHandler value) {
        this.documentFile = value;
    }

}
