package asq.choices.common;

import java.util.Iterator;
import java.util.LinkedList;

public class UserList {
	final public String type = "userlist";
	private LinkedList<UserEntry> users;

	public UserList() {
		users = new LinkedList<UserEntry>();
	}

	public void addUser(UserEntry ue) {
		users.add(ue);
	}

	public void removeUser(String name) {
		int i = 0;
		UserEntry ue;
		for (Iterator<UserEntry> it = users.iterator(); it.hasNext(); i++) {
			ue = it.next();
			if (ue.name.equals(name)) {
				break;
			}
		}
		users.remove(i);
	}

	public int getNrUsers() {
		return users.size();
	}

	public String getUserName(int id) {
		return users.get(id).name;
	}

	public String getUserkey(int id) {
		return users.get(id).pubKey;
	}

	public int getMyId(String name) {
		int i = 0;
		UserEntry ue;
		for (Iterator<UserEntry> it = users.iterator(); it.hasNext(); i++) {
			ue = it.next();
			if (ue.name.equals(name)) {
				break;
			}
		}
		return i;
	}

	public void printUsers() {
		System.out.println("Registered users:");
		for (Iterator<UserEntry> it = users.iterator(); it.hasNext(); ) {
			System.out.println(it.next().toString());
		}
	}

	public String[] getUsers() {
		String[] u = new String[users.size()];
		int i = 0;
		for (Iterator<UserEntry> it = users.iterator(); it.hasNext(); ) {
			u[i++] = it.next().name;
		}
		return u;
	}

}
