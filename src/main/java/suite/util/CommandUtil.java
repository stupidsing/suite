package suite.util;

import java.util.HashMap;
import java.util.Map;

import suite.adt.Pair;

public class CommandUtil<Command> {

	private Map<String, Command> commandsByName;
	private int maxLength;

	public CommandUtil(Command commands[]) {
		this(getCommandsByName(commands));
	}

	public CommandUtil(Map<String, Command> commandsByName) {
		this.commandsByName = commandsByName;
		maxLength = 0;
		for (String name : commandsByName.keySet())
			maxLength = Math.max(maxLength, name.length());
	}

	public Pair<Command, String> recognize(String input) {
		return recognize(input, 0);
	}

	public Pair<Command, String> recognize(String input, int start) {
		for (int end = Math.min(start + maxLength, input.length()); end >= start; end--) {
			String starts = input.substring(start, end);
			Command command = commandsByName.get(starts);
			if (command != null)
				return Pair.of(command, input.substring(end));
		}
		return null;
	}

	private static <Command> Map<String, Command> getCommandsByName(Command commands[]) {
		Map<String, Command> commandsByName = new HashMap<>();
		for (Command command : commands) {
			String name = command.toString();
			commandsByName.put(name, command);
		}
		return commandsByName;
	}

}
