/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for a device's battery.
 *
 * @author Radu
 */
public class Battery {

    private double currentLevel; // current battery level
    private long leftToRecharge; // duration until the device is fully recharged (in ticks)
    private static double maxLevel; // maximum battery level (i.e. when it is fully charged)
    private static long rechargeDuration; // duration of recharge (in ticks)
    private static double minBatteryThreshold; // threshold under which a node doesn't participate in the network any more
    private static final int NOT_CHARGING = -1;

    /**
     * Creates a {@link Battery} object.
     *
     * @param currentLevel starting level of this device's battery; if set to a
     * negative value, the level is chosen randomly between 0 and {@code maxLevel}
     * @param maxLevel the maximum allowed battery level
     * @param rechargeDuration the duration it takes for this battery to
     * recharge
     * @param minBatteryThreshold minimum battery threshold
     */
    public Battery(double currentLevel, double maxLevel, long rechargeDuration, double minBatteryThreshold) {
        this.currentLevel = currentLevel;
        this.leftToRecharge = NOT_CHARGING;

        Battery.maxLevel = maxLevel;
        Battery.rechargeDuration = rechargeDuration;
        Battery.minBatteryThreshold = minBatteryThreshold;
    }

    /**
     * Gets the current battery level.
     *
     * @return the current battery level
     */
    public double getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Gets the maximum battery level.
     *
     * @return the maximum battery level
     */
    public static double getMaxLevel() {
        return maxLevel;
    }

    /**
     * Update the device's current battery level.
     */
    public void updateBatteryLevel() {
        if (leftToRecharge == NOT_CHARGING) {
            currentLevel--;

            if (currentLevel <= 0) {
                currentLevel = 0;
                leftToRecharge = 0;
            }
        } else {
            leftToRecharge++;
            if (leftToRecharge >= rechargeDuration) {
                currentLevel = maxLevel;
                leftToRecharge = NOT_CHARGING;
            }
        }
    }

    /**
     * Checks whether the current node can participate in the network (i.e. if
     * the node's battery level is higher than the minimum battery threshold).
     *
     * @return {@code true} if the node can participate, {@code false} otherwise
     */
    public boolean canParticipate() {
        return currentLevel >= minBatteryThreshold;
    }
}