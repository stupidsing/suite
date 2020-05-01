package suite.ansi;

import com.sun.jna.Native;
import primal.Verbs.Equals;
import primal.adt.Pair;
import suite.ansi.Keyboard.VK;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.ReadLineMain
public class ReadLineMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var keyboard = new Keyboard((LibcJna) Native.load("c", LibcJna.class));
			var keys = keyboard.pusher().pushee();
			Pair<VK, Character> pair;

			while (!Equals.ab(pair = keys.pull(), Pair.of(null, 'q')))
				System.out.println(pair);

			return true;
		});
	}

}
