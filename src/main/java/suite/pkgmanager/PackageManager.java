package suite.pkgmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import suite.os.FileUtil;
import suite.parser.Wildcard;
import suite.pkgmanager.action.ExecCommandAction;
import suite.pkgmanager.action.ExtractFileAction;
import suite.pkgmanager.action.InstallAction;
import suite.streamlet.Read;
import suite.util.Fail;

public class PackageManager {

	private ObjectMapper objectMapper = new ObjectMapper();
	private Keeper keeper = new Keeper(objectMapper);
	private Log log = LogFactory.getLog(getClass());

	public boolean install(String packageFilename) {
		var packageManifest = getPackageManifest(packageFilename);

		var filenameMappings = Read //
				.from2(packageManifest.getFilenameMappings()) //
				.sort((p0, p1) -> p1.t0.length() - p0.t0.length()) //
				.toList();

		var installActions = new ArrayList<InstallAction>();

		try (var zipFile = new ZipFile(packageFilename)) {
			installActions.addAll(Read //
					.from(FileUtil.listZip(zipFile)) //
					.map(filename0 -> {
						var filename1 = filename0;
						for (var filenameMapping : filenameMappings) {
							String[] match;
							if ((match = Wildcard.match(filenameMapping.t0, filename1)) != null) {
								filename1 = Wildcard.apply(filenameMapping.t1, match);
								break;
							}
						}
						return new ExtractFileAction(packageFilename, filename0, filename1);
					}) //
					.toList());
		} catch (IOException ex) {
			return Fail.t(ex);
		}

		installActions.addAll(Read //
				.from(packageManifest.getCommands()) //
				.map(command -> new ExecCommandAction(command.getInstallCommand(), command.getUninstallCommand())) //
				.toList());

		var progress = 0;
		var isSuccess = true;

		for (; progress < installActions.size(); progress++)
			try {
				installActions.get(progress).act();
			} catch (Exception ex) {
				log.error("Error during installation", ex);
				isSuccess = false;
				break;
			}

		if (isSuccess)
			keeper.savePackageMemento(new PackageMemento(packageManifest, installActions));
		else
			progress = unact(installActions, progress);

		return isSuccess;
	}

	public boolean uninstall(String packageName) {
		var packageMemento = keeper.loadPackageMemento(packageName);
		var installActions = packageMemento.getInstallActions();
		unact(installActions, installActions.size());
		keeper.removePackageMemento(packageName);
		return true;
	}

	private int unact(List<InstallAction> installActions, int progress) {
		for (; 0 < progress; progress--)
			try {
				installActions.get(progress - 1).unact();
			} catch (Exception ex) {
				log.error("Error during un-installation", ex);
			}
		return progress;
	}

	private PackageManifest getPackageManifest(String packageFilename) {
		try (var zipFile = new ZipFile(packageFilename); var fis = zipFile.getInputStream(zipFile.getEntry("pm.json"))) {
			return objectMapper.readValue(fis, PackageManifest.class);
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

}
