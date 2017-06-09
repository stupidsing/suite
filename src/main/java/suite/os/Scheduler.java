package suite.os;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.streamlet.Read;
import suite.util.Object_;
import suite.util.Thread_;

public class Scheduler {

	private List<Schedule> schedules;

	public static Scheduler of(Schedule... schedules) {
		return new Scheduler(Arrays.asList(schedules));
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
