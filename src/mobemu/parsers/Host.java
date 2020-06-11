package mobemu.parsers;


import java.util.LinkedList;

import mobemu.node.Node;
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
    public int id;
    
    public int currentAP;
    public int lastAP;
    public long wifiDTime;
    
    public int movementType;
    public long returnTime; // the time this node will return to its community
    
    public Host() {}
    
    public Host(int protocol, int id) {
    	this.id = id;
    	this.protocol = protocol;
    	this.movementType = -1;
    	this.returnTime = -1;

    	// wifi direct trace
    	this.lastAP = -1;
    	this.currentAP = -1;
    	this.wifiDTime = FestivalMobility.SEC_IN_MIN;
    	
//    	switch (protocol) {
//    	case FestivalMobility.BLUETOOTH:
//    		maxPeers = FestivalMobility.MAX_PEERS_BT;
//    		break;
//    	case FestivalMobility.WIFIDIRECT:
//    		maxPeers = FestivalMobility.MAX_PEERS_WD;
//    		break;
//    	default:
//    		maxPeers = FestivalMobility.MAX_PEERS_WD;
//    	}
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(absSpeed);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(af);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + cellIdX;
		result = prime * result + cellIdY;
		result = prime * result + currentAP;
		temp = Double.doubleToLongBits(currentX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(currentY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(goalCurrentX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(goalCurrentY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + groupId;
		result = prime * result + id;
		result = prime * result + (isATraveler ? 1231 : 1237);
		result = prime * result + lastAP;
		result = prime * result + maxPeers;
		result = prime * result + movementType;
		temp = Double.doubleToLongBits(previousGoalX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(previousGoalY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + protocol;
		result = prime * result + (int) (returnTime ^ (returnTime >>> 32));
		temp = Double.doubleToLongBits(speed);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (wifiDTime ^ (wifiDTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		Host other = (Host) obj;

		if (groupId != other.groupId) {
			return false;
		}
		if (id != other.id) {
			return false;
		}

		return true;
	}

	public void resetWifiParams() {
    	this.lastAP = this.currentAP;
    	this.currentAP = -1;
    	this.wifiDTime = FestivalMobility.SEC_IN_MIN;
    }
}
