package suite.pkgmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import suite.Defaults;
import suite.inspect.Mapify;
import suite.node.util.Singleton;
import suite.os.FileUtil;

/**
 * Keeps track of the package installed in local machine.
 *
 * @author ywsing
 */
public class Keeper {

	private Path keeperDir = Defaults.tmp("keeper");

	private ObjectMapper objectMapper;
	private Mapify mapify = Singleton.me.mapify;

	public Keeper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PackageMemento loadPackageMemento(String packageName) throws IOException {
		try (var is = Files.newInputStream(keeperDir.resolve(packageName))) {
			return mapify.unmapify(PackageMemento.class, objectMapper.readValue(is, Map.class));
		}
	}

	public void savePackageMemento(PackageMemento packageMemento) throws IOException {
		var packageName = packageMemento.getPackageManifest().getName();

		try (var os = FileUtil.out(keeperDir.resolve(packageName))) {
			objectMapper.writeValue(os, mapify.mapify(PackageMemento.class, packageMemento));
		}
	}

	public void removePackageMemento(String packageName) throws IOException {
		Files.delete(keeperDir.resolve(packageName));
	}

}
