package org.suite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.suite.doer.Prover;
import org.suite.kb.Rule;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;
import org.util.Util;

public class SuiteImportUtil {

	// The directory of the file we are now importing
	private boolean isImportFromClasspath = false;
	private String importerRoot = "";

	public void addRule(RuleSet rs, String rule) {
		rs.addRule(Rule.formRule(Suite.parse(rule)));
	}

	public synchronized boolean importFrom(RuleSet rs, String name)
			throws IOException {
		if (isImportFromClasspath)
			return importResource(rs, name);
		else
			return importFile(rs, name);
	}

	public synchronized boolean importFile(RuleSet rs, String filename)
			throws IOException {
		FileInputStream is = null;

		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		filename = setImporterRoot(false, filename, oldRoot);

		try {
			is = new FileInputStream(filename);
			return RuleSetUtil.importFrom(rs, Suite.parse(is));
		} finally {
			Util.closeQuietly(is);
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
	}

	public synchronized boolean importResource(RuleSet rs, String classpath)
			throws IOException {
		ClassLoader cl = Suite.class.getClassLoader();
		InputStream is = null;

		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		classpath = setImporterRoot(true, classpath, oldRoot);

		try {
			is = cl.getResourceAsStream(classpath);
			if (is != null)
				return RuleSetUtil.importFrom(rs, Suite.parse(is));
			else
				throw new RuntimeException("Cannot find resource " + classpath);
		} finally {
			Util.closeQuietly(is);
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
	}

	public Prover createProver(String toImports[]) {
		return new Prover(createRuleSet(toImports));
	}

	public RuleSet createRuleSet(String toImports[]) {
		RuleSet rs = RuleSetUtil.create();
		try {
			for (String toImport : toImports)
				importResource(rs, toImport);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return rs;
	}

	private String setImporterRoot(boolean isFromClasspath, String name,
			String oldRoot) {
		isImportFromClasspath = isFromClasspath;

		if (!name.startsWith(File.separator))
			name = oldRoot + name;

		int pos = name.lastIndexOf(File.separator);
		importerRoot = pos >= 0 ? name.substring(0, pos + 1) : "";
		return name;
	}

}
