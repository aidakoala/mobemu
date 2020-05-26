package mobemu.parsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an HCMM cell.
 */
public class CellsItem {

	public int x, y;
    public double minX;
    public double minY;
    public int numberOfHosts;
    public List<Integer> groupIds;
    
    public CellsItem(int x, int y) {
    	this.groupIds = new ArrayList<Integer>();
    	this.x = x;
    	this.y = y;
    }
}