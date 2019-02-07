package ee.smit.digisign.domain;


import org.alfresco.util.Base64;

public class AsicSContainer {

    private final String filename;
    private final String filedataBase64;

    public AsicSContainer(String filename, String filedataBase64){
        this.filename = filename;
        this.filedataBase64 = filedataBase64;
    }

    public String getFilename(){
        return filename;
    }

    public String getFiledataBase64(){
        return filedataBase64;
    }

    public byte[] getFiledate(){
        if(filedataBase64 != null && !filedataBase64.isEmpty()){
            return Base64.decode(filedataBase64);
        }
        return null;
    }
}
