package suite.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

public class XmlUtil {

	private DocumentBuilder documentBuilder;
	private DOMImplementationLS di;
	private LSSerializer lss;

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

}
