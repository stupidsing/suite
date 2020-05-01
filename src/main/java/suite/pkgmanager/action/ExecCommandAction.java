package suite.pkgmanager.action;

import suite.os.Execute;

import static primal.statics.Fail.fail;

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
