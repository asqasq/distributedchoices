package asq.choices.common;

public class UserEntry {
	final public String type = "userentry";
	public String name;
	public String pubKey; // armored string representation of the public key

	public UserEntry(String name, String pubKey) {
		this.name = name;
		this.pubKey = pubKey;
	}

	public String toString() {
		return ("User:\n" + name + "\npubkey:\n" + pubKey + "\n");
	}
}
