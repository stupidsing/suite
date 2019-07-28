package suite.pkgmanager.action;

import static suite.util.Fail.fail;

import suite.os.Execute;

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
		var exec = new Execute(command);
		if (exec.code != 0)
			fail("command return code = " + exec.code + ": " + exec.err);
	}

}
