package ee.webmedia.mso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.FileCopyUtils;

@WebService(name = "Mso", targetNamespace = "http://webmedia.ee/mso", serviceName = "MsoService")
public class MsoEndpoint implements Mso {
    private static Logger log = Logger.getLogger(MsoServiceImpl.class);

    private MsoService msoService;

    @Override
    @WebMethod
    @WebResult(name = "msoOutput", targetNamespace = "")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "", className = "ee.webmedia.mso.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "", className = "ee.webmedia.mso.ConvertToPdfResponse")
    public MsoOutput convertToPdf(
            @WebParam(name = "msoInput", targetNamespace = "") MsoInput msoInput) {

        try {
            return msoService.convertToPdf(msoInput);
        } catch (Exception e) {
            log.debug("Error while processing request", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ee/webmedia/mso/context.xml");

        if (args.length == 0) {
            return;
        }

        try {
            File folder = new File(args[0]);
            log.info("Batch-converting all files to PDF in folder: " + folder.getAbsolutePath());
            File[] list = folder.listFiles();
            MsoService bean = (MsoService) applicationContext.getBean(MsoService.class);
            int pass = 1;
            while (true) {
                log.info("Starting pass " + pass);
                for (File file : list) {
                    if (!file.isFile() || file.getName().endsWith(".pdf")) {
                        continue;
                    }

                    File outputFile = new File(folder, file.getName() + ".pdf");
                    MsoInput msoInput = new MsoInput();
                    msoInput.setContent(new DataHandler(new FileDataSource(file)));
                    MsoOutput msoOutput;
                    try {
                        msoOutput = bean.convertToPdf(msoInput);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        continue; // continue with next file
                    }
                    if (!outputFile.exists()) {
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
                        FileCopyUtils.copy(msoOutput.getContent().getInputStream(), out);
                    }
                }
                pass++;
            }
            // log.info("Completed!");
        } catch (Exception e) {
            log.fatal(e.getMessage(), e);
            throw e;
        } finally {
            System.exit(0);
        }
    }

}
