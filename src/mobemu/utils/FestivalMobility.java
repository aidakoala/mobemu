package mobemu.utils;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.univocity.parsers.csv.CsvWriter;

import mobemu.node.Node;
import mobemu.parsers.CellsItem;
import mobemu.parsers.Host;
import mobemu.parsers.WifiDirectGO;
import mobemu.parsers.ChatPair;

public abstract class FestivalMobility {
	public static final int BLUETOOTH = 0;
	public static final int WIFIDIRECT = 1;
	public static final int BOTH = 2;

	public static final float BLUETOOTH_RADIUS = 5.0f;
	public static final float WIFIDIRECT_RADIUS = 30.0f;

	// TODO future work
	public static final int MAX_PEERS_BT = 7;
	public static final int MAX_PEERS_WD = 30;
	
	// types of movement
	// assuming the shows start on the hour, the communities will be able
	// to move between grids/stages when this feature will be implemented
	public static final int MOVE_NODE = 0;
	public static final int MOVE_BACK = 1;
	public static final int MOVE_GROUP = 2;
	// move nodes 3 times more often than moving a community
	public static final int ZIPF_SIZE = 4; // 0, 1, 2 -> MOVE_NODE, 3 -> MOVE_GROUP
	
	// types of destination for node/community movement
	public static final int EDGE_CELL = 0;
	public static final int FRIENDS_CELL = 1;
	public static final int DEST_TYPES = 2;
	
	// time spent by a node/community away
	public static final int EDGE_MIN = 10 * 60; // 10 min
	public static final int EDGE_MAX = 15 * 60; // 15 min
	public static final int FRIENDS_MIN = 10 * 60; // 10 min
	public static final int FRIENDS_MAX = 20 * 60; // 20 min
	// TODO future work
	public static final int NEW_SHOW_MIN = 40 * 60; // 40 min
	public static final int NEW_SHOW_MAX = 60 * 60; // 1h
	
	protected int noHosts;
	protected int noOfTravelers;
	protected int groupSize;
	protected int noOfGroups;
	protected double connectionThreshold = 1.0;
	
	protected float height;
	protected float width;
	protected int rows;
	protected int cols;
	protected float heightCell;
	protected float widthCell;
	/* radius of the transmission area */
	protected float wifiDirectRadius;
	protected float bluetoothRadius;
		
	protected double totalSimulationTime;
	protected double simTime = 0.0;
	/* refresh time */
	protected double stepInterval = 1.0;
	/* bounds of speed */
	protected float minHostSpeed;
	protected float maxHostSpeed;
	protected float travelerSpeed = 0.5f;

    /* set to true if the simulation should be shown graphically */
    protected boolean showRun = false;
    protected long sleepTime;
	
    /* internal data structures */
    public Host[] hosts;
    protected Host[] travelers;
    protected float interMat[][];
    protected int[][] groups;
    protected int[] numberOfMembers;
 
    CellsItem[][] cells;
    int[] edgeCellX;
    int[] edgeCellY;
    float[][] CA; // cell attractivity
    boolean eligibleGroup[];
    LinkedList<ChatPair> chatPairs;
    // wifi direct
    HashMap<Integer, WifiDirectGO> wifiDirectAPs;
    
    // density of people in a crowd mesured in people / m^2
    protected float maxDensity = 4.0f;
    protected float minDensity = 2.0f;
    protected float midDensity = 3.0f;
    
    boolean[][] isConnected;

    protected int seed;

    /* Components for the graphical part of the simulation */
	long contacts = 0;
	JFrame frame = null;
	JTextArea text = null;
	FestivalMobilityComponent component = null;
    
    protected abstract void startContact(int nodeA, int nodeB, double tick);
    protected abstract void endContact(int nodeA, int nodeB, double tick, CsvWriter csvWriter);
    protected abstract void generateInteractionMatrix();
    protected abstract void computeCommonFriends();
    
