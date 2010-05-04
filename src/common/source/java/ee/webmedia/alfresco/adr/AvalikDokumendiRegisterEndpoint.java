package ee.webmedia.alfresco.adr;

import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ee.webmedia.alfresco.adr.service.AdrService;

@WebService(name = "AvalikDokumendiRegister", targetNamespace = "http://alfresco/avalikdokumendiregister", serviceName = "AvalikDokumendiRegisterService")
public class AvalikDokumendiRegisterEndpoint implements AvalikDokumendiRegister {

    @Resource
    private WebServiceContext context;
    private WebApplicationContext webApplicationContext;

    private AdrService adrService;

    private AdrService getAdrService() {
        if (adrService == null) {
            if (webApplicationContext == null) {
                ServletContext servletContext = (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
                webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
            }
            adrService = (AdrService) webApplicationContext.getBean(AdrService.BEAN_NAME);
        }
        return adrService;
    }

    @Override
    @WebMethod
    @WebResult(name = "dokument", targetNamespace = "")
    @RequestWrapper(localName = "otsiDokumendid", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumendid")
    @ResponseWrapper(localName = "otsiDokumendidResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumendidResponse")
    public List<Dokument> otsiDokumendid(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final
            XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final
            XMLGregorianCalendar perioodiLoppKuupaev,
            @WebParam(name = "dokumendiLiik", targetNamespace = "") final
            String dokumendiLiik,
            @WebParam(name = "otsingusona", targetNamespace = "") final
            String otsingusona) {

        return AuthenticationUtil.runAs(new RunAsWork<List<Dokument>>() {
            @Override
            public List<Dokument> doWork() throws Exception {
                return getAdrService().otsiDokumendid(perioodiAlgusKuupaev, perioodiLoppKuupaev, dokumendiLiik, otsingusona);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentDetailidega", targetNamespace = "")
    @RequestWrapper(localName = "otsiDokumentDetailidega", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumentDetailidega")
    @ResponseWrapper(localName = "otsiDokumentDetailidegaResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumentDetailidegaResponse")
    public DokumentDetailidega dokumentDetailidega(
            @WebParam(name = "viit", targetNamespace = "") final
            String viit,
            @WebParam(name = "registreerimiseAeg", targetNamespace = "") final
            XMLGregorianCalendar registreerimiseAeg) {
        
        return AuthenticationUtil.runAs(new RunAsWork<DokumentDetailidega>() {
            @Override
            public DokumentDetailidega doWork() throws Exception {
                return getAdrService().dokumentDetailidega(viit, registreerimiseAeg);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "fail", targetNamespace = "")
    @RequestWrapper(localName = "otsiFailSisuga", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiFailSisuga")
    @ResponseWrapper(localName = "otsiFailSisugaResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiFailSisugaResponse")
    public Fail failSisuga(
            @WebParam(name = "viit", targetNamespace = "") final
            String viit,
            @WebParam(name = "registreerimiseAeg", targetNamespace = "") final
            XMLGregorianCalendar registreerimiseAeg,
            @WebParam(name = "failinimi", targetNamespace = "") final
            String filename) {
        
        return AuthenticationUtil.runAs(new RunAsWork<Fail>() {
            @Override
            public Fail doWork() throws Exception {
                return getAdrService().failSisuga(viit, registreerimiseAeg, filename);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumendiliik", targetNamespace = "")
    @RequestWrapper(localName = "otsiDokumendiliigid", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumendiliigid")
    @ResponseWrapper(localName = "otsiDokumendiliigidResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.OtsiDokumendiliigidResponse")
    public List<Dokumendiliik> dokumendiliigid() {

        return AuthenticationUtil.runAs(new RunAsWork<List<Dokumendiliik>>() {
            @Override
            public List<Dokumendiliik> doWork() throws Exception {
                return getAdrService().dokumendiliigid();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentDetailidegaFailSisuga", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidLisatudMuudetud", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.KoikDokumendidLisatudMuudetud")
    @ResponseWrapper(localName = "koikDokumendidLisatudMuudetudResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.KoikDokumendidLisatudMuudetudResponse")
    public List<DokumentDetailidega> koikDokumendidLisatudMuudetud(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "")
            final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "")
            final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<DokumentDetailidega>>() {
            @Override
            public List<DokumentDetailidega> doWork() throws Exception {
                return getAdrService().koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentKustutatud", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidKustutatud", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.KoikDokumendidKustutatud")
    @ResponseWrapper(localName = "koikDokumendidKustutatudResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.KoikDokumendidKustutatudResponse")
    public List<Dokument> koikDokumendidKustutatud(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "")
            final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "")
            final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<Dokument>>() {
            @Override
            public List<Dokument> doWork() throws Exception {
                return getAdrService().koikDokumendidKustutatud(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

}
