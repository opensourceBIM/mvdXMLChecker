package nl.tue.ddss.ifc_check;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import nl.tue.buildingsmart.express.parser.SchemaLoader;
import nl.tue.ddss.bcf.ReportWriter;
import nl.tue.ddss.ifc_check.IfcHashMapBuilder.ObjectToValue;
import nl.tue.ddss.rule_parse.MvdXMLv1_1Lexer;
import nl.tue.ddss.rule_parse.MvdXMLv1_1Parser;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.ifc.IfcModel;
import org.bimserver.ifc.step.deserializer.IfcStepDeserializer;
import org.bimserver.models.ifc2x3tc1.IfcElement;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.models.ifc2x3tc1.IfcSpatialStructureElement;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.schema.SchemaDefinition;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.AbstractRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.AttributeRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Concept;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ConceptRoot;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Definitions;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Definitions.Definition;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.EntityRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ExchangeRequirement;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Requirements.Requirement;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.TemplateRule;
import org.xml.sax.SAXException;

public class IfcChecker {
	String ifcFile;
	List<MVDConstraint> constraints;
	MVDConstraint constraint;
	IfcModelInterface ifcModel;
	ExchangeRequirement er;
	
	
	public IfcChecker(String ifcFile, ExchangeRequirement er,List<MVDConstraint> constraints){
		this.ifcFile=ifcFile;
		String uuid=er.getUuid();
		Concept concept=constraint.getConcept();		
		ConceptRoot conceptRoot=constraint.getConceptRoot();
		List<Requirement> requirements=concept.getRequirements().getRequirement();
		for (Requirement requirement:requirements){
			if (requirement.getExchangeRequirement().equals(uuid)){
				
			}
		}
	}

	public IfcChecker(IfcModelInterface ifcModel, MVDConstraint constraint) {
		this.constraint = constraint;
		this.ifcModel = ifcModel;
	}

	public IfcChecker(String ifcSchema, String ifcFile, MVDConstraint constraint)
			throws DeserializeException {
		this.ifcFile = ifcFile;
		this.constraint = constraint;
		IfcStepDeserializer p21Parser = new IfcStepDeserializer();
		SchemaDefinition schema = new SchemaLoader(ifcSchema)
				.getSchema();
		p21Parser.init(schema);
		this.ifcModel = (IfcModel) p21Parser.read(new File(ifcFile));
	}

