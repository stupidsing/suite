package suite.sample;

import static suite.util.Rethrow.ex;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import suite.cfg.Defaults;
import suite.util.Copy;
import suite.util.Thread_;

public class Ssh {

	public interface SshFun<I, O> {
		public O apply(I i) throws IOException, SftpException, JSchException;
	}

	public int execute(String command) throws JSchException, SftpException, IOException {
		return session(session -> channelExec(session, command, channel -> {
			while (!channel.isClosed())
				Thread_.sleepQuietly(100);

			var baos = new ByteArrayOutputStream();
			Copy.stream(channel.getInputStream(), baos);
			baos.close();

			return channel.getExitStatus();
		}));
	}

	public void putFile(String src, String dest) throws IOException, SftpException, JSchException {
		session(session -> channelSftp(session, channel -> {
			try (var fis = new FileInputStream(src)) {
				channel.put(fis, dest);
				return true;
			}
		}));
	}

	private <T> T channelExec(Session session, String command, SshFun<ChannelExec, T> fun)
			throws IOException, SftpException, JSchException {
		var channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand(command);
		channel.connect();
		try {
			return fun.apply(channel);
		} finally {
			channel.disconnect();
		}
	}

	private <T> T channelSftp(Session session, SshFun<ChannelSftp, T> fun) throws IOException, SftpException, JSchException {
		var channel = (ChannelSftp) session.openChannel("sftp");
		channel.connect();
		try {
			return fun.apply(channel);
		} finally {
			channel.disconnect();
		}
	}

	private <T> T session(SshFun<Session, T> fun) {
		return Defaults //
				.bindSecrets("ssh") //
				.map((host, portString, username, password) -> ex(() -> {
					return session(host, Integer.valueOf(portString), username, password, fun);
				}));
	}

	private <T> T session(String host, int port, String user, String password, SshFun<Session, T> fun)
			throws IOException, SftpException, JSchException {
		var jsch = new JSch();

		var config = new Properties();
		config.setProperty("StrictHostKeyChecking", "no");

		var session = jsch.getSession(user, host, port);
		session.setUserInfo(new UserInfo() {
			public String getPassphrase() {
				return null;
			}

			public String getPassword() {
				return password;
			}

			public boolean promptPassphrase(String arg0) {
				return true;
			}

			public boolean promptPassword(String arg0) {
				return true;
			}

			public boolean promptYesNo(String arg0) {
				return true;
			}

			public void showMessage(String arg0) {
			}
		});
		session.setConfig(config);
		session.connect();

		try {
			return fun.apply(session);
		} finally {
			session.disconnect();
		}
	}

}
