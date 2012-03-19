-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--

fc-setup-standard-precompile
	:- fc-add-standard-funs .do0 .do1
	, fc-setup-precompile STANDARD .do1/.do0
#

fc-setup-precompile .lib .do1/($$PRECOMPILE .pc)
	:- .pc = .vto .trs0/.trsx .frame/.ves .cs0/.csx/.ds0/.dsx/.resultReg
	, !, fc-parse .do1 .parsed
	, !, infer-type-rule .parsed .vtos .tr0/.trx NUMBER
	, !, not not ( -- Test type correctness
		.trs0 = .trsx, resolve-types .tr0/.trx
	)
	, !, fc-compile .parsed 0/.ve .c0/.cx/.d0/.dx/.reg
	, !, to.dump.string (
		infer-type-rule-using-libs (.lib, .libs) .do .vtos .tr0/.trx .type
			:- .trx = .trs0
			, infer-type-rule-using-libs .libs .do .vto .tr0/.trsx .type
	) .prog0
	, to.dump.string (
		fc-compile-using-libs (.lib, .libs) .do .frame/.ve .c0/.cx/.d0/.dx/.reg
			:- fc-compile-using-libs .libs .do .frame/.ves .cs0/.csx/.ds0/.dsx/.reg
	) .prog1
	, concat .prog0 "%0A#%0A%0A" .prog1 "%0A#%0A%0A" .contents
	, file-write 'src/main/resources/fc-precompiled.sl' .contents
#

fc-compile-using-libs .libs .do
	:- infer-type-rule-using-libs .libs .do ()/()/() .tr0/.trx _
	,  resolve-types .tr0/.trx
#

infer-type-rule-using-libs () .do .vto .tr0/.trx .type
	:- infer-type-rule .do .vto .tr0/.trx .type
#

fc-compile-using-libs () .do .fve .cdr
	:- !, fc-compile .do .fve .cdr
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

infer-type-rule ($$PRECOMPILE .vto .trs0/.trsx _) .vto .trs0/.trsx NUMBER :- ! #

-- Eager evaluation
fc-compile ($$PRECOMPILE _ _ .fve .cdr) .fve .cdr :- ! #

-- Lazy evaluation
fc-compile0 ($$PRECOMPILE _ _ .fve .cdr) .fve .cdr :- ! #

() :- import 'fc.sl'
	, import 'fc-lazy-evaluation.sl'
#
