package suite.jdk.gen.pass;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExprM.ArrayFunExpr;
import suite.jdk.gen.FunExprM.ArrayLengthFunExpr;
import suite.jdk.gen.FunExprM.AssignLocalFunExpr;
import suite.jdk.gen.FunExprM.BinaryFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.ConstantFunExpr;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.If1FunExpr;
import suite.jdk.gen.FunExprM.If2FunExpr;
import suite.jdk.gen.FunExprM.IfFunExpr;
import suite.jdk.gen.FunExprM.IfNonNullFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.jdk.gen.FunExprM.LocalFunExpr;
import suite.jdk.gen.FunExprM.NewFunExpr;
import suite.jdk.gen.FunExprM.PrintlnFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExprM.SeqFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.primitive.adt.map.IntIntMap;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.Read;
import suite.util.Switch;

public class FunGenerateBytecode {

	private String className;
	private InstructionFactory factory;
	private ConstantPoolGen cpg;
	private FunTypeInformation fti;

	public final IntObjMap<Object> constants = new IntObjMap<>();
	public final IntIntMap jumps = new IntIntMap();

	private List<Instruction> list = new ArrayList<>();

	public FunGenerateBytecode(String className, FunTypeInformation fti, ConstantPoolGen cpg) {
		this.className = className;
		this.fti = fti;
		this.cpg = cpg;
		this.factory = new InstructionFactory(cpg);
	}

	/**
	 * Generate bytecode suitable for method. Caller to dispose the returned
	 * InstructionList object.
	 */
	public InstructionList visit(FunExpr e, Type returnType) {
		visit_(e);
		list.add(InstructionFactory.createReturn(returnType));

		InstructionList il = new InstructionList();
		List<InstructionHandle> ihs = new ArrayList<>();

		for (Instruction instruction : list)
			ihs.add(instruction instanceof BranchInstruction //
					? il.append((BranchInstruction) instruction) //
					: il.append(instruction));

		jumps.forEach((src, tgt) -> ((BranchInstruction) ihs.get(src).getInstruction()).setTarget(ihs.get(tgt)));

		return il;
	}

