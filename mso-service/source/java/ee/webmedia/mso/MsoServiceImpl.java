package ee.webmedia.mso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.csvreader.CsvWriter;

public class MsoServiceImpl implements MsoService, InitializingBean {
    private static Logger log = Logger.getLogger(MsoServiceImpl.class);

    // * võetud ära output pdf valideerimine - pole vaja; ning kasutas üle 100 mb mälu kui oli 200+ leheküljeline pdf (kuigi ainult 9 MB)
    // * disabled word dialogs from macro
    //   + doesn't display dialog "this document contains links that may refer to other files. do you want to update them manually?"
    //   + doesn't display dialog about missing resources when opening html files
    //   ? supposedly doesn't display "install missing features" dialog

    // if opening wrong files with word, for example:
    //   - EXE file renders as text
    //   - XLS file exits with error
    //   - PDF file (10 MB) processes for 1 minute and winword takes 100 MB RAM, had to kill it

    // fail avalik_arvamus_2008_2_.docx (3,26 MB) -> PDF (9,56 MB), võttis DELTA poolel 47 sek, Mso Service poolel 44 sek, winword.exe kasutas ära 145-149 MB mälu

    // TODO kaks eraldi timeout'i: 1) kui fail edukalt avatakse (nt. 10 või 15 sek) 2) kui pdf konvert lõpeb (nt. 90 või 120 sek)
    // TODO kui new FileInputStream(successFile) peale tuleb FileNotFoundException, siis proovida uuesti mõnda aega
    // TODO implementeerida replaceFormulasAndConvertToPdf

    // TODO uus koormustest
    //      * et näha kas 128 MB-ga java protsess töötab järjest lõpuni - ... sisend+väljundfaili suurus äkki mõjutab mälukasutust?
    //      teha ainult edukad failid
    //      * et näha kas wordi mälukasutus kasvab või püsib, kui suureks jääb
    //      * et näha kas ajapikku läheb keskmine aeg suuremaks - kas word aeglasemaks
    //      + otsustada, kas wordile peab panema shutdown counteri
    // TODO ^^ testi puhu jälgia mälukasutust; paigaldusejuhendisse winword.exe vajab vähemalt 150 MB RAM
    // TODO testida et utf-8'na avamine ei muudaks docx/doc/rtf faile valeks mingitel juhtudel - seda näeb testi csv'st ja konkreetseid PDFe uurides
    // XXX testida kas mso sisse/välja lülitamisel DELTAs PDFiks tegemine töötab - tundub töötavat, aga OO puhul mõned encodingud on vist valed??
    // XXX testitud imapist lohistamisel, et contenttype on parsitav
    // XXX input-word.txt faili kirjutame system encodingus ja word loeb teda ka system encodingus, siiani tundub et täpitähed säilivad

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

    /*
     * TXT:
     * Converted input file encoding iso-8859-15 (1629 bytes) -> UTF-8 (1655 bytes) - OK
     * Converted input file encoding iso-8859-1 (160 bytes) -> UTF-8 (165 bytes) - OK
     * Converted input file encoding windows-1257 (1019 bytes) -> UTF-8 (1037 bytes) - OK
     * UTF-8 OK
     * 
     * HTML:
     * Converted input file encoding windows-1257 (2211 bytes) -> UTF-8 (2229 bytes) - OK, jäi sisse
     *      <meta http-equiv=Content-Type pdfFile="text/html; charset=windows-1257">
     * Converted input file encoding iso-8859-1 (3081 bytes) -> UTF-8 (3093 bytes), - OK, jäi sisse
     *      <meta http-equiv=Content-Type pdfFile="text/html; charset=iso-8859-1">
     * Converted input file encoding iso-8859-15 (2727 bytes) -> UTF-8 (2773 bytes) - OK, ei jäänud midagi sisse
     * Processed input file, encoding UTF-8 (53 bytes) -> UTF-8 (66 bytes)
     */

    // ==== NOTES ====

    // NB! for some HTML input files, Word uses "User name" value (from MS Office General Options) in PDF "Author" field
    //     (it appear this is for such HTML files which do not use
    // Otherwise, PDF "Author" field is left empty for all TXT and most of HTML input files, which is our wanted behaviour
    // Resolution: added instructions to installation guide to set User Name of MS Office to DELTA

    // Weird failure: When doing ConvertToPdf 1) word 2) excel 3) word, then 3rd Word stalls somewhere...; Only if before 1st, Word and Excel are both closed
    //      And other combinations don't produce this.
    // Resolution: added to Excel macro that Excel quits application after every conversion


