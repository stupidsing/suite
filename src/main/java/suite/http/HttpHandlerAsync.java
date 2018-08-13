package suite.http;

import suite.primitive.IoSink;

public interface HttpHandlerAsync {

	public HttpResponse handle(HttpRequest request, IoSink<HttpResponse> sink);

}
