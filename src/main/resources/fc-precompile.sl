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
	, rpn .preds .rpn
	, file.write .filename .rpn
#

fc-precompile .lib .do1/($$PRECOMPILE .pc) .preds
	:- .pc = .ues/.ves/.tes .trs/.trs .fcs
	, !, write "Parsing program", nl
	, !, fc-parse .do1 .do2
	, !, write "Inferencing types", nl
	, !, fc-infer-type-rule .do2 ()/()/() .tr/() NUMBER
	, !, fc-resolve-type-rules .tr
	, !, .pred0 = (
		fc-infer-type-rule-using-lib .lib .do .ue/.ve/.te .tr1 .type
			:- fc-dict-union-replace .ue .ues .ue1
			, fc-dict-union-replace .ve .ves .ve1
			, fc-dict-union-replace .te .tes .te1
			, fc-infer-type-rule .do .ue1/.ve1/.te1 .tr1 .type
	)
	, !, write 'Verifying intermediate output', nl
	, once (not is.cyclic .do2; fc-error "Cyclic data detected")
	, !, write "Lazyifying", nl
	, !, fc-lazyify .do2 .dol3
	, !, write "Optimizing", nl
	, !, fc-optimize-flow .do2 .do3
	, !, fc-optimize-flow .dol3 .dol4
	, !, fc-precompile-compile EAGER .lib .fcs .do3 .pred1
	, !, fc-precompile-compile LAZY .lib .fcs .dol4 .pred2
	, .preds = (.pred0 # .pred1 # .pred2 #)
	, !, write 'Verifying final output', nl
	, once (not is.cyclic .preds; fc-error "Cyclic data detected")
#

fc-precompile-compile .mode .lib .fcs .parsed .pred
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-precompile-compile-node .parsed .frame0/() .c0/.cx/.d0/.dx/.reg
	, .fcs = .frame1/.ves .cs0/.csx/.ds0/.dsx/.regs
	, cg-optimize-segment .c0/.cs0 .co0/.cso0
	, cg-optimize-segment .csx/.cx .csox/.cox
	, cg-optimize-segment .d0/.ds0 .do0/.dso0
	, cg-optimize-segment .dsx/.dx .dsox/.dox
	, .pred = (
		fc-compile-using-lib .mode .lib .do .frame0/.ve .co0/.cox/.do0/.dox/.reg
			:- fc-dict-union-bind .ve .ves .ve1 -- Import and export symbols
			, fc-compile .do .frame1/.ve1 .cso0/.csox/.dso0/.dsox/.regs
	)
#

fc-precompile-compile-node (USING .mode EXTERNAL .lib .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !, write 'Loading pre-compiled library' .lib, nl
	, fc-load-precompiled-library .lib (_ # .eagerPred # .lazyPred #)
	, once (.mode = EAGER, .pred = .eagerPred; .pred = .lazyPred)
	, generalize .pred (
		fc-compile-using-lib .mode .lib ($$PRECOMPILE _ _ .frame/.ve1 _) _/() _ :- .tail
	)
	, once .tail
	, fc-dict-union-bind .ve .ve1 .ve2
	, fc-precompile-compile-node .do .frame/.ve2 .c0/.cx/.d0/.dx/.reg
#
fc-precompile-compile-node .parsed .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- fc-compile .parsed .frame/.ve .c0/.cx/.d0/.dx/.reg
#

-- Parser
fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

-- Type inferencer
fc-infer-type-rule ($$PRECOMPILE .uvt .trs _) .uvt .trs NUMBER :- ! #

-- Lazyifier and optimizer
fc-transform ($$PRECOMPILE .p) ($$PRECOMPILE .p) .ts/.ts :- ! #

-- Code generation
fc-compile ($$PRECOMPILE _ _ .fve .cdr) .fve .cdr :- ! #

() :- import.path 'fc.sl' #
