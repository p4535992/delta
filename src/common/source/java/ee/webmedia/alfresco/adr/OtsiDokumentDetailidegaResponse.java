
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for dokumentDetailidegaResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dokumentDetailidegaResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dokumentDetailidega" type="{http://alfresco/avalikdokumendiregister}dokumentDetailidega" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dokumentDetailidegaResponse", propOrder = {
    "dokumentDetailidega"
})
public class OtsiDokumentDetailidegaResponse {

    protected DokumentDetailidega dokumentDetailidega;

    /**
     * Gets the value of the dokumentDetailidega property.
     * 
     * @return
     *     possible object is
     *     {@link DokumentDetailidega }
     *     
     */
    public DokumentDetailidega getDokumentDetailidega() {
        return dokumentDetailidega;
    }

    /**
     * Sets the value of the dokumentDetailidega property.
     * 
     * @param value
     *     allowed object is
     *     {@link DokumentDetailidega }
     *     
     */
    public void setDokumentDetailidega(DokumentDetailidega value) {
        this.dokumentDetailidega = value;
    }

}
