package org.bimserver.plugin.modelviewchecker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.xml.bind.JAXBException;

import nl.tue.ddss.ifc_check.IfcChecker;
import nl.tue.ddss.ifc_check.MVDConstraint;
import nl.tue.ddss.ifc_check.MvdXMLParser;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SExtendedData;
import org.bimserver.interfaces.objects.SExtendedDataSchema;
import org.bimserver.interfaces.objects.SFile;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.services.BimServerClientException;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class ModelViewCheckerServicePlugin extends ServicePlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelViewCheckerServicePlugin.class);

	private static final String MVD_XML_PARAMETER_NAME = "mvdXML";

	private boolean initialized;

	private ClassLoader pluginClassLoader;

	@Override
	public String getDescription() {
		return "Model View Checker";
	}

	@Override
	public String getDefaultName() {
		return "ModelViewChecker";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition objectDefinition = StoreFactory.eINSTANCE.createObjectDefinition();
		
		PrimitiveDefinition byteArrayType = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		byteArrayType.setType(PrimitiveEnum.BYTE_ARRAY);

		ParameterDefinition mvdxmlParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		mvdxmlParameter.setName(MVD_XML_PARAMETER_NAME);
		mvdxmlParameter.setType(byteArrayType);
		mvdxmlParameter.setDescription("Upload a valid mvdXML file");
		mvdxmlParameter.setRequired(true);
		objectDefinition.getParameters().add(mvdxmlParameter);
		
		return objectDefinition;
	}
	
	@Override
	public void init(PluginManager pluginManager) throws PluginException {
		super.init(pluginManager);
		pluginClassLoader = getPluginManager().getPluginContext(this).getClassLoader();
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public String getTitle() {
		return "Model View Checker";
	}

	@Override
	public void register(PluginConfiguration pluginConfiguration) {
		ServiceDescriptor serviceDescriptor = StoreFactory.eINSTANCE.createServiceDescriptor();
		serviceDescriptor.setProviderName("BIMserver");
		serviceDescriptor.setIdentifier(getClass().getName());
		serviceDescriptor.setName("ModelViewChecker");
		serviceDescriptor.setDescription("ModelViewChecker");
		serviceDescriptor.setNotificationProtocol(AccessMethod.INTERNAL);
		serviceDescriptor.setReadRevision(true);
		final String targetNamespace = "[TO BE DETERMINED]";
		serviceDescriptor.setWriteExtendedData(targetNamespace);
		serviceDescriptor.setTrigger(Trigger.NEW_REVISION);
		
		final byte[] mvdXMLData = pluginConfiguration.getByteArray(MVD_XML_PARAMETER_NAME);
		
		registerNewRevisionHandler(serviceDescriptor, new NewRevisionHandler() {
			@SuppressWarnings("unused")
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws ServerException, UserException {
				SSerializerPluginConfiguration sSerializer;
				try {
					IfcModelInterface model = bimServerClientInterface.getModel(poid, roid, false);
						
					System.out.println(new String(mvdXMLData, Charsets.UTF_8));
					MvdXMLParser mvdXMLParser = new MvdXMLParser(new ByteArrayInputStream(mvdXMLData), pluginClassLoader);
					ByteArrayOutputStream bcfOutput = new ByteArrayOutputStream();
					try {
						List<MVDConstraint> constraints=mvdXMLParser.generateConceptTrees();
						for (MVDConstraint constraint:constraints){
							IfcChecker ifcChecker = new IfcChecker(model, constraint);
							ifcChecker.checkIfcModel(bcfOutput);
						}
					} catch (JAXBException e) {
						e.printStackTrace();
					}

					
					List<IfcProject> ifcProjects = model.getAll(IfcProject.class);
					IfcProject mainIfcProject = null;
					if (!ifcProjects.isEmpty()) {
						mainIfcProject = ifcProjects.get(0);
					}

					SExtendedDataSchema extendedDataSchemaByNamespace = bimServerClientInterface.getBimsie1ServiceInterface().getExtendedDataSchemaByNamespace(targetNamespace);

					SFile file = new SFile();

					SExtendedData extendedData = new SExtendedData();
					extendedData.setTitle("ModelViewChecker Results");
					file.setFilename("modelviewchecker.bcfzip");
					extendedData.setSchemaId(extendedDataSchemaByNamespace.getOid());
					try {
						byte[] bytes = bcfOutput.toByteArray();
						file.setData(bytes);
						file.setMime("application/bcf");

						long fileId = bimServerClientInterface.getServiceInterface().uploadFile(file);
						extendedData.setFileId(fileId);

						bimServerClientInterface.getBimsie1ServiceInterface().addExtendedDataToRevision(roid, extendedData);
					} catch (Exception e) {
						LOGGER.error("", e);
					}
				} catch (PublicInterfaceNotFoundException e) {
					LOGGER.error("", e);
				} catch (BimServerClientException e) {
					LOGGER.error("", e);
				} catch (JAXBException e) {
					LOGGER.error("", e);
				}
			}
		});
	}
}