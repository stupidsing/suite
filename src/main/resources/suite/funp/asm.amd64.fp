expand (!asm-mmap %length) := !asm (EAX = 9; EDI = 0; ESI = %length; EDX = 3; R10D = 34; R8D = -1; R9D = 0;) { SYSCALL (); }/RAX ~
expand (!asm-munmap %p %length) := !asm (EAX = 11; RDI = %p; ESI = %length;) { SYSCALL (); }/EAX ~
expand !asm-rdtsc := !asm () { RDTSC (); }/EAX ~
expand !asm-rdtscp := !asm () { RDTSCP (); }/EAX ~
expand (!asm-read %p %length) := !asm (EAX = 0; EDI = 0; RSI = %p; EDX = %length;) { SYSCALL (); }/EAX ~
expand (!asm-write %p %length) := !asm (EAX = 1; EDI = 1; RSI = %p; EDX = %length;) { SYSCALL (); }/EAX ~

expand os-ps := 8 ~
