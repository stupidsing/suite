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
import suite.util.Fail;

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

	public PackageMemento loadPackageMemento(String packageName) {
		try (var is = Files.newInputStream(keeperDir.resolve(packageName))) {
			return mapify.unmapify(PackageMemento.class, objectMapper.readValue(is, Map.class));
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

	public void savePackageMemento(PackageMemento packageMemento) {
		var packageName = packageMemento.getPackageManifest().getName();

		FileUtil //
				.out(keeperDir.resolve(packageName)) //
				.write(os -> objectMapper.writeValue(os, mapify.mapify(PackageMemento.class, packageMemento)));
	}

	public void removePackageMemento(String packageName) {
		try {
			Files.delete(keeperDir.resolve(packageName));
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

}
