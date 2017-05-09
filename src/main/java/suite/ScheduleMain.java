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
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ScheduleMain
public class ScheduleMain extends ExecutableProgram {

	private List<Schedule> initialSchedule = Arrays.asList( //
			daily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
			new Schedule(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new));

	public static void main(String[] args) {
		Util.run(ScheduleMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		while (true) {
			LocalDateTime now = LocalDateTime.now();

			List<Schedule> toRuns = Read.from(schedules) //
					.filter(schedule -> now.isBefore(schedule.nextRunDateTime)) //
					.toList();

			for (Schedule schedule : toRuns)
				try {
					schedules.addAll(schedule.run.source());
				} catch (Exception ex) {
					LogUtil.error(ex);
				}

			LocalDateTime nextWakeUpDateTime = Read.from(schedules) //
					.map(schedule -> schedule.nextRunDateTime) //
					.min(Util::compare);

			Duration duration = Duration.between(now, nextWakeUpDateTime);

			Util.sleepQuietly(duration.toMillis());
		}
	}

	private List<Schedule> schedules = new ArrayList<>(initialSchedule);

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

	private class Schedule {
		private LocalDateTime nextRunDateTime;
		private Source<List<Schedule>> run;

		private Schedule(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
			this.nextRunDateTime = nextRunDateTime;
			this.run = run;
		}
	}

}
