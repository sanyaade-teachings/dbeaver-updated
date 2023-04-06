package org.jkiss.dbeaver.antlr.model.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.jkiss.dbeaver.antlr.model.internal.CustomXPathUtils.*;

public class CustomXPathFunctionResolver implements XPathFunctionResolver {

    @FunctionalInterface
    private interface MyXPathFunction {
        Object evaluate(List<?> args) throws XPathExpressionException;
    }

    private static java.util.Map.Entry<String, XPathFunction> xfunction(String name, MyXPathFunction impl) {
        return java.util.Map.entry(name, new XPathFunction() {
            @Override
            public Object evaluate(List<?> args) throws XPathFunctionException {
                try {
                    return impl.evaluate(args);
                } catch (XPathExpressionException ex) {
                    throw new XPathFunctionException(ex);
                }
            }
        });
    }
    
    private final Map<String, XPathFunction> functionByName = Map.ofEntries(
        xfunction("echo", args -> {
            for (Object o : args) {
                if (o instanceof NodeList) {
                    NodeList nodeList = (NodeList)o;
                    if (nodeList.getLength() == 0) {
                        System.out.println("[]");
                    } else {
                        System.out.println(
                            streamOf(nodeList).map(n -> "  " + n.getLocalName() + ": \"" + n.getNodeValue() + "\"")
                                .collect(Collectors.joining(",\n", "[\n", "\n]"))
                        );
                    }
                } else if (o instanceof Node) {
                    Node node = (Node) o;
                    System.out.println(node.getLocalName() + ": \"" + node.getNodeValue() + "\"");
                } else {
                    System.out.println(o);
                }
            }
            return args.size() > 0 ? args.get(0) : null;
        }),
        xfunction("rootOf", args -> {
            if (args.size() > 0 && args.get(0) instanceof NodeList) {
                NodeList nodeList = (NodeList) args.get(0);
                if (nodeList.getLength() > 0) {
                    Node node = nodeList.item(0);
                    while (node.getParentNode() != null) {
                        node = node.getParentNode();
                    }
                    return node;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }),
        xfunction("flatten", args -> {
            final String signatureDescr = "flatten(roots:NodeList, stepExpr:String, justLeaves:bool = false, incudeRoot:bool = ture)";
            if (args.size() < 2) {
                throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
            } else if (args.size() > 4) {
                throw new XPathFunctionException("No more than four arguments required for " + signatureDescr);
            } else {
                NodeList roots = (NodeList) args.get(0);
                XPathExpression stepExpr = prepareExpr(args.get(1).toString());
                boolean justLeaves = args.size() > 2 ? (Boolean) args.get(2) : false;
                boolean includeRoot = args.size() > 3 ? (Boolean) args.get(3) : true;
                
                NodesList<Node> result = new NodesList<>(); 
                if (includeRoot && !justLeaves) {
                    result.ensureCapacity(roots.getLength());
                }

                for (Node root : iterableOf(roots)) {
                    if (includeRoot && !justLeaves) {
                        result.add(root);
                    } else {
                        flattenExclusiveImpl(root, stepExpr, justLeaves, result);
                    }
                }
                
                return result;
            }
        }),
        xfunction("joinStrings", args -> {
            final String signatureDescr = "joinStrings(separator:String, nodes...:NodeList)";
            if (args.size() < 2) {
                throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
            } else {
                StringBuilder sb = new StringBuilder();
                String separator = args.get(0).toString();
                int count = 0;
                for (int i = 1; i < args.size(); i++) {
                    for (Node node : iterableOf((NodeList) args.get(i))) {
                        if (count > 0) {
                            sb.append(separator);
                        }
                        sb.append(node.getTextContent());
                        count++;
                    }
                }
                return sb.toString();
            }
        })
    );
    
    private final XPath xpath;
    private final Map<String, XPathExpression> exprs = new HashMap<>();
    
    public CustomXPathFunctionResolver(XPath xpath) {
        this.xpath = xpath;
    }
    
    private XPathExpression prepareExpr(String exprStr) throws XPathExpressionException {
        XPathExpression expr = exprs.get(exprStr);
        if (expr == null) {
            expr = xpath.compile(exprStr); 
            exprs.put(exprStr, expr);
        }
        return expr;
    }
    
    @Override
    public XPathFunction resolveFunction(QName functionName, int arity) {
        return functionByName.get(functionName.getLocalPart());
    }
    
}
