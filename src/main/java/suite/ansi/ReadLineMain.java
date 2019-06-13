package suite.ansi;

import java.util.Objects;

import com.sun.jna.Native;

import suite.adt.pair.Pair;
import suite.ansi.Keyboard.VK;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.ReadLineMain
public class ReadLineMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var keyboard = new Keyboard((LibcJna) Native.load("c", LibcJna.class));
			var keys = keyboard.pusher().pushee();
			Pair<VK, Character> pair;

			while (!Objects.equals(pair = keys.pull(), Pair.of(null, 'q')))
				System.out.println(pair);

			return true;
		});
	}

}
