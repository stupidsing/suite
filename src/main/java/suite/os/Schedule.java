package suite.os;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import suite.node.util.Mutable;
import suite.util.FunUtil.Source;

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

		Mutable<Source<List<Schedule>>> mutable = Mutable.nil();

		mutable.set(new Source<List<Schedule>>() {
			private LocalDateTime dateTime = firstRunDateTime1;

			public List<Schedule> source() {
				runnable.run();
				return Arrays.asList(new Schedule(dateTime = dateTime.plusDays(1), mutable.get()));
			}
		});

		return new Schedule(firstRunDateTime1, mutable.get());
	}

	public static Schedule ofRepeat(int seconds, Runnable runnable) {
		Mutable<Source<List<Schedule>>> m = Mutable.nil();
		m.set(() -> {
			runnable.run();
			return Arrays.asList(new Schedule(LocalDateTime.now().plusSeconds(seconds), m.get()));
		});
		return new Schedule(LocalDateTime.now(), m.get());
	}

	public final LocalDateTime nextRunDateTime;
	public final Source<List<Schedule>> run;

	Schedule(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
		this.nextRunDateTime = nextRunDateTime;
		this.run = run;
	}

}
