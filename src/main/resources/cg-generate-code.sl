-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize .c0 .cx
	:- cg-optimize-jump-returns .c0 .c1
	, cg-optimize-lp-tail-calls .c1 .cx
#

cg-optimize-jump-returns .c0 .cx
	:- cg-optimize-jumps .c0 .c1
	, cg-optimize-assign-return .c1 .cx
#

cg-optimize-jumps (JUMP l:(.redirInsn, _), .insns) .cx
	:- cg-redirect-instruction .redirInsn
	, !, cg-optimize-jumps (.redirInsn, .insns) .cx
#
cg-optimize-jumps (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-jumps .insns0 .insns1
#
cg-optimize-jumps () () #

cg-redirect-instruction (RETURN) #
cg-redirect-instruction (RETURN-VALUE _) #

cg-optimize-assign-return (
	ASSIGN-FRAME-REG .r0 0 .r, RETURN-VALUE .r1, .insns0
) (
	RETURN-VALUE .r, .insns1
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
	, .li2 = (.call .target, .li3)
	, cg-is-restore-csp-dsp .li5/.li6 .ri0/.ri1
	, cg-is-skip .li6/.li7
	, cg-is-returning .li8
	, cg-verify-push-pop-bind-pairs .pairs
	, .ri3 = (.jump .target, .ri4)
	, !
	, cg-optimize-lp-tail-calls .li6 .ri4
#
cg-optimize-lp-tail-calls (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-lp-tail-calls .insns0 .insns1
#
cg-optimize-lp-tail-calls () () #

cg-push-pop-bind-pairs
(BIND-MARK .pr0, PUSH .pr1, .i)/.i
(POP-ANY, .j)/.j
(TOP .pr2 -3, BIND-UNDO .pr3, .k)/.k
.pr0/.pr1/.pr2/.pr3
#
cg-push-pop-bind-pairs .i/.i .j/.j .k/.k ()/()/()/() #

cg-verify-push-pop-bind-pairs .pr0/.pr1/.pr2/.pr3
	:- same .pr0 .pr1, same .pr2 .pr3
#

-- Limits the number of push/pop pair rearrangement to 2 to avoid changing the
-- bind journal pointer stored at the third location
cg-push-pop-pairs
(PUSH .r0, PUSH .r1, .i)/.i (POP-ANY, POP-ANY, .j)/.j
(PUSH .r0, PUSH .r1, .k)/.k (POP-ANY, POP-ANY, .l)/.l
	:- !
#
cg-push-pop-pairs .i/.i .j/.j .k/.k .l/.l #

cg-is-restore-csp-dsp
(RESTORE-DSP .dspReg, RESTORE-CSP .cspReg, .i)/.i
(RESTORE-DSP .dspReg, RESTORE-CSP .cspReg, .j)/.j
	:- !
#
cg-is-restore-csp-dsp .i/.i .j/.j #

cg-is-skip (REMARK _, .i0)/.ix :- cg-is-skip .i0/.ix #
cg-is-skip .i/.i #

cg-is-returning (RETURN, _) #
