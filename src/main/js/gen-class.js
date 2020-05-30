fs = require('fs');

let clazz = 'P2';

let toObjects = (fields, s) => {
	return s.trim().split('\n').filter(line => line.length).map(line => {
		let array = line.split(' ');
		let object = {};
		for (let i in array) object[fields[i]] = array[i].trim().replace(',', ', ');
		return object;
	});
};

let objectType = type => {
	if (type === 'boolean') return 'Boolean';
	else if (type === 'int') return 'Integer';
	else if (type === 'long') return 'Long';
	else return type;
};

let defs = [
	['FunpAllocGlobal', `
		int size
		Funp value
		Funp expr
		Mutable<Operand> address
	`],
	['FunpAllocReg', `
		int size
		Funp value
		Funp expr
		Mutable<Operand> reg
	`],
	['FunpAllocStack', `
		int size
		Funp value
		Funp expr
		IntMutable stack
	`],
	['FunpAssignMem', `
		FunpMemory target
		Funp value
		Funp expr
	`],
	['FunpAssignOp', `
		FunpOperand target
		Funp value
		Funp expr
	`],
	['FunpAssignOp2', `
		FunpOperand2 target
		Funp value
		Funp expr
	`],
	['FunpCmp', `
		Operator operator
		FunpMemory left
		FunpMemory right
	`],
	['FunpData', `
		List<Pair<Funp,IntRange>> pairs
	`],
	['FunpFramePointer', `
	`],
	['FunpHeapAlloc', `
		boolean isDynamicSize
		int size
	`],
	['FunpHeapDealloc', `
		boolean isDynamicSize
		int size
		Funp reference
		Funp expr
	`],
	['FunpInvoke1', `
		Funp routine
		int is
		int os
		int istack
		int ostack
	`],
	['FunpInvoke2', `
		Funp routine
		int is
		int os
		int istack
		int ostack
	`],
	['FunpInvokeIo', `
		Funp routine
		int is
		int os
		int istack
		int ostack
	`],
	['FunpLambdaCapture', `
		FunpVariable fpIn
		FunpVariable frameVar
		FunpStruct struct
		String vn
		Funp expr
		Fct fct
	`, `P2.End`],
	['FunpMemory', `
		Funp pointer
		int start
		int end
	`, `P4.End`, `

		public int size() {
			return end - start;
		}`],
	['FunpOp', `
		int opSize
		Object operator
		Funp left
		Funp right
	`],
	['FunpOperand', `
		Mutable<Operand> operand
	`],
	['FunpOperand2', `
		Mutable<Operand> operand0
		Mutable<Operand> operand1
	`],
	['FunpRoutine1', `
		Funp frame
		Funp expr
		int is
		int os
		int istack
		int ostack
	`],
	['FunpRoutine2', `
		Funp frame
		Funp expr
		int is
		int os
		int istack
		int ostack
	`],
	['FunpRoutineIo', `
		Funp frame
		Funp expr
		int is
		int os
		int istack
		int ostack
	`],
	['FunpSaveRegisters0', `
		Funp expr
		Mutable<ArrayList<Pair<OpReg,Integer>>> saves
	`],
	['FunpSaveRegisters1', `
		Funp expr
		Mutable<ArrayList<Pair<OpReg,Integer>>> saves
	`],
	['FunpTypeAssign', `
		FunpVariable left
		Funp right
		Funp expr
	`, `P2.End`],
].map(([c, members, implements, extras]) => ({
	c,
	extras: extras || '',
	implements: implements || 'P4.End',
	members: toObjects(['type', 'name'], members),
}));

let java0 = `package suite.funp;

import java.util.*;

import primal.adt.Fixie_.*;
import primal.adt.*;
import primal.parser.Operator;
import primal.primitive.adt.*;
import suite.assembler.Amd64.*;
import suite.funp.Funp_.Funp;
import suite.funp.P0.*;

public class ${clazz} {

	public interface End {
	}

	${defs.map(({c, extras, implements, members}) => `public static class ${c} implements Funp, ${implements} {
		${members.map(e => `public ${e.type} ${e.name};`).join(`
		`)}

		public static ${c} of(${members.map(e => `${e.type} ${e.name}`).join(`, `)}) {
			var f = new ${c}();
			${members.map(({ name }) => `f.${name} = ${name};`).join(`
			`)}
			return f;
		}

		public <R> R apply(FixieFun${members.length}<${members.map(e => `${objectType(e.type)}, `).join(``)}R> fun) {
			return fun.apply(${members.map(e => e.name).join(`, `)});
		}${extras}
	}`).join(`

	`)}

}`;

let java = java0.split('\n').map(line => line && !/^\s*$/.test(line) ? line : '').join('\n');

// let data = fs.readFileSync(`src/main/java`, 'utf8');
fs.writeFileSync(`src/main/java/suite/funp/${clazz}.java`, java);
