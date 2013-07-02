-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

-- optimize tail calls
optimize .li0 .ri0
	:- push-pop-pairs .li0/.li1 .li2/.li3 .ri1/.ri2 .ri0/.ri1
	, member (CALL/JUMP, CALL-REG/JUMP-REG,) .call/.jump
	, .li1 = (_ .call .target, .li2)
	, is-returning .li3
	, .ri2 = (_ .jump .target, .ri3)
	, !
	, optimize .li3 .ri3
#
optimize (.inst, .insts0) (.inst, .insts1)
	:- !, optimize .insts0 .insts1
#
optimize () () #

push-pop-pairs
(_ PUSH .reg, .i0)/.ix (_ POP-ANY, .j0)/.jx
(_ PUSH .reg, .k0)/.kx (_ POP-ANY, .l0)/.lx
	:- !, push-pop-pairs .i0/.ix .j0/.jx .k0/.kx .l0/.lx
#
push-pop-pairs .i/.i .j/.j .k/.k .l/.l #

is-returning (_ LABEL, .insts) :- !, is-returning .insts #
is-returning (_ RETURN, _) #

generate-code .code :- assign-line-numbers 0 .code, ! #

assign-line-numbers _ () #
assign-line-numbers .n (.n _, .insts)
	:- let .n1 (.n + 1), assign-line-numbers .n1 .insts
#
