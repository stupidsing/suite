replace() {
  python -c "if True:
    wsc = ['Character', 'Char', 'Chr', 'char', 'char', 'chr']
    wsd = ['Double', 'Double', 'Dbl', 'double', 'double', 'dbl']
    wsf = ['Float', 'Float', 'Flt', 'float', 'float', 'flt']
    wsi = ['Integer', 'Int', 'Int', 'int', 'int', 'int']
    wsl = ['Long', 'Long', 'Lng', 'long', 'long', 'lng']
    wsx = ['{x0}', '{x1}', '{x2}', '{x3}', '{x4}', '{x5}']
    wsy = ['{y0}', '{y1}', '{y2}', '{y3}', '{y4}', '{y5}']
    wsz = ['{z0}', '{z1}', '{z2}', '{z3}', '{z4}', '{z5}']

    repls0 = [wsc, wsd, wsf, wsi, wsl]
    repls1 = [wsc, wsd, wsf, wsi, wsl]
    repls2 = [wsc, wsd, wsf, wsi, wsl]

    def r(ws0, ws1, s):
      for i in range(len(ws0)):
        s = s.replace(ws0[i], ws1[i])
      return s

    def replace(ws0, ws1, ws2, s):
      s = r(wsc, wsx, (r(wsd, wsy, (r(wsf, wsz, (s))))))
      s = r(wsx, ws0, (r(wsy, ws1, (r(wsz, ws2, (s))))))
      return s

    for repl0 in repls0:
      for repl1 in repls1:
        for repl2 in repls2:
          filename0 = '${1}'
          filename1 = replace(repl0, repl1, repl2, filename0)

          s0 = None
          with open(filename0) as f0: s0 = f0.read()
          s1 = replace(repl0, repl1, repl2, s0)
          sx, line0 = '', ''
          for line in s1.split('\n'):
            if line0 != line:
              sx, line0 = sx + line + '\n', line
          with open(filename1, 'w') as f1: f1.write(sx[:-1])
  "
}

replace src/main/java/suite/adt/map/ChrDblMap.java
replace src/main/java/suite/adt/map/ChrObjMap.java
replace src/main/java/suite/adt/map/ObjChrMap.java
replace src/main/java/suite/adt/pair/ChrDblPair.java
replace src/main/java/suite/adt/pair/ChrObjPair.java
replace src/main/java/suite/primitive/Chr_Dbl.java
replace src/main/java/suite/primitive/ChrDbl_Obj.java
replace src/main/java/suite/primitive/ChrDbl_Flt.java
replace src/main/java/suite/primitive/ChrDblPredicate.java
replace src/main/java/suite/primitive/ChrDblSink.java
replace src/main/java/suite/primitive/ChrDblSource.java
replace src/main/java/suite/primitive/ChrFun.java
replace src/main/java/suite/primitive/ChrFunUtil.java
replace src/main/java/suite/primitive/ChrMutable.java
replace src/main/java/suite/primitive/ChrObj_Dbl.java
replace src/main/java/suite/primitive/ChrObjFunUtil.java
replace src/main/java/suite/primitive/ChrPredicate.java
replace src/main/java/suite/primitive/ChrSink.java
replace src/main/java/suite/primitive/ChrSource.java
replace src/main/java/suite/primitive/Chars.java
replace src/main/java/suite/primitive/Chars_.java
replace src/main/java/suite/streamlet/ChrObjOutlet.java
replace src/main/java/suite/streamlet/ChrObjStreamlet.java
