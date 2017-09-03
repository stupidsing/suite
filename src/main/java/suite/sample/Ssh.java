package suite.sample;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import suite.Constants;
import suite.util.Copy;
import suite.util.Rethrow;
import suite.util.Thread_;

public class Ssh {

	@FunctionalInterface
	public interface SshFun<I, O> {
		public O apply(I i) throws IOException, SftpException, JSchException;
	}

	public int execute(String command) throws JSchException, SftpException, IOException {
		return session(session -> channelExec(session, command, channel -> {
			while (!channel.isClosed())
				Thread_.sleepQuietly(100);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Copy.stream(channel.getInputStream(), baos);
			baos.close();

			return channel.getExitStatus();
		}));
	}

	public void putFile(String src, String dest) throws IOException, SftpException, JSchException {
		session(session -> channelSftp(session, channel -> {
			try (InputStream fis = new FileInputStream(src)) {
				channel.put(fis, dest);
				return true;
			}
		}));
	}

	private <T> T channelExec(Session session, String command, SshFun<ChannelExec, T> fun)
			throws IOException, SftpException, JSchException {
		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand(command);
		channel.connect();
		try {
			return fun.apply(channel);
		} finally {
			channel.disconnect();
		}
	}

	private <T> T channelSftp(Session session, SshFun<ChannelSftp, T> fun) throws IOException, SftpException, JSchException {
		ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
		channel.connect();
		try {
			return fun.apply(channel);
		} finally {
			channel.disconnect();
		}
	}

	private <T> T session(SshFun<Session, T> fun) {
		return Constants //
				.bindSecrets("ssh") //
				.map((host, portString, username, password) -> Rethrow.ex(() -> {
					return session(host, Integer.valueOf(portString), username, password, fun);
				}));
	}

	private <T> T session(String host, int port, String user, String password, SshFun<Session, T> fun)
			throws IOException, SftpException, JSchException {
		JSch jsch = new JSch();

		Properties config = new Properties();
		config.setProperty("StrictHostKeyChecking", "no");

		Session session = jsch.getSession(user, host, port);
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
