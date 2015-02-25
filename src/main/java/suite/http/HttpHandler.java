package suite.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import suite.http.HttpServer.Handler;
import suite.util.os.FileUtil;

public abstract class HttpHandler implements Handler {

	protected HttpRequest request;
	protected HttpResponse response;

	protected abstract void handle(Reader reader, Writer writer) throws IOException;

	@Override
	public void handle(HttpRequest request, HttpResponse response) throws IOException {
		this.request = request;
		this.response = response;

		Reader reader = new InputStreamReader(request.inputStream, FileUtil.charset);
		Writer writer = new OutputStreamWriter(response.outputStream, FileUtil.charset);

		handle(reader, writer);

		writer.flush();
	}

}
