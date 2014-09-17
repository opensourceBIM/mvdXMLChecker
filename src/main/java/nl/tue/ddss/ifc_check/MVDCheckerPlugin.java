package nl.tue.ddss.ifc_check;

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.modelchecker.ModelChecker;
import org.bimserver.plugins.modelchecker.ModelCheckerPlugin;

public class MVDCheckerPlugin implements ModelCheckerPlugin{

	@Override
	public void init(PluginManager pluginManager) throws PluginException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "mvdXML Checker";
	}

	@Override
	public String getDefaultName() {
		// TODO Auto-generated method stub
		return "mvdXML Checker";
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ModelChecker createModelChecker(PluginConfiguration arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}