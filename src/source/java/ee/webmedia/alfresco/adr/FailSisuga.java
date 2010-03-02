
package ee.webmedia.alfresco.adr;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for failSisuga complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="failSisuga">
 *   &lt;complexContent>
 *     &lt;extension base="{http://alfresco/avalikdokumendiregister}fail">
 *       &lt;sequence>
 *         &lt;element name="sisu" type="{http://ws-i.org/profiles/basic/1.1/xsd}swaRef"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "failSisuga", propOrder = {
    "sisu"
})
public class FailSisuga
    extends Fail
{

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler sisu;

    /**
     * Gets the value of the sisu property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public DataHandler getSisu() {
        return sisu;
    }

    /**
     * Sets the value of the sisu property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSisu(DataHandler value) {
        this.sisu = value;
    }

}
