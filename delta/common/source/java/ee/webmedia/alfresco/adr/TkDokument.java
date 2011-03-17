
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for dokument complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dokument">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="viit" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="registreerimiseAeg" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="dokumendiLiik" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="saatja" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="saaja" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="pealkiri" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tkDokument", propOrder = {
    "viit",
    "registreerimiseAeg",
    "dokumendiLiik",
    "kellelt",
    "kellele"
})
@XmlSeeAlso({
    DokumentDetailidega.class
})
public class TkDokument {

    @XmlElement(required = true)
    protected String viit;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar registreerimiseAeg;
    @XmlElement(required = true)
    protected String dokumendiLiik;
    protected String kellelt;
    protected String kellele;

    /**
     * Gets the value of the viit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getViit() {
        return viit;
    }

    /**
     * Sets the value of the viit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setViit(String value) {
        this.viit = value;
    }

    /**
     * Gets the value of the registreerimiseAeg property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRegistreerimiseAeg() {
        return registreerimiseAeg;
    }

    /**
     * Sets the value of the registreerimiseAeg property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRegistreerimiseAeg(XMLGregorianCalendar value) {
        this.registreerimiseAeg = value;
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
     * Gets the value of the saatja property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKellelt() {
        return kellelt;
    }

    /**
     * Sets the value of the saatja property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKellelt(String value) {
        this.kellelt = value;
    }

    /**
     * Gets the value of the saaja property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKellele() {
        return kellele;
    }

    /**
     * Sets the value of the saaja property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKellele(String value) {
        this.kellele = value;
    }

}
