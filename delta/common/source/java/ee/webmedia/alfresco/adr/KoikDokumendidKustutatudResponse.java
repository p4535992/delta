
package ee.webmedia.alfresco.adr;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "koikDokumendidKustutatudResponse", propOrder = {
    "dokumentKustutatud"
})
public class KoikDokumendidKustutatudResponse {

    protected List<Dokument> dokumentKustutatud;

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
    public List<Dokument> getDokumentKustutatud() {
        if (dokumentKustutatud == null) {
            dokumentKustutatud = new ArrayList<Dokument>();
        }
        return this.dokumentKustutatud;
    }

}