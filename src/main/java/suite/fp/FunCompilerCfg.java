package suite.fp;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.node.Node;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class FunCompilerCfg {

	private Node node;
	private boolean isLazy = true;
	private List<String> libraries;
	private ProverConfig proverConfig;

	public FunCompilerCfg() {
		this(new ProverConfig(), new ArrayList<>(Suite.libraries));
	}

	public FunCompilerCfg(ProverConfig proverConfig, List<String> libraries) {
		this.proverConfig = proverConfig;
		this.libraries = libraries;
	}

	public void addLibrary(String library) {
		libraries.add(library);
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

	public Streamlet<String> getLibraries() {
		return Read.from(libraries);
	}

	public ProverConfig getProverConfig() {
		return proverConfig;
	}

}
