package ee.webmedia.mso;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msoInput", propOrder = { "content" })
public class MsoInput {

    @XmlElement(required = true, type = String.class)
    @XmlAttachmentRef
    protected DataHandler content;

    public DataHandler getContent() {
        return content;
    }

    public void setContent(DataHandler content) {
        this.content = content;
    }

}
