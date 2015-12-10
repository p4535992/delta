package ee.webmedia.alfresco.dvk.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.version.NodeServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookServiceImpl;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminServiceImpl;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigServiceImpl;
import ee.webmedia.alfresco.document.file.service.FileServiceImpl;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendReviewTask;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.service.ParametersServiceImpl;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.template.service.DocumentTemplateServiceImpl;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendDocumentsDecContainerCallback;
import ee.webmedia.xtee.client.dhl.DhlXTeeServiceImplTest;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport.DecRecipient;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport.DecSender;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BeanHelper.class, DvkServiceImpl.class, SendDocumentsDecContainerCallback.class, Node.class, DecContainerHandler.class, WorkflowUtil.class })
public class DecContainerV2_1Test extends TestCase {

    private static final NodeRef NODE_REF = new NodeRef("workspace://SpacesStore/e447a3c5-0e83-4fcc-98a7-8acf79fb810b");

    private static final String OWNER_NAME = "Owner Name";
    private static final String OWNER_E_MAIL = "OwnerEmail@Owner.email";
    private static final String ORG_NAME = "DVK_ORGANIZATION_NAME";
    private static final String SENDER_EMAIL = "container.test@sender.email";

    private DhlXTeeService dhl;

    public static junit.framework.Test suite() { // this is needed to run tests via ant target
        return new JUnit4TestAdapter(DecContainerV2_1Test.class);
    }

    private DhlXTeeService getDhlXTeeService() {
        if (dhl == null) {
            final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("ee/webmedia/xtee/client/dhl/service-impl-test.xml");
            dhl = (DhlXTeeService) context.getBean("dhlXTeeService");
        }
        return dhl;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public void testRecordSenderToDecOrganisationNameAndRecordCreatorContactDataEmailAreAdded() throws Exception {
        DecContainerDocument container = createDummyDecContainer();

        DvkSendDocuments sd = new DvkSendDocuments();
        sd.setSenderOrgName(ORG_NAME);
        sd.setSenderEmail("senderEmailAddress");
        sd.setRecipientsRegNrs(Arrays.asList("wmdelta"));
        sd.setOrgNames(Arrays.asList("wmdelta"));
        sd.setPersonNames(Arrays.asList("Mr Bean"));
        sd.setDocumentNodeRef(NODE_REF);
        sd.setTextContent(StringEscapeUtils.unescapeHtml(WebUtil.removeHtmlTags("content")));

        Pair<SendDocumentsDecContainerCallback, DvkService> callbackWithService = getCallbackInstanceWithParent("SimDhsSendDocumentsCallback", DvkSendDocuments.class, sd);
        SendDocumentsDecContainerCallback simDhsSendDocumentsCallback = callbackWithService.getFirst();

        disableUninterestingMethods(simDhsSendDocumentsCallback);

        Node mockNode = createMockDocumentNode();
        PowerMockito.whenNew(Node.class).withAnyArguments().thenReturn(mockNode);

        DocumentTypeVersion documentTypeVersionMock = PowerMock.createNiceMock(DocumentTypeVersion.class);
        Pair<DocumentType, DocumentTypeVersion> mockPair = new Pair<DocumentType, DocumentTypeVersion>(null, documentTypeVersionMock);

        DocumentConfigService configService = createMockServiceWithMockMethod(DocumentConfigServiceImpl.class, "getDocumentTypeAndVersion", Node.class);
        EasyMock.expect(configService.getDocumentTypeAndVersion(mockNode, true)).andReturn(mockPair);

        DocumentAdminService adminService = createMockServiceWithMockMethod(DocumentAdminServiceImpl.class, "getDocumentTypeName", Node.class);
        EasyMock.expect(adminService.getDocumentTypeName(mockNode)).andReturn(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.getLocalName());

        PowerMock.mockStaticPartial(BeanHelper.class, "getDocumentConfigService", "getDocumentAdminService");
        EasyMock.expect(BeanHelper.getDocumentConfigService()).andReturn(configService);
        EasyMock.expect(BeanHelper.getDocumentAdminService()).andReturn(adminService);
        // Bypass validation
        PowerMock.mockStatic(DecContainerHandler.class);
        EasyMock.expect(DecContainerHandler.validateMandatoryKeysPresent(EasyMock.anyObject(DecContainer.class), EasyMock.anyObject(Map.class))).andReturn(true);
        PowerMock.replay(BeanHelper.class, DecContainerHandler.class, configService, adminService, documentTypeVersionMock);

        DvkService dvkService = callbackWithService.getSecond();
        Method getOrganisationName = dvkService.getClass().getSuperclass().getDeclaredMethod("getOrganisationName", String.class);
        replacePrivateMetod(getOrganisationName, SENDER_EMAIL);
        ReflectionTestUtils.invokeSetterMethod(dvkService, "setParametersService", createMockParameterService());

        simDhsSendDocumentsCallback.doWithDocument(container);

        String orgName = container.getDecContainer().getRecordSenderToDec().getOrganisation().getName();
        String email = container.getDecContainer().getRecordCreator().getContactData().getEmail();

        assertEquals(ORG_NAME, orgName);
        assertEquals(SENDER_EMAIL, email);
    }

    private ParametersService createMockParameterService() {
        ParametersService parameterService = Mockito.mock(ParametersService.class);
        Mockito.when(parameterService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME)).thenReturn(ORG_NAME);
        Mockito.when(parameterService.getStringParameter(Parameters.DOC_SENDER_EMAIL)).thenReturn(SENDER_EMAIL);
        return parameterService;
    }

