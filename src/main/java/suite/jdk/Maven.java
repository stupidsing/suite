package suite.jdk;

import java.net.URL;

import suite.http.HttpUtil;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.XmlUtil;

public class Maven {

	public String getUrl(String groupId, String artifactId, String version) {
		return getUrl("http://repo.maven.apache.org/maven2/", groupId, artifactId, version);
	}

	public String getLatestUrl(String m2repo, String groupId, String artifactId) {
		URL url = To.url(m2repo + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml");

		String version = Rethrow.ex(() -> {
			return new XmlUtil() //
					.read(To.inputStream(HttpUtil.get(url).out)) //
					.children("metadata") //
					.uniqueResult() //
					.children("versioning") //
					.uniqueResult() //
					.children("latest") //
					.uniqueResult() //
					.text();
		});

		return getUrl(m2repo, groupId, artifactId, version);
	}

	private String getUrl(String m2repo, String groupId, String artifactId, String version) {
		return m2repo + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
	}

}
