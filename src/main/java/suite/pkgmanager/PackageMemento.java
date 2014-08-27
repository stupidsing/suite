package suite.pkgmanager;

import java.util.List;

import suite.pkgmanager.actions.InstallAction;

public class PackageMemento {

	private PackageManifest packageManifest;
	private List<InstallAction> installActions;

	public PackageManifest getPackageManifest() {
		return packageManifest;
	}

	public List<InstallAction> getInstallActions() {
		return installActions;
	}

}
