package suite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ScheduleMain
public class ScheduleMain extends ExecutableProgram {

	private List<Schedule> initialSchedule = Arrays.asList( //
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
				schedules.addAll(schedule.run.source());

			LocalDateTime nextWakeUpDateTime = Read.from(schedules) //
					.map(schedule -> schedule.nextRunDateTime) //
					.min(Util::compare);

			Duration duration = Duration.between(now, nextWakeUpDateTime);

			Util.sleepQuietly(duration.toMillis());
		}
	}

	private List<Schedule> schedules = new ArrayList<>(initialSchedule);

	private class Schedule {
		private LocalDateTime nextRunDateTime;
		private Source<List<Schedule>> run;

		private Schedule(LocalDateTime nextRunDateTime, Source<List<Schedule>> run) {
			this.nextRunDateTime = nextRunDateTime;
			this.run = run;
		}
	}

}
