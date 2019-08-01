package suite.os;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import primal.Verbs.Compare;
import primal.os.Log_;
import suite.streamlet.Read;
import suite.util.Thread_;

public class Scheduler {

	private List<Schedule> schedules;

	public static Scheduler of(Schedule... schedules) {
		return new Scheduler(List.of(schedules));
	}

	public Scheduler(List<Schedule> initialSchedule) {
		this.schedules = new ArrayList<>(initialSchedule);
	}

	public void run() {
		while (!schedules.isEmpty()) {
			var schedules1 = new ArrayList<Schedule>();
			var now = LocalDateTime.now();

			var nextWakeUpDateTime = Read //
					.from(schedules) //
					.map(schedule -> schedule.nextRunDateTime) //
					.min(Compare::objects);

			Thread_.sleepQuietly(Duration.between(now, nextWakeUpDateTime).toMillis());

			for (var schedule : schedules)
				if (now.isBefore(schedule.nextRunDateTime))
					schedules1.add(schedule);
				else
					try {
						schedules1.addAll(schedule.run.g());
					} catch (Exception ex) {
						Log_.error(ex);
					}

			schedules = schedules1;
		}
	}

}
