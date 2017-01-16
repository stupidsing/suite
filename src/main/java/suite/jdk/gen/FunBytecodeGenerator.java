package suite.jdk.gen;

import java.io.PrintStream;
import java.util.Objects;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javassist.bytecode.Opcode;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.CheckCastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.FieldFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.IfFunExpr;
import suite.jdk.gen.FunExpression.InstanceOfFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.NoOperationFunExpr;
import suite.jdk.gen.FunExpression.PrintlnFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
import suite.streamlet.Read;

public class FunBytecodeGenerator implements Opcodes {

	private MethodCreator mc;

	public FunBytecodeGenerator(MethodCreator mc) {
		this.mc = mc;
	}

	public void visit(MethodVisitor mv, FunExpr e) {
		if (e instanceof AssignFunExpr) {
			AssignFunExpr expr = (AssignFunExpr) e;
			visit(mv, expr.value);
			mv.visitVarInsn(TypeHelper.instance.choose(FunType.typeOf(expr.value), ASTORE, DSTORE, FSTORE, ISTORE, LSTORE), expr.index);
		} else if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			mv.visitInsn(expr.opcode.applyAsInt(FunType.typeOf(expr.left)));
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			visit(mv, expr.expr);
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			visit(mv, expr.expr);
			mv.visitTypeInsn(CHECKCAST, expr.type.getDescriptor());
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			mc.visitLdc(mv, expr);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			visit(mv, expr.object);
			mv.visitFieldInsn(GETFIELD, FunType.typeDesc(expr.object), expr.field, FunType.typeDesc(expr));
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			visit(mv, expr.if_);
			visitIf(IFEQ, mv, expr);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			visitIf(expr.opcode.applyAsInt(FunType.typeOf(expr.left)), mv, expr);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit(mv, expr.object);
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceType));
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			Type array[] = Read.from(expr.parameters) //
					.map(FunType::typeOf) //
					.toList() //
					.toArray(new Type[0]);

			int opcode = expr.method().getDeclaringClass().isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL;

			if (expr.object != null)
				visit(mv, expr.object);

			for (FunExpr parameter : expr.parameters)
				visit(mv, parameter);

			mv.visitMethodInsn( //
					opcode, //
					FunType.typeDesc(expr.object), //
					expr.methodName, //
					Type.getMethodDescriptor(FunType.typeOf(expr), array), //
					opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			mv.visitVarInsn(TypeHelper.instance.choose(FunType.typeOf(expr), ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.index);
		} else if (e instanceof NoOperationFunExpr)
			;
		else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String td = Type.getDescriptor(PrintStream.class);
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", td);
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit(mv, expr.left);
			if (!Objects.equals(FunType.typeOf(expr.left), Type.VOID_TYPE))
				mv.visitInsn(POP);
			visit(mv, expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			mv.visitFieldInsn(GETSTATIC, expr.clazzType, expr.field, expr.type.getDescriptor());
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private void visitIf(int opcode, MethodVisitor mv, IfFunExpr expr) {
		Label l0 = new Label();
		Label l1 = new Label();
		mv.visitJumpInsn(opcode, l0);
		visit(mv, expr.then);
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l0);
		visit(mv, expr.else_);
		mv.visitLabel(l1);
	}

}