    protected void initHosts(Random r) {    	
    	hosts = new Host[noHosts];
    	for (int i = 0; i < noHosts; i++) {
    		hosts[i] = new Host(BLUETOOTH, i);
    		hosts[i].speed = minHostSpeed + (maxHostSpeed - minHostSpeed) * r.nextDouble();
    	}
    	numberOfMembers = new int[groupSize];
    	
    	// wifi direct
    	this.wifiDirectAPs = new HashMap<Integer, WifiDirectGO>();
    	
    	// 10% of hosts chosen randomly will use WiFi Direct to communicate
//    	int wifiDHosts = (int) (0.1 * noHosts);
//    	while (wifiDHosts > 0) {
//    		int id = r.nextInt(noHosts);
//    		hosts[id].protocol = BOTH;
//    		wifiDHosts--;
//    	}
    	
    	travelers = new Host[noOfTravelers];
    	for (int i = 0; i < noOfTravelers; i++) {
    		travelers[i] = new Host(BLUETOOTH, i);
    		travelers[i].speed = travelerSpeed;
    	}
    	
    	groups = new int[noOfGroups][groupSize];
    	eligibleGroup = new boolean[noOfGroups];
    	Arrays.fill(eligibleGroup, true);

		isConnected = new boolean[noHosts][noHosts];
        // setup of the links
        for (int i = 0; i < noHosts; i++) {
            for (int j = 0; j < noHosts; j++) {
                isConnected[i][j] = false;
            }
        }
    }
    
    public void initEgdeCellCoords() {
    	int noEdgeCells = 2 * rows + cols - 2 - 2;
    	
    	edgeCellX = new int[noEdgeCells];
    	edgeCellY = new int[noEdgeCells];
    	
    	// left, right, bottom
    	int i = 0;
    	for (int l = 1; l < rows; l++) {
    		edgeCellX[i] = l;
   			edgeCellY[i] = 0;
   			i++;
    	}
    	for (int r = 1; r < rows; r++) {
    		edgeCellX[i] = r;
    		edgeCellY[i] = cols - 1;
    		i++;
    	}
    	for (int b = 1; b < cols - 1; b++) {
    		edgeCellX[i] = rows - 1;
    		edgeCellY[i] = b;
    		i++;
    	}
    }
    
    public void gridSetup() {
    	cells = new CellsItem[rows][cols];
    	
    	for (int i = 0; i < rows; i++) {
    		for (int j = 0; j < cols; j++) {
    			cells[i][j] = new CellsItem(i, j);
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cells[i][j].minX = ((float) i) * heightCell;
                cells[i][j].minY = ((float) j) * widthCell;
                cells[i][j].numberOfHosts = 0;
            }
        }
    	
    	CA = new float[rows][cols];
    }
    
    public void computeCoords(int hostId, Host[] hostVec, Random rand) {
    	int cellX, cellY;
    	double r;

   		cellX = hostVec[hostId].cellIdX;
   		cellY = hostVec[hostId].cellIdY;
    	do {
	   		// x1 and y1
    		hostVec[hostId].currentX = cells[cellX][cellY].minX + rand.nextDouble() * heightCell;
    		hostVec[hostId].currentY = cells[cellX][cellY].minY + rand.nextDouble() * widthCell;
    		// x2 and y2
    		hostVec[hostId].goalCurrentX = cells[cellX][cellY].minX + rand.nextDouble() * heightCell;
    		hostVec[hostId].goalCurrentY = cells[cellX][cellY].minY + rand.nextDouble() * widthCell;
    		
    		r = Math.pow(Math.pow(hostVec[hostId].goalCurrentX - hostVec[hostId].currentX, 2)
                       + Math.pow(hostVec[hostId].goalCurrentY - hostVec[hostId].currentY, 2), 1 / 2) / Math.sqrt(2);
    	} while (rand.nextDouble() >= r);

       hostVec[hostId].previousGoalX = hostVec[hostId].currentX;
       hostVec[hostId].previousGoalY = hostVec[hostId].currentY;
    }
    
    public void computeCoordsHost(int hostId, double minX, double minY,
    		float heightCell, float widthCell, Random rand) {
    	hosts[hostId].currentX = minX + rand.nextDouble() * heightCell;
    	hosts[hostId].currentY = minY + rand.nextDouble() * widthCell;

    	hosts[hostId].goalCurrentX = hosts[hostId].currentX;
        hosts[hostId].goalCurrentY = hosts[hostId].currentY;
    		    	
    	hosts[hostId].previousGoalX = hosts[hostId].currentX;
        hosts[hostId].previousGoalY = hosts[hostId].currentY;
    }
    
