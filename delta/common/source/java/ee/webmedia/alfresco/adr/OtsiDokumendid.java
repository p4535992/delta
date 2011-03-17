
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for otsiDokumendid complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="otsiDokumendid">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="perioodiAlgusKuupaev" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="perioodiLoppKuupaev" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="dokumendiLiik" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="otsingusona" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otsiDokumendid", propOrder = {
    "perioodiAlgusKuupaev",
    "perioodiLoppKuupaev",
    "dokumendiLiik",
    "otsingusona"
})
public class OtsiDokumendid {

    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar perioodiAlgusKuupaev;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar perioodiLoppKuupaev;
    protected String dokumendiLiik;
    protected String otsingusona;

    /**
     * Gets the value of the perioodiAlgusKuupaev property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPerioodiAlgusKuupaev() {
        return perioodiAlgusKuupaev;
    }

    /**
     * Sets the value of the perioodiAlgusKuupaev property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPerioodiAlgusKuupaev(XMLGregorianCalendar value) {
        this.perioodiAlgusKuupaev = value;
    }

    /**
     * Gets the value of the perioodiLoppKuupaev property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPerioodiLoppKuupaev() {
        return perioodiLoppKuupaev;
    }

    /**
     * Sets the value of the perioodiLoppKuupaev property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPerioodiLoppKuupaev(XMLGregorianCalendar value) {
        this.perioodiLoppKuupaev = value;
    }

    /**
     * Gets the value of the dokumendiLiik property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDokumendiLiik() {
        return dokumendiLiik;
    }

    /**
     * Sets the value of the dokumendiLiik property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDokumendiLiik(String value) {
        this.dokumendiLiik = value;
    }

    /**
     * Gets the value of the otsingusona property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtsingusona() {
        return otsingusona;
    }

    /**
     * Sets the value of the otsingusona property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtsingusona(String value) {
        this.otsingusona = value;
    }

}
