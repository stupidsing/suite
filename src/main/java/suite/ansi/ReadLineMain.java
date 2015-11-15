package suite.ansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.streamlet.Read;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn assembly:single && java -cp target/suite-1.0-jar-with-dependencies.jar suite.ansi.ReadLineMain
public class ReadLineMain extends ExecutableProgram {

	private Trie trie;

	private enum VK {
		UP___, //
		DOWN_, //
		LEFT_, //
		RIGHT, //
	}

	private class Trie {
		private VK vk;
		private Map<Integer, Trie> map = new HashMap<>();
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

		trie = new Trie();

		for (Pair<List<Integer>, VK> pair : pairs) {
			Trie t = trie;
			for (int ch : pair.t0)
				t = trie.map.computeIfAbsent(ch, ch_ -> new Trie());
			t.vk = pair.t1;
		}

		try (Termios termios = new Termios()) {
			System.out.println("abcdefgh\b\b\bxyz\n");

			Source<Pair<VK, Character>> source = FunUtil.suck(new Sink<Sink<Pair<VK, Character>>>() {
				private List<Character> chs = new ArrayList<>();
				private Trie t = trie;

				public void sink(Sink<Pair<VK, Character>> sink) {
					int ch_;

					while ((ch_ = Libc.getchar()) != -1) {
						if (ch_ != -1) {
							chs.add((char) ch_);
							t = t.map.get(ch_);
							if (t != null)
								if (t.vk != null) {
									sink.sink(Pair.of(t.vk, null));
									reset();
								} else
									;
							else
								flushChars(sink);
						}

						flushChars(sink);
					}
				}

				private void flushChars(Sink<Pair<VK, Character>> sink) {
					Read.from(chs).sink(ch -> sink.sink(Pair.of(null, ch)));
					reset();
				}

				private void reset() {
					chs.clear();
					t = trie;
				}
			});

			source.source();
		}

		return true;
	}

}
