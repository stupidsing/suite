-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize .c0 .cx
	:- tree.intern .key CG-OPTIMIZE ':' .c0
	, once (intern.map.get .key .cx
		; cg-optimize0 .c0 .cx
		, intern.map.put .key .cx
	)
#

cg-optimize0 .c0 .cx
	:- cg-optimize-jumps .c0 .c1
	, cg-optimize-assign-returns .c1 .c2
	, cg-optimize-lp-tail-calls .c2 .cx
#

cg-optimize-jumps (JUMP l:(.redirInsn, _), .insns) .cx
	:- cg-redirect-instruction .redirInsn
	, !, cg-optimize-jumps (.redirInsn, .insns) .cx
#
cg-optimize-jumps (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-jumps .insns0 .insns1
#
cg-optimize-jumps () () #

cg-redirect-instruction (JUMP _) #
cg-redirect-instruction (RETURN) #
cg-redirect-instruction (RETURN-VALUE _) #

cg-optimize-assign-returns (
	ASSIGN-FRAME-REG .r0 0 .r, RETURN-VALUE .r1, .insns0
) (
	RETURN-VALUE .r, .insns1
)
	:- same .r0 .r1
	, !, cg-optimize-assign-returns .insns0 .insns1
#
cg-optimize-assign-returns (.insn, .insns0) (.insn, .insns1)
	:- !, cg-optimize-assign-returns .insns0 .insns1
#
cg-optimize-assign-returns () () #

cg-optimize-lp-tail-calls .li0 .ri0
	:- cg-push-pop-bind-pairs .li0/.li1 .li4/.li5 .li7/.li8 .pairs
	, cg-push-pop-pairs .li1/.li2 .li3/.li4 .ri2/.ri3 .ri1/.ri2
	, member (CALL/JUMP, CALL-CLOSURE/JUMP-CLOSURE, CALL-REG/JUMP-REG,) .call/.jump
	, .li2 = (.call .op, .li3)
	, cg-is-restore-csp-dsp .li5/.li6 .ri0/.ri1
	, cg-is-skip .li6/.li7
	, cg-is-returning .li8
	, cg-verify-push-pop-bind-pairs .pairs
	, (.jump = JUMP, .op = l:.target, .ri3 = .target; .ri3 = (.jump .op,))
	, !
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
