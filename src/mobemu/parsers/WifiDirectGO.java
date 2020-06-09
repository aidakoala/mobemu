package mobemu.parsers;

import java.util.LinkedList;

import mobemu.node.Node;

public class WifiDirectGO {
	public int id;
	public long groupTimeout;
	public long breakTime;
	public boolean isBreakTime;
	public LinkedList<Host> clients;
	
	public WifiDirectGO(int id) {
		this.id = id;
		this.isBreakTime = false;
		// might edit
		this.breakTime = Node.MILLIS_IN_5MIN;
		this.groupTimeout = Node.MILLIS_IN_5MIN;
		this.clients = new LinkedList<Host>();
	}
	
	public void resetTimers() {
		this.isBreakTime = false;
		this.groupTimeout = Node.MILLIS_IN_5MIN;
		this.breakTime = Node.MILLIS_IN_5MIN;
	}
}
