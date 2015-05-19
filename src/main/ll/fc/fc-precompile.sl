-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--
-- to perform pre-compilation:
-- ./run.sh src/main/resources/fc-precompile.sl
-- ? fc-precompile-lib STANDARD #
--

fc-precompile-lib .lib
	:- fc-precompiled-library-filename .lib .filename
	, fc-load-library .lib .do0 .do1
	, fc-precompile .lib .do1/.do0 .preds
	, !, write 'Saving file' .filename, nl
	, persist.save .preds .filename
#

fc-precompile .lib .do0/($$PRECOMPILE .pc) .preds
	:- .pc = .ues/.ves/.tes .trs/.trs .fcs
	, !, write "Parsing program", nl
	, !, fc-parse .do0 .do1
	, !, write "Inferencing types", nl
	, !, fc-infer-type-rule .do1 ()/()/() .tr/() NUMBER
	, !, fc-resolve-type-rules .tr
	, !, .pred0 = (
		fc-infer-type-rule-using-lib .lib .do .ue/.ve/.te .tr1 .type
			:- fc-dict-union-replace .ue .ues .ue1
			, fc-dict-union-replace .ve .ves .ve1
			, append .te .tes .te1
			, fc-infer-type-rule .do .ue1/.ve1/.te1 .tr1 .type
	)
	, !, write 'Verifying intermediate output', nl
	, once (not is.cyclic .do1; fc-error "Cyclic data detected")
	, !, write "Lazyifying", nl
	, !, fc-lazyify .do1 .dol2
	, !, write "Reducing tail recursions", nl
	, !, fc-reduce-tail-call .do1 .do2
	, !, write "Optimizing", nl
	, !, fc-optimize-flow .do2 .do3
	, !, fc-optimize-flow .dol2 .dol3
	, !, fc-precompile-compile EAGER .lib .fcs .do3 .pred1
	, !, fc-precompile-compile LAZY .lib .fcs .dol3 .pred2
	, !, .preds = (.pred0 # .pred1 # .pred2 #)
#

fc-precompile-compile .mode .lib .fcs .parsed .pred
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-precompile-compile-node .parsed .frame0/() .c0/.cx/.reg
	, .fcs = .frame1/.ves .cs0/.csx/.regs
	, .pred = (
		fc-compile-using-lib .mode .lib .do .frame0/.ve .c0/.cx/.reg
			:- fc-dict-union-bind .ve .ves .ve1 -- Import and export symbols
			, fc-compile .do .frame1/.ve1 .cs0/.csx/.regs
	)
#

fc-precompile-compile-node (USING .mode EXTERNAL .lib .do) .frame/.ve .c0/.cx/.reg
	:- !, write 'Loading pre-compiled library' .lib, nl
	, fc-load-precompiled-library .lib (_ # .eagerPred # .lazyPred #)
	, once (.mode = EAGER, .pred = .eagerPred; .pred = .lazyPred)
	, clone .pred (
		fc-compile-using-lib .mode .lib ($$PRECOMPILE _ _ .frame/.ve1 _) _/() _ :- .tail
	)
	, once .tail
	, fc-dict-union-bind .ve .ve1 .ve2
	, fc-precompile-compile-node .do .frame/.ve2 .c0/.cx/.reg
#
fc-precompile-compile-node .parsed .frame/.ve .c0/.cx/.reg
	:- fc-compile .parsed .frame/.ve .c0/.cx/.reg
#

-- Parser
fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

-- Type inferencer
fc-infer-type-rule ($$PRECOMPILE .uvt .trs _) .uvt .trs NUMBER :- ! #

-- Lazyifier and optimizer
fc-rewrite ($$PRECOMPILE .p) ($$PRECOMPILE .p) .ts/.ts :- ! #

-- Code generation
fc-compile ($$PRECOMPILE _ _ .fve .cr) .fve .cr :- ! #

() :- import.path "fc/fc.sl" #
