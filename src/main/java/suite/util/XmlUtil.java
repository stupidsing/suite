package suite.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XmlUtil {

	public String format(String xml) throws ParserConfigurationException, SAXException {
		Document document;

		try {
			document = parse(xml);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return format(document);
	}

	public String format(Document document) {
		OutputFormat format = new OutputFormat(document);
		format.setLineWidth(132);
		format.setIndenting(true);
		format.setIndent(2);

		Writer out = new StringWriter();

		try {
			new XMLSerializer(out, format).serialize(document);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return out.toString();
	}

	public Document parse(String in) throws IOException, SAXException {
		return parse(new StringReader(in));
	}

	public Document parse(Reader reader) throws IOException, SAXException {
		InputSource is = new InputSource(reader);
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex);
		}
	}

}