    public void computeCoordsGroups(Random rand) {
    	int groupsCell, groupId;
    	double minX, minY;
    	float height, width;
    	int done = 0, limit;

    	for (int i = 0; i < rows; i++) {
    		for (int j = 0; j < cols; j++) {
    			// check how many groups can that cell hold
    			groupsCell = cells[i][j].groupIds.size();
    			minX = cells[i][j].minX;
        		minY = cells[i][j].minY;
        		
        		if (groupsCell > 2) {
        			// we divide the width cell into groupsCell / 2 areas so the we would only
        			// mix the members of 2 groups in a certain area of the cell
        			// otherwise we would have nodes in the same community distributed randomly
        			// in the cell, which does not resemble reality
	        		groupsCell /= 2;
//	            	width = widthCell / groupsCell;            		
//	            	for (int k = 0; k < cells[i][j].groupIds.size(); k++) {
//	            		groupId = cells[i][j].groupIds.get(k);
//	        			if (done == 2) {
//	        				minY = minY + width;
//	        				done = 0;
//	        			}
//	            		for (int l = 0; l < groupSize; l++) {
//	            			computeCoordsHost(groups[groupId][l], minX, minY, heightCell, width, rand);
//	            		}
//	        			done++;
//	            	}
	        		// same strategy as the one commented above, but dividing the height
	            	height  = heightCell / groupsCell;            		
	            	for (int k = 0; k < cells[i][j].groupIds.size(); k++) {
	            		groupId = cells[i][j].groupIds.get(k);
	        			if (done == 2) {
	        				minX = minX + height;
	        				done = 0;
	        			}
	            		for (int l = 0; l < groupSize; l++) {
	            			computeCoordsHost(groups[groupId][l], minX, minY, height, widthCell, rand);
	            		}
	        			done++;
	            	}
        		} else {
        			width = widthCell / groupsCell;
            		for (int k = 0; k < cells[i][j].groupIds.size(); k++)
            			for (int l = 0; l < groupSize; l++) {
            				groupId = cells[i][j].groupIds.get(k);
            				computeCoordsHost(groups[groupId][l], minX, minY, heightCell, width, rand);
            			}
        		}
        	}
    	}
    }
    
    protected void placeGroupsOnGrid(Random rand) {
    	// compute maximum number of groups allowed in a cell
        // maximum density is of 4 people / m^2
    	int cellArea = (int) (heightCell * widthCell);
    	int maxNoHosts4 = (int) (cellArea * maxDensity);
        int maxNoHosts3 = (int) (cellArea * midDensity);
        int maxNoHosts2 = (int) (cellArea * minDensity);
        System.out.println("cellArea = " + cellArea);
    	
        // crowd density at a concert is not the same in every cell in the map
        // it mostly depends on the position of the stage
        int[][] hostsCells = new int[rows][cols];
        int start = 0;
        int limit = rows / 6;
        for (int i = 0; i < limit; i++) {
            for (int j = 0; j < cols; j++) {
                hostsCells[i][j] = maxNoHosts4;
            }
        }
        start = limit;
        limit = start + (rows - limit) * 2 / 3 ;
        for (int i = start; i < limit; i++) {
            for (int j = 0; j < cols; j++) {
                hostsCells[i][j] = maxNoHosts3;
            }
        }
        start = limit;
        for (int i = start; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                hostsCells[i][j] = maxNoHosts2;
            }
        }
        
        // place a group in a cell taking into account crowd density
        int cellIdX, cellIdY;
        for (int i = 0; i < noOfGroups; i++) {
        	 do {
                 cellIdX = rand.nextInt(rows);
                 cellIdY = rand.nextInt(cols);
             } while (hostsCells[cellIdX][cellIdY] - groupSize < 0);

        	 cells[cellIdX][cellIdY].groupIds.add(i);
        	 hostsCells[cellIdX][cellIdY] -= groupSize;
        	 
        	 for (int j = 0; j < groupSize; j++) {
                 int hostId = groups[i][j];
                 hosts[hostId].cellIdX = cellIdX;
                 hosts[hostId].cellIdY = cellIdY;

                 // increment the number of the hosts in that cell
                 cells[cellIdX][cellIdY].numberOfHosts += 1;
             }
        }
     
