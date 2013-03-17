-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--
-- to perform pre-compilation:
-- ./run.sh src/main/resources/fc-precompile.sl
-- ? fc-setup-standard-precompile #
--

fc-setup-precompile .lib
	:- load-library .lib
	, home.dir .homeDir
	, concat .homeDir "/" .lib ".rpn" .filename
	, fc-add-functions .lib .do0 .do1
	, fc-setup-precompile0 .lib .do1/.do0 .filename
#

fc-setup-precompile0 .lib .do1/($$PRECOMPILE .pc) .filename
	:- .pc = .ues/.ves/.tes/.oes .trs/.trs .fcs
	, !, write 'Parsing program', nl
	, !, fc-parse .do1 .parsed
	, !, write 'Inferencing types', nl
	, !, infer-type-rule .parsed ()/()/()/() .tr0/.trx NUMBER
	, !, resolve-types .tr0/.trx
	, append .ues .ue .ue1
	, append .ves .ve .ve1
	, append .tes .te .te1
	, append .oes .oe .oe1
	, !, .prog0 = (
		infer-type-rule-using-libs (.lib, .libs) .do .ue/.ve/.te/.oe .tr .type
			:- infer-type-rule-using-libs .libs .do .ue1/.ve1/.te1/.oe1 .tr .type
	)
	, !, fc-dump-precompile EAGER .lib .fcs .parsed .prog1
	, !, fc-dump-precompile LAZY .lib .fcs .parsed .prog2
	, .prog3 = fc-imported-precompile-library .lib
	, rpn (.prog0 # .prog1 # .prog2 # .prog3 #) .rpn
	, file.write .filename .rpn
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

fc-dump-precompile .mode .lib .fcs .parsed .prog
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-compile .mode .parsed .frame0/() .c0/.cx/.d0/.dx/.reg
	, member .fcs .mode/.fc
	, .fc = .frame1/.wes .cs0/.csx/.ds0/.dsx/.regs
	, append .wes .we .we1
	, .prog = (
		fc-compile-using-libs .mode (.lib, .libs) .do .frame0/.we .c0/.cx/.d0/.dx/.reg
			:- fc-compile-using-libs .mode .libs .do .frame1/.we1 .cs0/.csx/.ds0/.dsx/.regs
	)
#

infer-type-rule ($$PRECOMPILE .uvto .trs _) .uvto .trs NUMBER :- ! #

-- Eager evaluation
fc-compile EAGER ($$PRECOMPILE _ _ .pcc) .fveCdr :- !, member .pcc EAGER/.fveCdr #

-- Lazy evaluation
fc-lazy-compile-to-value ($$PRECOMPILE _ _ .pcc) .fveCdr :- !, member .pcc LAZY/.fveCdr #

() :- import.file 'fc.sl'
	, import.file 'fc-eager-evaluation.sl'
	, import.file 'fc-lazy-evaluation.sl'
#
