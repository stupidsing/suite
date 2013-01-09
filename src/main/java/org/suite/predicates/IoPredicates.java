package org.suite.predicates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.predicates.SystemPredicates.SystemPredicate;
import org.util.FormatUtil;
import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util;

public class IoPredicates {

	public static class Dump implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.print(Formatter.dump(ps));
			return true;
		}
	}

	public static class DumpStack implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			String date = FormatUtil.dtFmt.format(new Date());
			String trace = prover.getTracer().getStackTrace();
			LogUtil.info("STACK-TRACE", "-- Trace at " + date + " --\n" + trace);
			return true;
		}
	}

	public static class Exec implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			if (ps instanceof Str)
				try {
					String cmd = ((Str) ps).getValue();
					return Runtime.getRuntime().exec(cmd).waitFor() == 0;
				} catch (Exception ex) { // IOException or InterruptedException
					log.error(this, ex);
				}
			return false;
		}

		private static Log log = LogFactory.getLog(Util.currentClass());
	}

	public static class FileExists implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return new File(Formatter.display(ps)).exists();
		}
	}

	public static class FileRead implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			try {
				final Node params[] = Predicate.getParameters(ps, 2);
				String filename = Formatter.display(params[0]);
				InputStream is = new FileInputStream(filename);
				String content = IoUtil.readStream(is);
				return prover.bind(new Str(content), params[1]);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static class FileWrite implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			try {
				final Node params[] = Predicate.getParameters(ps, 2);
				String filename = Formatter.display(params[0]);
				String content = Formatter.display(params[1]);
				IoUtil.writeStream(new FileOutputStream(filename), content);
				return true;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static class HomeDir implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			String homeDir = System.getProperty("home.dir");
			homeDir = homeDir != null ? homeDir : ".";
			return prover.bind(new Str(homeDir), ps);
		}
	}

	public static class Nl implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.println();
			return true;
		}
	}

	public static class Write implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.print(Formatter.display(ps));
			return true;
		}
	}

}
