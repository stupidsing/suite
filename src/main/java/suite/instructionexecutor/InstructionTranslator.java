package suite.instructionexecutor;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import suite.adt.BiMap;
import suite.adt.HashBiMap;
import suite.fp.intrinsic.Intrinsics;
import suite.instructionexecutor.InstructionAnalyzer.AnalyzedFrame;
import suite.instructionexecutor.InstructionAnalyzer.AnalyzedRegister;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRun;
import suite.jdk.JdkLoadClassUtil;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.parser.Subst;
import suite.util.FunUtil;

/**
 * Possible register types: boolean, thunk, int, node
 * (atom/number/reference/tree)
 */
public class InstructionTranslator implements Closeable {

	public static int invokeJavaEntryPoint = -1;

	private static AtomicInteger counter = new AtomicInteger();

	private BiMap<Integer, Node> constantPool = new HashBiMap<>();

	private String packageName;
	private String className;

	private JdkLoadClassUtil jdkLoadClassUtil;

	private StringBuilder clazzsec = new StringBuilder();
	private StringBuilder localsec = new StringBuilder();
	private StringBuilder switchsec = new StringBuilder();

	private Subst subst = new Subst("#{", "}");

	private InstructionAnalyzer analyzer = new InstructionAnalyzer();

	private int currentIp;

	private String compare = "comparer.compare(#{reg-node}, #{reg-node})";

	public InstructionTranslator(Path basePath) throws MalformedURLException {
		packageName = getClass().getPackage().getName();
		jdkLoadClassUtil = new JdkLoadClassUtil(basePath, basePath);
	}

	@Override
	public void close() throws IOException {
		jdkLoadClassUtil.close();
	}

	public TranslatedRun translate(Node node) throws IOException {
		List<Instruction> instructions = new ArrayList<>();

		try (InstructionExtractor extractor = new InstructionExtractor(constantPool)) {
			instructions.addAll(extractor.extractInstructions(node));
		}

		int exitPoint = instructions.size();
		instructions.add(new Instruction(Insn.EXIT__________, 0, 0, 0));

		analyzer.analyze(instructions);
		analyzer.transform(instructions);
		translateInstructions(instructions);

		className = "TranslatedRun" + counter.getAndIncrement();

		String java = String.format("" //
				+ "package " + packageName + "; \n" //
				+ "import java.util.*; \n" //
				+ "import suite.*; \n" //
				+ "import suite.fp.intrinsic.*; \n" //
				+ "import suite.instructionexecutor.*; \n" //
				+ "import suite.lp.*; \n" //
				+ "import suite.lp.doer.*; \n" //
				+ "import suite.lp.kb.*; \n" //
				+ "import suite.lp.predicate.*; \n" //
				+ "import suite.node.*; \n" //
				+ "import suite.node.util.*; \n" //
				+ "import suite.primitive.*; \n" //
				+ "import suite.util.*; \n" //
				+ "import " + FunComparer.class.getCanonicalName() + "; \n" //
				+ "import " + FunUtil.class.getCanonicalName() + ".*; \n" //
				+ "import " + IOException.class.getCanonicalName() + "; \n" //
				+ "import " + Intrinsics.class.getCanonicalName() + ".*; \n" //
				+ "import " + TermOp.class.getCanonicalName() + "; \n" //
				+ "import " + TranslatedRunUtil.class.getCanonicalName() + ".*; \n" //
				+ "\n" //
				+ "public class " + className + " implements TranslatedRun { \n" //
				+ "private static int stackSize = 4096; \n" //
				+ "\n" //
				+ "private static Atom FALSE = Atom.FALSE; \n" //
				+ "private static Atom TRUE = Atom.TRUE; \n" //
				+ "\n" //
				+ "%s" //
				+ "\n" //
				+ "public Node exec(TranslatedRunConfig config, Thunk thunk) { \n" //
				+ "Frame frame = thunk.frame; \n" //
				+ "int ip = thunk.ip; \n" //
				+ "Node returnValue = null; \n" //
				+ "int cs[] = new int[stackSize]; \n" //
				+ "Node ds[] = new Node[stackSize]; \n" //
				+ "Object fs[] = new Object[stackSize]; \n" //
				+ "int csp = 0, dsp = 0, cpsp = 0; \n" //
				+ "int n; \n" //
				+ "Node node, n0, n1, var; \n" //
				+ "Tree tree; \n" //
				+ "\n" //
				+ "Prover prover = new Prover(config.ruleSet); \n" //
				+ "Trail trail = prover.getTrail(); \n" //
				+ "SystemPredicates systemPredicates = new SystemPredicates(prover); \n" //
				+ "IntrinsicCallback callback = TranslatedRunUtil.getIntrinsicCallback(config, this); \n" //
				+ "Comparer comparer = new FunComparer(callback::yawn); \n" //
				+ "\n" //
				+ "%s \n" //
				+ "\n" //
				+ "cs[csp++] = " + exitPoint + "; \n" //
				+ "\n" //
				+ "while (true) { \n" //
				// + "System.out.println(ip); \n" //
				+ "switch(ip) { \n" //
				+ "%s \n" //
				+ "case " + invokeJavaEntryPoint + ": \n" //
				+ "IntrinsicFrame iframe = (IntrinsicFrame) frame; \n" //
				+ "returnValue = iframe.intrinsic.invoke(callback, Arrays.asList(iframe.node)); \n" //
				+ "ip = cs[--csp]; \n" //
				+ "continue; \n" //
				+ "default: \n" //
				+ "} \n" //
				+ "} \n" //
				+ "} \n" //
				+ "} \n" //
				, clazzsec, localsec, switchsec);

		return getTranslatedRun(java);
	}

