package suite.ansi;

import java.io.IOException;
import java.util.Objects;

import suite.adt.Pair;
import suite.ansi.Keyboard.VK;
import suite.streamlet.Outlet;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.ReadLineMain
public class ReadLineMain extends ExecutableProgram {

	public static void main(String args[]) throws IOException {
		Util.run(ReadLineMain.class, args);
	}

	protected boolean run(String args[]) {
		try (Keyboard keyboard = new Keyboard()) {
			Outlet<Pair<VK, Character>> keys = keyboard.keys();
			Pair<VK, Character> pair;

			while (!Objects.equals(pair = keys.next(), Pair.of(null, 'q')))
				System.out.println(pair);

			return true;
		}
	}
}
