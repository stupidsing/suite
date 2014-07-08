-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize .c0 _
	:- not bound .c0, !
#
cg-optimize .c0 .cx
	:- tree.intern .key0 CG-OPTIMIZE ':' .c0
	, once (intern.map.contains .key0, .cached = true; .cached = false)
	, intern.map.put .key0 .cx
	, once (.cached = true
		; cg-optimize0 .c0 .cx, tree.intern .keyx CG-OPTIMIZE ':' .cx, intern.map.put .keyx .cx
	)
#

cg-optimize0 (.insn0, .insns0) .cx
	:- cg-optimize .insns0 .insns1
	, .c0 = (.insn0, .insns1)
	, cg-optimize-jumps .c0 .c1
	, cg-optimize-assign-returns .c1 .c2
	, cg-optimize-stack-usage .c2 .c3
	, cg-optimize-tail-calls .c3 .c4
	, cg-optimize-branches .c4 .cx
#
cg-optimize0 () () #

cg-optimize-jumps (JUMP l:(.redirInsn, _), .insns) .cx
	:- member (JUMP _, RETURN,) .redirInsn
	, !, cg-optimize (.redirInsn, .insns) .cx
#
cg-optimize-jumps (CALL l:(RETURN, _), .insns) .cx
	:- !, cg-optimize .insns .cx
#
cg-optimize-jumps .insns .insns #

cg-optimize-assign-returns .li0 .ri0
	:- .li0 = (
		ASSIGN-FRAME-REG .r0 0 .r
		, SET-RESULT .r1
		, RETURN
		, .insns)
	, .mi0 = (SET-RESULT .r
		, RETURN
		, .insns)
	same .r0 .r1
	, !
	, cg-optimize .mi0 .ri0
#
cg-optimize-assign-returns .insns .insns #

cg-optimize-stack-usage .li0 .ri0
	:- .li0 = (PUSH .r0
		, PUSH .r1
		, .call .op
		, POP-ANY
		, POP-ANY
		, .li1)
	, .mi0 = (POP-ANY
		, POP-ANY
		, PUSH .r0
		, PUSH .r1
		, .call .op
		, .mi1)
	, member (CALL, CALL-CLOSURE,) .call
	, (.li1/.mi1 = .li2/.mi2; append2 .li1/.li2 .mi1/.mi2 (LEAVE,))
	, append2 .li2/.insns .mi2/.insns (RETURN,)
	, cg-optimize .mi0 .ri0
	, !
#
cg-optimize-stack-usage .insns .insns #

-- Return instruction would actually perform leave (i.e. frame restoration).
-- We can skip the LEAVE instruction if it obstruct optimizations.
cg-optimize-tail-calls .li0 .ri0
	:- .li0 = (.call .op, .li1)
	, member (CALL/JUMP, CALL-CLOSURE/JUMP-CLOSURE,) .call/.jump
	, (.li1/.mi0 = .li2/.mi1; append2 .li1/.li2 .mi0/.mi1 (RESTORE-DSP _, RESTORE-CSP _,))
	, (.li2 = .li3; .li2 = (LEAVE, .li3))
	, .mi1 = (.jump .op, .mi2)
	, append2 .li3/.insns .mi2/.insns (RETURN,)
	, cg-optimize .mi0 .ri0
	, !
#
cg-optimize-tail-calls .insns .insns #

cg-optimize-branches (.insn l:.b0, .insns) (.insn l:.bx, .insns)
	:- bound .b0, !, cg-optimize .b0 .bx
#
cg-optimize-branches (.insn .op0 l:.b0, .insns) (.insn .op0 l:.bx, .insns)
	:- bound .b0, !, cg-optimize .b0 .bx
#
cg-optimize-branches (.insn .op0 .op1 l:.b0, .insns) (.insn .op0 .op1 l:.bx, .insns)
	:- bound .b0, !, cg-optimize .b0 .bx
#
cg-optimize-branches .insns .insns #

append2 .li0/.lix .ri0/.rix .insns
	:- append .insns .lix .li0
	, append .insns .rix .ri0
#
