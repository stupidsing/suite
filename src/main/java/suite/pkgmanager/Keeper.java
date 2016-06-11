package suite.pkgmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import suite.Constants;
import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.os.FileUtil;

/**
 * Keeps track of the package installed in local machine.
 *
 * @author ywsing
 */
public class Keeper {

	private Path keeperDir = Constants.tmp.resolve("keeper");

	private ObjectMapper objectMapper;
	private Mapify mapify = new Mapify(new Inspect());

	public Keeper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PackageMemento loadPackageMemento(String packageName) throws IOException {
		try (InputStream is = Files.newInputStream(keeperDir.resolve(packageName))) {
			return mapify.unmapify(PackageMemento.class, objectMapper.readValue(is, Map.class));
		}
	}

	public void savePackageMemento(PackageMemento packageMemento) throws IOException {
		String packageName = packageMemento.getPackageManifest().getName();

		try (OutputStream os = FileUtil.out(keeperDir.resolve(packageName))) {
			objectMapper.writeValue(os, mapify.mapify(PackageMemento.class, packageMemento));
		}
	}

	public void removePackageMemento(String packageName) throws IOException {
		Files.delete(keeperDir.resolve(packageName));
	}

}
