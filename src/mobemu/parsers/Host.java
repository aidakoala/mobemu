package mobemu.parsers;

import mobemu.utils.FestivalMobility;

/**
 * Class representing an HCMM host.
 */
public class Host {

    public double currentX;
    public double currentY;
    public double relativeX;
    public double relativeY;
    public double goalRelativeX;
    public double goalRelativeY;
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
    
    public Host() {}
    
    public Host(int protocol) {
    	this.protocol = protocol;
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
