package suite.pkgmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import suite.pkgmanager.PackageManifest.Command;
import suite.pkgmanager.actions.ExecCommandAction;
import suite.pkgmanager.actions.ExtractFileAction;
import suite.pkgmanager.actions.InstallAction;
import suite.util.FileUtil;
import suite.util.Pair;
import suite.wildcard.WildcardUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PackageManager {

	private ObjectMapper objectMapper = new ObjectMapper();
	private Log log = LogFactory.getLog(getClass());

	public boolean install(String packageFilename) throws IOException {
		PackageManifest packageManifest = getPackageManifest(packageFilename);

		List<Pair<String, String>> filenameMappings = packageManifest.getFilenameMappings().entrySet().stream() //
				.map(entry -> Pair.of(entry.getKey(), entry.getValue())) //
				.sorted((p0, p1) -> p1.t0.length() - p0.t0.length()) //
				.collect(Collectors.toList());

		List<InstallAction> installActions;

		try (ZipFile zipFile = new ZipFile(packageFilename)) {
			installActions = FileUtil.listZip(zipFile).stream() //
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
					.collect(Collectors.toList());
		}

		for (Command command : packageManifest.getCommands())
			installActions.add(new ExecCommandAction(command.getInstallCommand(), command.getUninstallCommand()));

		int i = 0;
		boolean isSuccess = true;

		for (; i < installActions.size(); i++)
			try {
				installActions.get(i).act();
			} catch (Exception ex) {
				log.error("Error during installation", ex);
				isSuccess = false;
				break;
			}

		if (!isSuccess)
			i = unact(installActions, i);

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
