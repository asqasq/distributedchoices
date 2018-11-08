package asq.choices.common;

public class ForwardValues {
	final public String type = "forwardValues";

	public String initiator;
	public int roundNumber;

	public String sender;
	public String receiver;

	public String encryptedValues;

	public ForwardValues(String initiator, int roundNumber,
			String sender, String receiver, String encryptedValues) {
		this.initiator = initiator;
		this.roundNumber = roundNumber;
		this.sender = sender;
		this.receiver = receiver;
		this.encryptedValues = encryptedValues;
	}
}
