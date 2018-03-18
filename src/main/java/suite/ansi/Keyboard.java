package suite.ansi;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Trie;
import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.streamlet.Signal;
import suite.streamlet.Signal.Redirector;
import suite.util.FunUtil.Sink;

public class Keyboard {

	private LibcJna libc;
	private Trie<Integer, VK> trie = new Trie<>();

	public enum VK {
		DEL__, //
		DOWN_, //
		END__, //
		HOME_, //
		INS__, //
		LEFT_, //
		PGUP_, //
		PGDN_, //
		RIGHT, //
		UP___, //
	}

	public Keyboard(LibcJna libc) {
		this.libc = libc;

		List<Pair<List<Integer>, VK>> pairs = new ArrayList<>();
		pairs.add(Pair.of(List.of(27, 91, 50, 126), VK.INS__));
		pairs.add(Pair.of(List.of(27, 91, 51, 126), VK.DEL__));
		pairs.add(Pair.of(List.of(27, 91, 53, 126), VK.PGUP_));
		pairs.add(Pair.of(List.of(27, 91, 54, 126), VK.PGDN_));
		pairs.add(Pair.of(List.of(27, 91, 65), VK.UP___));
		pairs.add(Pair.of(List.of(27, 91, 66), VK.DOWN_));
		pairs.add(Pair.of(List.of(27, 91, 67), VK.RIGHT));
		pairs.add(Pair.of(List.of(27, 91, 68), VK.LEFT_));
		pairs.add(Pair.of(List.of(27, 91, 70), VK.END__));
		pairs.add(Pair.of(List.of(27, 91, 72), VK.HOME_));

		for (Pair<List<Integer>, VK> pair : pairs)
			trie.add(pair.t0, pair.t1);
	}

	public void loop(Sink<Signal<Pair<VK, Character>>> sink) {
		Signal.loop(this::get, signal -> sink.sink(signal.redirect(redirector)));
	}

	public Signal<Pair<VK, Character>> signal() {
		return Signal.from(this::get).redirect(redirector);
	}

	private Character get() {
		int ch = libc.getchar();
		return 0 <= ch ? (char) ch : null;
	}

	private Redirector<Character, Pair<VK, Character>> redirector = new Redirector<>() {
		private List<Character> chs = new ArrayList<>();
		private Trie<Integer, VK> t = trie;

		public void accept(Character ch_, Sink<Pair<VK, Character>> fire) {
			if (ch_ != null) {
				Trie<Integer, VK> t1 = t.getMap().get((int) ch_);
				VK vk;

				chs.add(ch_);

				if (t1 != null)
					if ((vk = (t = t1).getValue()) != null) {
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
	};

}
