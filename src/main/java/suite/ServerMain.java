package suite;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import suite.cfg.Defaults;
import suite.http.HttpHandler;
import suite.http.HttpHeaderUtil;
import suite.http.HttpResponse;
import suite.http.HttpServe;
import suite.immutable.IMap;
import suite.node.Str;
import suite.os.Schedule;
import suite.os.Scheduler;
import suite.telegram.TelegramBot;
import suite.util.RunUtil;
import suite.util.Thread_;
import suite.util.To;

// mvn compile exec:java -Dexec.mainClass=suite.ServerMain
public class ServerMain {

	public static void main(String[] args) {
		RunUtil.run(() -> new ServerMain().run());
	}

	private boolean run() {
		Thread_.startThread(this::runHttpServer);
		Thread_.startThread(this::runScheduler);
		Thread_.startThread(this::runTelegramBot);
		return true;
	}

	private void runHttpServer() {
		BiPredicate<String, String> authenticate = (username, password) -> Defaults //
				.secrets() //
				.prove(Suite.substitute("auth .0 .1", new Str(username), new Str(password)));

		HttpHandler handler0 = request -> HttpResponse.of(To.outlet("" //
				+ "<html>" //
				+ "<br/>method = " + request.method //
				+ "<br/>server = " + request.server //
				+ "<br/>paths = " + request.paths //
				+ "<br/>attrs = " + HttpHeaderUtil.getAttrs(request.query) //
				+ "<br/>headers = " + request.headers //
				+ "</html>"));

		var handler1 = HttpHandler.ofDispatch(IMap //
				.<String, HttpHandler> empty() //
				.put("hello", HttpHandler.ofData("Hello world")) //
				.put("path", HttpHandler.ofPath(Defaults.tmp)) //
				.put("site", HttpHandler.ofSession(authenticate, handler0)));

		new HttpServe(8051).serve(handler1);
	}

	private void runScheduler() {
		new Scheduler(List.of( //
				Schedule.ofDaily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
				Schedule.ofRepeat(5, () -> System.out.println("." + LocalDateTime.now())), //
				Schedule.of(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new)) //
		).run();
	}

	private void runTelegramBot() {
		new TelegramBot().bot((userId, message) -> message);
	}

}