	private void translateInstructions(List<Instruction> instructions) {
		Node constant;
		int ip = 0;

		while (ip < instructions.size()) {
			Instruction insn = instructions.get(currentIp = ip++);
			int op0 = insn.op0, op1 = insn.op1, op2 = insn.op2;

			app("// (#{num}) #{str}", currentIp, insn);

			app("case #{num}:", currentIp);

			switch (insn.insn) {
			case ASSIGNCONST___:
				constant = constantPool.get(op1);
				app("#{reg} = #{str}", op0, defineConstant(constant));
				break;
			case ASSIGNFRAMEREG:
				if (op1 != 0) {
					String prevs = "";
					for (int i = op1; i < 0; i++)
						prevs += ".previous";
					app("#{reg} = TranslatedRunUtil.toNode(#{fr}#{str}.r#{num})", op0, prevs, op2);
				} else
					app("#{reg} = TranslatedRunUtil.toNode(#{reg})", op0, op2);
				break;
			case ASSIGNINT_____:
				app("#{reg} = #{num}", op0, op1);
				break;
			case ASSIGNRESULT__:
				restoreFrame();
				app("#{reg} = returnValue", op0);
				break;
			case ASSIGNTHUNK___:
				app("#{reg} = new Thunk(#{fr}, #{num})", op0, op1);
				break;
			case ASSIGNTHUNKRES:
				restoreFrame();
				app("#{reg} = returnValue", op0);
				app("#{reg-clos}.result = #{reg}", op1, op0);
				break;
			case BACKUPCSP_____:
				app("#{reg} = csp", op0);
				break;
			case BACKUPDSP_____:
				app("#{reg} = dsp", op0);
				break;
			case BIND__________:
				app("if (!Binder.bind(#{reg-node}, #{reg-node}, trail)) #{jump}", op0, op1, op2);
				break;
			case BINDMARK______:
				app("#{reg} = trail.getPointInTime()", op0);
				break;
			case BINDUNDO______:
				app("trail.unwind(#{reg-num})", op0);
				break;
			case CALL__________:
				backupFrame();
				pushCallee(ip);
				app("#{jump}", op0);
				break;
			case CALLINTRINSIC_:
				app("{");
				app("Data<?> data = (Data<?>) callback.yawn((Node) ds[--dsp])");
				app("List<Node> list = new ArrayList<>(3)");
				for (int i = 1; i < insn.op1; i++)
					app("list.add((Node) ds[--dsp])");
				app("Intrinsic intrinsic = Data.get(data)");
				app("#{reg} = intrinsic.invoke(callback, list)", op0);
				app("}");
				break;
			case CALLTHUNK_____:
				app("if (#{reg-clos}.result == null) {", op0);
				backupFrame();
				pushCallee(ip);
				app("frame = #{reg-clos}.frame", op0);
				app("ip = #{reg-clos}.ip", op0);
				app("continue");
				app("} else returnValue = #{reg-clos}.result", op0);
				break;
			case COMPARE_______:
				app("n0 = (Node) ds[--dsp]");
				app("n1 = (Node) ds[--dsp]");
				app("#{reg} = comparer.compare(n0, n1)", op0);
				break;
			case CONSLIST______:
				app("n0 = (Node) ds[--dsp]");
				app("n1 = (Node) ds[--dsp]");
				app("#{reg} = Tree.of(TermOp.OR____, n0, n1)", op0);
				break;
			case CONSPAIR______:
				app("n0 = (Node) ds[--dsp]");
				app("n1 = (Node) ds[--dsp]");
				app("#{reg} = Tree.of(TermOp.AND___, n0, n1)", op0);
				break;
			case DATACHARS_____:
				app("#{reg} = new Data<Chars>(To.chars(((Str) #{reg}).getValue()))", op0, op1);
				break;
			case DECOMPOSETREE0:
				app("node = #{reg-node}.finalNode()", op0);
				insn = instructions.get(ip++);
				app("if (node instanceof Tree) {");
				app("Tree tree = (Tree) node;");
				app("if (tree.getOperator() == TermOp.#{str}) {", insn.op0);
				app("#{reg} = tree.getLeft()", insn.op1);
				app("#{reg} = tree.getRight()", insn.op2);
				app("} else #{jump}", op1);
				app("} else if (node instanceof Reference) {");
				app("Tree tree = Tree.of(op, #{reg} = new Reference(), #{reg} = new Reference())", insn.op1, insn.op2);
				app("trail.addBind((Reference) node, tree)");
				app("} else #{jump}", op1);
				break;
			case ENTER_________:
				boolean isRequireParent = analyzer.getFrame(currentIp).isRequireParent();
				String previousFrame = isRequireParent ? "(#{prev-fr-class}) frame" : "null";
				app("#{fr} = new #{fr-class}(" + previousFrame + ")");
				break;
			case ERROR_________:
				app("throw new RuntimeException(\"Error termination\")");
				break;
			case EVALADD_______:
				app("#{reg} = #{reg-num} + #{reg-num}", op0, op1, op2);
				break;
			case EVALDIV_______:
				app("#{reg} = #{reg-num} / #{reg-num}", op0, op1, op2);
				break;
			case EVALEQ________:
				app("#{reg} = " + compare + " == 0", op0, op1, op2);
				break;
			case EVALLE________:
				app("#{reg} = " + compare + " <= 0", op0, op1, op2);
				break;
			case EVALLT________:
				app("#{reg} = " + compare + " < 0", op0, op1, op2);
				break;
			case EVALNE________:
				app("#{reg} = " + compare + " != 0", op0, op1, op2);
				break;
			case EVALMOD_______:
				app("#{reg} = #{reg-num} % #{reg-num}", op0, op1, op2);
				break;
			case EVALMUL_______:
				app("#{reg} = #{reg-num} * #{reg-num}", op0, op1, op2);
				break;
			case EVALSUB_______:
				app("#{reg} = #{reg-num} - #{reg-num}", op0, op1, op2);
				break;
			case EXIT__________:
				app("return returnValue"); // Grand exit point
				break;
			case FORMTREE0_____:
				insn = instructions.get(ip++);
				app("#{reg} = Tree.of(TermOp.#{str}, #{reg-node}, #{reg-node})", insn.op1,
						TermOp.find(((Atom) constantPool.get(insn.op0)).name), op0, op1);
				break;
			case FRAMEBEGIN____:
			case FRAMEEND______:
				break;
			case GETINTRINSIC__:
				app("{");
				app("Atom atom = (Atom) callback.yawn((Node) ds[--dsp])");
				app("String intrinsicName = atom.toString().split(\"!\")[1]");
				app("#{reg} = new Data<>(Intrinsics.intrinsics.get(intrinsicName))", op0);
				app("}");
				break;
			case HEAD__________:
				app("#{reg} = Tree.decompose((Node) ds[--dsp]).getLeft()", op0);
				break;
			case IFFALSE_______:
				app("if (!#{reg-bool}) #{jump}", op0, op1);
				break;
			case IFNOTCONS_____:
				app("if ((tree = Tree.decompose(#{reg-node}, TermOp.OR____)) != null) {", op0);
				app("ds[dsp++] = tree.getLeft()");
				app("ds[dsp++] = tree.getRight()");
				app("} else #{jump}", op1);
				break;
			case IFNOTEQUALS___:
				app("if (#{reg} != #{reg}) #{jump}", op1, op2, op0);
				break;
			case IFNOTPAIR_____:
				app("if ((tree = Tree.decompose(#{reg-node}, TermOp.AND___)) != null) {", op0);
				app("ds[dsp++] = tree.getLeft()");
				app("ds[dsp++] = tree.getRight()");
				app("} else #{jump}", op1);
				break;
			case ISCONS________:
				app("#{reg} = Tree.decompose((Node) ds[--dsp]) != null", op0);
				break;
			case JUMP__________:
				app("#{jump}", op0);
				break;
			case JUMPCLOSURE___:
				app("if (#{reg-clos}.result == null) {", op0);
				pushCallee(ip);
				app("frame = #{reg-clos}.frame", op0);
				app("ip = #{reg-clos}.ip", op0);
				app("continue");
				app("} else returnValue = #{reg-clos}.result", op0);
				break;
			case LEAVE_________:
				generateFrame();
				break;
			case LOGREG________:
				app("LogUtil.info(#{reg}.toString())", op0);
				break;
			case NEWNODE_______:
				app("#{reg} = new Reference()", op0);
				break;
			case POP___________:
				app("#{reg} = ds[--dsp]", op0);
				break;
			case POPANY________:
				app("--dsp");
				break;
			case PROVESYS______:
				app("if (!systemPredicates.call(#{reg-node})) #{jump}", op0, op1);
				break;
			case PUSH__________:
				app("ds[dsp++] = #{reg-node}", op0);
				break;
			case REMARK________:
				break;
			case RESTORECSP____:
				app("csp = #{reg-num}", op0);
				break;
			case RESTOREDSP____:
				app("dsp = #{reg-num}", op0);
				break;
			case RETURN________:
				popCaller();
				break;
			case SETRESULT_____:
				app("returnValue = #{reg-node}", op0);
				break;
			case TAIL__________:
				app("#{reg} = Tree.decompose((Node) ds[--dsp]).getRight()", op0);
				break;
			case TOP___________:
				app("#{reg} = ds[dsp + #{num}]", op0, op1);
				break;
			default:
				throw new RuntimeException("Unknown instruction " + insn);
			}
		}
	}

