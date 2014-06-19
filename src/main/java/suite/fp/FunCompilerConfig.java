package suite.fp;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.node.Node;

public class FunCompilerConfig {

	private Node node;
	private boolean isLazy = true;
	private List<String> libraries = new ArrayList<>();
	private ProverConfig proverConfig = new ProverConfig();

	public FunCompilerConfig() {
		this(new ProverConfig(), new ArrayList<>(Suite.libraries));
	}

	public FunCompilerConfig(ProverConfig proverConfig, List<String> libraries) {
		this.proverConfig = proverConfig;
		this.libraries = libraries;
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

}
