package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import smit.ametnik.services.Ametnik;
import smit.ametnik.services.Yksus;

/**
 * Test {@link AMRServiceImpl}
 */
public class AMRServiceImplTest extends TestCase {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRServiceImplTest.class);
    static final AMRService amrService;
    static final ClassPathXmlApplicationContext appC;
    static String ametnikId;
    static String ametnikFirstName;
    static {
        appC = new ClassPathXmlApplicationContext("ee/webmedia/alfresco/orgstructure/amr/service/amrService-context.xml");
        appC.refresh();
        amrService = (AMRService) appC.getBean(AMRService.BEAN_NAME);
    }

    public void testYksusByAsutusId() {
        Yksus[] yksusArray = amrService.getYksusByAsutusId();
        assertTrue(yksusArray.length > 0);
        for (Yksus yksus : yksusArray) {
            BigInteger unitId = yksus.getId();
            String name = yksus.getNimetus();
            BigInteger superUnitId = yksus.getYlemYksusId();
            assertNotNull(unitId);
            assertNotNull(name);
            log.debug("unitId=" + unitId + "; name=" + name + "; superUnitId=" + superUnitId);
        }
    }

    public void testGetAmetnikByAsutusId() {
        Ametnik[] ametnikArray = amrService.getAmetnikByAsutusId();
        assertTrue(ametnikArray.length > 0);
        for (Ametnik ametnik : ametnikArray) {
            ametnikId = ametnik.getIsikukood(); // save id for next test
            ametnikFirstName = ametnik.getEesnimi(); // save id for next test
            String lastName = ametnik.getPerekonnanimi();
            BigInteger unitId = ametnik.getYksusId();
            String jobTitle = ametnik.getAmetikoht();
            String phone = ametnik.getKontakttelefon();
            String email = ametnik.getEmail();
            log.debug("firstName=" + ametnikFirstName + "; lastName=" + lastName + "; id=" + ametnikId
                    + "; unitId=" + unitId + "; jobTitle=" + jobTitle + "; phone=" + phone + "; email=" + email);
        }
    }

    public void testGetAmetnikByIsikukood() {
        Ametnik ametnik = amrService.getAmetnikByIsikukood(ametnikId);
        if (ametnik != null) {
            String eesnimi = ametnik.getEesnimi();
            assertEquals(ametnik.getIsikukood(), ametnikId);
            assertEquals(ametnik.getEesnimi(), ametnikFirstName);
            log.debug("Id:" + ametnikId + "; firstName:" + eesnimi);
        }
        assertNull(amrService.getAmetnikByIsikukood("1"));
    }

}
