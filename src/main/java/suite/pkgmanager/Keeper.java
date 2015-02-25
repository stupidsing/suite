package suite.pkgmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.util.os.FileUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Keeps track of the package installed in local machine.
 * 
 * @author ywsing
 */
public class Keeper {

	private String keeperDirectory = FileUtil.tmp + "/keeper";

	private ObjectMapper objectMapper;
	private Mapify mapifyUtil = new Mapify(new Inspect());

	public Keeper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PackageMemento loadPackageMemento(String packageName) throws IOException {
		try (InputStream is = new FileInputStream(keeperDirectory + "/" + packageName)) {
			return mapifyUtil.unmapify(PackageMemento.class, objectMapper.readValue(is, Map.class));
		}
	}

	public void savePackageMemento(PackageMemento packageMemento) throws IOException {
		String packageName = packageMemento.getPackageManifest().getName();

		try (OutputStream os = FileUtil.out(keeperDirectory + "/" + packageName)) {
			objectMapper.writeValue(os, mapifyUtil.mapify(PackageMemento.class, packageMemento));
		}
	}

	public boolean removePackageMemento(String packageName) {
		return new File(keeperDirectory + "/" + packageName).delete();
	}

}
