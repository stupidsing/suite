package suite.ansi;

import java.util.Objects;

import com.sun.jna.Native;

import suite.adt.pair.Pair;
import suite.ansi.Keyboard.VK;
import suite.streamlet.Outlet;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.ReadLineMain
public class ReadLineMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(ReadLineMain.class, args);
	}

	protected boolean run(String[] args) {
		Keyboard keyboard = new Keyboard((LibcJna) Native.loadLibrary("c", LibcJna.class));
		Outlet<Pair<VK, Character>> keys = keyboard.signal().outlet();
		Pair<VK, Character> pair;

		while (!Objects.equals(pair = keys.next(), Pair.of(null, 'q')))
			System.out.println(pair);

		return true;
	}
}
