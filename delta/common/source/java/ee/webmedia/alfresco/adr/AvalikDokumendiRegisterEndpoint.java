package ee.webmedia.alfresco.adr;

import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.ServletContext;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.adr.ws.AvalikDokumendiRegister;
import ee.webmedia.alfresco.adr.ws.Dokumendiliik;
import ee.webmedia.alfresco.adr.ws.Dokument;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidega;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidegaResponse;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidegaV2;
import ee.webmedia.alfresco.adr.ws.DokumentId;
import ee.webmedia.alfresco.adr.ws.Fail;
import ee.webmedia.alfresco.adr.ws.FailV2;
import ee.webmedia.alfresco.adr.ws.OtsiDokumendiliigid;
import ee.webmedia.alfresco.adr.ws.OtsiDokumendiliigidResponse;
import ee.webmedia.alfresco.adr.ws.OtsiDokumentDetailidega;
import ee.webmedia.alfresco.adr.ws.OtsiFailSisuga;
import ee.webmedia.alfresco.adr.ws.OtsiFailSisugaResponse;
import ee.webmedia.alfresco.adr.ws.OtsiFailSisugaV2;
import ee.webmedia.alfresco.adr.ws.OtsiFailSisugaV2Response;

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
    @RequestWrapper(localName = "otsiDokumendid", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.OtsiDokumendid")
    @ResponseWrapper(localName = "otsiDokumendidResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.OtsiDokumendidResponse")
    public List<Dokument> otsiDokumendid(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiLoppKuupaev,
            @WebParam(name = "dokumendiLiik", targetNamespace = "") final String dokumendiLiik,
            @WebParam(name = "otsingusona", targetNamespace = "") final String otsingusona) {

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
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public DokumentDetailidegaResponse dokumentDetailidega(
            @WebParam(name = "otsiDokumentDetailidega", targetNamespace = "", partName = "parameters") final OtsiDokumentDetailidega parameters) {

        DokumentDetailidega result = AuthenticationUtil.runAs(new RunAsWork<DokumentDetailidega>() {
            @Override
            public DokumentDetailidega doWork() throws Exception {
                return getAdrService().dokumentDetailidega(parameters.getViit(), parameters.getRegistreerimiseAeg());
            }
        }, AuthenticationUtil.getSystemUserName());
        DokumentDetailidegaResponse wrapper = new DokumentDetailidegaResponse();
        wrapper.setDokumentDetailidega(result);
        return wrapper;
    }

    @Override
    @WebMethod
    @WebResult(name = "fail", targetNamespace = "")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public OtsiFailSisugaResponse failSisuga(
            @WebParam(name = "otsiFailSisuga", targetNamespace = "", partName = "parameters") final OtsiFailSisuga parameters) {

        Fail result = AuthenticationUtil.runAs(new RunAsWork<Fail>() {
            @Override
            public Fail doWork() throws Exception {
                return getAdrService().failSisuga(parameters.getViit(), parameters.getRegistreerimiseAeg(), parameters.getFailinimi());
            }
        }, AuthenticationUtil.getSystemUserName());
        OtsiFailSisugaResponse wrapper = new OtsiFailSisugaResponse();
        wrapper.setFail(result);
        return wrapper;
    }

    @Override
    @WebMethod
    @WebResult(name = "otsiFailSisugaV2Response", targetNamespace = "", partName = "parameters")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public OtsiFailSisugaV2Response failSisugaV2(
            @WebParam(name = "otsiFailSisugaV2", targetNamespace = "", partName = "parameters") final OtsiFailSisugaV2 parameters) {

        FailV2 result = AuthenticationUtil.runAs(new RunAsWork<FailV2>() {
            @Override
            public FailV2 doWork() throws Exception {
                return getAdrService().failSisugaV2(new NodeRef(parameters.getDokumentId()), parameters.getFailinimi());
            }
        }, AuthenticationUtil.getSystemUserName());
        OtsiFailSisugaV2Response wrapper = new OtsiFailSisugaV2Response();
        wrapper.setFail(result);
        return wrapper;
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumendiliik", targetNamespace = "")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public OtsiDokumendiliigidResponse dokumendiliigid(
            @WebParam(name = "otsiDokumendiliigid", targetNamespace = "", partName = "parameters") final OtsiDokumendiliigid parameters) {

        List<Dokumendiliik> result = AuthenticationUtil.runAs(new RunAsWork<List<Dokumendiliik>>() {
            @Override
            public List<Dokumendiliik> doWork() throws Exception {
                return getAdrService().dokumendiliigid();
            }
        }, AuthenticationUtil.getSystemUserName());
        OtsiDokumendiliigidResponse wrapper = new OtsiDokumendiliigidResponse();
        wrapper.getDokumendiliik().addAll(result);
        return wrapper;
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentDetailidegaFailSisuga", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidLisatudMuudetud", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidLisatudMuudetud")
    @ResponseWrapper(localName = "koikDokumendidLisatudMuudetudResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidLisatudMuudetudResponse")
    public List<DokumentDetailidega> koikDokumendidLisatudMuudetud(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<DokumentDetailidega>>() {
            @Override
            public List<DokumentDetailidega> doWork() throws Exception {
                return getAdrService().koikDokumendidLisatudMuudetud(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentDetailidega", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidLisatudMuudetudV2", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidLisatudMuudetudV2")
    @ResponseWrapper(localName = "koikDokumendidLisatudMuudetudV2Response", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidLisatudMuudetudV2Response")
    public List<DokumentDetailidegaV2> koikDokumendidLisatudMuudetudV2(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<DokumentDetailidegaV2>>() {
            @Override
            public List<DokumentDetailidegaV2> doWork() throws Exception {
                return getAdrService().koikDokumendidLisatudMuudetudV2(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentKustutatud", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidKustutatud", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidKustutatud")
    @ResponseWrapper(localName = "koikDokumendidKustutatudResponse", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidKustutatudResponse")
    public List<Dokument> koikDokumendidKustutatud(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<Dokument>>() {
            @Override
            public List<Dokument> doWork() throws Exception {
                return getAdrService().koikDokumendidKustutatud(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    @WebMethod
    @WebResult(name = "dokumentKustutatud", targetNamespace = "")
    @RequestWrapper(localName = "koikDokumendidKustutatudV2", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidKustutatudV2")
    @ResponseWrapper(localName = "koikDokumendidKustutatudV2Response", targetNamespace = "http://alfresco/avalikdokumendiregister", className = "ee.webmedia.alfresco.adr.ws.KoikDokumendidKustutatudV2Response")
    public List<DokumentId> koikDokumendidKustutatudV2(
            @WebParam(name = "perioodiAlgusKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiAlgusKuupaev,
            @WebParam(name = "perioodiLoppKuupaev", targetNamespace = "") final XMLGregorianCalendar perioodiLoppKuupaev) {

        return AuthenticationUtil.runAs(new RunAsWork<List<DokumentId>>() {
            @Override
            public List<DokumentId> doWork() throws Exception {
                return getAdrService().koikDokumendidKustutatudV2(perioodiAlgusKuupaev, perioodiLoppKuupaev);
            }
        }, AuthenticationUtil.getSystemUserName());
    }

}
