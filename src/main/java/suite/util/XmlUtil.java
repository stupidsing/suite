package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.fp.Funs.Source;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class XmlUtil {

	private DocumentBuilder documentBuilder;
	private DOMImplementationLS di;
	private LSSerializer lss;

	public interface XmlNode {
		public int nodeType();

		public String namespaceUri();

		public String nodeName();

		public String text();

		public Streamlet<XmlNode> children();

		public Streamlet<XmlNode> children(String tagName);
	}

	public XmlUtil() {
		ex(() -> {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			di = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
			lss = di.createLSSerializer();
			lss.getDomConfig().setParameter("format-pretty-print", true);
			return lss;
		});
	}

	public String format(String xml) throws SAXException {
		try (var is = new ByteArrayInputStream(xml.getBytes(Utf8.charset)); var writer = new StringWriter()) {
			var lso = di.createLSOutput();
			lso.setEncoding(Utf8.charset.name());
			lso.setCharacterStream(writer);

			lss.write(documentBuilder.parse(is), lso);
			return writer.toString();
		} catch (IOException ex) {
			return fail(ex);
		}
	}

	public XmlNode read(InputStream is) throws SAXException {
		return node(ex(() -> {
			var document = documentBuilder.parse(is);
			document.normalize();
			return document;
		}));
	}

	private XmlNode node(Node n) {
		return new XmlNode() {
			public int nodeType() {
				return n.getNodeType();
			}

			public String namespaceUri() {
				return n.getNamespaceURI();
			}

			public String nodeName() {
				return n.getNodeName();
			}

			public String text() {
				return n.getTextContent();
			}

			public Streamlet<XmlNode> children() {
				return xmlNodes(n.getChildNodes());
			}

			public Streamlet<XmlNode> children(String tagName) {
				var nodeList = n.getChildNodes();

				return Read.from(() -> new Source<XmlNode>() {
					private int i = 0;

					public XmlNode g() {
						Node child;
						while (i < nodeList.getLength())
							if (Equals.string((child = nodeList.item(i++)).getNodeName(), tagName))
								return node(child);
						return null;
					}
				});
			}

			private Streamlet<XmlNode> xmlNodes(NodeList nodeList) {
				return Read.from(() -> new Source<XmlNode>() {
					private int i = 0;

					public XmlNode g() {
						return i < nodeList.getLength() ? node(nodeList.item(i)) : null;
					}
				});
			}
		};
	}

}
