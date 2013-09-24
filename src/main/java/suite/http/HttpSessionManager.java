package suite.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import suite.http.HttpServer.Handler;
import suite.util.FileUtil;
import suite.util.HtmlUtil;
import suite.util.Util;

public class HttpSessionManager {

	public static class HttpSessionHandler implements Handler {
		public void handle(String method //
				, String server //
				, String path //
				, String query //
				, Map<String, String> headers //
				, InputStream is //
				, OutputStream os) throws IOException {
			if (headers.containsKey("Cookie")) {
				// TODO check if session outdated
				// TODO update session timestamp
			} else if (Util.equals(path, "/login")) {
				// TODO if too many sessions, remove outdated sessions
				// TODO authenticate user
				// TODO redirect to url
			} else
				try (Writer writer = new OutputStreamWriter(os, FileUtil.charset)) {

					// Shows login page
					writer.write("<html>" //
							+ "<head><title>Login</title></head>" //
							+ "<body>" //
							+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
							+ "<form name=\"login\" action=\"/login\" method=\"get\">" //
							+ "Username <input type=\"text\" name=\"username\" />" //
							+ "Password <input type=\"password\" name=\"password\" />" //
							+ "<input type=\"hidden\" name=\"url\" value=\"" + HtmlUtil.encode(path) + "\" />" //
							+ "<input type=\"submit\" value=\"Login\">" //
							+ "</form>" //
							+ "</font>" //
							+ "</body>" //
							+ "</html>");
				}
		}
	}

}
