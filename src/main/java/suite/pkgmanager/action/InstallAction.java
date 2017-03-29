package suite.pkgmanager.action;

import java.io.IOException;

public interface InstallAction {

	public void act() throws IOException;

	public void unact() throws IOException;

}
