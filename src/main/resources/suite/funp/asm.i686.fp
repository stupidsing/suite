expand (!asm.adjust.pointer .p .add) := !asm (EAX = .p; EBX = .add;) { ADD (EAX, EBX); }/EAX ~
expand (!asm.mmap .length) := (let ms := [0, .length, 3, 34, -1, 0,] ~ !asm (EAX = 90; EBX = numberp:pointer address.of ms;) { INT (+x80); }/EAX) ~
expand (!asm.munmap .p .length) := !asm (EAX = 91; EBX = .p; ECX = .length;) { INT (+x80); }/EAX ~
expand !asm.rdtsc := !asm () { RDTSC (); }/EAX ~
expand !asm.rdtscp := !asm () { RDTSCP (); }/EAX ~
expand (!asm.read .p .length) := !asm (EAX = 3; EBX = 0; ECX = .p; EDX = .length;) { INT (+x80); }/EAX ~
expand (!asm.write .p .length) := !asm (EAX = 4; EBX = 1; ECX = .p; EDX = .length;) { INT (+x80); }/EAX ~

expand os.ps := 4 ~