	private void generateFrame() {
		AnalyzedFrame frame = currentFrame();
		List<AnalyzedRegister> registers = frame != null ? frame.getRegisters() : null;

		app(localsec, "#{fr-class} #{fr} = null");

		app(clazzsec, "private static class #{fr-class} implements Frame {");
		app(clazzsec, "private #{prev-fr-class} previous");
		app(clazzsec, "private #{fr-class}(#{prev-fr-class} previous) { this.previous = previous; }");

		for (int r = 0; r < registers.size(); r++) {
			AnalyzedRegister register = registers.get(r);
			Class<?> clazz = register.getClazz();
			String typeName = clazz.getSimpleName();

			if (!register.isTemporal())
				app(clazzsec, "private #{str} r#{num}", typeName, r);
			else {
				String init = clazz == boolean.class ? "false" : clazz == int.class ? "0" : "null";
				app(localsec, "#{str} f#{num}_r#{num} = #{str}", typeName, frame.getFrameBeginIp(), r, init);
			}
		}

		app(clazzsec, "}");
	}

	private void pushCallee(int ip) {
		app("cs[csp++] = " + ip);
	}

	private void popCaller() {
		app("ip = cs[--csp]");
		app("continue");
	}

	private void backupFrame() {
		app("fs[csp] = #{fr}");
	}

