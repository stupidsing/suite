package suite.lp.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.IndexedReader;
import suite.node.Data;
import suite.node.Node;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;

public class MonadIntrinsics {

	public static class Popen implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			Fun<Node, Node> unwrapper = bridge::unwrap;
			List<String> list = new ArrayList<>();

			Source<Node> source = ExpandUtil.expandList(unwrapper, inputs.get(0));
			Node node;

			while ((node = source.source()) != null)
				list.add(ExpandUtil.expandString(unwrapper, node));

			Node in = inputs.get(1);

			try {
				Process process = Runtime.getRuntime().exec(list.toArray(new String[list.size()]));
				InputStreamReader isr = new InputStreamReader(process.getInputStream(), FileUtil.charset);
				BufferedReader br = new BufferedReader(isr);
				Node result = new Data<>(new IndexedReader(br));

				// Use a separate thread to write to the process, so that read
				// and write occur at the same time and would not block up.
				// The input stream is also closed by this thread.
				// Have to make sure the executors are thread-safe!
				new Thread(() -> {
					try {
						try (InputStream pes = process.getErrorStream();
								OutputStream pos = process.getOutputStream();
								Writer writer = new OutputStreamWriter(pos)) {
							ExpandUtil.expandToWriter(unwrapper, in, writer);
						}

						process.waitFor();
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
				}).start();

				return result;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static class Seq implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			ExpandUtil.expandFully(bridge::unwrap, inputs.get(0));
			return inputs.get(1);
		}
	}

}
