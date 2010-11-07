package ee.webmedia.mso;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replaceFormulas", propOrder = { "msoDocumentAndFormulasInput" })
public class ReplaceFormulas {

    @XmlElement(required = true)
    protected MsoDocumentAndFormulasInput msoDocumentAndFormulasInput;

    public MsoDocumentAndFormulasInput getMsoDocumentAndFormulasInput() {
        return msoDocumentAndFormulasInput;
    }

    public void setMsoDocumentAndFormulasInput(MsoDocumentAndFormulasInput msoDocumentAndFormulasInput) {
        this.msoDocumentAndFormulasInput = msoDocumentAndFormulasInput;
    }

}
