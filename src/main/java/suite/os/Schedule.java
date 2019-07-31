package suite.os;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

import primal.fp.Funs.Source;
import suite.streamlet.Read;

public class Schedule {

	public static Schedule ofDaily(LocalTime time, Runnable runnable) {
		var now = LocalDateTime.now();
		var today = now.toLocalDate();
		var firstRunDateTime0 = today.atTime(time);
		LocalDateTime firstRunDateTime1;

		if (firstRunDateTime0.isBefore(now))
			firstRunDateTime1 = firstRunDateTime0.plusDays(1);
		else
			firstRunDateTime1 = firstRunDateTime0;

		Source<List<Schedule>> source = new Object() {
			private LocalDateTime dateTime = firstRunDateTime1;

			private List<Schedule> source() {
				runnable.run();
				return List.of(new Schedule(dateTime = dateTime.plusDays(1), this::source));
			}
		}::source;

		return of(firstRunDateTime1, source);
	}

	public static Schedule ofRepeat(int seconds, Runnable runnable) {
		return of(LocalDateTime.now(), new Object() {
			private List<Schedule> source() {
				runnable.run();
				return List.of(new Schedule(LocalDateTime.now().plusSeconds(seconds), this::source));
			}
		}::source);
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
		return Schedule.of(nextRunDateTime, () -> Read //
				.from(run.g()) //
				.map(schedule -> {
					var t = schedule.nextRunDateTime;
					while (!pred.test(t))
						t = t.plusHours(1);
					return Schedule.of(t, schedule.run);
				}) //
				.toList());
	}

}
