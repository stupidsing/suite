declare time = this;

declare time-yield = [] (
	invoke svc-service [svc-halt_, 0, 0,];
);

declare time-sleep = [ms,] (
	declare ticks1 = invoke svc-service [svc-ticks, 0, 0,] + ms / 10;
	while (invoke svc-service [svc-ticks, 0, 0,] < ticks1) do (
		time:time-yield [];
	);
);
