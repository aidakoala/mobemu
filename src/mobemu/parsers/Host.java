package mobemu.parsers;

import mobemu.utils.FestivalMobility;

/**
 * Class representing an HCMM host.
 */
public class Host {

    public double currentX;
    public double currentY;
    public double goalCurrentX;
    public double goalCurrentY;
    public double previousGoalX;
    public double previousGoalY;
    public int cellIdX;
    public int cellIdY;

    public double speed;
    public double absSpeed;
    public double af;
    public boolean isATraveler;

    public int protocol;
    public int maxPeers;
    public int groupId;
    
    public int movementType;
    public long returnTime; // the time this node will return to its community
    
    public Host() {}
    
    public Host(int protocol) {
    	this.protocol = protocol;
    	this.movementType = -1;
    	this.returnTime = -1;
    	switch (protocol) {
    	case FestivalMobility.BLUETOOTH:
    		maxPeers = FestivalMobility.MAX_PEERS_BT;
    		break;
    	case FestivalMobility.WIFIDIRECT:
    		maxPeers = FestivalMobility.MAX_PEERS_WD;
    		break;
    	default:
    		maxPeers = FestivalMobility.MAX_PEERS_WD;
    	}
    }
}
