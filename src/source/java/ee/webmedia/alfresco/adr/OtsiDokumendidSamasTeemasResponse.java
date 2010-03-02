
package ee.webmedia.alfresco.adr;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for otsiDokumendidSamasTeemasResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="otsiDokumendidSamasTeemasResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dokument" type="{http://alfresco/avalikdokumendiregister}dokument" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otsiDokumendidSamasTeemasResponse", propOrder = {
    "dokument"
})
public class OtsiDokumendidSamasTeemasResponse {

    protected List<Dokument> dokument;

    /**
     * Gets the value of the dokument property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dokument property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDokument().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Dokument }
     * 
     * 
     */
    public List<Dokument> getDokument() {
        if (dokument == null) {
            dokument = new ArrayList<Dokument>();
        }
        return this.dokument;
    }

}
