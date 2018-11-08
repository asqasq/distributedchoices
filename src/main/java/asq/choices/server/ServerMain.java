package asq.choices.server;

import java.net.ServerSocket;
import java.net.Socket;

import asq.choices.common.NetworkUtilities;

public class ServerMain {
	final static int SERVER_PORT = 63425;

	public static void main(String[] args) throws Exception {
		new ServerMain().runServer();
	}

	public void runServer() throws Exception {
		Users users = new Users();

		ServerSocket ssocket = new ServerSocket(SERVER_PORT);

		while (true) {
			Socket newSocket = ssocket.accept();
			Thread t = new ProtocolHandler(newSocket, users);
			t.start();
		}
	}
}
