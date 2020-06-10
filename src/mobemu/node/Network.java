/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import mobemu.parsers.FestivalMobility;

/**
 * Class for representing an opportunistic node's network capabilities.
 *
 * @author Radu
 */
public class Network {

    private double transferSpeed; // node transfer speed (in messages per tick)
    public static final double UNLIMITED_TRANSFER_SPEED = Double.MAX_VALUE;
	public static final double BLUTOOTH_TRANSFERS_SPEED = 0.0025; // 100KB messages/ms
	public static final double WIFIDIRECT_TRANSFERS_SPEED = 0.3; // 100KB messages/ms
    /**
     * Instantiates a {@code Network} object.
     *
     * @param transferSpeed transfer speed of the current node
     */
    public Network(double transferSpeed) {
        this.transferSpeed = transferSpeed;
    }

    /**
     * Instantiates a {@code Network} object with unlimited transfer speed.
     */
    public Network() {
        this.transferSpeed = UNLIMITED_TRANSFER_SPEED;
    }
    
    public void setTransferSpeed(double transferSpeed) {
    	this.transferSpeed = transferSpeed;
    }

    /**
     * Computes the number of messages that this node can receive during a
     * contact.
     *
     * @param contactDuration contact duration in ticks
     * @return the number of messages that this node can receive during a
     * contact
     */
    public int computeMaxMessages(long contactDuration) {
        return (transferSpeed == UNLIMITED_TRANSFER_SPEED) ? Integer.MAX_VALUE : (int) (transferSpeed * (float) contactDuration + 1);
    }
    
    public int computeMaxMessages(long contactDuration, boolean contactType) {
        return (contactType == FestivalMobility.BLUETOOTH_CONTACT) ? (int) (BLUTOOTH_TRANSFERS_SPEED * (float) contactDuration + 1) :
        	(int) (WIFIDIRECT_TRANSFERS_SPEED * (float) contactDuration + 1);
    }
}
