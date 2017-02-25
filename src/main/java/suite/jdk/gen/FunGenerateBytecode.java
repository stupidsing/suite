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

	private class Record {
		private List<Instruction> list = new ArrayList<>();
		private Map<BranchInstruction, Integer> labels = new HashMap<>();
	}

	public FunGenerateBytecode(FunTypeInformation fti, ConstantPoolGen cpg) {
		this.fti = fti;
		this.factory = new InstructionFactory(cpg);
	}

	public InstructionList visit(FunExpr e, Type returnType) {
		Record r = new Record();
		visit0(r, e);
		r.list.add(InstructionFactory.createReturn(returnType));

		InstructionList il = new InstructionList();
		List<InstructionHandle> ihs = new ArrayList<>();

		for (Instruction instruction : r.list)
			ihs.add(instruction instanceof BranchInstruction //
					? il.append((BranchInstruction) instruction) //
					: il.append(instruction));

		for (Entry<BranchInstruction, Integer> entry : r.labels.entrySet())
			entry.getKey().setTarget(ihs.get(entry.getValue()));

		return il;
	}

	public void visit0(Record r, FunExpr e) {
		if (e instanceof AssignFunExpr) {
			AssignFunExpr expr = (AssignFunExpr) e;
			visit0(r, expr.value);
			r.list.add(InstructionFactory.createStore(fti.typeOf(expr.value), expr.index));
		} else if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit0(r, expr.left);
			visit0(r, expr.right);
			r.list.add(InstructionFactory.createBinaryOperation(expr.op, fti.typeOf(expr.left)));
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			visit0(r, expr.expr);
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			visit0(r, expr.expr);
			r.list.add(factory.createCheckCast(expr.type));
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			r.list.add(factory.createConstant(expr.constant));
		} else if (e instanceof FieldTypeFunExpr) {
			FieldTypeFunExpr expr = (FieldTypeFunExpr) e;
			visit0(r, expr.object);
			r.list.add(factory.createGetField(((ObjectType) fti.typeOf(expr.object)).getClassName(), expr.field, fti.typeOf(expr)));
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			visit0(r, expr.if_);
			visitIf(r, Const.IFEQ, expr);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			visit0(r, expr.left);
			visit0(r, expr.right);
			visitIf(r, (short) expr.opcode.applyAsInt(fti.typeOf(expr.left)), expr);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit0(r, expr.object);
			r.list.add(factory.createInstanceOf(expr.instanceType));
		} else if (e instanceof InvokeMethodFunExpr) {
			InvokeMethodFunExpr expr = (InvokeMethodFunExpr) e;
			Type array[] = Read.from(expr.parameters) //
					.map(fti::typeOf) //
					.toList() //
					.toArray(new Type[0]);

			short opcode = fti.invokeMethodOf(expr).getDeclaringClass().isInterface() ? Const.INVOKEINTERFACE : Const.INVOKEVIRTUAL;

			if (expr.object != null)
				visit0(r, expr.object);

			for (FunExpr parameter : expr.parameters)
				visit0(r, parameter);

			r.list.add(factory.createInvoke( //
					((ObjectType) fti.typeOf(expr.object)).getClassName(), //
					expr.methodName, //
					fti.typeOf(expr), //
					array, //
					opcode));
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			r.list.add(InstructionFactory.createLoad(fti.typeOf(expr), expr.index));
		} else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String sys = System.class.getName();
			r.list.add(factory.createGetStatic(sys, "out", Type.getType(PrintStream.class)));
			visit0(r, expr.expression);
			r.list.add(factory.createInvoke(sys, "println", fti.typeOf(expr), new Type[] { Type.STRING, }, Const.INVOKEVIRTUAL));
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit0(r, expr.left);
			if (!Objects.equals(fti.typeOf(expr.left), Type.VOID))
				r.list.add(InstructionConst.POP);
			visit0(r, expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			r.list.add(factory.createGetStatic(expr.clazzType, expr.field, expr.type));
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private void visitIf(Record r, short opcode, IfFunExpr expr) {
		BranchInstruction bh0 = InstructionFactory.createBranchInstruction(opcode, null);
		BranchInstruction bh1 = InstructionFactory.createBranchInstruction(Const.GOTO, null);
		r.list.add(bh0);
		visit0(r, expr.then);
		r.list.add(bh1);
		r.labels.put(bh0, r.list.size());
		visit0(r, expr.else_);
		r.labels.put(bh1, r.list.size());
	}

}