	public void visit_(FunExpr e0) {
		new Switch<FunExpr>(e0 //
		).doIf(ArrayFunExpr.class, e1 -> {
			FunExpr[] elements = e1.elements;
			list.add(factory.createConstant(elements.length));
			list.add(factory.createNewArray(Type.getType(e1.clazz), (short) 1));
			for (int i = 0; i < elements.length; i++) {
				FunExpr element = elements[i];
				if (element != null) {
					list.add(InstructionFactory.createDup(1));
					list.add(factory.createConstant(i));
					visit_(element);
					list.add(InstructionFactory.createArrayStore(fti.typeOf(element)));
				}
			}
		}).doIf(ArrayLengthFunExpr.class, e1 -> {
			visit_(e1.expr);
			list.add(new ARRAYLENGTH());
		}).doIf(AssignLocalFunExpr.class, e1 -> {
			visit_(e1.value);
			list.add(InstructionFactory.createStore(fti.typeOf(e1.value), ((LocalFunExpr) e1.var).index));
		}).doIf(BinaryFunExpr.class, e1 -> {
			visit_(e1.left);
			visit_(e1.right);
			list.add(InstructionFactory.createBinaryOperation(e1.op, fti.typeOf(e1.left)));
		}).doIf(CastFunExpr.class, e1 -> {
			visit_(e1.expr);
		}).doIf(CheckCastFunExpr.class, e1 -> {
			visit_(e1.expr);
			list.add(factory.createCheckCast(e1.type));
		}).doIf(ConstantFunExpr.class, e1 -> {
			list.add(factory.createConstant(e1.constant));
		}).doIf(FieldStaticFunExpr.class, e1 -> {
			list.add(factory.createGetStatic(className, e1.fieldName, e1.fieldType));
		}).doIf(FieldTypeFunExpr.class, e1 -> {
			visit_(e1.object);
			list.add(factory.createGetField(((ObjectType) fti.typeOf(e1.object)).getClassName(), e1.fieldName, e1.fieldType));
		}).doIf(If1FunExpr.class, e1 -> {
			visit_(e1.if_);
			visitIf(Const.IFEQ, e1);
		}).doIf(If2FunExpr.class, e1 -> {
			visit_(e1.left);
			visit_(e1.right);
			visitIf((short) e1.opcode.apply(fti.typeOf(e1.left)), e1);
		}).doIf(IfNonNullFunExpr.class, e1 -> {
			visit_(e1.object);
			visitIf(Const.IFNULL, e1);
		}).doIf(IndexFunExpr.class, e1 -> {
			visit_(e1.array);
			visit_(e1.index);
			list.add(InstructionFactory.createArrayLoad(fti.typeOf(e1)));
		}).doIf(InstanceOfFunExpr.class, e1 -> {
			visit_(e1.object);
			list.add(factory.createInstanceOf(e1.instanceType));
		}).doIf(InvokeMethodFunExpr.class, e1 -> {
			Type[] array = Read.from(e1.parameters).map(fti::typeOf).toArray(Type.class);

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
				visit_(e1.object);

			for (FunExpr parameter : e1.parameters)
				visit_(parameter);

			list.add(factory.createInvoke( //
					className, //
					e1.methodName, //
					fti.typeOf(e1), //
					array, //
					opcode));
		}).doIf(LocalFunExpr.class, e1 -> {
			list.add(InstructionFactory.createLoad(fti.typeOf(e1), e1.index));
		}).doIf(NewFunExpr.class, e1 -> {
			Class<?> implClass = e1.implementationClass;
			String implClassName = e1.className;
			int classIndex = cpg.addClass(implClassName);
			list.add(new NEW(classIndex));
			list.add(InstructionFactory.createDup(1));
			list.add(factory.createInvoke(implClassName, "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));

			for (Entry<String, FunExpr> e : e1.fieldValues.entrySet()) {
				FunExpr value = e.getValue();
				list.add(InstructionFactory.createDup(1));
				visit_(value);
				list.add(factory.createPutField(implClassName, e.getKey(), fti.typeOf(value)));
			}

			constants.put(classIndex, implClass);
		}).doIf(PrintlnFunExpr.class, e1 -> {
			String sys = System.class.getName();
			list.add(factory.createGetStatic(sys, "out", Type.getType(PrintStream.class)));
			visit_(e1.expression);
			list.add(factory.createInvoke(sys, "println", fti.typeOf(e1), new Type[] { Type.STRING, }, Const.INVOKEVIRTUAL));
		}).doIf(ProfileFunExpr.class, e1 -> {
			list.add(InstructionFactory.createLoad(Type.OBJECT, 0));
			list.add(InstructionFactory.createDup(1));
			list.add(factory.createGetField(className, e1.counterFieldName, Type.INT));
			list.add(factory.createConstant(1));
			list.add(InstructionFactory.createBinaryOperation("+", Type.INT));
			list.add(factory.createPutField(className, e1.counterFieldName, Type.INT));
			visit_(e1.do_);
		}).doIf(SeqFunExpr.class, e1 -> {
			visit_(e1.left);
			if (!Objects.equals(fti.typeOf(e1.left), Type.VOID))
				list.add(InstructionConst.POP);
			visit_(e1.right);
		}).nonNullResult();
	}

	private void visitIf(short opcode, IfFunExpr expr) {
		int p0 = list.size();
		list.add(InstructionFactory.createBranchInstruction(opcode, null));
		visit_(expr.then);
		int p1 = list.size();
		list.add(InstructionFactory.createBranchInstruction(Const.GOTO, null));
		jumps.put(p0, list.size());
		visit_(expr.else_);
		jumps.put(p1, list.size());
	}

}
