
package ee.webmedia.alfresco.adr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for failSisugaResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="failSisugaResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="failSisuga" type="{http://alfresco/avalikdokumendiregister}failSisuga" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otsiFailSisugaResponse", propOrder = {
    "fail"
})
public class OtsiFailSisugaResponse {

    protected Fail fail;

    /**
     * Gets the value of the failSisuga property.
     * 
     * @return
     *     possible object is
     *     {@link FailSisuga }
     *     
     */
    public Fail getFail() {
        return fail;
    }

    /**
     * Sets the value of the failSisuga property.
     * 
     * @param value
     *     allowed object is
     *     {@link FailSisuga }
     *     
     */
    public void setFail(Fail fail) {
        this.fail = fail;
    }

}
