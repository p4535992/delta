
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for failRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="failRequest">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="viit" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="registreerimiseAeg" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="failinimi" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otsiFailSisuga", propOrder = {
    "viit",
    "registreerimiseAeg",
    "failinimi"
})
public class OtsiFailSisuga {

    @XmlElement(required = true)
    protected String viit;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar registreerimiseAeg;
    @XmlElement(required = true)
    protected String failinimi;

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
     * Gets the value of the failinimi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFailinimi() {
        return failinimi;
    }

    /**
     * Sets the value of the failinimi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFailinimi(String value) {
        this.failinimi = value;
    }

}
