package suite.pkgmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import suite.pkgmanager.actions.ExecCommandAction;
import suite.pkgmanager.actions.ExtractFileAction;
import suite.pkgmanager.actions.InstallAction;
import suite.streamlet.Read;
import suite.util.FileUtil;
import suite.util.Pair;
import suite.wildcard.WildcardUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PackageManager {

	private ObjectMapper objectMapper = new ObjectMapper();
	private Log log = LogFactory.getLog(getClass());

	public boolean install(String packageFilename) throws IOException {
		PackageManifest packageManifest = getPackageManifest(packageFilename);

		List<Pair<String, String>> filenameMappings = Read.from(packageManifest.getFilenameMappings()) //
				.sort((p0, p1) -> p1.t0.length() - p0.t0.length()) //
				.toList();

		List<InstallAction> installActions = new ArrayList<>();

		try (ZipFile zipFile = new ZipFile(packageFilename)) {
			installActions.addAll(Read.from(FileUtil.listZip(zipFile)) //
					.map(filename0 -> {
						String filename1 = filename0;
						for (Pair<String, String> filenameMapping : filenameMappings) {
							String match[];
							if ((match = WildcardUtil.match(filenameMapping.t0, filename1)) != null) {
								filename1 = WildcardUtil.apply(filenameMapping.t1, match);
								break;
							}
						}
						return new ExtractFileAction(packageFilename, filename0, filename1);
					}) //
					.toList());
		}

		installActions.addAll(Read.from(packageManifest.getCommands()) //
				.map(command -> new ExecCommandAction(command.getInstallCommand(), command.getUninstallCommand())) //
				.toList());

		int progress = 0;
		boolean isSuccess = true;

		for (; progress < installActions.size(); progress++)
			try {
				installActions.get(progress).act();
			} catch (Exception ex) {
				log.error("Error during installation", ex);
				isSuccess = false;
				break;
			}

		if (!isSuccess)
			progress = unact(installActions, progress);

		PackageMemento packageMemento = new PackageMemento(packageManifest, installActions);

		// TODO saves memento
		packageMemento.getClass();

		return isSuccess;
	}

	private int unact(List<InstallAction> installActions, int i) {
		for (; i > 0; i--)
			try {
				installActions.get(i - 1).unact();
			} catch (Exception ex) {
				log.error("Error during un-installation", ex);
			}
		return i;
	}

	private PackageManifest getPackageManifest(String packageFilename) throws IOException {
		try (ZipFile zipFile = new ZipFile(packageFilename); InputStream fis = zipFile.getInputStream(zipFile.getEntry("pm.json"))) {
			return objectMapper.readValue(fis, PackageManifest.class);
		}
	}

}
