package ee.webmedia.mso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.csvreader.CsvWriter;
import com.itextpdf.text.pdf.PdfReader;

public class MsoServiceImpl implements MsoService, InitializingBean {
    private static Logger log = Logger.getLogger(MsoServiceImpl.class);

    // XXX currently, don't convert TXT files with this service, because we haven't implemented passing encoding information to Word (it will guess or use system default)
    // XXX input mimetype (or file extension) doesn't matter - caller must call this service only with word files
    //   - EXE file renders as text
    //   - XLS file exits with error
    //   - PDF file (10 MB) processes for 1 minute and winword takes 100 MB RAM, had to kill it

    // TODO kui new FileInputStream(successFile) peale tuleb FileNotFoundException, siis proovida uuesti mõnda aega
    // TODO mingi kavalusega detecida kui word avab dialoogi ja siis klikkida sellele

    // TODO uus koormustest
    //      * et näha kas 128 MB-ga java protsess töötab järjest lõpuni
    //      teha ainult edukad failid
    //      * et näha kas wordi mälukasutus kasvab või püsib, kui suureks jääb
    //      * et näha kas ajapikku läheb keskmine aeg suuremaks - kas word aeglasemaks
    //      + otsustada, kas wordile peab panema shutdown counteri

    /*
     * Test #1
     * Kaustas kokku 2258 faili (rtf/docm/docx/dot/doc)
     * PDF-id tehti 2251 failist; kõik PDF'id olid loetavad (valideeritakse iText teegiga)
     * Kokku läbiti 2258 faili 16 korda = 35000 teisendamist
     * Java protsess (mso-service.exe) töötas selle aja järjest, ilma kokku jooksmata; oli Xmx=256m [aga praegu muutsime 128m ja sellega pole järjest testinud]
     * Word tegi järjest maksimaalselt 1434 teisendust, vea korral suleti või tapeti; kõik jätkamised olid edukad
     * Alati olid samad 7 faili mille teisendamine ebaõnnestus (1 väljus ise veaga, 6 puhul timeout ja word tapeti)
     * 4 korda (35000-st) oli selline asi:
     *    java.io.FileNotFoundException: C:\Programs\mso-input\2010-09-24-00-38-38-777.success (The process cannot access the file because it is being used by another process)
     *    java.io.FileNotFoundException: C:\Programs\mso-input\2010-09-24-02-19-09-943.success (The process cannot access the file because it is being used by another process)
     *    java.io.FileNotFoundException: C:\Programs\mso-input\2010-09-24-05-21-14-972.success (The process cannot access the file because it is being used by another process)
     *    java.io.FileNotFoundException: C:\Programs\mso-input\2010-09-24-06-04-28-799.success (The process cannot access the file because it is being used by another process)
     *    tuli selle rea peale: new FileInputStream(successFile);
     */

    public static final String MIMETYPE_PDF = "application/pdf";
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

    private File workFolder;
    private long timeout;
    
    private String classExecPath;
    private String macroDocumentPath;
    private File csvFile;

    private String filename;
    private long conversionDuration;
    private long inputFileSize;
    private long outputFileSize;
    private String inputMimeType;
    private String wordSaveFormat;
    private String wordSaveEncoding;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Service started with following configuration:\n    workFolder=" + workFolder.getAbsolutePath() + "\n    timeout = " + timeout + " ms");
        if (!workFolder.exists()) {
            throw new Exception("WorkFolder does not exist!");
        }
        if (!workFolder.isDirectory()) {
            throw new Exception("WorkFolder is not a directory!");
        }

