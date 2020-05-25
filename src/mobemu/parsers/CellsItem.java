package mobemu.parsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an HCMM cell.
 */
public class CellsItem {

    public double minX;
    public double minY;
    public int numberOfHosts;
    public List<Integer> groupIds;
    
    public CellsItem() {
    	this.groupIds = new ArrayList<Integer>();
    }
}