    private void disableUninterestingMethods(SendDocumentsDecContainerCallback simDhsSendDocumentsCallback) throws NoSuchMethodException {
        Method addDocumentMetadata = simDhsSendDocumentsCallback.getClass().getDeclaredMethod("addDocumentMetadata", Map.class, DocumentTypeVersion.class, DecContainer.class);
        Method addAccessRestriction = simDhsSendDocumentsCallback.getClass().getDeclaredMethod("addAccessRestriction", Map.class, DecContainer.class);
        Method addOrganisationRecipients = simDhsSendDocumentsCallback.getClass().getDeclaredMethod("addOrganisationRecipients", DecContainer.class);
        Method addPersonRecipients = simDhsSendDocumentsCallback.getClass().getDeclaredMethod("addPersonRecipients", DecContainer.class);
        Method addSignatureInformation = simDhsSendDocumentsCallback.getClass().getDeclaredMethod("addSignatureInformation", DecContainer.class);

        replacePrivateMetod(addDocumentMetadata, new Pair<Boolean, Map<String, String>>(true, null));
        replacePrivateMetod(addAccessRestriction, null);
        replacePrivateMetod(addOrganisationRecipients, null);
        replacePrivateMetod(addPersonRecipients, null);
        replacePrivateMetod(addSignatureInformation, null);
    }

    private Node createMockDocumentNode() throws Exception {
        Node mockNode = PowerMockito.mock(Node.class);
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(ContentModel.PROP_OWNER.toString(), "JUNIT");
        properties.put(DocumentCommonModel.Props.REG_DATE_TIME.toString(), new Date());
        PowerMockito.when(mockNode.getProperties()).thenReturn(properties);
        mockFinalMethod(mockNode, "getNodeRef", NODE_REF);
        return mockNode;
    }

