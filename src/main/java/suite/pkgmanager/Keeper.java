package suite.pkgmanager;

import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import primal.Nouns.Tmp;
import primal.Verbs.DeleteFile;
import primal.Verbs.ReadFile;
import primal.Verbs.WriteFile;
import suite.inspect.Mapify;
import suite.node.util.Singleton;

/**
 * Keeps track of the package installed in local machine.
 *
 * @author ywsing
 */
public class Keeper {

	private Path keeperDir = Tmp.path("keeper");

	private ObjectMapper om;
	private Mapify mapify = Singleton.me.mapify;

	public Keeper(ObjectMapper om) {
		this.om = om;
	}

	public PackageMemento loadPackageMemento(String packageName) {
		return ReadFile //
				.from(keeperDir.resolve(packageName)) //
				.doRead(is -> mapify.unmapify(PackageMemento.class, om.readValue(is, Map.class)));
	}

	public void savePackageMemento(PackageMemento packageMemento) {
		var packageName = packageMemento.getPackageManifest().getName();

		WriteFile //
				.to(keeperDir.resolve(packageName)) //
				.doWrite(os -> om.writeValue(os, mapify.mapify(PackageMemento.class, packageMemento)));
	}

	public void removePackageMemento(String packageName) {
		DeleteFile.on(keeperDir.resolve(packageName));
	}

}
