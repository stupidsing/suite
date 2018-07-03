package suite.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import suite.cfg.Defaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class XmlUtil {

	private DocumentBuilder documentBuilder;
	private DOMImplementationLS di;
	private LSSerializer lss;

	public interface XmlNode {
		public int nodeType();

		public String namespaceUri();

		public String localName();

		public String text();

		public Streamlet<XmlNode> children();

		public Streamlet<XmlNode> children(String tagName);
	}

	public XmlUtil() {
		Rethrow.ex(() -> {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			di = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
			lss = di.createLSSerializer();
			lss.getDomConfig().setParameter("format-pretty-print", true);
			return lss;
		});
	}

	public String format(String xml) throws SAXException {
		try (var is = new ByteArrayInputStream(xml.getBytes(Defaults.charset)); var writer = new StringWriter()) {
			var lso = di.createLSOutput();
			lso.setEncoding(Defaults.charset.name());
			lso.setCharacterStream(writer);

			lss.write(documentBuilder.parse(is), lso);
			return writer.toString();
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

	public XmlNode read(InputStream is) throws SAXException {
		return node(Rethrow.ex(() -> {
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

			public String localName() {
				return n.getLocalName();
			}

			public String text() {
				return n.getTextContent();
			}

			public Streamlet<XmlNode> children() {
				return xmlNodes(n.getChildNodes());
			}

			public Streamlet<XmlNode> children(String tagName) {
				return xmlNodes(((Element) n).getElementsByTagName(tagName));
			}

			private Streamlet<XmlNode> xmlNodes(NodeList nodeList) {
				return Read.from(() -> new Source<XmlNode>() {
					private int i = 0;

					public XmlNode source() {
						return i < nodeList.getLength() ? node(nodeList.item(i)) : null;
					}
				});
			}
		};
	}

}
