replace() {
  python -c "if True:
    wsc = ['Character', 'Char', 'Chr', 'char', 'char', 'chr']
    wsf = ['Float', 'Float', 'Flt', 'float', 'float', 'flt']
    wsi = ['Integer', 'Int', 'Int', 'int', 'int', 'int']
    wss = ['Short', 'Short', 'Sht', 'short', 'short', 'sht']
    wsx = ['{x0}', '{x1}', '{x2}', '{x3}', '{x4}', '{x5}']
    wsy = ['{y0}', '{y1}', '{y2}', '{y3}', '{y4}', '{y5}']
    wsz = ['{z0}', '{z1}', '{z2}', '{z3}', '{z4}', '{z5}']

    repls0 = [wsc, wsf, wsi, wss]
    repls1 = [wsc, wsf, wsi, wss]
    repls2 = [wsc, wsf, wsi, wss]

    def r(ws0, ws1, s):
      for i in range(len(ws0)):
        s = s.replace(ws0[i], ws1[i])
      return s

    def replace(ws0, ws1, ws2, s):
      s = r(wsc, wsx, (r(wsf, wsy, (r(wss, wsz, (s))))))
      s = r(wsx, ws0, (r(wsy, ws1, (r(wsz, ws2, (s))))))
      return s

    for repl0 in repls0:
      for repl1 in repls1:
        for repl2 in repls2:
          filename0 = '${1}'
          filename1 = replace(repl0, repl1, repl2, filename0)

          s0 = None
          with open(filename0) as f0: s0 = f0.read()
          sx = replace(repl0, repl1, repl2, s0)
          with open(filename1, 'w') as f1: f1.write(sx)
  "
}

replace src/main/java/suite/adt/map/ChrObjMap.java
replace src/main/java/suite/adt/map/ObjChrMap.java
replace src/main/java/suite/adt/pair/ChrFltPair.java
replace src/main/java/suite/adt/pair/ChrObjPair.java
replace src/main/java/suite/primitive/Chr_Flt.java
replace src/main/java/suite/primitive/ChrFlt_Obj.java
replace src/main/java/suite/primitive/ChrFlt_Sht.java
replace src/main/java/suite/primitive/ChrFlt_ShtRethrow.java
replace src/main/java/suite/primitive/ChrFltPredicate.java
replace src/main/java/suite/primitive/ChrFltRethrow.java
replace src/main/java/suite/primitive/ChrFltSink.java
replace src/main/java/suite/primitive/ChrFltSource.java
replace src/main/java/suite/primitive/ChrFun.java
replace src/main/java/suite/primitive/ChrMutable.java
replace src/main/java/suite/primitive/ChrObj_Flt.java
replace src/main/java/suite/primitive/ChrObjFunUtil.java
replace src/main/java/suite/primitive/ChrPredicate.java
replace src/main/java/suite/primitive/ChrRethrow.java
replace src/main/java/suite/primitive/ChrSink.java
replace src/main/java/suite/primitive/ChrSource.java
replace src/main/java/suite/primitive/Chars.java
replace src/main/java/suite/primitive/Chars_.java
replace src/main/java/suite/streamlet/ChrObjOutlet.java
replace src/main/java/suite/streamlet/ChrObjStreamlet.java
