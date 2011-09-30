package ee.webmedia.mso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.activation.DataHandler;
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
    @WebResult(name = "msoPdfOutput", targetNamespace = "")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "", className = "ee.webmedia.mso.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "", className = "ee.webmedia.mso.ConvertToPdfResponse")
    public MsoPdfOutput convertToPdf(
            @WebParam(name = "msoDocumentInput", targetNamespace = "") MsoDocumentInput msoDocumentInput) {

        try {
            return msoService.convertToPdf(msoDocumentInput);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    @WebMethod
    @WebResult(name = "msoDocumentOutput", targetNamespace = "")
    @RequestWrapper(localName = "replaceFormulas", targetNamespace = "", className = "ee.webmedia.mso.ReplaceFormulas")
    @ResponseWrapper(localName = "replaceFormulasResponse", targetNamespace = "", className = "ee.webmedia.mso.ReplaceFormulasResponse")
    public MsoDocumentOutput replaceFormulas(
            @WebParam(name = "msoDocumentAndFormulasInput", targetNamespace = "") MsoDocumentAndFormulasInput msoDocumentAndFormulasInput) {

        try {
            return msoService.replaceFormulas(msoDocumentAndFormulasInput);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    @WebMethod
    @WebResult(name = "msoDocumentAndPdfOutput", targetNamespace = "")
    @RequestWrapper(localName = "replaceFormulasAndConvertToPdf", targetNamespace = "", className = "ee.webmedia.mso.ReplaceFormulasAndConvertToPdf")
    @ResponseWrapper(localName = "replaceFormulasAndConvertToPdfResponse", targetNamespace = "", className = "ee.webmedia.mso.ReplaceFormulasAndConvertToPdfResponse")
    public MsoDocumentAndPdfOutput replaceFormulasAndConvertToPdf(
            @WebParam(name = "msoDocumentAndFormulasInput", targetNamespace = "") MsoDocumentAndFormulasInput msoDocumentAndFormulasInput) {

        try {
            return msoService.replaceFormulasAndConvertToPdf(msoDocumentAndFormulasInput);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    @WebMethod
    @WebResult(name = "modifiedFormulasOutput", targetNamespace = "")
    @RequestWrapper(localName = "modifiedFormulas", targetNamespace = "", className = "ee.webmedia.mso.ModifiedFormulas")
    @ResponseWrapper(localName = "modifiedFormulasResponse", targetNamespace = "", className = "ee.webmedia.mso.ModifiedFormulasResponse")
    public ModifiedFormulasOutput getModifiedFormulas(@WebParam(name = "msoDocumentInput", targetNamespace = "") MsoDocumentInput msoDocumentInput) {
        try {
            return msoService.getModifiedFormulas(msoDocumentInput);
        } catch (Exception e) {
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
            MsoService bean = applicationContext.getBean(MsoService.class);
            int pass = 1;
            while (true) {
                log.info("Starting pass " + pass);
                for (File file : list) {
                    if (!file.isFile() || file.getName().endsWith(".pdf")) {
                        continue;
                    }

                    File outputFile = new File(folder, file.getName() + ".pdf");
                    MsoDocumentInput msoDocumentInput = new MsoDocumentInput();
                    msoDocumentInput.setDocumentFile(new DataHandler(new FileDataSource(file, "TODO")));
                    MsoPdfOutput msoPdfOutput;
                    try {
                        msoPdfOutput = bean.convertToPdf(msoDocumentInput);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        continue; // continue with next file
                    }
                    if (!outputFile.exists()) {
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
                        FileCopyUtils.copy(msoPdfOutput.getPdfFile().getInputStream(), out);
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
