package suite.sgf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import suite.adt.Pair;
import suite.os.FileUtil;
import suite.util.Rethrow;
import suite.util.Util;

// wget -O - http://www.flygo.net/mjxj/WeiQiTianDi/dlxs_sdl.sgf | iconv -c -f CN-GB -t UTF-8
public class Sgf {

	public static class PosPair<T> {
		public int pos;
		public T t;

		public static <T> PosPair<T> of(int pos, T t) {
			PosPair<T> intPair = new PosPair<>();
			intPair.pos = pos;
			intPair.t = t;
			return intPair;
		}
	}

	public class Node {
		public List<List<Pair<String, List<String>>>> commandsList = new ArrayList<>();
		public List<Node> variations = new ArrayList<>();
	}

	public void fromFile(String filename) {
		String in = Rethrow.ioException(() -> FileUtil.read(filename));
		from(in);
	}

	public void from(String in0) {
		String in = in0.replace("\n", "");
		if (in.charAt(0) == '(')
			Util.dump(readNode(in, 1).t);
		else
			throw new RuntimeException();
	}

	private PosPair<Node> readNode(String in, int pos) {
		Node node = new Node();

		while (pos < in.length())
			switch (in.charAt(pos++)) {
			case ';':
				PosPair<List<Pair<String, List<String>>>> ipCommands = readCommands(in, pos);
				pos = ipCommands.pos;
				node.commandsList.add(ipCommands.t);
				break;
			case '(':
				PosPair<Node> ipNode = readNode(in, pos);
				pos = ipNode.pos;
				node.variations.add(ipNode.t);
				break;
			case ')':
				return PosPair.of(pos, node);
			default:
				throw new RuntimeException();
			}

		throw new RuntimeException("Unexpected end of input");
	}

	private PosPair<List<Pair<String, List<String>>>> readCommands(String in, int pos) {
		List<Pair<String, List<String>>> commands = new ArrayList<>();

		while (pos < in.length())
			switch (in.charAt(pos)) {
			case ';':
			case '(':
			case ')':
				return PosPair.of(pos, commands);
			default:
				PosPair<Pair<String, List<String>>> ipCommand = readCommand(in, pos);
				pos = ipCommand.pos;
				commands.add(ipCommand.t);
			}

		throw new RuntimeException("Unexpected end of input");
	}

	private PosPair<Pair<String, List<String>>> readCommand(String in, int pos) {
		List<String> ids = new ArrayList<>();
		PosPair<String> ipId = readIf(in, pos, ch -> Character.isAlphabetic(ch) || Character.isDigit(ch));
		pos = ipId.pos;

		while (pos < in.length())
			switch (in.charAt(pos)) {
			case '[':
				PosPair<String> ip = readIf(in, pos + 1, ch -> ch != ']');
				pos = ip.pos + 1;
				ids.add(ip.t);
				break;
			default:
				return PosPair.of(pos, Pair.of(ipId.t, ids));
			}

		throw new RuntimeException("Unexpected end of input");
	}

	private PosPair<String> readIf(String in, int pos0, IntPredicate predicate) {
		int pos1 = pos0;

		while (pos0 < in.length()) {
			char ch = in.charAt(pos0);
			if (predicate.test(ch))
				pos0++;
			else
				return PosPair.of(pos0, in.substring(pos1, pos0));
		}

		throw new RuntimeException("Unexpected end of input");
	}

}