    public static final String MIMETYPE_PDF = "application/pdf";
    public static final String MIMETYPE_TEXT = "text/plain";
    public static final String MIMETYPE_HTML = "text/html";
    public static final String MIMETYPE_DOC_DOT = "application/msword";
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    public static final String MACRO_CONVERT_TO_PDF = "ConvertToPdf";
    public static final String MACRO_REPLACE_FORMULAS = "ReplaceFormulas";
    public static final String MACRO_REPLACE_FORMULAS_AND_CONVERT_TO_PDF = "ReplaceFormulasAndConvertToPdf";
    public static final String MACRO_MODIFIED_FORMULAS = "ModifiedFormulas";

    private static class MsoProgram {
        private String programName;
        private String macroDocumentPath;
        private String imageName;
        private File macroInputFile;
        private String macroCommandTemplate;

        public MsoProgram(String programName, String macroDocumentPath, String imageName, File macroInputFolderPath, String macroCommandTemplate) {
            this.programName = programName;
            this.macroDocumentPath = macroDocumentPath;
            this.imageName = imageName;
            this.macroInputFile = new File(macroInputFolderPath, "input-" + programName + ".txt");
            this.macroCommandTemplate = macroCommandTemplate;
        }

        public String getProgramName() {
            return programName;
        }

        public String getMacroDocumentPath() {
            return macroDocumentPath;
        }

        public String getImageName() {
            return imageName;
        }

        public File getMacroInputFile() {
            return macroInputFile;
        }

        public String getMacroCommandTemplate() {
            return macroCommandTemplate;
        }

        @Override
        public String toString() {
            return "\n    programName=" + getProgramName() + "\n    imageName=" + getImageName() + "\n    macroDocumentPath=" + getMacroDocumentPath()
                    + "\n    macroInputFile=" + getMacroInputFile().getAbsolutePath() + "\n    macroCommandTemplate=" + getMacroCommandTemplate();
        }

    }

    // Set from Spring config
    private File workFolder;
    private long timeout;

    // Initialized in afterPropertiesSet
    private String classExecPath;
    private File csvFile;
    private Map<String, MsoProgram> programsByMimeType;

