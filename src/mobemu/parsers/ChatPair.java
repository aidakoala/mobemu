package mobemu.parsers;

public class ChatPair {
	public int nodeAway;
	public int nodeDest;
	public long leaveTime;
	public long returnTime;

	public ChatPair(int nodeAway, int nodeDest, long leaveTime, long returnTime) {
		super();
		this.nodeAway = nodeAway;
		this.nodeDest = nodeDest;
		this.leaveTime = leaveTime;
		this.returnTime = returnTime;
	}

}
