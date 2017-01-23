package suite.jdk.gen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import suite.editor.Listen.SinkEx;
import suite.jdk.gen.FunExpression.ConstantFunExpr;

public class Method_ implements Opcodes {

	public void create(ClassWriter cw, String mn, String md, SinkEx<MethodVisitor, ReflectiveOperationException> sink) {
		create(cw, mn, md, false, sink);
	}

	public void create(ClassWriter cw, String mn, String md, boolean isLog,
			SinkEx<MethodVisitor, ReflectiveOperationException> sink) {
		Textifier textifier = isLog ? new Textifier() : null;
		MethodVisitor mv0 = cw.visitMethod( //
				ACC_PUBLIC, //
				mn, //
				md, //
				null, //
				null);
		MethodVisitor mv = textifier != null ? new TraceMethodVisitor(mv0, textifier) : mv0;

		try {
			sink.sink(mv);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}

		mv.visitEnd();

		if (textifier != null) {
			String log;

			try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
				textifier.print(pw);
				log = sw.toString();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			System.out.println(log);
		}
	}

	public void visitLdc(MethodVisitor mv, ConstantFunExpr expr) {
		if (expr.constant == null)
			mv.visitInsn(ACONST_NULL);
		else if (Objects.equals(expr.type, Type.DOUBLE_TYPE)) {
			double d = ((Number) expr.constant).doubleValue();
			if (d == 0d)
				mv.visitInsn(DCONST_0);
			else if (d == 1d)
				mv.visitInsn(DCONST_1);
			else
				mv.visitLdcInsn(expr.constant);
		} else if (Objects.equals(expr.type, Type.FLOAT_TYPE)) {
			float f = ((Number) expr.constant).floatValue();
			if (f == 0f)
				mv.visitInsn(FCONST_0);
			else if (f == 1f)
				mv.visitInsn(FCONST_1);
			else if (f == 2f)
				mv.visitInsn(FCONST_2);
			else
				mv.visitLdcInsn(expr.constant);
		} else if (Objects.equals(expr.type, Type.INT_TYPE)) {
			int i = ((Number) expr.constant).intValue();
			switch (i) {
			case -1:
				mv.visitInsn(ICONST_M1);
				break;
			case 0:
				mv.visitInsn(ICONST_0);
				break;
			case 1:
				mv.visitInsn(ICONST_1);
				break;
			case 2:
				mv.visitInsn(ICONST_2);
				break;
			case 3:
				mv.visitInsn(ICONST_3);
				break;
			case 4:
				mv.visitInsn(ICONST_4);
				break;
			case 5:
				mv.visitInsn(ICONST_5);
				break;
			default:
				mv.visitLdcInsn(expr.constant);
			}
		} else if (Objects.equals(expr.type, Type.LONG_TYPE)) {
			long l = ((Number) expr.constant).longValue();
			if (l == 0l)
				mv.visitInsn(LCONST_0);
			else if (l == 1l)
				mv.visitInsn(LCONST_1);
			else
				mv.visitLdcInsn(expr.constant);
		} else
			mv.visitLdcInsn(expr.constant);
	}

}
