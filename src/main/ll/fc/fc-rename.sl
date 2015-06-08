fc-rename (DEF-VARS (.var0 .value0, .vvs0) .do0) (DEF-VARS (.varx .valuex, .vvsx) .dox) .ps
	:- !
	, fc-dict-add (.var0 .varx) .ps/.ps1
	, fc-rename .value0 .valuex .ps1
	, fc-rename .do0 .do1 .ps1
	, fc-rename (DEF-VARS .vvs0 .do1) (DEF-VARS .vvsx .dox) .ps
	, temp .varx
#
fc-rename (VAR .var0) (VAR .varx) .ps
	:- !
	, fc-dict-get (.var0 .varx) .ps
#
fc-rename .do0 .dox
	:- fc-rewrite .do0 .dox .ts/()
	, fold .t (fc-rename .t) .ts
#
