package org.suite;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.suite.doer.ProverConfig;
import org.suite.node.Node;
import org.util.IoUtil;

public class FunCompilerConfig {

	private Node node;
	private boolean isLazy;
	private List<String> libraries = new ArrayList<>();
	private ProverConfig proverConfig = new ProverConfig();
	private boolean isDumpCode = Suite.isDumpCode;
	private Reader in = new InputStreamReader(System.in, IoUtil.charset);
	private Writer out = new OutputStreamWriter(System.out, IoUtil.charset);

	public FunCompilerConfig() {
		if (Suite.libraries != null)
			addLibraries(Suite.libraries);
	}

	public void addLibrary(String library) {
		libraries.add(library);
	}

	public void addLibraries(List<String> libs) {
		libraries.addAll(libs);
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}

	public List<String> getLibraries() {
		return libraries;
	}

	public void setLibraries(List<String> libraries) {
		this.libraries = libraries;
	}

	public ProverConfig getProverConfig() {
		return proverConfig;
	}

	public void setProverConfig(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	public boolean isDumpCode() {
		return isDumpCode;
	}

	public void setDumpCode(boolean isDumpCode) {
		this.isDumpCode = isDumpCode;
	}

	public Reader getIn() {
		return in;
	}

	public void setIn(Reader in) {
		this.in = in;
	}

	public Writer getOut() {
		return out;
	}

	public void setOut(Writer out) {
		this.out = out;
	}

}
