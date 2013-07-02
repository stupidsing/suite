-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

optimize-segment .c0/() .co0/.cox
	:- cg-optimize .c0 .co
	, append .co .cox .co0
#

-- optimize tail calls
cg-optimize .li0 .ri0
	:- cg-push-pop-pairs .li0/.li1 .li2/.li3 .ri1/.ri2 .ri0/.ri1
	, member (CALL/JUMP, CALL-REG/JUMP-REG,) .call/.jump
	, .li1 = (_ .call .target, .li2)
	, cg-is-returning .li3
	, .ri2 = (_ .jump .target, .ri3)
	, !
	, cg-optimize .li3 .ri3
#
cg-optimize (.inst, .insts0) (.inst, .insts1)
	:- !, cg-optimize .insts0 .insts1
#
cg-optimize () () #

cg-push-pop-pairs
(_ PUSH .reg, .i0)/.ix (_ POP-ANY, .j0)/.jx
(_ PUSH .reg, .k0)/.kx (_ POP-ANY, .l0)/.lx
	:- !, cg-push-pop-pairs .i0/.ix .j0/.jx .k0/.kx .l0/.lx
#
cg-push-pop-pairs .i/.i .j/.j .k/.k .l/.l #

cg-is-returning (_ LABEL, .insts) :- !, cg-is-returning .insts #
cg-is-returning (_ RETURN, _) #

generate-code .code :- cg-assign-line-numbers 0 .code, ! #

cg-assign-line-numbers _ () #
cg-assign-line-numbers .n (.n _, .insts)
	:- let .n1 (.n + 1), cg-assign-line-numbers .n1 .insts
#
