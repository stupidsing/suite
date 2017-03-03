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

	private FunTypeInformation fti;
	private InstructionFactory factory;

	public final Map<Integer, Integer> jumps = new HashMap<>();
	private List<Instruction> list = new ArrayList<>();

	public FunGenerateBytecode(FunTypeInformation fti, ConstantPoolGen cpg) {
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

	public void visit0(FunExpr e) {
		if (e instanceof AssignFunExpr) {
			AssignFunExpr expr = (AssignFunExpr) e;
			visit0(expr.value);
			list.add(InstructionFactory.createStore(fti.typeOf(expr.value), expr.index));
		} else if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit0(expr.left);
			visit0(expr.right);
			list.add(InstructionFactory.createBinaryOperation(expr.op, fti.typeOf(expr.left)));
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			visit0(expr.expr);
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			visit0(expr.expr);
			list.add(factory.createCheckCast(expr.type));
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			list.add(factory.createConstant(expr.constant));
		} else if (e instanceof FieldTypeFunExpr) {
			FieldTypeFunExpr expr = (FieldTypeFunExpr) e;
			visit0(expr.object);
			list.add(factory.createGetField(((ObjectType) fti.typeOf(expr.object)).getClassName(), expr.field, fti.typeOf(expr)));
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			visit0(expr.if_);
			visitIf(Const.IFEQ, expr);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			visit0(expr.left);
			visit0(expr.right);
			visitIf((short) expr.opcode.applyAsInt(fti.typeOf(expr.left)), expr);
		} else if (e instanceof IfNonNullFunExpr) {
			IfNonNullFunExpr expr = (IfNonNullFunExpr) e;
			visit0(expr.object);
			visitIf(Const.IFNULL, expr);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit0(expr.object);
			list.add(factory.createInstanceOf(expr.instanceType));
		} else if (e instanceof InvokeMethodFunExpr) {
			InvokeMethodFunExpr expr = (InvokeMethodFunExpr) e;
			Type array[] = Read.from(expr.parameters).map(fti::typeOf).toArray(Type.class);

			Class<?> clazz = expr.clazz;
			String className = clazz != null ? clazz.getName() : ((ObjectType) fti.typeOf(expr.object)).getClassName();
			short opcode;

			if (expr.object == null)
				opcode = Const.INVOKESTATIC;
			else if (fti.invokeMethodOf(expr).getDeclaringClass().isInterface())
				opcode = Const.INVOKEINTERFACE;
			else
				opcode = Const.INVOKEVIRTUAL;

			if (expr.object != null)
				visit0(expr.object);

			for (FunExpr parameter : expr.parameters)
				visit0(parameter);

			list.add(factory.createInvoke( //
					className, //
					expr.methodName, //
					fti.typeOf(expr), //
					array, //
					opcode));
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			list.add(InstructionFactory.createLoad(fti.typeOf(expr), expr.index));
		} else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String sys = System.class.getName();
			list.add(factory.createGetStatic(sys, "out", Type.getType(PrintStream.class)));
			visit0(expr.expression);
			list.add(factory.createInvoke(sys, "println", fti.typeOf(expr), new Type[] { Type.STRING, }, Const.INVOKEVIRTUAL));
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit0(expr.left);
			if (!Objects.equals(fti.typeOf(expr.left), Type.VOID))
				list.add(InstructionConst.POP);
			visit0(expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			list.add(factory.createGetStatic(expr.clazzType, expr.field, expr.type));
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
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
