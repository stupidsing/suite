package suite.ansi;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Trie;
import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.streamlet.Signal;
import suite.streamlet.Signal.Redirector;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class Keyboard {

	private LibcJna libc;
	private Trie<Integer, VK> trie;

	public enum VK {
		LEFT_, //
		RIGHT, //
		UP___, //
		DOWN_, //
	}

	public Keyboard(LibcJna libc) {
		this.libc = libc;

		List<Pair<List<Integer>, VK>> pairs = new ArrayList<>();
		pairs.add(Pair.of(List.of(27, 91, 68), VK.LEFT_));
		pairs.add(Pair.of(List.of(27, 91, 67), VK.RIGHT));
		pairs.add(Pair.of(List.of(27, 91, 65), VK.UP___));
		pairs.add(Pair.of(List.of(27, 91, 66), VK.DOWN_));

		trie = new Trie<>();

		for (Pair<List<Integer>, VK> pair : pairs)
			trie.add(pair.t0, pair.t1);
	}

	public Signal<Pair<VK, Character>> signal() {
		Source<Character> source0 = () -> {
			int ch = libc.getchar();
			return 0 <= ch ? (char) ch : null;
		};

		return Signal //
				.from(source0) //
				.redirect(new Redirector<Character, Pair<VK, Character>>() {
					private List<Character> chs = new ArrayList<>();
					private Trie<Integer, VK> t = trie;

					public void accept(Character ch_, Sink<Pair<VK, Character>> fire) {
						if (ch_ != null) {
							chs.add(ch_);
							VK vk;

							if ((t = t.getMap().get((int) ch_)) != null)
								if ((vk = t.getValue()) != null) {
									fire.sink(Pair.of(vk, null));
									reset();
								} else
									;
							else
								flush(fire);
						} else
							flush(fire);
					}

					private void flush(Sink<Pair<VK, Character>> fire) {
						Read.from(chs).sink(ch -> fire.sink(Pair.of(null, ch)));
						reset();
					}

					private void reset() {
						chs.clear();
						t = trie;
					}
				});
	}

}
