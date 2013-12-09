-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize-segment .c/() .co0/.cox
	:- cg-optimize .c .co
	, append .co .cox .co0
#

cg-optimize .c0 .cx
	:- cg-optimize-dup-labels .c0 .c1
	, cg-optimize-jumps .c1 .c2
	, cg-optimize-tail-calls .c2 .cx
#

cg-optimize-dup-labels (.label LABEL, .label LABEL, .insns0) .insns1
	:- !, cg-optimize-dup-labels (.label LABEL, .insns0) .insns1
#
cg-optimize-dup-labels (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-dup-labels .insns0 .insns1
#
cg-optimize-dup-labels () () #

cg-optimize-jumps .c .d
	:- clone .c .cc
	, cg-label-next-instruction .c .cc
	, cg-substitute-redirections .c .cc .d -- Perform several passes?
#

cg-label-next-instruction .c0 .cc0
	:- .c0 = (_ LABEL, _ .nextInsn, .cs)
	, .cc0 = (.nextInsn LABEL, _, .ccs)
	, !, cg-label-next-instruction .cs .ccs
#
cg-label-next-instruction (_, .cs) (() _, .ccs)
	:- !, cg-label-next-instruction .cs .ccs
#
cg-label-next-instruction () () #

cg-substitute-redirections (.label _, .cs) (_ JUMP .redirInsn, .ccs) (.label .redirInsn, .ds)
	:- cg-redirect-instruction .redirInsn
	, !, cg-substitute-redirections .cs .ccs .ds
#
cg-substitute-redirections (.insn, .cs) (_, .ccs) (.insn, .ds)
	:- !, cg-substitute-redirections .cs .ccs .ds
#
cg-substitute-redirections () () () #

cg-redirect-instruction (JUMP _) #
cg-redirect-instruction (RETURN) #
cg-redirect-instruction (RETURN-VALUE _) #

cg-optimize-tail-calls .li0 .ri0
	:- cg-push-pop-bind-pairs .li0/.li1 .li4/.li5 .li7/.li8 .pairs
	, cg-push-pop-pairs .li1/.li2 .li3/.li4 .ri2/.ri3 .ri1/.ri2
	, member (CALL/JUMP, CALL-REG/JUMP-REG,) .call/.jump
	, .li2 = (_ .call .target, .li3)
	, cg-is-restore-csp-dsp .li5/.li6 .ri0/.ri1
	, cg-is-skip .li6/.li7
	, cg-is-returning .li8
	, cg-verify-push-pop-bind-pairs .pairs
	, .ri3 = (_ .jump .target, .ri4)
	, !
	, cg-optimize-tail-calls .li6 .ri4
#
cg-optimize-tail-calls (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-tail-calls .insns0 .insns1
#
cg-optimize-tail-calls () () #

cg-push-pop-bind-pairs
(_ BIND-MARK .pr0, _ PUSH .pr1, .i)/.i
(_ POP-ANY, .j)/.j
(_ TOP .pr2 -3, _ BIND-UNDO .pr3, .k)/.k
.pr0/.pr1/.pr2/.pr3
#
cg-push-pop-bind-pairs .i/.i .j/.j ()/()/()/() #

cg-verify-push-pop-bind-pairs .pr0/.pr1/.pr2/.pr3
	:- same .pr0 .pr1, same .pr2 .pr3
#

cg-push-pop-pairs
(_ PUSH .reg, .i0)/.ix (_ POP-ANY, .j0)/.jx
(_ PUSH .reg, .k0)/.kx (_ POP-ANY, .l0)/.lx
	:- !, cg-push-pop-pairs .i0/.ix .j0/.jx .k0/.kx .l0/.lx
#
cg-push-pop-pairs .i/.i .j/.j .k/.k .l/.l #

cg-is-restore-csp-dsp
(_ RESTORE-DSP .dspReg, _ RESTORE-CSP .cspReg, .i)/.i
(_ RESTORE-DSP .dspReg, _ RESTORE-CSP .cspReg, .j)/.j
	:- !
#
cg-is-restore-csp-dsp .i/.i .j/.j #

cg-is-skip (_ LABEL, .i0)/.ix :- cg-is-skip .i0/.ix #
cg-is-skip (_ REMARK _, .i0)/.ix :- cg-is-skip .i0/.ix #
cg-is-skip .i/.i #

cg-is-returning (_ RETURN, _) #

cg-generate-code .code :- cg-assign-line-numbers 0 .code, ! #

cg-assign-line-numbers _ () #
cg-assign-line-numbers .n (.n _, .insns)
	:- let .n1 (.n + 1), cg-assign-line-numbers .n1 .insns
#
