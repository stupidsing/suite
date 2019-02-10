expand (!asm.adjust.pointer .p .add) := asm (EAX = .p; EBX = .add;) { ADD (EAX, EBX); } ~
expand (!asm.mmap .length) := (let ms := [0, .length, 3, 34, -1, 0,] ~ asm (EAX = 90; EBX = address.of ms;) { INT (+x80); }) ~
expand (!asm.munmap .length .p) := asm (EAX = 91; EBX = .p; ECX = .length;) { INT (+x80); } ~
expand (!asm.peek .p) := asm (EBX = .p;) { MOV (EAX, `EBX`); } ~
expand (!asm.poke .p .value) := asm (EAX = .value; EBX = .p;) { MOV (`EBX`, EAX); } ~
expand (!asm.read .p .length) := asm (EAX = 3; EBX = 0; ECX = .p; EDX = .length;) { INT (+x80); } ~
expand (!asm.write .p .length) := asm (EAX = 4; EBX = 1; ECX = .p; EDX = .length;) { INT (+x80); } ~

expand os.ps := 4 ~
