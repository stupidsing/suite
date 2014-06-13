-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize-segment .c/() .co0/.cox
	:- cg-optimize .c .co
	, append .co .cox .co0
#

cg-optimize .c0 .cx
	:- cg-optimize-dup-labels .c0 .c1
	, cg-optimize-jump-returns .c1 .c2
	, cg-optimize-lp-tail-calls .c2 .c3
	, cg-interprete-labels .c3 .cx
#

cg-optimize-dup-labels (.label LABEL, .label LABEL, .insns0) .insns1
	:- !, cg-optimize-dup-labels (.label LABEL, .insns0) .insns1
#
cg-optimize-dup-labels (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-dup-labels .insns0 .insns1
#
cg-optimize-dup-labels () () #

cg-optimize-jump-returns .c0 .cx
	:- cg-optimize-jumps .c0 .c1
	, cg-optimize-assign-return .c1 .cx
#

cg-optimize-jumps (_ JUMP l:(_ LABEL, _ .redirInsn, _), .insns) .cx
	:- cg-redirect-instruction .redirInsn
	, !, cg-optimize-jumps (_ .redirInsn, .insns) .cx
#
cg-optimize-jumps (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-jumps .insns0 .insns1
#
cg-optimize-jumps () () #

cg-redirect-instruction (JUMP _) #
cg-redirect-instruction (RETURN) #
cg-redirect-instruction (RETURN-VALUE _) #

cg-optimize-assign-return (
	_ ASSIGN-FRAME-REG .r0 0 .r, _ RETURN-VALUE .r1, .insns0
) (
	_ RETURN-VALUE .r, .insns1
)
	:- same .r0 .r1
	, !, cg-optimize-assign-return .insns0 .insns1
#
cg-optimize-assign-return (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-assign-return .insns0 .insns1
#
cg-optimize-assign-return () () #

cg-optimize-lp-tail-calls .li0 .ri0
	:- cg-push-pop-bind-pairs .li0/.li1 .li4/.li5 .li7/.li8 .pairs
	, cg-push-pop-pairs .li1/.li2 .li3/.li4 .ri2/.ri3 .ri1/.ri2
	, member (CALL/JUMP, CALL-CLOSURE/JUMP-CLOSURE, CALL-REG/JUMP-REG,) .call/.jump
	, .li2 = (_ .call .target, .li3)
	, cg-is-restore-csp-dsp .li5/.li6 .ri0/.ri1
	, cg-is-skip .li6/.li7
	, cg-is-returning .li8
	, cg-verify-push-pop-bind-pairs .pairs
	, .ri3 = (_ .jump .target, .ri4)
	, !
	, cg-optimize-lp-tail-calls .li6 .ri4
#
cg-optimize-lp-tail-calls (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-lp-tail-calls .insns0 .insns1
#
cg-optimize-lp-tail-calls () () #

cg-push-pop-bind-pairs
(_ BIND-MARK .pr0, _ PUSH .pr1, .i)/.i
(_ POP-ANY, .j)/.j
(_ TOP .pr2 -3, _ BIND-UNDO .pr3, .k)/.k
.pr0/.pr1/.pr2/.pr3
#
cg-push-pop-bind-pairs .i/.i .j/.j .k/.k ()/()/()/() #

cg-verify-push-pop-bind-pairs .pr0/.pr1/.pr2/.pr3
	:- same .pr0 .pr1, same .pr2 .pr3
#

-- Limits the number of push/pop pair rearrangement to 2 to avoid changing the
-- bind journal pointer stored at the third location
cg-push-pop-pairs
(_ PUSH .r0, _ PUSH .r1, .i)/.i (_ POP-ANY, _ POP-ANY, .j)/.j
(_ PUSH .r0, _ PUSH .r1, .k)/.k (_ POP-ANY, _ POP-ANY, .l)/.l
	:- !
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

cg-interprete-labels (.insn0, .insns0) (.insn1, .insns1)
	:- !, cg-interprete-label .insn0 .insn1
	, cg-interprete-labels .insns0 .insns1
#
cg-interprete-labels () () #

cg-interprete-label (.label .insn .op0 .op1 l:(.refLabel .id, _)) (.label .insn .op0 .op1 i:.refLabel) :- same .id LABEL, ! #
cg-interprete-label (.label .insn .op0 l:(.refLabel .id, _)) (.label .insn .op0 i:.refLabel) :- same .id LABEL, ! #
cg-interprete-label (.label .insn l:(.refLabel .id, _)) (.label .insn i:.refLabel) :- same .id LABEL, ! #
cg-interprete-label .insn .insn #
