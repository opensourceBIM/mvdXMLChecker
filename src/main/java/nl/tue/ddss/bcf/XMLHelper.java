/*
 * XMLHelper.java
 *
 * Created on June 7, 2007, 11:03 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package nl.tue.ddss.bcf;

import java.util.LinkedList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author bwjoran
 */
public class XMLHelper {
    static private XPath xpath;
    
    private XMLHelper() {
    }

    static public Element element(Node context, String expression) {
        try {
            return (Element) xpath.evaluate(expression, context, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            throw new RuntimeException("invalid xpath expresion used");
        }
    }
    
    static public String string(Node context, String expression) {
        try {
            String result = (String) xpath.evaluate(expression, context, XPathConstants.STRING);            
            if(result == null || result.length() == 0) return null;
            else return result;
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            throw new RuntimeException("invalid xpath expresion used");
        }
    }

    static public String string(Node context, String expression, String defaultValue) {
        try {
            String result = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
            if(result == null || result.length() == 0) return defaultValue;
            else return result;
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            throw new RuntimeException("invalid xpath expresion used");
        }
    }
    
    static public Boolean bool(Node context, String expression) {
        String value = string(context, expression);
        if(value != null) return !value.equals("0");
        else return null;
    }

    static public boolean bool(Node context, String expression, boolean defaultValue) {
        String value = string(context, expression);
        if(value == null) return defaultValue;
        return !value.equals("0");
    }
    
    public static Double number(Element context, String expression) {
        String value = string(context, expression);
        if(value == null) return null;
        return Double.parseDouble(value);
    }

    public static Double number(Element context, String expression, double defaultValue) {
        String value = string(context, expression);
        if(value == null) return defaultValue;
        return Double.parseDouble(value);
    }
    
    static public List<Element> elements(Node context, String expression) {
        try {
            NodeList nodeList = (NodeList) xpath.evaluate(expression, context, XPathConstants.NODESET);
            List<Element> result = new LinkedList<Element>();
            for(int i = 0, len = nodeList.getLength(); i < len; i++) {
                result.add((Element)nodeList.item(i));
            }
            return result;
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            throw new RuntimeException("invalid xpath expresion used");
        }        
    }
    
    static public void textContentTag(Element parent, String tagName, String content) {
        Element tag = parent.getOwnerDocument().createElement(tagName);
        tag.setTextContent(content);
        parent.appendChild(tag);
    }

    static public void booleanContentTag(Element parent, String tagName, boolean content) {
        Element tag = parent.getOwnerDocument().createElement(tagName);
        if(content) tag.setTextContent("1");
        else tag.setTextContent("0");
        parent.appendChild(tag);
    }

    public static void cdataContentTag(Element parent, String tagName, String cdata) {
        Element tag = parent.getOwnerDocument().createElement(tagName);
        tag.appendChild(parent.getOwnerDocument().createCDATASection(cdata));
        parent.appendChild(tag);
    }
    
    static public Element createElement(Element parent, String tagName) {
        Element tag = parent.getOwnerDocument().createElement(tagName);
        parent.appendChild(tag);  
        return tag;
    }
        
    static {
        xpath = XPathFactory.newInstance().newXPath();
    }
}