	public void checkIfcModel(OutputStream outputStream) throws JAXBException {

		String rootName = constraint.getConceptRoot().getApplicableRootEntity();
		List<AttributeRule> attributeRules = constraint.getAttributeRules();

		List<TemplateRule> templateRules = constraint.getTemplateRules();
		try {
			Class cls = Class.forName("org.bimserver.models.ifc2x3tc1."
					+ rootName);
			List<Object> allRoots = ifcModel.getAllWithSubTypes(cls);
			int issueIndex = 0;
			for (Object ifcObject : allRoots) {
				IfcHashMapBuilder ifcHashMapBuilder = new IfcHashMapBuilder(
						ifcObject, attributeRules);
				List<HashMap<AbstractRule, ObjectToValue>> hashMaps = ifcHashMapBuilder
						.getHashMaps();
				String comment = new String();
				for (HashMap<AbstractRule, ObjectToValue> hashMap : hashMaps) {
					comment = templateLevelRuleCheck(hashMap);
				}
				if (templateRules.size() > 0) {
					for (TemplateRule templateRule : templateRules) {
						List<Boolean> resultList = new ArrayList<Boolean>();
						for (int i = 0; i < hashMaps.size(); i++) {
							Boolean result = conceptLevelRuleCheck(
									templateRule.getParameters(),
									hashMaps.get(i));
							if (result != null && result == true) {
								break;
							}
							if (result == false && i == hashMaps.size() - 1) {
								comment = comment
										+ "\n This Object has to fulfil the requirements of "
										+ templateRule.getParameters();
							}
						}
					}
				}
				if (comment.length() > 0) {
					issueIndex++;
					Definitions definitions = constraint.getConcept()
							.getDefinitions();
					if (definitions != null) {
						List<Definition> definitionList = definitions
								.getDefinition();
						for (Definition definition : definitionList) {
							comment = comment + "\n"
									+ definition.getBody().getValue();
						}
					}
					String type = ifcObject.getClass().getSimpleName();
					type = type.substring(0, type.length() - 4);
					if (ifcObject instanceof IfcElement) {
						String spatialStructureElement = new String();
						spatialStructureElement = getIfcSpatialStructure((IfcElement) ifcObject);
						List<String> componantGuids = new LinkedList<String>();
						componantGuids = getComponantGuids(componantGuids,
								(IfcProduct) ifcObject);
						
						ReportWriter reportWriter = new ReportWriter(ifcModel);
						try {
							reportWriter.addIssue(spatialStructureElement,
									((IfcProduct) ifcObject).getGlobalId(),
									comment, componantGuids);
							reportWriter.writeReport(outputStream);
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					System.out.println(type + " "
							+ ((IfcRoot) ifcObject).getGlobalId() + ": "
							+ comment);
				}
			}
		} catch (DeserializeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String getIfcSpatialStructure(IfcElement ifcObject) {
		String guid = new String();
		List<IfcRelContainedInSpatialStructure> ircsisss = ifcObject
				.getContainedInStructure();
		if (ircsisss != null && ircsisss.size()==1) {
			IfcSpatialStructureElement isse = ircsisss.get(0)
					.getRelatingStructure();
			guid = isse.getGlobalId();
		}
		return guid;
	}

	private List<String> getComponantGuids(List<String> guids,
			IfcObjectDefinition ifcObject) {
		if (ifcObject.getIsDecomposedBy().size()>=1) {
			List<IfcRelDecomposes> irds = ifcObject.getIsDecomposedBy();
			for (IfcRelDecomposes ird : irds) {
				List<IfcObjectDefinition> ifcObjects = ird.getRelatedObjects();
				for (IfcObjectDefinition io : ifcObjects) {
					getComponantGuids(guids, io);
				}
			}
		} else
			guids.add(ifcObject.getGlobalId());
		return guids;
	}

	public String templateLevelRuleCheck(
			HashMap<AbstractRule, ObjectToValue> hashMap) {
		String report = new String();
		Set<AbstractRule> rules = hashMap.keySet();
		for (AbstractRule rule : rules) {
			ObjectToValue objectToValue = hashMap.get(rule);
			Object ifcObject = objectToValue.getIfcObject();
			Object value = objectToValue.getValue();
			List<Object> valueList = new ArrayList<Object>();
			if (value instanceof Collection) {
				for (Object object : ((Collection) value)) {
					valueList.add(object);
				}
			} else {
				valueList.add(value);
			}
			if (rule instanceof AttributeRule) {
				String cardinality = ((AttributeRule) rule).getCardinality();
				if (cardinalityCheck(cardinality, valueList) == false) {
					if (ifcObject instanceof IfcRoot) {
						report = report + "\n"
								+ ((IfcRoot) ifcObject).getGlobalId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality;
					} else if (ifcObject instanceof IdEObject) {
						report = report + "\n"
								+ ((IdEObject) ifcObject).getExpressId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality;
					}
				}
			} else {
				String cardinality = ((EntityRule) rule).getCardinality();
				if (cardinalityCheck(cardinality, valueList) == false) {
					if (ifcObject instanceof IfcRoot) {
						report = report + "\n"
								+ ((IfcRoot) ifcObject).getGlobalId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality + " "
								+ ((EntityRule) rule).getEntityName();
					} else if (ifcObject instanceof IdEObject) {
						report = report + "\n"
								+ ((IdEObject) ifcObject).getExpressId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality + " "
								+ ((EntityRule) rule).getEntityName();
					}
				}
			}
		}
		return report;
	}

	public Boolean conceptLevelRuleCheck(String rule,
			HashMap<AbstractRule, ObjectToValue> hashMap) {
		Boolean result = false;
		CharStream charStream = new ANTLRStringStream(rule);
		MvdXMLv1_1Lexer lexer = new MvdXMLv1_1Lexer(charStream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		MvdXMLv1_1Parser parser = new MvdXMLv1_1Parser(tokenStream, hashMap);
		try {
			result = parser.expression();
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public Boolean cardinalityCheck(String cardinality, List<Object> valueList) {
		Boolean result = true;
		if (cardinality != null) {
			if (cardinality.equals("Zero")) {
				if (valueList.size() > 0) {
					result = false;
				}

			} else if (cardinality.equals("ZeroToOne")) {
				if (valueList.size() > 1) {
					result = false;
				}
			} else if (cardinality.equals("One")) {
				if (valueList.size() != 1) {
					result = false;
				}
			}
		} else if (cardinality.equals("OneToMany")) {
			if (valueList.size() == 0) {
				result = false;
			}
		} else if (cardinality.equals("_asSchema")) {

		} else if (cardinality.matches(".+:.+")) {

		} else {
			System.out.println("Cardinality Syntax error of mvdXML");
		}
		return result;
	}

	public List<Boolean> entityTypeCheck(List<EntityRule> entityRules,
			List<Object> valueList) {
		List<Boolean> result = new ArrayList<Boolean>();
		List<Class> entityTypes = new ArrayList<Class>();
		for (EntityRule entityRule : entityRules) {
			try {
				Class cls = Class.forName("org.bimserver.models.ifc2x3tc1."
						+ entityRule.getEntityName());
				entityTypes.add(cls);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (int i = 0; i < valueList.size(); i++) {
			Object value = valueList.get(i);
			if (value == null) {
				result.add(true);
			}
			if (value instanceof Collection) {
				List<Boolean> cTypeCheckResult = new ArrayList<Boolean>();
				for (Object obj : (Collection) value) {
					for (int j = 0; j < entityTypes.size(); j++) {
						if (entityTypes.get(j).isInstance(value)) {
							cTypeCheckResult.add(true);
							break;
						} else if (j == entityTypes.size() - 1) {
							cTypeCheckResult.add(false);
						}
					}
				}
				for (int j = 0; j < cTypeCheckResult.size(); j++) {
					if (cTypeCheckResult.get(j) == false) {
						result.add(false);
						break;
					} else if (cTypeCheckResult.get(j) == true
							&& j == cTypeCheckResult.size() - 1) {
						result.add(true);
					}
				}
			} else {
				for (int j = 0; j < entityTypes.size(); j++) {
					if (entityTypes.get(j).isInstance(value)) {
						result.add(true);
						break;
					} else if (j == entityTypes.size() - 1) {
						result.add(false);
					}
				}
			}
		}
		return result;
	}

}
