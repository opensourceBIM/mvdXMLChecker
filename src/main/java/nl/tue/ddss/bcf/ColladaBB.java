/*
 * ColladaBB.java
 *
 * Created on May 31, 2007, 2:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package nl.tue.ddss.bcf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.vecmath.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 *
 * @author bwjoran
 */
public class ColladaBB {
    Map<String, BoundingBox> bdMap=new HashMap<String, BoundingBox>();
    BoundingBox boundingBox;
    Map<String, Element> idMap = new HashMap<String, Element>();
    Map<String, Element> guidMap = new HashMap<String, Element>();
    Document doc;
    XPath xpath;
    
    /**
     * Creates a new instance of ColladaBB
     */
    private ColladaBB() {
        xpath = XPathFactory.newInstance().newXPath();
    }
    
    private void geometry(Matrix4d matrix, Element geometry) {
        Element mesh = XMLHelper.element(geometry, "mesh");
        if(mesh == null) return;
        
        Element vertices = XMLHelper.element(mesh, "vertices");
        if(vertices == null) throw new RuntimeException("found a mesh without vertices");
        
        Element input = XMLHelper.element(vertices, "input[@semantic='POSITION']");
        if(input == null) throw new RuntimeException("found a vertices without an input with POSITION semantic");
        
        Element source = getSource(input.getAttribute("source"));
        if(source == null) throw new RuntimeException("could not find source of input");
        
        Element technique_common = XMLHelper.element(source, "technique_common");
        if(technique_common == null) throw new RuntimeException("could not find common technique for vertices in mesh");
        
        Element accessor = XMLHelper.element(technique_common, "accessor");
        if(accessor == null) throw new RuntimeException("could not find accessor for vertices in mesh");        

        int stride;
        if(!accessor.getAttribute("stride").isEmpty()) stride = Integer.parseInt(accessor.getAttribute("stride"));
        else stride = 1;
        
        int offset;
        if(!accessor.getAttribute("offset").isEmpty()) offset = Integer.parseInt(accessor.getAttribute("offset"));
        else offset = 0;
        
        int count = Integer.parseInt(accessor.getAttribute("count"));
        
        List<Element> params = childElements(accessor);
        int accessLength = params.size();
        int indexX = -1;
        int indexY = -1;
        int indexZ = -1;
        
        for(int i = 0; i < accessLength; i++) {
            Element param = params.get(i);
            if(!param.getTagName().equals("param")) throw new RuntimeException("Found something else then a parameter in an accessor");
            if(!param.getAttribute("type").equals("float")) throw new RuntimeException("I can only understand vertices of the type float");
                 if(param.getAttribute("name").equals("X")) indexX = i;
            else if(param.getAttribute("name").equals("Y")) indexY = i;
            else if(param.getAttribute("name").equals("Z")) indexZ = i;
        }
        if(indexX == -1 || indexY == -1 || indexZ == -1) throw new RuntimeException("I only understand 3d mesh data");
        
        double[] floats = float_array(getSource(accessor.getAttribute("source")));
        
        int j = offset;
        for(int i = 0; i < count ;i++) {
            Point3d p = new Point3d(floats[j + indexX], floats[j + indexY], floats[j + indexZ]);
            matrix.transform(p);
            boundingBox.add(p);
            j += stride;
        }
    }
    
    private void node(Matrix4d matrix, Element node) {
        for(Element element : childElements(node)) {
            if(element.getTagName().equals("matrix")) {
                Matrix4d m = matrix(element);
                m.mul(matrix, m);
                matrix = m;
            }
            else if(element.getTagName().equals("translate")) {
                Vector3d t = vector(element);
                Matrix4d m = new Matrix4d();
                m.setIdentity();
                m.setTranslation(t);
                m.mul(matrix, m);
                matrix = m;     
            }
            else if(element.getTagName().equals("rotate")) {
                AxisAngle4d r = rotate(element);
                r.setAngle(Math.toRadians(r.getAngle()));
                Matrix4d m = new Matrix4d();
                m.setIdentity();
                m.setRotation(r);
                m.mul(matrix, m);
                matrix = m;     
            }
            else if(element.getTagName().equals("scale")) {
                Vector3d s = vector(element);
                Matrix4d m = new Matrix4d();
                m.setM00(s.x);
                m.setM11(s.y);
                m.setM22(s.z);
                m.setM33(1.0);
                m.mul(matrix, m);
                matrix = m;     
            }
            else if(element.getTagName().equals("scew")) {
                throw new RuntimeException("encountered a scew element that is not supported");
            }
            else if(element.getTagName().equals("node")) {
                node(matrix, element);
            }
            else if(element.getTagName().equals("instance_node")) {
                node(matrix, getSource(element.getAttribute("url")));
            }
            else if(element.getTagName().equals("instance_geometry")) {
                geometry(matrix, getSource(element.getAttribute("url")));
            }            
        }
    }
    
