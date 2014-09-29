package nl.tue.ddss.bcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import nl.tue.buildingsmart.express.parser.SchemaLoader;

import org.bimserver.ifc.IfcModel;
import org.bimserver.ifc.step.deserializer.IfcStepDeserializer;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.schema.SchemaDefinition;
import org.xml.sax.SAXException;

public class IfcParserAdd {


		public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
			@SuppressWarnings("unused")
			IfcParserAdd reader = new IfcParserAdd();		
		}

		public IfcParserAdd() throws IOException, SAXException, ParserConfigurationException
		{
			try {
				IfcStepDeserializer p21Parser = new IfcStepDeserializer();
			SchemaDefinition schema = new SchemaLoader("Ifc2x3_TC1.exp")
					.getSchema();
			p21Parser.init(schema);
				IfcModel ifcModel = (IfcModel) p21Parser.read(new File("simpel huisje.ifc"));
				ReportWriter reportWriter = new ReportWriter(ifcModel);
				String guid="1AbdMDvZjDiP8GfhziNl_j";
				List<String> guids=new ArrayList<String>();
				guids.add(guid);
				reportWriter.addIssue("ifcSpatialStructureElement", guid, "comment",guids);
				
			} catch (DeserializeException e) {
				e.printStackTrace();
			}
			
		}
	}