    private <T> void mockFinalMethod(Object mockObject, String methodName, final T returnValue) throws Exception {
        PowerMockito.doAnswer(new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                return returnValue;
            }
        }).when(mockObject, methodName);
    }

    private <T> void replacePrivateMetod(Method methodToReplace, final T returnValue) {
        PowerMockito.replace(methodToReplace).with(new InvocationHandler() {
            @Override
            public T invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return returnValue;
            }
        });
    }

    public void testCanAddElementsToIncomingDecElementButNotToOutgoingElement() throws Exception {
        FieldDetailsDialog dialog = new FieldDetailsDialog();

        @SuppressWarnings("unchecked")
        List<String> tempkeys = new ArrayList<String>(((Map<String, List<String>>) ReflectionTestUtils.getField(new DecContainerHandler(), "OUTGOING_FORBIDDEN_KEYS")).keySet());
        List<String> keys = new ArrayList<String>();
        Collections.copy(tempkeys, keys);

        ReflectionTestUtils.setField(dialog, "field", getMockField(FieldType.TEXT_FIELD));

        String datefield = "<SignatureMetaData><SignatureVerificationDate>";
        keys.remove(datefield);

        for (String textField : keys) {
            isValidIncomingAndInvalidOutgoingValue(dialog, textField);
        }

        ReflectionTestUtils.setField(dialog, "field", getMockField(FieldType.DATE));
        isValidIncomingAndInvalidOutgoingValue(dialog, datefield);
    }

    private ee.webmedia.alfresco.docadmin.service.Field getMockField(final FieldType fieldType) {
        ee.webmedia.alfresco.docadmin.service.Field field = Mockito.mock(ee.webmedia.alfresco.docadmin.service.Field.class, new Answer<FieldType>() {
            @Override
            public FieldType answer(InvocationOnMock invocation) throws Throwable {
                return fieldType;
            }
        });
        return field;
    }

    private void isValidIncomingAndInvalidOutgoingValue(FieldDetailsDialog dialog, String key) {
        List<String> keyList = Collections.singletonList(key);
        boolean incomingValid = (Boolean) ReflectionTestUtils.invokeMethod(dialog, "validateDecMappings", true, keyList, null);
        assertTrue("This field must be valid: " + key, incomingValid);

        boolean outgoingValid = (Boolean) ReflectionTestUtils.invokeMethod(dialog, "validateDecMappings", true, new ArrayList<String>(), keyList);
        assertFalse("This field must not be valid: " + key, outgoingValid);
    }

    public void testReviewNotificationContainerDoesNotContainStructuralUnit() throws Exception {
        DecContainerDocument container = createDummyDecContainer();

        DvkSendReviewTask sendReviewTask = new DvkSendReviewTask();
        sendReviewTask.setSenderRegNr("asdfasdf");
        sendReviewTask.setSenderOrgName("wmdelta");
        sendReviewTask.setSenderName("JUnit");
        sendReviewTask.setSenderEmail("e-mail");
        sendReviewTask.setRecipientsRegNr("1234569876543");
        sendReviewTask.setRecipientStructuralUnit("John Rambo");
        sendReviewTask.setInstitutionName("Classified");
        sendReviewTask.setWorkflowTitle("Compound workflow title");
        sendReviewTask.setTaskId("e447a3c5-0e83-4fcc-98a7-8acf79fb810b");

        DocumentTemplateService templateService = createMockServiceWithMockMethod(DocumentTemplateServiceImpl.class, "getCompoundWorkflowUrl");
        EasyMock.expect(templateService.getCompoundWorkflowUrl(EasyMock.anyObject(NodeRef.class))).andReturn("www.junit.org");

        PowerMock.mockStaticPartial(BeanHelper.class, "getDocumentTemplateService");
        PowerMock.mockStaticPartial(WorkflowUtil.class, "marshalDeltaKK", JAXBElement.class, Document.class);
        EasyMock.expect(BeanHelper.getDocumentTemplateService()).andReturn(templateService);

        PowerMock.replay(BeanHelper.class, DecContainerHandler.class, templateService);

        Document domDoc = (Document) ReflectionTestUtils.invokeMethod(new DvkServiceSimImpl(), "createDeltaKKRootTypeNode", getMockTask());

        sendReviewTask.setRecipientDocNode(domDoc);

        SendDocumentsDecContainerCallback dhsSendReviewNotificationCallback = getCallbackInstance("DhsSendReviewNotificationCallback", DvkSendReviewTask.class, sendReviewTask);
        dhsSendReviewNotificationCallback.doWithDocument(container);

        assertTrue(validateStructuralUnitElementsAreNotPresent(container));
    }

    private boolean validateStructuralUnitElementsAreNotPresent(DecContainerDocument container) {
        Transport transport = container.getDecContainer().getTransport();
        if (transport == null) {
            return true;
        }
        DecSender decSender = transport.getDecSender();
        if (decSender != null) {
            String structuralUnit = decSender.getStructuralUnit();
            if (StringUtils.isNotBlank(structuralUnit)) {
                throw new RuntimeException("<Transport><DecSender><StructuralUnit> must not be present!");
            }
        }

        List<DecRecipient> decRecipients = transport.getDecRecipientList();
        if (CollectionUtils.isNotEmpty(decRecipients)) {
            for (DecRecipient recipient : decRecipients) {
                if (StringUtils.isNotBlank(recipient.getStructuralUnit())) {
                    throw new RuntimeException("<Transport><DecRecipient><StructuralUnit> must not be present!");
                }
            }
        }
        return true;
    }

    public void testDecContainerPhoneNrCanContainAnySymbol() throws Exception {
        DecContainerDocument container = createDummyDecContainer();
        DecContainer decContainer = container.getDecContainer();
        String fieldName = "PhoneNr";
        String key = "<RecordCreator><ContactData><Phone>";
        DecContainerHandler.setValue(fieldName, key, decContainer, "+372 55 555-555");
        DecContainerHandler.setValue(fieldName, key, decContainer, "+37255555555");
        DecContainerHandler.setValue(fieldName, key, decContainer, " 55 55 55 55");
        DecContainerHandler.setValue(fieldName, key, decContainer, "(+372) 55 55 55 55");
    }

    public void testDocIsSentOverDvkIfOrgIsDecTaskCapableAndViaEmailOtherwise() throws Exception {
        Object result = getMockDvkService(true).sendTaskNotificationDocument(getMockTask(), null);
        assertNotNull(result);

        Object result2 = getMockDvkService(false).sendTaskNotificationDocument(getMockTask(), null);
        assertNull("sendTaskNotificationDocument() must return null when DEC_TASK_CAPABLE is false", result2);
    }

    private DvkService getMockDvkService(boolean decTaskCapablePropValue) throws NoSuchMethodException {
        AddressbookServiceImpl addressBookService = createMockServiceWithMockMethod(AddressbookServiceImpl.class, "getOrganizationNodeRef");
        EasyMock.expect(addressBookService.getOrganizationNodeRef(OWNER_E_MAIL, OWNER_NAME)).andReturn(NODE_REF);

        ParametersServiceImpl parametersService = createMockServiceWithMockMethod(ParametersServiceImpl.class, "getStringParameter");
        EasyMock.expect(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME)).andReturn("DVK_ORGANIZATION_NAME").anyTimes();
        EasyMock.expect(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL)).andReturn("DOC_SENDER_EMAIL").anyTimes();

        FileServiceImpl fileService = createMockServiceWithMockMethod(FileServiceImpl.class, "getAllFileRefs");
        EasyMock.expect(fileService.getAllFileRefs(NODE_REF, true)).andReturn(new ArrayList<NodeRef>());

        PowerMock.mockStaticPartial(BeanHelper.class, "getAddressbookService", "getParametersService", "getFileService");
        EasyMock.expect(BeanHelper.getAddressbookService()).andReturn(addressBookService);
        EasyMock.expect(BeanHelper.getParametersService()).andReturn(parametersService).times(2);
        EasyMock.expect(BeanHelper.getFileService()).andReturn(fileService);

        NodeService nodeService = Mockito.mock(NodeServiceImpl.class);
        Mockito.when(nodeService.getProperty(NODE_REF, AddressbookModel.Props.DVK_CAPABLE)).thenReturn(Boolean.TRUE);
        Mockito.when(nodeService.getProperty(NODE_REF, AddressbookModel.Props.DEC_TASK_CAPABLE)).thenReturn(decTaskCapablePropValue);
        Mockito.when(nodeService.getProperty(NODE_REF, AddressbookModel.Props.ORGANIZATION_CODE)).thenReturn("1234567");

        DvkService dvkService = new DvkServiceSimImpl();
        Method sendDocuments = dvkService.getClass().getSuperclass().getDeclaredMethod("sendDocuments", Collection.class, DvkSendDocuments.class, boolean.class);

        replacePrivateMetod(sendDocuments, "123456");

        PowerMock.replay(BeanHelper.class, addressBookService, parametersService, fileService);
        ReflectionTestUtils.invokeSetterMethod(dvkService, "setNodeService", nodeService);
        return dvkService;
    }

    private <T> T createMockServiceWithMockMethod(Class<T> bean, String mockedMethodName, Class<?>... parameterTypes) {
        if (parameterTypes != null && parameterTypes.length > 0) {
            return EasyMock.createMockBuilder(bean).addMockedMethod(mockedMethodName, parameterTypes).createNiceMock();
        }
        return EasyMock.createMockBuilder(bean).addMockedMethod(mockedMethodName).createNiceMock();
    }

    private Task getMockTask() {
        CompoundWorkflow cwf = Mockito.mock(CompoundWorkflow.class);
        Mockito.when(cwf.getNodeRef()).thenReturn(new NodeRef("workspace://SpacesStore/e447a3c5-0e83-4fcc-2222-8acf79fb810b"));
        Mockito.when(cwf.getTitle()).thenReturn("CompoundWorkflow title");
        Mockito.when(cwf.isDocumentWorkflow()).thenReturn(true);
        Mockito.when(cwf.getOwnerName()).thenReturn("CWF Owner");

        Workflow wf = Mockito.mock(Workflow.class);
        Mockito.when(wf.getParent()).thenReturn(cwf);
        Mockito.when(wf.getResolution()).thenReturn("");
        Mockito.when(wf.getType()).thenReturn(WorkflowCommonModel.Types.WORKFLOW);

        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getParent()).thenReturn(wf);
        Mockito.when(task.getNodeRef()).thenReturn(NODE_REF);
        Mockito.when(task.getOutcome()).thenReturn("outcome");
        Mockito.when(task.getResolution()).thenReturn("");
        Mockito.when(task.getOwnerName()).thenReturn(OWNER_NAME);
        Mockito.when(task.getOwnerEmail()).thenReturn(OWNER_E_MAIL);
        Mockito.when(task.getDueDateStr()).thenReturn("31.12.2999");
        Mockito.when(task.isStatus(Status.IN_PROGRESS)).thenReturn(true);

        return task;
    }

    private SendDocumentsDecContainerCallback getCallbackInstance(String callbackClassName, Class<?> parameterType, Object argument) throws Exception {
        return getCallbackInstanceWithParent(callbackClassName, parameterType, argument).getFirst();
    }

    private Pair<SendDocumentsDecContainerCallback, DvkService> getCallbackInstanceWithParent(String callbackClassName, Class<?> parameterType, Object argument) throws Exception {
        Class<? extends SendDocumentsDecContainerCallback> callbackClass = null;
        SendDocumentsDecContainerCallback callbackInstance = null;
        DvkServiceSimImpl dvkServiceSimImpl = new DvkServiceSimImpl();
        @SuppressWarnings("unchecked")
        Class<? extends SendDocumentsDecContainerCallback>[] classes =
        (Class<? extends SendDocumentsDecContainerCallback>[]) dvkServiceSimImpl.getClass().getSuperclass().getDeclaredClasses();
        for (Class<? extends SendDocumentsDecContainerCallback> clazz : classes) {
            if (clazz.getSimpleName().equals(callbackClassName)) {
                callbackClass = clazz;
                break;
            }
        }
        if (callbackClass != null) {
            if (parameterType != null) {
                Constructor<? extends SendDocumentsDecContainerCallback> constructor = callbackClass.getConstructor(DvkServiceImpl.class, parameterType);
                callbackInstance = constructor.newInstance(dvkServiceSimImpl, argument);
            }
        }
        return new Pair<DhlXTeeService.SendDocumentsDecContainerCallback, DvkService>(callbackInstance, dvkServiceSimImpl);
    }

    private DecContainerDocument createDummyDecContainer() throws NoSuchFieldException, IllegalAccessException {
        final Set<ContentToSend> contentsToSend = DhlXTeeServiceImplTest.getContentsToSend();

        Field sendDocumentsHelper = getDhlXTeeService().getClass().getDeclaredField("sendDocumentsHelper");
        sendDocumentsHelper.setAccessible(true);
        Object sendDocumentsHelperInstance = sendDocumentsHelper.get(getDhlXTeeService());

        DecContainerDocument container = (DecContainerDocument) ReflectionTestUtils.invokeMethod(sendDocumentsHelperInstance, "constructDecContainerDocument", contentsToSend,
                createDecSender(), createDecRecipients());
        return container;
    }

    private DecRecipient[] createDecRecipients() {
        DecRecipient[] recipients = new DecRecipient[1];
        DecRecipient recipient = DecRecipient.Factory.newInstance();
        recipient.setOrganisationCode("wmdelta");
        recipients[0] = recipient;
        return recipients;
    }

    private DecSender createDecSender() {
        DecSender sender = DecSender.Factory.newInstance();
        sender.setOrganisationCode("wmdelta");
        sender.setStructuralUnit("DVK_ORGANIZATION_NAME");
        return sender;
    }

}