    private void visual_scene(Matrix4d matrix, List<Element> nodes) {
        for(Element element : nodes) {            	
                node(matrix, element);
        }
    }
    
    private Vector3d vector(Element element) {
        String[] parts = element.getTextContent().trim().split("\\s+");
        if(parts.length != 3) throw new RuntimeException("found a vector without 3 components");        
        return new Vector3d(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }
    
    private AxisAngle4d rotate(Element element) {
        String[] parts = element.getTextContent().trim().split("\\s+");
        if(parts.length != 4) throw new RuntimeException("found a rotation without 4 components");        
        return new AxisAngle4d(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    
    private Matrix4d matrix(Element element) {
        String[] parts = element.getTextContent().trim().split("\\s+");
        if(parts.length != 16) throw new RuntimeException("found a matrix without 16 components");                
        Matrix4d m = new Matrix4d();
        for(int r = 0 ; r < 4; r++) for(int c = 0; c < 4; c++) {
            m.setElement(r, c, Double.parseDouble(parts[4 * r + c]));
        }
        
        return m;
    }
    
    private double[] float_array(Element element) {
        String[] parts = element.getTextContent().trim().split("\\s+");
        double[] float_array = new double[parts.length];
        for(int i = 0; i < parts.length; i++) {
            float_array[i] = Double.parseDouble(parts[i]);
        }
        return float_array;
    }
    
    private List<Element> childElements(Element element) {
        List<Element> elements = new LinkedList<Element>();
        if (element!=null){
        Node candidate = element.getFirstChild();
        while(candidate!=null) {
            if(candidate instanceof Element) elements.add((Element) candidate);
            candidate = candidate.getNextSibling();
        }
    }
        return elements;
    }
    
    private Element parentElement(Element element) {
        Element parent=null;        
        Node candidate = element.getParentNode();      
            if(candidate instanceof Element) parent=(Element)candidate;
        return parent;
    }
    
    
    private Element getSource(String source) {
        if(source.startsWith("#")) {
            Element result = idMap.get(source.substring(1));
            if(result == null) throw new RuntimeException("Unable to find source: "+source);
            return result;
        }
        else {
            throw new RuntimeException("unable to parse source xpointer: "+source);
        }
    }
    
    private void buildGuidMap(){
    	try {
    		NodeList instanceGeometries = (NodeList) xpath.evaluate("//instance_geometry", doc, XPathConstants.NODESET);
    		System.out.println(instanceGeometries.getLength());
            for(int i = 0, n = instanceGeometries.getLength(); i < n; i++) {
                Element instanceGeometry = (Element) instanceGeometries.item(i);
                guidMap.put(getSource(instanceGeometry.getAttribute("url")).getAttribute("name"), parentElement(instanceGeometry));
            }
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void buildIdMap() {
        try {
            NodeList idNodes = (NodeList) xpath.evaluate("//*[@id]", doc, XPathConstants.NODESET);
            for(int i = 0, n = idNodes.getLength(); i < n; i++) {
                Element idNode = (Element) idNodes.item(i);
                idMap.put(idNode.getAttribute("id"), idNode);
            }
        } catch (XPathExpressionException ex) { 
            // This never happens
            ex.printStackTrace();
        }
    }
    
    private BoundingBox getBoundingBox(Document doc, Matrix4d matrix, List<String> guids) {
        this.doc = doc;
        buildIdMap();
        buildGuidMap();
        boundingBox=new BoundingBox();
        List<Element> nodes=new LinkedList<Element>();
        for (String guid:guids){
        	nodes.add(guidMap.get(guid));
        }

        if(matrix == null) {
            matrix = new Matrix4d();
            matrix.setIdentity();
        }
        
        // Handle the unit
        Double meter = XMLHelper.number(doc.getDocumentElement(), "/COLLADA/asset/unit/@meter");
        if(meter != null) {
            Matrix4d m = new Matrix4d();
            m.setM00(meter);
            m.setM11(meter);
            m.setM22(meter);
            m.setM33(meter);
            matrix.mul(m);
        }
        visual_scene(matrix, nodes);             
        return boundingBox;
    }
            
    
    static public BoundingBox getBoundingBox(File file, Matrix4d matrix, List<String> guids) throws ParserConfigurationException, SAXException, IOException {        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ColladaBB colladaBB = new ColladaBB();
        return colladaBB.getBoundingBox(builder.parse(file), matrix, guids);
    }

    static public BoundingBox getBoundingBox(InputSource is, Matrix4d matrix,List<String> guids) throws ParserConfigurationException, SAXException, IOException {        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ColladaBB colladaBB = new ColladaBB();
        return colladaBB.getBoundingBox(builder.parse(is), matrix, guids);
    }

    static public BoundingBox getBoundingBox(InputStream is, Matrix4d matrix,List<String> guids) throws ParserConfigurationException, SAXException, IOException {        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ColladaBB colladaBB = new ColladaBB();
        return colladaBB.getBoundingBox(builder.parse(is), matrix, guids);
    }
}
