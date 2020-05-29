package mobemu.parsers;

public class TimeAway {
	public int nodeId;
	public long leaveTime;
	public long returnTime;

	public TimeAway(int nodeId, long leaveTime, long returnTime) {
		super();
		this.nodeId = nodeId;
		this.leaveTime = leaveTime;
		this.returnTime = returnTime;
	}

}
