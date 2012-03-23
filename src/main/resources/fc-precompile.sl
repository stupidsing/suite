-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--
-- to perform pre-compilation:
-- ./run.sh src/main/resources/fc-precompile.sl
-- ? fc-setup-standard-precompile #
--

fc-setup-standard-precompile
	:- fc-add-standard-funs .do0 .do1
	, fc-setup-precompile STANDARD .do1/.do0 'src/main/resources/fc-precompiled.sl'
#

fc-setup-precompile .lib .do1/($$PRECOMPILE .pc) .filename
	:- .pc = .ves/.tes/.oes .trs0/.trsx .fcs
	, !, write 'Parsing program', nl
	, !, fc-parse .do1 .parsed
	, !, write 'Inferencing types', nl
	, !, infer-type-rule .parsed ()/()/() .tr0/.trx NUMBER
	, !, not not ( -- Test type correctness
		.trs0 = .trsx, resolve-types .tr0/.trx
	)
	, append .ves .ve .ve1
	, append .tes .te .te1
	, append .oes .oe .oe1
	, !, to.dump.string (
		infer-type-rule-using-libs (.lib, .libs) .do .ve/.te/.oe .tr0/.trx .type
			:- infer-type-rule-using-libs .libs .do .ve1/.te1/.oe1 .trs0/.trsx .type
	) .prog0
	, fc-dump-precompile EAGER .lib .fcs .parsed .prog1
	, fc-dump-precompile LAZY .lib .fcs .parsed .prog2
	, concat .prog0 "%0A#%0A%0A" .prog1 "%0A#%0A%0A" .prog2 "%0A#%0A%0A" .contents
	, file-write .filename .contents
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

fc-dump-precompile .mode .lib .fcs .parsed .prog
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-compile .mode .parsed 0/() .c0/.cx/.d0/.dx/.reg
	, member .fcs .mode/.fc
	, .fc = .frame/.wes .cs0/.csx/.ds0/.dsx/.resultReg
	, append .wes .we .we1
	, to.dump.string (
		fc-compile-using-libs .mode (.lib, .libs) .do .frame/.we .c0/.cx/.d0/.dx/.reg
			:- fc-compile-using-libs .mode .libs .do .frame/.we1 .cs0/.csx/.ds0/.dsx/.reg
	) .prog
#

infer-type-rule ($$PRECOMPILE .vto .trs _) .vto .trs NUMBER :- ! #

-- Eager evaluation
fc-eager-compile ($$PRECOMPILE _ _ .pcc) .fveCdr :- !, member .pcc EAGER/.fveCdr #

-- Lazy evaluation
fc-lazy-compile0 ($$PRECOMPILE _ _ .pcc) .fveCdr :- !, member .pcc LAZY/.fveCdr #

() :- import 'fc.sl'
	, import 'fc-eager-evaluation.sl'
	, import 'fc-lazy-evaluation.sl'
#
