package suite.pkgmanager.actions;

import suite.os.Execute;

public class ExecCommandAction implements InstallAction {

	private String installCommand[];
	private String uninstallCommand[];

	public ExecCommandAction(String installCommand[], String uninstallCommand[]) {
		this.installCommand = installCommand;
		this.uninstallCommand = uninstallCommand;
	}

	public void act() {
		exec(installCommand);
	}

	public void unact() {
		exec(uninstallCommand);
	}

	private void exec(String command[]) {
		Execute exec = new Execute(command);
		if (exec.code != 0)
			throw new RuntimeException("Command return code = " + exec.code + ": " + exec.err);
	}

}
