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

fc-setup-precompile0 .lib .do1/.do0 .filename
	:- fc-precompile .lib .do1/.do0 .prog
	, !, write 'Saving file' .filename, nl
	, rpn .prog .rpn
	, file.write .filename .rpn
#

fc-precompile .lib .do1/($$PRECOMPILE .pc) .prog
	:- .pc = .ues/.ves/.tes .trs/.trs .fcs
	, !, write 'Parsing program', nl
	, !, fc-parse .do1 .parsed
	, !, write 'Inferencing types', nl
	, !, infer-type-rule .parsed ()/()/() .tr/() NUMBER
	, !, resolve-type-rules .tr
	, !, .prog0 = (
		infer-type-rule-using-lib .lib .do .ue/.ve/.te .tr1 .type
			:- fc-dict-merge-replace .ue .ues .ue1
			, fc-dict-merge-replace .ve .ves .ve1
			, fc-dict-merge-replace .te .tes .te1
			, infer-type-rule .do .ue1/.ve1/.te1 .tr1 .type
	)
	, !, fc-dump-precompile EAGER .lib .fcs .parsed .prog1
	, !, fc-dump-precompile LAZY .lib .fcs .parsed .prog2
	, .prog3 = fc-imported-precompile-library .lib
	, .prog = (.prog0 # .prog1 # .prog2 # .prog3 #)
	, !, write 'Verifying output', nl
	, once (not is.cyclic .prog; fc-error "Cyclic output detected")
#

fc-dump-precompile .mode .lib .fcs .parsed .prog
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-compile .mode .parsed .frame0/() .c0/.cx/.d0/.dx/.reg
	, once member .fcs .mode/.fc
	, .fc = .frame1/.ves .cs0/.csx/.ds0/.dsx/.regs
	, (.mode = EAGER -- Eager code is always compiled into .c0/.cx section
		, cg-optimize-segment .c0/.cs0 .co0/.cso0
		, cg-optimize-segment .csx/.cx .csox/.cox
		, cg-optimize-segment .d0/.ds0 .do0/.dso0
		, cg-optimize-segment .dsx/.dx .dsox/.dox
	; .mode = LAZY -- Lazy code is always compiled into .d0/.dx section
		, cg-optimize-segment .c0/.cx .co0/.cox
		, cg-optimize-segment .d0/.cs0 .do0/.cso0
		, cg-optimize-segment .csx/.ds0 .csox/.dso0
		, cg-optimize-segment .dsx/.dx .dsox/.dox
	)
	, .prog = (
		fc-compile-using-lib .mode .lib .do .frame0/.ve .co0/.cox/.do0/.dox/.reg
			:- fc-dict-merge-replace .ve .ves .ve1
			, fc-compile .mode .do .frame1/.ve1 .cso0/.csox/.dso0/.dsox/.regs
	)
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

infer-type-rule ($$PRECOMPILE .uvt .trs _) .uvt .trs NUMBER :- ! #

-- Eager evaluation
fc-compile EAGER ($$PRECOMPILE _ _ .pcc) .fve .cdr :- , member .pcc EAGER/(.fve .cdr), ! #

-- Lazy evaluation
fc-lazy-compile-to-value ($$PRECOMPILE _ _ .pcc) .fve .cdr :- member .pcc LAZY/(.fve .cdr), ! #

() :- import.file 'fc.sl'
	, import.file 'fc-eager-evaluation.sl'
	, import.file 'fc-lazy-evaluation.sl'
#
