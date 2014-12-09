package suite.pkgmanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import suite.util.FileUtil;
import suite.util.InspectUtil;
import suite.util.MapifyUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Keeps track of the package installed in local machine.
 * 
 * @author ywsing
 */
public class Keeper {

	private String keeperDirectory = FileUtil.tmp + "/keeper";

	private ObjectMapper objectMapper;
	private MapifyUtil mapifyUtil = new MapifyUtil(new InspectUtil());

	public Keeper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PackageMemento loadPackageMemento(String packageName) throws IOException {
		try (InputStream is = new FileInputStream(keeperDirectory + "/" + packageName)) {
			return mapifyUtil.unmapify(PackageMemento.class, objectMapper.readValue(is, HashMap.class));
		}
	}

	public void savePackageMemento(PackageMemento packageMemento) throws IOException {
		String packageName = packageMemento.getPackageManifest().getName();

		try (OutputStream os = FileUtil.out(keeperDirectory + "/" + packageName)) {
			objectMapper.writeValue(os, mapifyUtil.mapify(PackageMemento.class, packageMemento));
		}
	}

}
