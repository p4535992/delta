
package ee.webmedia.alfresco.adr;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "koikDokumendidLisatudMuudetudResponse", propOrder = {
    "dokumentDetailidegaFailSisuga"
})
public class KoikDokumendidLisatudMuudetudResponse {

    protected List<DokumentDetailidega> dokumentDetailidegaFailSisuga;

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
    public List<DokumentDetailidega> getDokumentDetailidegaFailSisuga() {
        if (dokumentDetailidegaFailSisuga == null) {
            dokumentDetailidegaFailSisuga = new ArrayList<DokumentDetailidega>();
        }
        return this.dokumentDetailidegaFailSisuga;
    }

}
