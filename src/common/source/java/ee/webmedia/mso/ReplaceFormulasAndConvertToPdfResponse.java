
package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for replaceFormulasAndConvertToPdfResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="replaceFormulasAndConvertToPdfResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msoDocumentAndPdfOutput" type="{http://webmedia.ee/mso}msoDocumentAndPdfOutput"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulasAndConvertToPdfResponse", propOrder = {
    "msoDocumentAndPdfOutput"
})
public class ReplaceFormulasAndConvertToPdfResponse {

    @XmlElement(required = true)
    protected MsoDocumentAndPdfOutput msoDocumentAndPdfOutput;

    /**
     * Gets the value of the msoDocumentAndPdfOutput property.
     * 
     * @return
     *     possible object is
     *     {@link MsoDocumentAndPdfOutput }
     *     
     */
    public MsoDocumentAndPdfOutput getMsoDocumentAndPdfOutput() {
        return msoDocumentAndPdfOutput;
    }

    /**
     * Sets the value of the msoDocumentAndPdfOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link MsoDocumentAndPdfOutput }
     *     
     */
    public void setMsoDocumentAndPdfOutput(MsoDocumentAndPdfOutput value) {
        this.msoDocumentAndPdfOutput = value;
    }

}
