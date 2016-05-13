() :- import.path "ic/ic-bind.sl"
	, import.path "ic/ic-compile.sl"
	, import.path "ic/ic-erase-type.sl"
	, import.path "ic/ic-erase-variable.sl"
	, import.path "ic/ic-infer-type.sl"
	, import.path "ic/ic-parse.sl"
	, import.path "ic/ic-rewrite.sl"
#

compile-imperative .do0 .e0/.ex
	:- ic-parse .do0 .do1
	, !, ic-infer-type () .do1 .type
	, !, (.type = I32; ic-condition-type .type)
	, !, ic-erase-type .do1 .do2
	, !, ic-erase-variable ()/() .do2 .do3
	, !, ic-compile-register .do3 .e0/.ex
	, !
#

ic-error .m :- throw .m #
