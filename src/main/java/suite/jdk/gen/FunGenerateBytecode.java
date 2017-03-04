package suite.jdk.gen;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.CheckCastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.FieldTypeFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.IfFunExpr;
import suite.jdk.gen.FunExpression.IfNonNullFunExpr;
import suite.jdk.gen.FunExpression.InstanceOfFunExpr;
import suite.jdk.gen.FunExpression.InvokeMethodFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.PrintlnFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
import suite.streamlet.Read;

public class FunGenerateBytecode {

	private String className;
	private FunTypeInformation fti;
	private InstructionFactory factory;

	public final Map<Integer, Integer> jumps = new HashMap<>();
	private List<Instruction> list = new ArrayList<>();

	public FunGenerateBytecode(String className, FunTypeInformation fti, ConstantPoolGen cpg) {
		this.className = className;
		this.fti = fti;
		this.factory = new InstructionFactory(cpg);
	}

	/**
	 * Generate bytecode suitable for method. Caller to dispose the returned
	 * InstructionList object.
	 */
	public InstructionList visit(FunExpr e, Type returnType) {
		visit0(e);
		list.add(InstructionFactory.createReturn(returnType));

		InstructionList il = new InstructionList();
		List<InstructionHandle> ihs = new ArrayList<>();

		for (Instruction instruction : list)
			ihs.add(instruction instanceof BranchInstruction //
					? il.append((BranchInstruction) instruction) //
					: il.append(instruction));

		for (Entry<Integer, Integer> entry : jumps.entrySet())
			((BranchInstruction) ihs.get(entry.getKey()).getInstruction()).setTarget(ihs.get(entry.getValue()));

		return il;
	}

	public void visit0(FunExpr e0) {
		if (e0 instanceof AssignFunExpr) {
			AssignFunExpr e1 = (AssignFunExpr) e0;
			visit0(e1.value);
			list.add(InstructionFactory.createStore(fti.typeOf(e1.value), e1.index));
		} else if (e0 instanceof BinaryFunExpr) {
			BinaryFunExpr e1 = (BinaryFunExpr) e0;
			visit0(e1.left);
			visit0(e1.right);
			list.add(InstructionFactory.createBinaryOperation(e1.op, fti.typeOf(e1.left)));
		} else if (e0 instanceof CastFunExpr) {
			CastFunExpr e1 = (CastFunExpr) e0;
			visit0(e1.expr);
		} else if (e0 instanceof CheckCastFunExpr) {
			CheckCastFunExpr e1 = (CheckCastFunExpr) e0;
			visit0(e1.expr);
			list.add(factory.createCheckCast(e1.type));
		} else if (e0 instanceof ConstantFunExpr) {
			ConstantFunExpr e1 = (ConstantFunExpr) e0;
			list.add(factory.createConstant(e1.constant));
		} else if (e0 instanceof FieldTypeFunExpr) {
			FieldTypeFunExpr e1 = (FieldTypeFunExpr) e0;
			visit0(e1.object);
			list.add(factory.createGetField(((ObjectType) fti.typeOf(e1.object)).getClassName(), e1.field, fti.typeOf(e1)));
		} else if (e0 instanceof If1FunExpr) {
			If1FunExpr e1 = (If1FunExpr) e0;
			visit0(e1.if_);
			visitIf(Const.IFEQ, e1);
		} else if (e0 instanceof If2FunExpr) {
			If2FunExpr e1 = (If2FunExpr) e0;
			visit0(e1.left);
			visit0(e1.right);
			visitIf((short) e1.opcode.applyAsInt(fti.typeOf(e1.left)), e1);
		} else if (e0 instanceof IfNonNullFunExpr) {
			IfNonNullFunExpr e1 = (IfNonNullFunExpr) e0;
			visit0(e1.object);
			visitIf(Const.IFNULL, e1);
		} else if (e0 instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr e1 = (InstanceOfFunExpr) e0;
			visit0(e1.object);
			list.add(factory.createInstanceOf(e1.instanceType));
		} else if (e0 instanceof InvokeMethodFunExpr) {
			InvokeMethodFunExpr e1 = (InvokeMethodFunExpr) e0;
			Type array[] = Read.from(e1.parameters).map(fti::typeOf).toArray(Type.class);

			Class<?> clazz = e1.clazz;
			String className = clazz != null ? clazz.getName() : ((ObjectType) fti.typeOf(e1.object)).getClassName();
			short opcode;

			if (e1.object == null)
				opcode = Const.INVOKESTATIC;
			else if (fti.invokeMethodOf(e1).getDeclaringClass().isInterface())
				opcode = Const.INVOKEINTERFACE;
			else
				opcode = Const.INVOKEVIRTUAL;

			if (e1.object != null)
				visit0(e1.object);

			for (FunExpr parameter : e1.parameters)
				visit0(parameter);

			list.add(factory.createInvoke( //
					className, //
					e1.methodName, //
					fti.typeOf(e1), //
					array, //
					opcode));
		} else if (e0 instanceof LocalFunExpr) {
			LocalFunExpr e1 = (LocalFunExpr) e0;
			list.add(InstructionFactory.createLoad(fti.typeOf(e1), e1.index));
		} else if (e0 instanceof PrintlnFunExpr) {
			PrintlnFunExpr e1 = (PrintlnFunExpr) e0;
			String sys = System.class.getName();
			list.add(factory.createGetStatic(sys, "out", Type.getType(PrintStream.class)));
			visit0(e1.expression);
			list.add(factory.createInvoke(sys, "println", fti.typeOf(e1), new Type[] { Type.STRING, }, Const.INVOKEVIRTUAL));
		} else if (e0 instanceof SeqFunExpr) {
			SeqFunExpr e1 = (SeqFunExpr) e0;
			visit0(e1.left);
			if (!Objects.equals(fti.typeOf(e1.left), Type.VOID))
				list.add(InstructionConst.POP);
			visit0(e1.right);
		} else if (e0 instanceof StaticFunExpr) {
			StaticFunExpr e1 = (StaticFunExpr) e0;
			list.add(factory.createGetStatic(className, e1.field, e1.type));
		} else
			throw new RuntimeException("Unknown expression " + e0.getClass());
	}

	private void visitIf(short opcode, IfFunExpr expr) {
		int p0 = list.size();
		list.add(InstructionFactory.createBranchInstruction(opcode, null));
		visit0(expr.then);
		int p1 = list.size();
		list.add(InstructionFactory.createBranchInstruction(Const.GOTO, null));
		jumps.put(p0, list.size());
		visit0(expr.else_);
		jumps.put(p1, list.size());
	}

}
