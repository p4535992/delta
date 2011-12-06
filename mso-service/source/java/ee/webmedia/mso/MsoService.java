package ee.webmedia.mso;


public interface MsoService {

    MsoPdfOutput convertToPdf(MsoDocumentInput msoDocumentInput) throws Exception;

    MsoDocumentOutput replaceFormulas(MsoDocumentAndFormulasInput input) throws Exception;

    MsoDocumentAndPdfOutput replaceFormulasAndConvertToPdf(MsoDocumentAndFormulasInput input) throws Exception;

    ModifiedFormulasOutput modifiedFormulas(MsoDocumentInput msoDocumentInput) throws Exception;

}