	private void restoreFrame() {
		app("#{fr} = (#{fr-class}) fs[csp]");
	}

	private String defineConstant(Node node) {
		String result = "const" + counter.getAndIncrement();
		String decl = "private static Node #{str} = Suite.parse(\"#{str}\")";
		app(clazzsec, decl, result, Formatter.dump(node));
		return result;
	}

	private void app(String fmt, Object... ps) {
		app(switchsec, fmt, ps);
	}

	private void app(StringBuilder section, String fmt, Object... ps) {
		List<Object> list = Arrays.asList(ps);
		Iterator<Object> iter = list.iterator();

		subst.subst(fmt, key -> decode(key, iter), section);

		char lastChar = section.charAt(section.length() - 1);

		if (lastChar != ';' && lastChar != '{' && lastChar != '}')
			section.append(";");

		section.append("\n");
	}

	private String decode(String s, Iterator<Object> iter) {
		int reg;
		AnalyzedFrame frame = currentFrame();
		AnalyzedFrame parentFrame = frame != null ? frame.getParent() : null;
		List<AnalyzedRegister> registers = frame != null ? frame.getRegisters() : null;

		switch (s) {
		case "fr":
			s = String.format("f%d", frame.getFrameBeginIp());
			break;
		case "fr-class":
			s = String.format("Frame%d", frame.getFrameBeginIp());
			break;
		case "jump":
			s = String.format("{ ip = %d; continue; }", iter.next());
			break;
		case "num":
			s = String.format("%d", iter.next());
			break;
		case "prev-fr":
			s = parentFrame != null ? String.format("f%d", parentFrame.getFrameBeginIp()) : "frame";
			break;
		case "prev-fr-class":
			s = parentFrame != null ? String.format("Frame%d", parentFrame.getFrameBeginIp()) : "Frame";
			break;
		case "reg":
			s = reg((int) iter.next());
			break;
		case "reg-bool":
			reg = (int) iter.next();
			s = reg(reg);
			if (Node.class.isAssignableFrom(registers.get(reg).getClazz()))
				s = "(" + s + " == TRUE)";
			break;
		case "reg-clos":
			reg = (int) iter.next();
			s = reg(reg);
			if (Node.class.isAssignableFrom(registers.get(reg).getClazz()))
				s = "((Thunk) " + s + ")";
			break;
		case "reg-node":
			reg = (int) iter.next();
			s = reg(reg);
			Class<?> sourceClazz = registers.get(reg).getClazz();
			if (sourceClazz == boolean.class)
				s = "(" + s + " ? TRUE : FALSE)";
			else if (sourceClazz == int.class)
				s = "Int.of(" + s + ")";
			break;
		case "reg-num":
			reg = (int) iter.next();
			s = reg(reg);
			if (Node.class.isAssignableFrom(registers.get(reg).getClazz()))
				s = "((Int) " + s + ").number";
			break;
		case "str":
			s = String.format("%s", iter.next());
		}

		return s;
	}

	private String reg(int reg) {
		AnalyzedFrame frame = currentFrame();
		int frameNo = frame.getFrameBeginIp();

		if (!frame.getRegisters().get(reg).isTemporal())
			return String.format("f%d.r%d", frameNo, reg);
		else
			return String.format("f%d_r%d", frameNo, reg);
	}

	private AnalyzedFrame currentFrame() {
		return analyzer.getFrame(currentIp);
	}

	private TranslatedRun getTranslatedRun(String java) throws IOException {
		String canonicalName = (!packageName.isEmpty() ? packageName + "." : "") + className;
		return jdkLoadClassUtil.newInstance(TranslatedRun.class, canonicalName, java);
	}

}
