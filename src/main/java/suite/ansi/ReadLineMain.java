package suite.ansi;

import java.util.Objects;

import com.sun.jna.Native;

import suite.adt.pair.Pair;
import suite.ansi.Keyboard.VK;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.ReadLineMain
public class ReadLineMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(ReadLineMain.class, args);
	}

	protected boolean run(String[] args) {
		var keyboard = new Keyboard((LibcJna) Native.loadLibrary("c", LibcJna.class));
		var keys = keyboard.signal().outlet();
		Pair<VK, Character> pair;

		while (!Objects.equals(pair = keys.next(), Pair.of(null, 'q')))
			System.out.println(pair);

		return true;
	}
}
