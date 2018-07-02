package suite.pkgmanager;

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

	public PackageMemento loadPackageMemento(String packageName) {
		return FileUtil //
				.in(keeperDir.resolve(packageName)) //
				.doRead(is -> mapify.unmapify(PackageMemento.class, objectMapper.readValue(is, Map.class)));
	}

	public void savePackageMemento(PackageMemento packageMemento) {
		var packageName = packageMemento.getPackageManifest().getName();

		FileUtil //
				.out(keeperDir.resolve(packageName)) //
				.doWrite(os -> objectMapper.writeValue(os, mapify.mapify(PackageMemento.class, packageMemento)));
	}

	public void removePackageMemento(String packageName) {
		FileUtil.delete(keeperDir.resolve(packageName));
	}

}
