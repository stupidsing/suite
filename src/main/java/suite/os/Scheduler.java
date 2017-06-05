package suite.os;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.DailyMain;
import suite.streamlet.Read;
import suite.util.Object_;
import suite.util.Thread_;

public class Scheduler {

	private List<Schedule> schedules;

	public Scheduler() {
		this(Arrays.asList( //
				Schedule.ofDaily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
				Schedule.ofRepeat(5, () -> System.out.println("." + LocalDateTime.now())), //
				new Schedule(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new)));
	}

	public Scheduler(List<Schedule> initialSchedule) {
		this.schedules = new ArrayList<>(initialSchedule);
	}

	public void run() {
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
	}

}