    // Reset at the beginning of every service call
    private String filename;
    private String programName;
    private String macroName;
    private long conversionDuration;
    private long inputFileSize;
    private long outputFileSize;
    private String inputContentType;
    private String inputMimeType;
    private String inputEncoding;
    private boolean writeHtmlTags;
    private String inputSaveFormat;
    private String inputSaveEncoding;
    private String outputSaveFormat;
    private String outputSaveEncoding;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Service started with following configuration:"
                + "\n    workFolder=" + workFolder.getAbsolutePath()
                + "\n    timeout=" + timeout + "ms");
        if (!workFolder.exists()) {
            throw new Exception("WorkFolder does not exist!");
        }
        if (!workFolder.isDirectory()) {
            throw new Exception("WorkFolder is not a directory!");
        }

        File serviceFolder = new File(".");
        classExecPath = new File(serviceFolder, "classExec.exe").getAbsolutePath();
        csvFile = new File(serviceFolder, "mso-service.csv");

        MsoProgram word = new MsoProgram("word", new File(serviceFolder, "mso-service.docm").getAbsolutePath(), "winword.exe", serviceFolder, "[%s]");
        MsoProgram excel = new MsoProgram("excel", new File(serviceFolder, "mso-service.xlsm").getAbsolutePath(), "excel.exe", serviceFolder, "[Run(\\\"%s\\\")]");

        programsByMimeType = new HashMap<String, MsoProgram>();
        // Keep mimeTypes as lowercase!
        programsByMimeType.put(MIMETYPE_DOC_DOT, word); // DOC
        programsByMimeType.put("application/rtf", word); // RTF
        programsByMimeType.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", word); // DOCX

        programsByMimeType.put(MIMETYPE_TEXT, word); // TXT
        programsByMimeType.put(MIMETYPE_HTML, word); // HTML

        programsByMimeType.put("application/vnd.excel", excel); // XLS
        programsByMimeType.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel); // XLSX

        log.info("Assuming the following external locations:"
                + "\n    classExec=" + classExecPath
                + "\n    csvFile=" + csvFile.getAbsolutePath());

        StringBuilder s = new StringBuilder("Supported input formats:");
        for (Entry<String, MsoProgram> entry : programsByMimeType.entrySet()) {
            s.append("\n    inputMimeType=").append(entry.getKey());
            s.append(StringUtils.replace(entry.getValue().toString(), "\n", "\n    "));
        }
        log.info(s.toString());
    }

    @Override
    public synchronized ModifiedFormulasOutput getModifiedFormulas(MsoDocumentInput msoDocumentInput) throws Exception {
        reset();
        Exception exception = null;
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start request: " + filename);
            }
            cleanWorkFolder();
            MsoProgram program = saveFile(msoDocumentInput);
            programName = program.getProgramName();
            macroName = MACRO_MODIFIED_FORMULAS;

            startConvertAndWaitToComplete(program, null);

            @SuppressWarnings("unchecked")
            List<String> lines = FileUtils.readLines(buildOutput());
            Set<Formula> formulas = new HashSet<Formula>(lines.size());
            for (String line : lines) {
                String[] split = line.split("=");
                Formula f = new Formula();
                f.setKey(split[0]);
                f.setValue(split[1]);
                formulas.add(f);
            }

            ModifiedFormulasOutput output = new ModifiedFormulasOutput();
            output.setModifiedFormulas(formulas);
            if (log.isDebugEnabled()) {
                log.debug("End request: " + filename);
            }
            return output;

        } catch (Exception e) {
            exception = e;
            throw new Exception("Error processing request " + filename + " : " + e.getMessage(), e);
        } finally {
            writeStatistics(exception, startTime);
        }
    }

    // Only one request can be processed simultaneously
    @Override
    public synchronized MsoPdfOutput convertToPdf(MsoDocumentInput msoDocumentInput) throws Exception {
        reset();
        Exception exception = null;
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start request: " + filename);
            }
            cleanWorkFolder();
            MsoProgram program = saveFile(msoDocumentInput);
            programName = program.getProgramName();
            macroName = MACRO_CONVERT_TO_PDF;

            startConvertAndWaitToComplete(program, null);
            File outputFile = buildOutput();

            MsoPdfOutput output = new MsoPdfOutput();
            output.setPdfFile(new DataHandler(new FileDataSource(outputFile, MIMETYPE_PDF)));
            if (log.isDebugEnabled()) {
                log.debug("End request: " + filename);
            }
            return output;

        } catch (Exception e) {
            exception = e;
            throw new Exception("Error processing request " + filename + " : " + e.getMessage(), e);
        } finally {
            writeStatistics(exception, startTime);
        }
    }

    @Override
    public synchronized MsoDocumentOutput replaceFormulas(MsoDocumentAndFormulasInput input) throws Exception {
        reset();
        Exception exception = null;
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start request: " + filename);
            }
            cleanWorkFolder();
            MsoProgram program = saveFile(input);
            programName = program.getProgramName();
            macroName = MACRO_REPLACE_FORMULAS;

            if (!"word".equals(programName)) {
                throw new RuntimeException("Replacing formulas is only supported with Word, mimeType must be '" + MIMETYPE_DOC_DOT + "', this input mimeType="
                        + inputMimeType + " programName=" + programName);
            }

            String formulaString = prepareFormulas(input.getFormula());
            startConvertAndWaitToComplete(program, formulaString);
            File outputFile = buildOutput();

            MsoDocumentOutput output = new MsoDocumentOutput();
            output.setDocumentFile(new DataHandler(new FileDataSource(outputFile, MIMETYPE_DOC_DOT)));
            if (log.isDebugEnabled()) {
                log.debug("End request: " + filename);
            }
            return output;

        } catch (Exception e) {
            exception = e;
            throw new Exception("Error processing request " + filename + " : " + e.getMessage(), e);
        } finally {
            writeStatistics(exception, startTime);
        }
    }

    @Override
    public synchronized MsoDocumentAndPdfOutput replaceFormulasAndConvertToPdf(MsoDocumentAndFormulasInput input) throws Exception {
        reset();
        Exception exception = null;
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start request: " + filename);
            }
            cleanWorkFolder();
            MsoProgram program = saveFile(input);
            programName = program.getProgramName();
            macroName = MACRO_REPLACE_FORMULAS_AND_CONVERT_TO_PDF;

            if (!"word".equals(programName)) {
                throw new RuntimeException("Replacing formulas is only supported with Word, mimeType must be '" + MIMETYPE_DOC_DOT + "', this input mimeType="
                        + inputMimeType + " programName=" + programName);
            }

            prepareFormulas(input.getFormula());

            throw new RuntimeException("ReplaceFormulasAndConvertToPdf is not supported");

        } catch (Exception e) {
            exception = e;
            throw new Exception("Error processing request " + filename + " : " + e.getMessage(), e);
        } finally {
            writeStatistics(exception, startTime);
        }
    }

    private String prepareFormulas(List<Formula> formulaList) {
        // Prepare formulas for Word macro
        String special = new String(new char[] {'\u001f'}); // ASCII 31
        String lineBreak = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append(formulaList.size());
        for (Formula formula : formulaList) {
            String formulaKey = formula.getKey();
            Assert.doesNotContain(formulaKey, "=", "Formula key must not contain '=' character: " + formula.getKey());
            String formulaValue = StringUtils.replace(formula.getValue(), "\r\n", special);
            formulaValue = StringUtils.replace(formulaValue, "\n", special);
            formulaValue = StringUtils.replace(formulaValue, "\r", special);
            s.append(lineBreak).append(formulaKey).append("=").append(formulaValue);
        }
        return s.toString();
    }

    private void reset() {
        filename = dateFormat.format(new Date()); // XXX This generated filename cannot previously exist (only if computer clock is modified)
        programName = "";
        macroName = "";
        conversionDuration = -1;
        inputFileSize = -1;
        outputFileSize = -1;
        inputContentType = "";
        inputMimeType = "";
        inputEncoding = "";
        writeHtmlTags = false;
        inputSaveFormat = "";
        inputSaveEncoding = "";
        outputSaveFormat = "";
        outputSaveEncoding = "";
    }

    private void writeStatistics(Exception exception, long startTime) throws Exception {
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
         * 3) program name (word/excel)
         * 4) macro name (ConvertToPdf/ReplaceFormulas)
         * 3) time to process total
         * 4) time to wait for convert
         * 5) input file size
         * 6) output file size
         * 7) input pdfFile type - received with http request, unmodified
         * 8) input mime type - parsed from pdfFile type
         * 9) input encoding - parsed from pdfFile type
         * 10) if extra html open and close tags were written
         * 11) word SaveFormat 6=RTF, 2=TXT, 12=DOCX, 0=DOC
         * 12) word SaveEncoding DOC/DOCX/RTF: mostly 1200, rarely 1257; TXT 65001 (=msoEncodingUTF8)
         * 13) exception message
         */
        try {
            writer.writeRecord(new String[] {
                    filename,
                    exception == null ? "SUCCESS" : "ERROR",
                    programName,
                    macroName,
                    Long.toString(System.currentTimeMillis() - startTime),
                    Long.toString(conversionDuration),
                    Long.toString(inputFileSize),
                    Long.toString(outputFileSize),
                    inputContentType,
                    inputMimeType,
                    inputEncoding,
                    Boolean.toString(writeHtmlTags),
                    inputSaveFormat,
                    inputSaveEncoding,
                    outputSaveFormat,
                    outputSaveEncoding,
                    exception == null ? "" : exception.getMessage()
            });
        } finally {
            writer.close();
        }
    }

    private MsoProgram saveFile(MsoDocumentInput msoDocumentInput) throws Exception {
        if (msoDocumentInput == null) {
            throw new RuntimeException("Invalid request, input is missing");
        }
        DataHandler content = msoDocumentInput.getDocumentFile();
        if (content == null) {
            throw new RuntimeException("Invalid request, pdfFile is missing");
        }
        inputContentType = content.getContentType();
        ContentType contentType = new ContentType(inputContentType);
        inputMimeType = contentType.getBaseType().toLowerCase();
        inputEncoding = contentType.getParameter("charset");
        if (inputEncoding == null) {
            inputEncoding = "UTF-8";
        }


        // We use input mimeType only to select appropriate program: Word or Excel;
        // We don't pass on mimeType information to Word or Excel; we just issue file open command in Word or Excel macro
        // and Word or Excel itselt can detect file format based on file contents
        // And if it is an unsupported or invalid file, then Word should (in theory)
        // * render as text
        // * exit with error
        // * process very long, that we have to kill it

        MsoProgram program = programsByMimeType.get(inputMimeType);
        if (program == null) {
            throw new RuntimeException("Input mimeType is not supported: " + inputMimeType);
        }

        File inputFile = new File(workFolder, filename);
        if (inputFile.exists()) {
            throw new RuntimeException("Input file already exists locally: " + inputFile);
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(inputFile));
        try {
            InputStream in = content.getInputStream();
            try {

                // 1. If file is TXT or HTML, then Word needs UTF-8 encoding
                // 2. If file is HTML, then Word needs <HTML></HTML> tags

                if (MIMETYPE_HTML.equals(inputMimeType)
                        || (MIMETYPE_TEXT.equals(inputMimeType) && !"UTF-8".equalsIgnoreCase(inputEncoding))) {

                    File inputFileTmp = new File(workFolder, filename + ".tmp");
                    if (inputFileTmp.exists()) {
                        throw new RuntimeException("Input tmp file already exists locally: " + inputFile);
                    }
                    OutputStream outTmp = new BufferedOutputStream(new FileOutputStream(inputFileTmp));
                    try {
                        IOUtils.copy(in, outTmp);
                    } finally {
                        outTmp.close();
                    }
                    long inputLength = inputFileTmp.length();
                    if (inputLength > 9437184) { // If larger than 9 MB
                        throw new RuntimeException("Input file that is HTML or non-UTF-8 TXT cannot be larger than 9 MB: mimeType=" + inputMimeType
                                + " encoding=" + inputEncoding + " length=" + inputLength);
                    }
                    InputStream inTmp = new BufferedInputStream(new FileInputStream(inputFileTmp));
                    String contentString;
                    try {
                        // 9 MB input file is OK, but 10 MB input file gives OutOfMemoryError with 128 MB heap
                        contentString = IOUtils.toString(inTmp, inputEncoding); // This uses much memory!
                    } finally {
                        inTmp.close();
                    }

                    // Some HTML mail doesn't contain <HTML>/<HEAD>/<BODY> tags, just HTML contents
                    // We have to start with <HTML> for Word to recognize the format, otherwise Word opens it as plain text
                    if (MIMETYPE_HTML.equals(inputMimeType)) {
                        // The following method is slower, but doesn't use extra memory compared to converting entire string to lowercase
                        if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(contentString, "<html")) {
                            writeHtmlTags = true;
                        }
                    }

                    if (writeHtmlTags) {
                        IOUtils.write("<html>", out, "UTF-8");
                    }
                    IOUtils.write(contentString, out, "UTF-8");
                    if (writeHtmlTags) {
                        IOUtils.write("<html>", out, "UTF-8");
                    }

                    // HTML may contain <META HTTP-EQUIV=CONTENT-TYPE tag with charset value
                    // Word macro opens all files with UTF-8 encoding forced, so Word ignores this HTML tag, and so we don't have to remove it

                } else {
                    IOUtils.copy(in, out);
                }

            } finally {
                in.close();
            }
        } finally {
            out.close();
        }
        inputFileSize = inputFile.length();
        return program;
    }

    private void startConvertAndWaitToComplete(MsoProgram program, String extraInput) throws Exception {
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
        FileUtils.writeStringToFile(program.getMacroInputFile(), inputFilePath + (extraInput == null ? "" : System.getProperty("line.separator") + extraInput));

        String[] args = new String[] {
                classExecPath,
                program.getMacroDocumentPath(),
                "--action",
                "open",
                "--command",
                String.format(program.getMacroCommandTemplate(), "MsoService." + macroName)
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
                killProgram(program.getImageName());
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
                killProgram(program.getImageName());
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
            inputSaveFormat = lines.get(0);
        }
        if (lines.size() >= 2) {
            inputSaveEncoding = lines.get(1);
        }
        if (lines.size() >= 3) {
            outputSaveFormat = lines.get(2);
        }
        if (lines.size() >= 4) {
            outputSaveEncoding = lines.get(3);
        }

        return;
    }

    private void killProgram(String imageName) throws Exception {
        String[] args = new String[] {
                "taskkill",
                "/im",
                imageName,
                "/t",
                "/f"
        };
        Process process = Runtime.getRuntime().exec(args);
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            log.warn(StringUtils.arrayToDelimitedString(args, " ") + " exitValue=" + exitValue); // probably means that winword.exe process didn't exist
        }
    }

    private File buildOutput() throws IOException {
        // Excel behaves differently than word, it adds file name extension if the name doesn't end with it, i.e. .out.pdf
        File outputFile = new File(workFolder, filename + (programName.equals("excel") ? ".pdf" : ".out"));
        if (!outputFile.exists()) {
            throw new RuntimeException("Output file does not exist: " + outputFile);
        }
        outputFileSize = outputFile.length();
        return outputFile;
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
                File inputFileTmp = new File(workFolder, inputFileName + ".tmp");
                File outputFile = new File(workFolder, inputFileName + ".out");
                File outputPdfFile = new File(workFolder, inputFileName + ".pdf");
                // Try to delete pdf first; if it is locked by java then we delete this group later
                if (outputPdfFile.exists()) {
                    if (!deleteFile(outputPdfFile)) {
                        continue;
                    }
                }
                if (outputFile.exists()) {
                    if (!deleteFile(outputFile)) {
                        continue;
                    }
                }
                if (inputFileTmp.exists()) {
                    if (!deleteFile(inputFileTmp)) {
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