        File serviceFolder = new File(".");
        classExecPath = new File(serviceFolder, "classExec.exe").getAbsolutePath();
        macroDocumentPath = new File(serviceFolder, "mso-service.docm").getAbsolutePath();
        csvFile = new File(serviceFolder, "mso-service.csv");
        log.info("Assuming the following external locations:\n    classExec=" + classExecPath + "\n    macroDocument=" + macroDocumentPath + "\n    csvFile="
                + csvFile.getAbsolutePath());
    }

    // Only one request can be processed simultaneously
    public synchronized MsoOutput convertToPdf(MsoInput msoInput) throws Exception {
        filename = dateFormat.format(new Date()); // TODO add loop if exists?
        conversionDuration = -1;
        inputFileSize = -1;
        outputFileSize = -1;
        inputMimeType = "";
        wordSaveFormat = "";
        wordSaveEncoding = "";
        Exception exception = null;
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start request: " + filename);
            }
            cleanWorkFolder();
            saveFile(msoInput);
            startConvertAndWaitToComplete();
            MsoOutput output = buildOutput();
            if (log.isDebugEnabled()) {
                log.debug("End request: " + filename);
            }
            return output;

        } catch (Exception e) {
            exception = e;
            throw new Exception("Error processing request " + filename + " : " + e.getMessage(), e);
        } finally {
            if (!csvFile.exists()) {
                OutputStream outputStream = new FileOutputStream(csvFile);
                try {
                    // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                    outputStream.write("\ufeff".getBytes("UTF-8"));
                } finally {
                    outputStream.close();
                }
            }
            CsvWriter writer = new CsvWriter(new FileOutputStream(csvFile, true), ';', Charset.forName("UTF-8"));
            /* CSV columns:
             * 1) request ID
             * 2) status = success / error
             * 3) time to process total
             * 4) time to wait for convert
             * 5) input file size
             * 6) output file size
             * 7) input mime type
             * 8) word SaveFormat 6=RTF, 2=TXT, 12=DOCX, 0=DOC
             * 9) word SaveEncoding 1200, 1257 for TXT
             * 10) exception message
             */
            try {
                writer.writeRecord(new String[] {
                        filename,
                        exception == null ? "SUCCESS" : "ERROR",
                        Long.toString(System.currentTimeMillis() - startTime),
                        Long.toString(conversionDuration),
                        Long.toString(inputFileSize),
                        Long.toString(outputFileSize),
                        inputMimeType,
                        wordSaveFormat,
                        wordSaveEncoding,
                        exception == null ? "" : exception.getMessage()
                });
            } finally {
                writer.close();
            }
        }
    }

    private String saveFile(MsoInput msoInput) throws IOException {
        if (msoInput == null) {
            throw new RuntimeException("Invalid request, input is missing");
        }
        DataHandler content = msoInput.getContent();
        if (content == null) {
            throw new RuntimeException("Invalid request, content is missing");
        }
        inputMimeType = content.getContentType();

        // XXX We don't do input content-type validating
        // Word can detect file format based on file contents
        // And if it is an unsupported or invalid file, then Word should (in theory)
        // * render as text
        // * exit with error
        // * process very long, that we have to kill it

        File inputFile = new File(workFolder, filename);
        if (inputFile.exists()) {
            throw new RuntimeException("Input file already exists locally: " + inputFile);
        }
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(inputFile));
        FileCopyUtils.copy(content.getInputStream(), out);
        inputFileSize = inputFile.length();
        return filename;
    }

    private void validatePdf(File file) {
        try {
            PdfReader pdfReader = new PdfReader(new BufferedInputStream(new FileInputStream(file)));
            pdfReader.close();
        } catch (Exception e) {
            throw new RuntimeException("Error validating result PDF file " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    private void startConvertAndWaitToComplete() throws Exception {
        File successFile = new File(workFolder, filename + ".success");
        File errorFile = new File(workFolder, filename + ".error");
        if (successFile.exists()) {
            throw new RuntimeException("Success file already exists: " + successFile);
        }
        if (errorFile.exists()) {
            throw new RuntimeException("Error file already exists: " + successFile);
        }

        File inputFile = new File(workFolder, filename);
        String inputFilePath = inputFile.getAbsolutePath();
        Assert.doesNotContain(inputFilePath, "\"");
        String[] args = new String[] {
                classExecPath,
                macroDocumentPath,
                "--action",
                "open",
                "--command",
                "[MsoService.ConvertToPdf \\\"" + inputFilePath + "\\\"]"
        };
        if (log.isDebugEnabled()) {
            log.debug("Executing " + StringUtils.arrayToDelimitedString(args, " "));
        }
        long startTime = System.currentTimeMillis();
        Process process = Runtime.getRuntime().exec(args);
        int exitValue;
        while (true) {
            try {
                exitValue = process.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                // Ignore, process is still running
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                killWinword();
                process.destroy();
                throw new RuntimeException("Timeout while waiting process");
            }
            if (log.isDebugEnabled()) {
                log.debug("Process has not exited, sleeping 100 ms");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (exitValue != 0) {
            throw new RuntimeException("Process exit value is " + exitValue);
        }

        if (log.isDebugEnabled()) {
            log.debug("Waiting for success or error file");
        }
        while (!successFile.exists() && !errorFile.exists()) {
            // If Word displays a GUI dialog (for example if html file has missing resources), then we can't do anything about it on the macro side
            // Then it stalls forever, and the only option is to kill Word process
            if (System.currentTimeMillis() - startTime > timeout) {
                killWinword();
                throw new RuntimeException("Timeout while waiting success or error file");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        conversionDuration = System.currentTimeMillis() - startTime;
        
        if (errorFile.exists()) {
            FileInputStream in = new FileInputStream(errorFile);
            List<?> lines = IOUtils.readLines(in);
            in.close();
            String message = StringUtils.collectionToDelimitedString(lines, "\n");
            throw new Exception("Macro returned error: " + message);
        }

        FileInputStream in = new FileInputStream(successFile);
        @SuppressWarnings("unchecked")
        List<String> lines = IOUtils.readLines(in);
        in.close();
        if (lines.size() >= 1) {
            wordSaveFormat = lines.get(0);
        }
        if (lines.size() >= 2) {
            wordSaveEncoding = lines.get(1);
        }

        return;
    }

    private void killWinword() throws Exception {
        String[] args = new String[] {
                "taskkill",
                "/im",
                "winword.exe",
                "/t",
                "/f"
        };
        Process process = Runtime.getRuntime().exec(args);
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            log.warn("taskkill exitValue=" + exitValue); // probably means that winword.exe process didn't exist
        }
    }

    private MsoOutput buildOutput() throws IOException {
        File outputFile = new File(workFolder, filename + ".pdf");
        if (!outputFile.exists()) {
            throw new RuntimeException("Output file does not exist: " + outputFile);
        }
        outputFileSize = outputFile.length();
        validatePdf(outputFile);
        MsoOutput output = new MsoOutput();
        output.setContent(new DataHandler(new FileDataSource(outputFile, MIMETYPE_PDF)));
        return output;
    }

    private void cleanWorkFolder() {
        String[] successFileNames = workFolder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".success");
            }
        });
        long now = System.currentTimeMillis();
        for (String successFileName : successFileNames) {
            File successFile = new File(workFolder, successFileName);
            if (now - successFile.lastModified() > 60000) { // files older than 1 min
                String inputFileName = StringUtils.replace(successFileName, ".success", "");
                File inputFile = new File(workFolder, inputFileName);
                File outputFile = new File(workFolder, inputFileName + ".pdf");
                // Try to delete pdf first; if it is locked by java then we delete this group later
                if (outputFile.exists()) {
                    if (!deleteFile(outputFile)) {
                        continue;
                    }
                }
                if (inputFile.exists()) {
                    if (!deleteFile(inputFile)) {
                        continue;
                    }
                }
                if (successFile.exists()) {
                    deleteFile(successFile);
                }
            }
        }
    }

    private boolean deleteFile(File file) {
        boolean result = file.delete();
        if (log.isDebugEnabled()) {
            log.debug((result ? "Successfully" : "Unsuccessfully") + " deleted " + file);
        }
        return result;
    }

    public void setWorkFolder(String workFolder) {
        this.workFolder = new File(workFolder);
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout * 1000;
    }

}
