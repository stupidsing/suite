package suite.pkgmanager.actions;

import java.io.IOException;

import suite.util.ExecUtil;

public class ExecCommandAction implements InstallAction {

	private String installCommand[];
	private String uninstallCommand[];

	public ExecCommandAction(String installCommand[], String uninstallCommand[]) {
		this.installCommand = installCommand;
		this.uninstallCommand = uninstallCommand;
	}

	public void act() throws IOException {
		exec(installCommand);
	}

	public void unact() throws IOException {
		exec(uninstallCommand);
	}

	private void exec(String command[]) throws IOException {
		ExecUtil exec = new ExecUtil(command, "");
		if (exec.getCode() != 0)
			throw new IOException("Command return code = " + exec.getCode() + ": " + exec.getErr());
	}

}
