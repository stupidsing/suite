package suite.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class XmlUtil {

	private DocumentBuilder documentBuilder;
	private DOMImplementationLS di;
	private LSSerializer lss;

	public class XmlPath {
		public XmlPath parent;
		public int nodeType;
		public String namespaceUri;
		public String localName;
		public String text;

		public XmlPath(XmlPath parent, int nodeType, String namespaceUri, String localName, String text) {
			this.parent = parent;
			this.nodeType = nodeType;
			this.namespaceUri = namespaceUri;
			this.localName = localName;
			this.text = text;
		}
	}

	public XmlUtil() throws IOException {
		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			di = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
			lss = di.createLSSerializer();
			lss.getDomConfig().setParameter("format-pretty-print", true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public String format(String xml) throws SAXException {
		try (InputStream is = new ByteArrayInputStream(xml.getBytes(FileUtil.charset)); Writer writer = new StringWriter()) {
			LSOutput lso = di.createLSOutput();
			lso.setEncoding(FileUtil.charset.name());
			lso.setCharacterStream(writer);

			lss.write(documentBuilder.parse(is), lso);
			return writer.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Streamlet<XmlPath> parse(InputStream is) throws SAXException {
		return Rethrow.ex(() -> {
			Document document = documentBuilder.parse(is);
			Element element = document.getDocumentElement();
			return traverse(null, element);
		});
	}

	private Streamlet<XmlPath> traverse(XmlPath parent, Node node) {
		short nodeType = node.getNodeType();
		XmlPath xp = createXmlPath(parent, node);
		if (nodeType != Node.ATTRIBUTE_NODE && nodeType != Node.TEXT_NODE)
			return Read.from(source(node.getChildNodes())).concatMap(e -> traverse(xp, e));
		else
			return Read.from(xp);
	}

	private Source<Node> source(NodeList nodeList) {
		return new Source<Node>() {
			private int i = 0;

			public Node source() {
				return i < nodeList.getLength() ? nodeList.item(i) : null;
			}
		};
	}

	private XmlPath createXmlPath(XmlPath parent, Node node) {
		short nodeType = node.getNodeType();
		return new XmlPath(parent //
				, nodeType //
				, node.getNamespaceURI() //
				, node.getLocalName() //
				, nodeType == Node.TEXT_NODE ? node.getTextContent() : null);
	}

}
