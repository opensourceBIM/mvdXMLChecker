package nl.tue.ddss.ifc_check;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.buildingsmart_tech.mvdxml.mvdxml1_1.AbstractRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.AttributeRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Concept;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ConceptRoot;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ConceptTemplate;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.EntityRule;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ModelView;
//import org.apache.commons.jxpath.JXPathContext;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.MvdXML;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.ObjectFactory;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Rules;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.TemplateRule;

public class MvdXMLParser {

	private MvdXML mvdXML;
	private List<AttributeRule> attributeRules = new ArrayList<AttributeRule>();
	private List<TemplateRule> templateRules = new ArrayList<TemplateRule>();
	
	public MvdXML getMvdXML() {
		return mvdXML;
	}

	public MvdXMLParser(String fileName, ClassLoader classLoader) throws JAXBException, FileNotFoundException {
		this(new FileInputStream(new File(fileName)), classLoader);
	}
	
	public MvdXMLParser(InputStream inputStream, ClassLoader classLoader) throws JAXBException {
		System.out.println(classLoader);
		JAXBContext mvdXMLSchema = JAXBContext.newInstance("org.buildingsmart_tech.mvdxml.mvdxml1_1", classLoader);
		Unmarshaller unmarshaller = mvdXMLSchema.createUnmarshaller();
		unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
		StreamSource streamSource = new StreamSource(inputStream);
		JAXBElement<MvdXML> root = unmarshaller.unmarshal(streamSource, MvdXML.class);
		this.mvdXML = root.getValue();
	}

	// Generate ConceptRoot list.
	public List<ConceptRoot> extractConceptRoots() throws JAXBException {
		MvdXML.Views views = getMvdXML().getViews();
		List<ModelView> modelViews = views.getModelView();
		List<ModelView.Roots> rootsCollection = new ArrayList<ModelView.Roots>();

		for (ModelView modelView : modelViews) {
			ModelView.Roots roots = modelView.getRoots();
			rootsCollection.add(roots);
		}

		List<ConceptRoot> conceptRoots = new ArrayList<ConceptRoot>();
		for (ModelView.Roots roots : rootsCollection) {
			conceptRoots.addAll(roots.getConceptRoot());
		}
		return conceptRoots;
	}

	public List<ConceptTemplate> getTemplates() throws JAXBException {
		List<ConceptTemplate> conceptTemplates = getMvdXML().getTemplates()
				.getConceptTemplate();
		return conceptTemplates;
	}

	public List<MVDConstraint> generateConceptTrees() throws JAXBException {
		List<MVDConstraint> conceptTrees = new ArrayList<MVDConstraint>();
		for (ConceptRoot conceptRoot : extractConceptRoots()) {
			List<Concept> concepts = conceptRoot.getConcepts().getConcept();
			for (Concept concept : concepts) {
				String templateRef = concept.getTemplate().getRef();
				for (ConceptTemplate conceptTemplate : getTemplates()) {
					if (conceptTemplate.getUuid().equals(templateRef)) {
						conceptTrees.add(new MVDConstraint(conceptRoot,
								concept, conceptTemplate));
					}
				}
			}
		}
		return conceptTrees;
	}


	private void parseConceptTemplate(
			ConceptTemplate conceptTemplate) {
		Rules rules = conceptTemplate.getRules();
		List<JAXBElement<? extends AbstractRule>> abstractRules = rules
				.getAbstractRule();
		for (JAXBElement<? extends AbstractRule> jAXBElementRule : abstractRules) {
			AttributeRule attributeRule = (AttributeRule) jAXBElementRule
					.getValue();
			attributeRules.add(attributeRule);
			if (conceptTemplate.getSubTemplates() != null) {
				List<ConceptTemplate> conceptTemplates = conceptTemplate
						.getSubTemplates().getConceptTemplate();
				for (ConceptTemplate subConceptTemplate : conceptTemplates) {
					parseConceptTemplate(subConceptTemplate);
				}
			}

		}
	}


	public void parseConcept(Concept concept) {
		Rules rules = concept.getRules();
		List<JAXBElement<? extends AbstractRule>> abstractRules = rules
				.getAbstractRule();
		for (JAXBElement<? extends AbstractRule> jAXBElementRule : abstractRules) {
			TemplateRule templateRule = (TemplateRule) jAXBElementRule
					.getValue();
			templateRules.add(templateRule);
		}
		if (concept.getSubConcepts() != null) {
			List<Concept> subconcepts = concept.getSubConcepts().getConcept();
			for (Concept subconcept : subconcepts) {
				parseConcept(subconcept);
			}
		}
	}

	public List<AttributeRule.Constraints.Constraint> getConstraints(
			AttributeRule attributeRule) {
		List<AttributeRule.Constraints.Constraint> constraints = attributeRule
				.getConstraints().getConstraint();
		return constraints;
	}

	public List<EntityRule.Constraints.Constraint> getConstraints(
			EntityRule entityRule) {
		List<EntityRule.Constraints.Constraint> constraints = entityRule
				.getConstraints().getConstraint();
		return constraints;
	}

	
/*	private void buildIdMap(){
		JXPathContext context = JXPathContext.newContext(mvdXML);
   
	}

	public void getConstraints(ExchangeRequirement er) {
		JXPathContext context = JXPathContext.newContext(mvdXML);
		List<Concept> concepts = (List<Concept>) context
				.getValue("Views/ModelView/Roots/ConceptRoot/Concepts/Concept[@Requirements/Requirement/exchangeRequirement="
						+ er.getUuid());
        for (Concept concept:concepts){
        
        }
	}*/

	public class TemplateParameter {
		private String parameter;
		private String value;

		public String getParameter() {
			return parameter;
		}

		public void setParameter(String parameter) {
			this.parameter = parameter;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public TemplateParameter(String parameter, String value) {
			this.parameter = parameter;
			this.value = value;
		}
	}

}
