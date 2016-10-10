package suite.ansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.Pair;
import suite.adt.Trie;
import suite.streamlet.Reactive;
import suite.streamlet.Reactive.Redirector;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn assembly:single && java -cp target/suite-1.0-jar-with-dependencies.jar suite.ansi.ReadLineMain
public class ReadLineMain extends ExecutableProgram {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);
	private Trie<Integer, VK> trie;

	private enum VK {
		UP___, //
		DOWN_, //
		LEFT_, //
		RIGHT, //
	}

	public static void main(String args[]) throws IOException {
		Util.run(ReadLineMain.class, args);
	}

	protected boolean run(String args[]) {
		List<Pair<List<Integer>, VK>> pairs = new ArrayList<>();
		pairs.add(Pair.of(Arrays.asList(27, 91, 65), VK.UP___));
		pairs.add(Pair.of(Arrays.asList(27, 91, 66), VK.DOWN_));
		pairs.add(Pair.of(Arrays.asList(27, 91, 68), VK.LEFT_));
		pairs.add(Pair.of(Arrays.asList(27, 91, 67), VK.RIGHT));

		trie = new Trie<>();

		for (Pair<List<Integer>, VK> pair : pairs)
			trie.add(pair.t0, pair.t1);

		try (Termios termios = new Termios(libc)) {
			Source<Character> source0 = () -> {
				int ch = libc.getchar();
				return 0 <= ch ? (char) ch : null;
			};

			Reactive.from(source0).redirect(new Redirector<Character, Pair<VK, Character>>() {
				private List<Character> chs = new ArrayList<>();
				private Trie<Integer, VK> t = trie;

				public void accept(Character ch_, Reactive<Pair<VK, Character>> reactive) {
					if (ch_ != null) {
						chs.add(ch_);
						VK vk;

						if ((t = t.getMap().get(ch_)) != null)
							if ((vk = t.getValue()) != null) {
								reactive.fire(Pair.of(vk, null));
								reset();
							} else
								;
						else
							flush(reactive);
					} else
						flush(reactive);
				}

				private void flush(Reactive<Pair<VK, Character>> reactive) {
					Read.from(chs).sink(ch -> reactive.fire(Pair.of(null, ch)));
					reset();
				}

				private void reset() {
					chs.clear();
					t = trie;
				}
			}).outlet().source().source();
		}

		return true;
	}

}
