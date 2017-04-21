package suite.pkgmanager;

import java.util.List;
import java.util.Map;

public class PackageManifest {

	private String name;
	private String version;
	private Map<String, String> filenameMappings;
	private List<Command> commands;

	public static class Command {
		private String[] installCommand;
		private String[] uninstallCommand;

		public String[] getInstallCommand() {
			return installCommand;
		}

		public String[] getUninstallCommand() {
			return uninstallCommand;
		}
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public Map<String, String> getFilenameMappings() {
		return filenameMappings;
	}

	public List<Command> getCommands() {
		return commands;
	}

}
