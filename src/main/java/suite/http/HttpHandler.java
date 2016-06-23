package suite.http;

import java.io.IOException;

public interface HttpHandler {

	public void handle(HttpRequest request, HttpResponse response) throws IOException;

}
