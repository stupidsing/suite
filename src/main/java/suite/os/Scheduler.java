package suite.os;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import primal.Verbs.Sleep;
import primal.os.Log_;
import suite.adt.PriorityQueue;

public class Scheduler {

	private PriorityQueue<Schedule> schedules;

	public static Scheduler of(Schedule... schedules) {
		return new Scheduler(List.of(schedules));
	}

	public Scheduler(List<Schedule> initialSchedule) {
		this.schedules = new PriorityQueue<>(Schedule.class, 256, Comparator.comparing(s -> s.nextRunDateTime));
	}

	public void run() {
		while (!schedules.isEmpty()) {
			var now0 = LocalDateTime.now();
			var nextWakeUpDateTime = schedules.min().nextRunDateTime;

			Sleep.quietly(Duration.between(now0, nextWakeUpDateTime).toMillis());

			var now1 = LocalDateTime.now();

			while (!now1.isBefore(schedules.min().nextRunDateTime)) {
				var schedule = schedules.extractMin();
				List<Schedule> schedules_;

				try {
					schedules_ = schedule.run.g();
				} catch (Exception ex) {
					Log_.error(ex);
					schedules_ = List.of();
				}

				for (var schedule_ : schedules_)
					schedules.insert(schedule_);
			}
		}
	}

}
