package suite.java7util;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import suite.util.FunUtil.Source;

public class FileUtil {

	public static Source<File> findFiles(File file) {
		Deque<File> stack = new ArrayDeque<>();
		stack.push(file);

		return () -> {
			while (!stack.isEmpty()) {
				File f = stack.pop();

				if (f.isDirectory())
					for (File child : f.listFiles())
						stack.push(child);
				else
					return f;
			}

			return null;
		};
	}

}
