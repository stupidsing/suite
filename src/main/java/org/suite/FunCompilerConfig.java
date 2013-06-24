package org.suite;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.ProverConfig;
import org.suite.node.Node;

public class FunCompilerConfig {

	private Node node;
	private boolean isLazy = true;
	private List<String> libraries = new ArrayList<>();
	private ProverConfig proverConfig = new ProverConfig();
	private boolean isDumpCode = Suite.isDumpCode;

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

}