        // generate x, y coordinates for every host in a group
        computeCoordsGroups(rand);

//        for (int i = 0; i < noOfTravelers; i++) {
//        	// assign every traveler to a staring cell
//        	 travelers[i].cellIdX = rand.nextInt(rows);
//             travelers[i].cellIdY = rand.nextInt(cols);
//             computeCoords(i, travelers, rand);
//        }
    }
    
    public CellsItem computeCAHostHelper(int hostId, float[][] CA) {
    	CellsItem resCell = null;
    	float maxCA = 0.0f;
    	int row, col;

		// sum the weights from the interaction matrix
    	for (int i = 0; i < noHosts; i++) {
    		// do not take into account nodes from the friends community
    		if (interMat[hostId][i] != 0.0f && interMat[hostId][i] != 1.0f) {
    			row = hosts[i].cellIdX;
    			col = hosts[i].cellIdY;
    		} else
    			continue;
    		CA[row][col] += interMat[hostId][i];
    	}
    	// normalize the results and compute the next goal cell
    	for (int i = 0; i < rows; i++)
    		for (int j = 0; j < cols; j++) {
    			CA[i][j] /= cells[i][j].numberOfHosts;
    			if (maxCA < CA[i][j]) {
    				maxCA = CA[i][j];
    				resCell = cells[i][j];
    			}
    		}
 
    	return resCell;
    }
    
    public CellsItem computeCAHost(int hostId) {
    	for (int i = 0; i < rows; i++)
			Arrays.fill(CA[i], 0.0f);
    	
    	return computeCAHostHelper(hostId, CA);
    }
    
    public CellsItem computeCAGroup(int groupId) {
    	CellsItem resCell = null;
    	float maxCA = 0.0f;
    	
		for (int i = 0; i < rows; i++)
			Arrays.fill(CA[i], 0.0f);
		
		for (int i = 0; i < groupSize; i++)
			computeCAHostHelper(groups[groupId][i], CA);
		
    	for (int i = 0; i < rows; i++)
    		for (int j = 0; j < cols; j++) {
    			if (maxCA < CA[i][j]) {
    				maxCA = CA[i][j];
    				resCell = cells[i][j];
    			}
    		}

    	return resCell;
    }
    
    public void computeGoalCoords(int id, Host[] vec, Random rand) {
    	int x, y;

    	x = vec[id].cellIdX;
    	y = vec[id].cellIdY;
    	
    	vec[id].previousGoalX = vec[id].currentX;
    	vec[id].previousGoalY = vec[id].currentY;
    	
    	vec[id].goalCurrentX = cells[x][y].minX + rand.nextDouble() * heightCell;
		vec[id].goalCurrentY = cells[x][y].minY + rand.nextDouble() * widthCell;
    }
    
    public boolean moveTowardsGoal(int id, Host[] vec) {
    	  if ((vec[id].currentX > vec[id].goalCurrentX + vec[id].speed)
                  || (vec[id].currentX < vec[id].goalCurrentX - vec[id].speed)
                  || (vec[id].currentY > vec[id].goalCurrentY + vec[id].speed)
                  || (vec[id].currentY < vec[id].goalCurrentY - vec[id].speed)) {
              // move towards the goal
              if (vec[id].currentX < (vec[id].goalCurrentX - vec[id].speed)) {
                  vec[id].currentX = vec[id].currentX + vec[id].speed;
              }
              if (vec[id].currentX > (vec[id].goalCurrentX + vec[id].speed)) {
                  vec[id].currentX = (vec[id].currentX) - vec[id].speed;
              }
              if (vec[id].currentY < (vec[id].goalCurrentY - vec[id].speed)) {
                  vec[id].currentY = (vec[id].currentY) + vec[id].speed;
              }
              if (vec[id].currentY > (vec[id].goalCurrentY + vec[id].speed)) {
                  vec[id].currentY = (vec[id].currentY) - vec[id].speed;
              }
              return true;
    	  }
    	  return false;
    }
    
    public void moveTravelers(Random rand) {
    	for (int i = 0; i < noOfTravelers; i++) {
    		if (!moveTowardsGoal(i, travelers)) {
    			// generate a new goal
    			if (rand.nextDouble() > 0.999) {
    		    	travelers[i].cellIdX = rand.nextInt(rows);
    		     	travelers[i].cellIdY = rand.nextInt(cols);
    				computeGoalCoords(i, travelers, rand);
    			}
    		}
    	}
    }
    
    public void coordsNearHost(Host host1, Host host2) {
    	int x = host1.cellIdX, y = host1.cellIdY;
    	host2.goalCurrentX = host1.currentX;
    	// place host2 to the left of host1 or to the right, 0.5m away
    	host2.goalCurrentY = host1.currentY + 0.5;
    	if (host2.currentY > cells[x][y].minY + width)
    		host2.goalCurrentY = host1.currentY - 0.5;
    }
    
    public void disconnectAPClients(int id) {
    	WifiDirectGO wifiDirectAP = this.wifiDirectAPs.get(id);
    	
    	for (Host host :  wifiDirectAP.clients) {
    		host.resetWifiParams();
    	}
    	
    	wifiDirectAP.clients.clear();
    }
    
    public void moveHosts(long simTime) {
    	int peerId;
    	
    	for (int i = 0; i < noHosts; i++) {
    		// if the node has reached his goal cell
    		if (!moveTowardsGoal(i, hosts)) {
    			if (hosts[i].movementType == MOVE_NODE) {
    				// check if the node needs to get back to its community
    				if (hosts[i].returnTime < simTime) {
    					// update the goal to the cell where its community is
    					hosts[i].previousGoalX = hosts[i].currentX;
    					hosts[i].previousGoalY = hosts[i].currentY;
    					
    					// pick a member of its community and generate coordinates near it
    					for (int j = 0; j < groupSize; j++)
    						if (groups[hosts[i].groupId][j] != i) {
    							peerId = groups[hosts[i].groupId][j];
    							hosts[i].cellIdX = hosts[peerId].cellIdX;
    							hosts[i].cellIdY = hosts[peerId].cellIdY;
    							coordsNearHost(hosts[peerId], hosts[i]);
    							break;
    						}
    					
    					hosts[i].movementType = MOVE_BACK;
    					hosts[i].returnTime = -1;
    				}
    			} else if (hosts[i].movementType == MOVE_BACK) {
    				hosts[i].movementType = -1;
    				hosts[i].returnTime = -1;
        			eligibleGroup[hosts[i].groupId] = true;
        			
        			// once the node is reunited with its community it stops being an AP
        			disconnectAPClients(i);
        			this.wifiDirectAPs.remove(i);
        			hosts[i].protocol = BLUETOOTH;

    			} else if (hosts[i].movementType == MOVE_GROUP) {
    				int groupId = hosts[i].groupId;
    				for (int j = 0; j < groupSize; j++) {
    					hosts[groups[groupId][j]].movementType = -1;
    				}
    				eligibleGroup[groupId] = true;
    			}
    		}
    	}
    }
    
    public int updateTarget() {
    	int target = 0;
    
    	for (int i = 0; i < noHosts; i++)
    		if (hosts[i].movementType != -1)
    			target++;
 
    	return target;
    }
    
    public void computeReturnTime(Host host, int destType, double simTime, Random rand) {
    	if (destType == EDGE_CELL)
    		host.returnTime = (long) (simTime + EDGE_MIN
    				+ (EDGE_MAX - EDGE_MIN) * rand.nextDouble());
    	else
    		host.returnTime = (long) (simTime + FRIENDS_MIN 
    				+ (FRIENDS_MAX - FRIENDS_MIN) * rand.nextDouble());
    }
    
    public double getDistance(Host h1, Host h2) {
    	return Math.sqrt((h1.currentX - h2.currentX) * (h1.currentX - h2.currentX)
                + (h1.currentY - h2.currentY) * (h1.currentY - h2.currentY));
    }
    
    public void generateContactsWifiDirect(WifiDirectGO ap, CsvWriter csvWriter) {
    	float radius = FestivalMobility.BLUETOOTH_RADIUS;
    	
//    	if (ap.isBreakTime) {
//    		ap.breakTime--;
//    		// if the break is over
//    		if (ap.breakTime == 0) {
//    			ap.resetTimers();
//    			
//    			for (int i = 0; i < noHosts; i++) {
//    				if (i == ap.id)
//    					continue;
//    				// a host can be connected to only one access point in legacy mode
//    				if (hosts[i].currentAP != -1)
//    					continue;
//    				
//    				double currentDist = getDistance(hosts[ap.id], hosts[i]);
//    				if (currentDist < radius) {
//    					isConnected[ap.id][i] = true;
//                            
//    					hosts[i].currentAP = ap.id;
//    					hosts[i].wifiDTime = Node.MILLIS_IN_1MIN;
//                        ap.clients.add(hosts[i]);
//                            
//                        startContact(ap.id, i, simTime);
//                        contacts++;
//                     }
//    			}
//    		}
//    	} else {
//    		ap.groupTimeout--;
//    		if (ap.groupTimeout == 0) {
//    			disconnectAPClients(ap.id);
//    			ap.isBreakTime = true;
//    			return;
//    		}
    		
    		// see what hosts should be disconnected
    		Iterator<Host> it = ap.clients.iterator();
    	    while (it.hasNext()) {
    	    	Host host = it.next();
    	    	host.wifiDTime--;
    			if (host.wifiDTime == 0) {
    				host.wifiDTime = Node.MILLIS_IN_1MIN;
    				isConnected[ap.id][host.id] = false;
    				host.lastAP = ap.id;
    				host.currentAP = -1;
                    endContact(ap.id, host.id, simTime, csvWriter);
                    it.remove();
    			}
    	    }
    	    	
    	    int newContacts = MAX_PEERS_WD - ap.clients.size();
    	    for (int i = 0; (i < noHosts) && (newContacts > 0); i++) {
    			if (i == ap.id)
    				continue;
    			
    			if (hosts[i].currentAP != -1)
					continue;
    			
    			double currentDist = getDistance(hosts[ap.id], hosts[i]);
    			if (currentDist < radius) {
    				// if the hosts has been previously disconnected and the host was not just disconnected
    				// from this access point, then they must be connected
                   if ((!isConnected[ap.id][i]) && (hosts[i].lastAP != ap.id)) {
                	   	isConnected[ap.id][i] = true;
                       	startContact(ap.id, i, simTime);
                       	hosts[i].currentAP = ap.id;
   						hosts[i].wifiDTime = Node.MILLIS_IN_1MIN;
   						ap.clients.add(hosts[i]);
                       	newContacts--;
                       	contacts++;
                   }
    			} else {
    				if (isConnected[ap.id][i])
    					if (simTime != 0) {
    						// if the hosts has been previously connected, then they must be disconnected
                            isConnected[ap.id][i] = false;
                            hosts[i].wifiDTime = Node.MILLIS_IN_1MIN;
                            hosts[i].currentAP = -1;
            				hosts[i].lastAP = ap.id;
                            endContact(ap.id, i, simTime, csvWriter);
                            // remove from client list
                            ap.clients.remove(hosts[i]);
                        }
    			}
    		}
//    	}	
    }
    
    public void generateContacts(CsvWriter csvWriter) {
    	double radius;
 
    	for (int i = 0; i < noHosts; i++) {
    		if (wifiDirectAPs.get(i) != null) {
    			generateContactsWifiDirect(wifiDirectAPs.get(i), csvWriter);
    			continue;
    		}
    		
    		for (int j = 0; j < noHosts; j++) {
    			if (i != j) {
    				// skip the node if it is a Wifi Direct AP
    				if (hosts[i].protocol == -1)
    					continue;
//    				if (hosts[i].protocol != hosts[j].protocol)
//    					continue;
    				radius = FestivalMobility.BLUETOOTH_RADIUS;
    				double currentDist = getDistance(hosts[i], hosts[j]);
    				if (currentDist < radius) {
    			        // if the hosts has been previously disconnected, then they must be connected
                        if (!isConnected[i][j]) {
                            isConnected[i][j] = true;
                            startContact(i, j, simTime);
                            contacts++;
                        }
    				} else {
    				      if (isConnected[i][j])
                              if (simTime != 0) {
                                  // if the hosts has been previously connected, then they must be disconnected
                                  isConnected[i][j] = false;
                                  endContact(i, j, simTime, csvWriter);
                              }
    				}
    			}
    		}
    	}
    }
        
	protected void runSimulation(CsvWriter csvWriter, LinkedList<ChatPair> chatPairs) {
		this.chatPairs = chatPairs;
		
		Random rand = new Random(seed);
		initHosts(rand);
		initEgdeCellCoords();
		generateInteractionMatrix();
		computeCommonFriends();
		
		if (showRun)
			setupDisplay();
		
		gridSetup();
		placeGroupsOnGrid(rand);
		
        for (simTime = 0.0; simTime < totalSimulationTime; simTime += stepInterval) {
        	if (showRun) {
        		component.repaint();
				text.setText(generateStatsString(simTime, contacts));

				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        	}
        	
        	// compute next goal for traveler nodes
        	// moveTravelers(rand);
        	
        	// compute next goal for hosts
        	moveHosts((long)simTime);
        	// according to Zurich festival paper, 20% of the nodes are on the move at any given time
        	// our target is to move 5% of the nodes at any time, given the simulation size
        	int target = (int) (noHosts * 0.05);
        	target -= updateTarget();

        	while (target > 0) {
            	// pick a node
        		int id = rand.nextInt(noHosts);
        		if (eligibleGroup[hosts[id].groupId]) {
        			eligibleGroup[hosts[id].groupId] = false;
        			// decide between moving a node or its entire community
        			int decision = zipfDistribution(rand.nextDouble(), ZIPF_SIZE);
        			if (target % groupSize != 0)
        				decision = 2;
        			if (decision > 2) {
        				target = moveGroup(id, rand, target);
        			} else {
            			target = moveNode(id, rand, target);
        			}
        		}
        	}
        	generateContacts(csvWriter);        	
        }
        // finish the simulation
        for (int i = 0; i < noHosts; i++) {
            for (int j = 0; j < noHosts; j++) {
                if (isConnected[i][j] && simTime != 0) {
                    //if the hosts have been previously connected, then they must be disconnected
                    isConnected[i][j] = false;
                }
            }
        }
	}
	
	public int moveNode(int id, Random rand, int target) {
		int x, y;
		
		hosts[id].movementType = MOVE_NODE;
		x = hosts[id].cellIdX;
		y = hosts[id].cellIdY;
		// pick a destination according to Zipf's law
		int destType = zipfDistribution(rand.nextDouble(), DEST_TYPES);
		if (destType == EDGE_CELL) {
			int edgeCell = rand.nextInt(edgeCellX.length);
			hosts[id].cellIdX = edgeCellX[edgeCell];
			hosts[id].cellIdY = edgeCellY[edgeCell];
		} else if (destType == FRIENDS_CELL) {
			CellsItem goal = computeCAHost(id);
			hosts[id].cellIdX = goal.x;
			hosts[id].cellIdY = goal.y;
		}
		
		// the node will use WiFi Direct
		this.wifiDirectAPs.put(id, new WifiDirectGO(id));
		// the other nodes will not consider this node for bluetooth contacts
		hosts[id].protocol = -1;
		
		// generate coords in that cell
		computeGoalCoords(id, hosts, rand);
		// compute the time to return to its community
		computeReturnTime(hosts[id], destType, simTime, rand);

		// log the leave and return time for future message generation
		// pick a pair for this node to chat with
		int groudId = hosts[id].groupId, peerId = 0;
		// if we have a situation where this node was away from its community
    	// for a period of time, pick a community member and later
    	// generate messages between these two nodes
		for (int j = 0; j < groups[groudId].length; j++) {
			if (groups[groudId][j] != id) {
				peerId = groups[groudId][j];
				break;
			}
		}
		ChatPair chatPair = new ChatPair(id, peerId, (long)simTime, hosts[id].returnTime, true);
		chatPairs.add(chatPair);

		cells[x][y].numberOfHosts--;
		cells[hosts[id].cellIdX][hosts[id].cellIdY].numberOfHosts++;
		target--;
		
		return target;
	}

	public int moveGroup(int id, Random rand, int target) {
		int x, y, newX = 0, newY = 0;
		int groupId = hosts[id].groupId;
		
		x = hosts[id].cellIdX;
		y = hosts[id].cellIdY;
		for (int i = 0; i < groupSize; i++) {
			hosts[groups[groupId][i]].movementType = MOVE_GROUP;
		}
		
		int destType = zipfDistribution(rand.nextDouble(), DEST_TYPES);
		if (destType == EDGE_CELL) {
			int edgeCell = rand.nextInt(edgeCellX.length);
			newX = edgeCellX[edgeCell];
			newY = edgeCellY[edgeCell];
			for (int i = 0; i < groupSize; i++) {
				hosts[groups[groupId][i]].cellIdX = edgeCellX[edgeCell];
				hosts[groups[groupId][i]].cellIdY = edgeCellY[edgeCell];
			}
		} else if (destType == FRIENDS_CELL) {
			CellsItem goal = computeCAGroup(groupId);
			newX = goal.x;
			newY = goal.y;
			for (int i = 0; i < groupSize; i++) {
				hosts[groups[groupId][i]].cellIdX = goal.x;
				hosts[groups[groupId][i]].cellIdY = goal.y;
			}
		}
		
		// pick the cell partition where this community is headed
		int groupsCell = cells[newX][newY].numberOfHosts / groupSize;
		int partitions = groupsCell /  2;
		int partitionNo = rand.nextInt(partitions);
		
		float height = heightCell / partitions;
		double minX = cells[newX][newY].minX + partitionNo * height;
		
		for (int i = 0; i < groupSize; i++) {
			computeCoordsHost(groups[groupId][i], minX, cells[newX][newY].minY, height, widthCell, rand);
		}
		
		cells[x][y].numberOfHosts -= groupSize;
		cells[hosts[id].cellIdX][hosts[id].cellIdY].numberOfHosts += groupSize;
		target -= groupSize;
		return target;
	}
	
    /**
     * Generates a Zipf distribution.
     * @double value random value for the distribution
     * @return Zipf value for the given index
     */
    private static int zipfDistribution(double value, int zipfSize) {
        int zipfExponent = 1;
        // ordinea frecventei conform zifp's law frequency = 1/rank 0,1,2,3, ...
        double sum = 0;

        for (int i = 1; i <= zipfSize; i++) {
            double up = 1.0 / Math.pow(i, zipfExponent);
            double down = 0;
            for (int j = 1; j <= zipfSize; j++) {
                down += 1 / Math.pow(j, zipfExponent);
            }
            sum += up / down;
            if (value < sum) {
                return i - 1;
            }
        }

        return 0;
    }
	
    public void setupDisplay() {
    	frame = new JFrame();
		text = new JTextArea();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(false);
		text.setEditable(false);
		component = new FestivalMobilityComponent(this);
		frame.getContentPane().add(component, BorderLayout.CENTER);
		frame.getContentPane().add(text, BorderLayout.SOUTH);
		frame.pack();
		frame.setTitle("Simulation");

		// Display the frame
		int frameWidth = 500;
		int frameHeight = 1000;
		frame.setSize(frameWidth, frameHeight);
		frame.setVisible(true);
    }
    
	private String generateStatsString(double simTime, long contacts) {
		String s = "";

		s += "Simulation time: " + (long) simTime + " seconds";
		s += System.getProperty("line.separator");
		s += "Nodes: " + noHosts + " (" + noOfGroups + " communities, " + noOfTravelers + " travelers)";
		s += System.getProperty("line.separator");
		s += "Grid size: " + rows + "x" + cols + " cells, " + (int) height + "x"
				+ (int) width + " metres";
		s += System.getProperty("line.separator");
		s += "Contacts: " + contacts;

		return s;
	}

} 
