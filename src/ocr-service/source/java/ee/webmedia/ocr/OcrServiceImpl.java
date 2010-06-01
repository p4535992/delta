package ee.webmedia.ocr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.activation.DataHandler;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.itextpdf.text.pdf.PdfReader;

public class OcrServiceImpl implements OcrService, InitializingBean {
    private static Logger log = Logger.getLogger(OcrServiceImpl.class);

    // TODO if hot folder stops, send email to admin
    // Future TODO csv stats (parse other fields from log string) (log pages, recog time, err/warn, uncert chars to our csv file)
    // Doubtful TODO move invalid input files away for later parsing (if errors then save log and input output files to error dir for later inspection)

    public static final String MIMETYPE_PDF = "application/pdf";
    public static final long PDF_MINIMUM_SIZE = 100;
    
    private static final String LOG_BIG_SEPARATOR = "==============================";
    private static final String LOG_SMALL_SEPARATOR = "------------------------------";

    private String inputFolder;
    private String outputFolder;
    private String taskName;

    private File logFile;

    @Override
    public void afterPropertiesSet() throws Exception {
        logFile = new File(outputFolder + "/" + taskName + " logs.txt");
    }

    // Only one request can be processed simultaneously
    public synchronized OcrOutput convertToPdf(OcrInput ocrInput) throws IOException {
        log.debug("Start request");
        cleanInputFolder();
        cleanOutputFolder(null);
        String filename = saveFile(ocrInput);
        waitForOcrComplete();
        String ocrLog = parseLog();
        OcrOutput output = buildOutput(filename, ocrLog);
        cleanInputFolder();
        cleanOutputFolder(filename);
        log.debug("End request: " + filename + ".pdf");
        return output;
    }

    private String saveFile(OcrInput ocrInput) throws IOException {
        DataHandler content = ocrInput.getContent();
        if (content == null) {
            throw new RuntimeException("Invalid request, content is missing");
        }
        if (!MIMETYPE_PDF.equals(content.getContentType())) {
            throw new RuntimeException("Input content-type \"" + content.getContentType() + "\" is not supportted. Only content-type \"" + MIMETYPE_PDF + "\" is supported");
        }
        validatePdf(content.getInputStream());
        String filename = Long.toString(System.currentTimeMillis());
        // Save as 12345678.pdf.tmp and rename later, which is an atomic operation
        File inputTmpFile = new File(inputFolder + "/" + filename + ".pdf.tmp");
        if (inputTmpFile.exists()) {
            throw new RuntimeException("Input file already exists locally: " + inputTmpFile);
        }
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(inputTmpFile));
        FileCopyUtils.copy(content.getInputStream(), out);
        if (inputTmpFile.length() < PDF_MINIMUM_SIZE) {
            throw new RuntimeException("Input file size is too small: " + inputTmpFile.length() + " bytes");
        }

        // Rename to pdf, which is an atomic operation
        File inputFile = new File(inputFolder + "/" + filename + ".pdf");
        if (inputFile.exists()) {
            throw new RuntimeException("Input file already exists locally: " + inputTmpFile);
        }
        inputTmpFile.renameTo(inputFile);
        return filename;
    }

    private void validatePdf(InputStream inputStream) throws IOException {
        PdfReader pdfReader = new PdfReader(inputStream);
        pdfReader.close();
    }

    private void waitForOcrComplete() {
        // C:\Users\alar\AppData\Local\ABBYY\HotFolder\10.00\ocr logs.txt - written constantly
        // D:\Workspace\ocr-output\ocr logs.txt - copied from previous, only at the end
        // XXX if I poll the second file length too often (without pause), then I don't see file length change, but in reality it has
        if (!logFile.exists()) {
            throw new RuntimeException("Log file does not exist: " + logFile);
        }

        long logLength = logFile.length();
        log.debug("Initial log file size " + logLength + " bytes");
        while (true) {
            long newLength = logFile.length();
            if (newLength != logLength) {
                log.debug((newLength > logLength ? "Grown" : "Shrunk") + " log file size " + newLength + " bytes");
                logLength = newLength;
                break;
            }
            try {
                // TODO can it happen that HotFolder jõuab kirjutada veel ühe tühja scannimise logi otsa, selle vahepeal kui päris scan lõppend ja enne kui me logi jõuame lugema?
                // siis peaks suurendama tihedust, nt 100 ms peale või 50 ms, mitte väiksemaks...
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return;
    }

    private List<String> readLog() throws IOException {
        int i = 0;
        while (true) {
            try {
                return IOUtils.readLines(new FileInputStream(logFile), "UTF-16");
            } catch (FileNotFoundException e) {
                if (i >= 5) {
                    throw e;
                }
                log.debug("Log file probably in use, retrying");
                i++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String parseLog() throws IOException {
        List<String> lines = readLog();
        String lastLine = lines.get(lines.size() - 1);
        if (!LOG_BIG_SEPARATOR.equals(lastLine)) {
            throw new RuntimeException("Unexpected last line while parsing log file: " + lastLine);
        }
        int smallSeparatorCount = 0;
        int i = lines.size() - 2;
        for (; i >= 0 && smallSeparatorCount < 2; i--) {
            if (LOG_SMALL_SEPARATOR.equals(lines.get(i))) {
                smallSeparatorCount++;
            }
        }
        if (smallSeparatorCount != 2) {
            throw new RuntimeException("Separator line not found while parsing log file");
        }
        i += 2;
        StringBuilder s = new StringBuilder();
        for (; i < lines.size() - 1; i++) {
            if (s.length() > 0) {
                s.append('\n');
            }
            s.append(lines.get(i));
        }
        if (s.indexOf("No export errors occurred.") == -1) {
            throw new RuntimeException("Success message not found while parsing log file:\n" + s.toString());
        }
        return s.toString();
    }

    private OcrOutput buildOutput(String filename, String ocrLog) {
        File outputFile = new File(outputFolder + "/" + filename + ".pdf");
        if (!outputFile.exists()) {
            throw new RuntimeException("Output file does not exist: " + outputFile);
        }
        if (outputFile.length() < PDF_MINIMUM_SIZE) {
            throw new RuntimeException("Output file size is too small: " + outputFile.length() + " bytes");
        }
        OcrOutput ocrOutput = new OcrOutput();
        ocrOutput.setContent(new DataHandler(new FileDataSource(outputFile, MIMETYPE_PDF)));
        ocrOutput.setLog(ocrLog);
        return ocrOutput;
    }

    private void cleanInputFolder() {
        for (File file : new File(inputFolder).listFiles()) {
            boolean result = file.delete();
            log.debug((result ? "Successfully" : "Unsuccessfully") + " deleted " + file);
        }
    }

    private void cleanOutputFolder(final String excludeFilename) {
        File[] files = new File(outputFolder).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return StringUtils.endsWithIgnoreCase(name, ".pdf") && (excludeFilename == null || !name.equals(excludeFilename + ".pdf"));
            }
        });
        for (File file : files) {
            // Delete only when modified longer than 30 minutes ago
            if (file.lastModified() < System.currentTimeMillis() - 1800000) {
                boolean result = file.delete();
                log.debug((result ? "Successfully" : "Unsuccessfully") + " deleted " + file);
            } else {
                log.debug("Didn't delete " + file + " - modified only " + ((System.currentTimeMillis() - file.lastModified()) / 1000) + " seconds ago");
            }
        }
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

}
