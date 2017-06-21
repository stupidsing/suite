package suite.os;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Object_;

public class Schedule {

	public static Schedule ofDaily(LocalTime time, Runnable runnable) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = now.toLocalDate();
		LocalDateTime firstRunDateTime0 = today.atTime(time);
		LocalDateTime firstRunDateTime1;

		if (firstRunDateTime0.isBefore(now))
			firstRunDateTime1 = firstRunDateTime0.plusDays(1);
		else
			firstRunDateTime1 = firstRunDateTime0;

		Source<List<Schedule>> source = Object_.fix(m -> new Source<List<Schedule>>() {
			private LocalDateTime dateTime = firstRunDateTime1;

			public List<Schedule> source() {
				runnable.run();
				return Arrays.asList(new Schedule(dateTime = dateTime.plusDays(1), m.get()));
			}
		});

		return of(firstRunDateTime1, source);
	}

	public static Schedule ofRepeat(int seconds, Runnable runnable) {
		Source<List<Schedule>> source = Object_.fix(m -> () -> {
			runnable.run();
			return Arrays.asList(new Schedule(LocalDateTime.now().plusSeconds(seconds), m.get()));
		});
		return of(LocalDateTime.now(), source);
	}

	public static Schedule of(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
		return new Schedule(nextRunDateTime, run);
	}

	public final LocalDateTime nextRunDateTime;
	public final Source<List<Schedule>> run;

	private Schedule(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
		this.nextRunDateTime = nextRunDateTime;
		this.run = run;
	}

	public Schedule filterTime(Predicate<LocalDateTime> pred) {
		return Schedule.of(nextRunDateTime,
				() -> Read //
						.from(run.source()) //
						.map(schedule -> {
							LocalDateTime t = schedule.nextRunDateTime;
							while (!pred.test(t))
								t = t.plusHours(1);
							return Schedule.of(t, schedule.run);
						}) //
						.toList());
	}

}
