-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--
-- to perform pre-compilation:
-- ./run.sh src/main/resources/fc-precompile.sl
-- ? fc-setup-standard-precompile #
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
			:- infer-type-rule-using-libs .libs .do .vto .trs0/.trsx .type
	) .prog0
	, to.dump.string (
		fc-compile-using-libs (.lib, .libs) .do .frame/.ve .c0/.cx/.d0/.dx/.reg
			:- fc-compile-using-libs .libs .do .frame/.ves .cs0/.csx/.ds0/.dsx/.reg
	) .prog1
	, concat .prog0 "%0A#%0A%0A" .prog1 "%0A#%0A%0A" .contents
	, file-write 'src/main/resources/fc-precompiled.sl' .contents
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

infer-type-rule ($$PRECOMPILE .vto .trs _) .vto .trs NUMBER :- ! #

-- Eager evaluation
fc-compile ($$PRECOMPILE _ _ .fve .cdr) .fve .cdr :- ! #

-- Lazy evaluation
fc-compile0 ($$PRECOMPILE _ _ .fve .cdr) .fve .cdr :- ! #

() :- import 'fc.sl'
	, import 'fc-eager-evaluation.sl'
#
