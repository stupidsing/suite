package suite.util;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;

public class CommandUtil<Command> {

	private Map<String, Command> commandByName;
	private int maxLength;

	public CommandUtil(Command[] commands) {
		this(getCommandByName(commands));
	}

	public CommandUtil(Map<String, Command> commandByName) {
		this.commandByName = commandByName;
		maxLength = 0;
		for (var name : commandByName.keySet())
			maxLength = max(maxLength, name.length());
	}

	public Pair<Command, String> recognize(String input) {
		return recognize(input, 0);
	}

	public Pair<Command, String> recognize(String input, int start) {
		for (var end = min(start + maxLength, input.length()); start <= end; end--) {
			var starts = input.substring(start, end);
			var command = commandByName.get(starts);
			if (command != null)
				return Pair.of(command, input.substring(end));
		}
		return null;
	}

	private static <Command> Map<String, Command> getCommandByName(Command[] commands) {
		return Read.from(commands).toMap(Command::toString);
	}

}
