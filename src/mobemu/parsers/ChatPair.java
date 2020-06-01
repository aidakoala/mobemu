package mobemu.parsers;

import com.univocity.parsers.annotations.*;

public class ChatPair {
	@Parsed(index = 0)
	public int nodeAway;
	@Parsed(index = 1)
	public int nodeDest;
	@Parsed(index = 2)
	public long leaveTime;
	@Parsed(index = 3)
	public long returnTime;

	public ChatPair(int nodeAway, int nodeDest, long leaveTime, long returnTime) {
		super();
		this.nodeAway = nodeAway;
		this.nodeDest = nodeDest;
		// convert seconds to milliseconds
		this.leaveTime = leaveTime * 1000;
		this.returnTime = returnTime * 1000;
	}

}
