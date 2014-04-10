declare time = this;

declare time-yield = [] (
	svc:svc-service [svc-halt_, 0, 0,];
);

declare time-sleep = [ms,] (
	declare ticks1 = svc:svc-service [svc-ticks, 0, 0,] + ms / 10;
	while (svc:svc-service [svc-ticks, 0, 0,] < ticks1) do (
		time:time-yield [];
	);
);
