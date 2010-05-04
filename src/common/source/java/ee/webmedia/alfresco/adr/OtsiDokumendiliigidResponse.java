package ee.webmedia.alfresco.adr;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otsiDokumendiliigidResponse", propOrder = { "dokumendiliik" })
public class OtsiDokumendiliigidResponse {

    protected List<Dokumendiliik> dokumendiliik;

    public List<Dokumendiliik> getDokument() {
        if (dokumendiliik == null) {
            dokumendiliik = new ArrayList<Dokumendiliik>();
        }
        return this.dokumendiliik;
    }

}
