package asq.choices.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;

import com.google.gson.*;

import asq.choices.common.NetworkUtilities;
import asq.choices.common.UserEntry;
import asq.choices.common.UserList;


public class Users {
	private UserList userList;
	private Gson g;
	private Hashtable<Socket, String> connections;
	private NetworkUtilities n;

	public Users() {
		this.n = new NetworkUtilities();
		userList = new UserList();
		g = new Gson();
		connections = new Hashtable<Socket, String>();
	}

	public synchronized void sendUserList() {
		String us = g.toJson(userList);
		System.out.println(us);
		byte[] b = us.getBytes();

		for (Iterator<Socket> it = connections.keySet().iterator(); it.hasNext(); ) {
			try {
				OutputStream out = it.next().getOutputStream();
//				out.write(b);
//				out.flush();
				n.sendString(out, b);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Failed to send userlist");
			}
		}
	}

	public synchronized void loginUser(String json, Socket socket) {
		UserEntry ue = g.fromJson(json, UserEntry.class);
		userList.addUser(ue);
		connections.put(socket, ue.name);
		sendUserList();
	}

	public synchronized void removeUser(Socket socket) {
		String name = connections.get(socket);
		connections.remove(socket);
		if (name != null) {
			userList.removeUser(name);
		}
		sendUserList();
	}

	public synchronized void abort() {
		// send abort message
	}

	public synchronized void sendNeueWahl() {
	}

	public synchronized void printUsers() {
		userList.printUsers();
		sendUserList();
	}

	public synchronized void generateNewChoice(String json) {
		for (Iterator<Socket> it = connections.keySet().iterator(); it.hasNext(); ) {
			try {
				OutputStream out = it.next().getOutputStream();
//				out.write(json.getBytes());
//				out.flush();
				n.sendString(out, json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Failed to send generateNewChoice");
			}
		}
	}

	public synchronized void forwardValues(String json) {
		for (Iterator<Socket> it = connections.keySet().iterator(); it.hasNext(); ) {
			try {
				OutputStream out = it.next().getOutputStream();
//				out.write(json.getBytes());
//				out.flush();
				n.sendString(out, json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Failed to send forwardValues");
			}
		}
	}
	public synchronized void commitChoice(String json) {
		for (Iterator<Socket> it = connections.keySet().iterator(); it.hasNext(); ) {
			try {
				OutputStream out = it.next().getOutputStream();
//				out.write(json.getBytes());
//				out.flush();
				n.sendString(out, json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Failed to send forwardValues");
			}
		}
	}
}
