
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "koikDokumendidKustutatud", propOrder = {
    "perioodiAlgusKuupaev",
    "perioodiLoppKuupaev"
})
public class KoikDokumendidKustutatud {

    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar perioodiAlgusKuupaev;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar perioodiLoppKuupaev;

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

}
