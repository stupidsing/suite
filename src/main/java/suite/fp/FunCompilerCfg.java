package suite.fp;

import java.util.ArrayList;
import java.util.List;

import primal.streamlet.Streamlet;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.node.Node;
import suite.streamlet.Read;

public class FunCompilerCfg {

	private Node node;
	private boolean isLazy = true;
	private List<String> libraries;
	private ProverCfg proverCfg;

	public FunCompilerCfg() {
		this(new ProverCfg(), new ArrayList<>(Suite.libraries));
	}

	public FunCompilerCfg(ProverCfg proverCfg, List<String> libraries) {
		this.proverCfg = proverCfg;
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

	public ProverCfg getProverConfig() {
		return proverCfg;
	}

}
