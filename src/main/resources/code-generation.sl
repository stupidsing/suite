-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

generate-code .code0 .codex
	:- optimize .code0 .codex
	, !, assign-line-number 0 .codex
	, !
#

-- optimize tail calls
optimize (_ PUSH .reg0
	, _ PUSH .reg1
	, _ .call .target
	, _ POP-ANY
	, _ POP-ANY
	, .label0 LABEL
	, .label1 LABEL
	, _ RETURN
	, .insts0
) (_ POP-ANY
	, _ POP-ANY
	, _ PUSH .reg0
	, _ PUSH .reg1
	, _ .jump .target
	, .label0 LABEL
	, .label1 LABEL
	, _ RETURN
	, .insts1
)
	:- !
	, member (CALL/JUMP, CALL-REG/JUMP-REG,) .call/.jump
	, optimize .insts0 .insts1
#
optimize (.inst, .insts0) (.inst, .insts1)
	:- !, optimize .insts0 .insts1
#
optimize () () #

assign-line-number _ () #
assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), assign-line-number .n1 .remains
#
