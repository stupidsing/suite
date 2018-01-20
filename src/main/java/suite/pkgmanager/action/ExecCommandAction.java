package suite.pkgmanager.action;

import suite.os.Execute;
import suite.util.Fail;

public class ExecCommandAction implements InstallAction {

	private String[] installCommand;
	private String[] uninstallCommand;

	public ExecCommandAction(String[] installCommand, String[] uninstallCommand) {
		this.installCommand = installCommand;
		this.uninstallCommand = uninstallCommand;
	}

	public void act() {
		exec(installCommand);
	}

	public void unact() {
		exec(uninstallCommand);
	}

	private void exec(String[] command) {
		Execute exec = new Execute(command);
		if (exec.code != 0)
			Fail.t("command return code = " + exec.code + ": " + exec.err);
	}

}
