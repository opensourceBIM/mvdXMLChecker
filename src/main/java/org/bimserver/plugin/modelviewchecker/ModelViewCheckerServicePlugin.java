package org.bimserver.plugin.modelviewchecker;

import org.bimserver.interfaces.objects.SInternalServicePluginConfiguration;
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
import org.bimserver.plugins.services.ServicePlugin;

public class ModelViewCheckerServicePlugin extends ServicePlugin {
	private static final String MVD_XML_PARAMETER_NAME = "mvdXML";

	private ClassLoader pluginClassLoader;
	private ServiceDescriptor serviceDescriptor;

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
	public String getTitle() {
		return "Model View Checker";
	}

	@Override
	public void register(SInternalServicePluginConfiguration internalServicePluginConfiguration, PluginConfiguration pluginConfiguration) {
		final String targetNamespace = "[TO BE DETERMINED]";

		serviceDescriptor = StoreFactory.eINSTANCE.createServiceDescriptor();
		serviceDescriptor.setProviderName("BIMserver");
		serviceDescriptor.setIdentifier("" + internalServicePluginConfiguration.getOid());
		serviceDescriptor.setName(internalServicePluginConfiguration.getName());
		serviceDescriptor.setDescription(internalServicePluginConfiguration.getDescription());
		serviceDescriptor.setNotificationProtocol(AccessMethod.INTERNAL);
		serviceDescriptor.setReadRevision(true);
		serviceDescriptor.setWriteExtendedData(targetNamespace);
		serviceDescriptor.setTrigger(Trigger.NEW_REVISION);
		
		final byte[] mvdXMLData = pluginConfiguration.getByteArray(MVD_XML_PARAMETER_NAME);

		if (mvdXMLData != null) {
			registerNewRevisionHandler(serviceDescriptor, new ModelViewCheckerNewRevisionHandler(mvdXMLData, pluginClassLoader, targetNamespace));
		}
	}
	
	public void unregister(SInternalServicePluginConfiguration internalServicePluginConfiguration) {
		unregisterNewRevisionHandler(serviceDescriptor);
	}
}