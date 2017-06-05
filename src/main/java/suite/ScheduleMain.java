package suite;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.node.util.Mutable;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.Thread_;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ScheduleMain
public class ScheduleMain extends ExecutableProgram {

	private List<Schedule> initialSchedule = Arrays.asList( //
			daily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
			repeat(5, () -> System.out.println("." + LocalDateTime.now())), //
			new Schedule(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new));

	private List<Schedule> schedules = new ArrayList<>(initialSchedule);

	public static void main(String[] args) {
		Util.run(ScheduleMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		while (!schedules.isEmpty()) {
			List<Schedule> schedules1 = new ArrayList<>();
			LocalDateTime now = LocalDateTime.now();

			LocalDateTime nextWakeUpDateTime = Read.from(schedules) //
					.map(schedule -> schedule.nextRunDateTime) //
					.min(Object_::compare);

			Thread_.sleepQuietly(Duration.between(now, nextWakeUpDateTime).toMillis());

			for (Schedule schedule : schedules)
				if (now.isBefore(schedule.nextRunDateTime))
					schedules1.add(schedule);
				else
					try {
						schedules1.addAll(schedule.run.source());
					} catch (Exception ex) {
						LogUtil.error(ex);
					}

			schedules = schedules1;
		}

		return true;
	}

	private Schedule daily(LocalTime time, Runnable runnable) {
		LocalDateTime firstRunDateTime;

		if (LocalTime.now().isBefore(time))
			firstRunDateTime = LocalDate.now().atTime(time);
		else
			firstRunDateTime = LocalDate.now().plusDays(1).atTime(time);

		Mutable<Source<List<Schedule>>> mutable = Mutable.nil();

		mutable.set(new Source<List<Schedule>>() {
			private LocalDateTime dateTime = firstRunDateTime;

			public List<Schedule> source() {
				runnable.run();
				dateTime = dateTime.plusDays(1);
				return Arrays.asList(new Schedule(dateTime, mutable.get()));
			}
		});

		return new Schedule(firstRunDateTime, mutable.get());
	}

	private Schedule repeat(int seconds, Runnable runnable) {
		Mutable<Source<List<Schedule>>> m = Mutable.nil();
		m.set(() -> {
			runnable.run();
			return Arrays.asList(new Schedule(LocalDateTime.now().plusSeconds(seconds), m.get()));
		});
		return new Schedule(LocalDateTime.now(), m.get());
	}

	private class Schedule {
		private LocalDateTime nextRunDateTime;
		private Source<List<Schedule>> run;

		private Schedule(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
			this.nextRunDateTime = nextRunDateTime;
			this.run = run;
		}
	}

}
