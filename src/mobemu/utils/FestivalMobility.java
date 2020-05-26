package mobemu.utils;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import mobemu.parsers.CellsItem;
import mobemu.parsers.Host;
import mobemu.parsers.ProbRange;

public abstract class FestivalMobility {
	public static final int BLUETOOTH = 0;
	public static final int WIFIDIRECT = 1;
	public static final int BOTH = 2;

	public static final float BLUETOOTH_RADIUS = 5.0f;
	public static final float WIFIDIRECT_RADIUS = 30.0f;

	public static final int MAX_PEERS_BT = 7;
	public static final int MAX_PEERS_WD = 200;
	
	// types of movement
	// assuming the shows start on the hour, the communities will be able
	// to move between grids/stages when this feature will be implemented
	public static final int MOVE_GROUP = 0;
	public static final int MOVE_NODE = 1;
	public static final int MOVE_BACK = 2;
	
	// types of destination for node/community movement
	public static final int EDGE_CELL = 0;
	public static final int FRIENDS_CELL = 1;
	
	// time spent by a node/community away
	public static final int EDGE_MIN = 10 * 60; // 10 min
	public static final int EDGE_MAX = 20 * 60; // 20 min
	public static final int FRIENDS_MIN = 10 * 60; // 10 min
	public static final int FRIENDS_MAX = 30 * 60; // 30 min
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
	
	protected double rewiringProb;
	/*  probability of remaining in a non-home cell */
	protected double remainingProb;

    /* first run of the algorithm */
    protected boolean firstTime = true;
    /* set to true if the simulation should be shown graphically */
    protected boolean showRun = false;
    protected long sleepTime;
	
    /* internal data structures */
    protected Host[] hosts;
    protected Host[] travelers;
    protected float interMat[][];
    protected int[][] groups;
    protected int[] numberOfMembers;
 
    CellsItem[][] cells;
    int[] edgeCellX;
    int[] edgeCellY;
    float[][] CA; // cell attractivity
    boolean eligibleGroup[];
    
    // density of people in a crowd mesured in people / m^2
    protected float maxDensity = 4.0f;
    protected float minDensity = 2.0f;
    protected float midDensity = 3.0f;
    
    boolean[][] isConnected;

    FileWriter fstream;
    BufferedWriter out = null;
    protected int seed;

    /* Components for the graphical part of the simulation */
	long contacts = 0;
	JFrame frame = null;
	JTextArea text = null;
	FestivalMobilityComponent component = null;
    
    protected abstract void startContact(int nodeA, int nodeB, double tick);
    protected abstract void endContact(int nodeA, int nodeB, double tick);
    protected abstract void generateInteractionMatrix();
    
    protected void initHosts() {
    	hosts = new Host[noHosts];
    	for (int i = 0; i < noHosts; i++) {
    		hosts[i] = new Host(BLUETOOTH);
    		// maybe generate a random speed
    		Random r = new Random();
    		hosts[i].speed = minHostSpeed + (maxHostSpeed - minHostSpeed) * r.nextDouble();
    	}
    	numberOfMembers = new int[groupSize];
    	
    	travelers = new Host[noOfTravelers];
    	for (int i = 0; i < noOfTravelers; i++) {
    		travelers[i] = new Host(BLUETOOTH);
    		// maybe generate a random speed
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
                // lastValues[i][j] = 0.0;
            }
        }
    }
    
    public void initEgdeCellCoords() {
    	int noEdgeCells = 2 * rows + cols - 2;
    	
    	edgeCellX = new int[noEdgeCells];
    	edgeCellY = new int[noEdgeCells];
    	
    	// left, right, bottom
    	int i = 0;
    	for (int l = 0; l < rows; l++) {
    		edgeCellX[i] = l;
   			edgeCellY[i] = 0;
   			i++;
    	}
    	for (int r = 0; r < rows; r++) {
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
    
    protected void placeGroupsOnGrid() {
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
        Random rand = new Random(seed);
        for (int i = 0; i < noOfGroups; i++) {
//        	System.out.println("group id " + i);
        	 do {
                 cellIdX = rand.nextInt(rows);
                 cellIdY = rand.nextInt(cols);
//
//                 System.out.println("x = " + cellIdX + " y = " + cellIdY + 
//                 		" hostCellMem = " +  hostsCells[cellIdX][cellIdY]);
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

        for (int i = 0; i < noOfTravelers; i++) {
        	// assign every traveler to a staring cell
        	 travelers[i].cellIdX = rand.nextInt(rows);
             travelers[i].cellIdY = rand.nextInt(cols);
             computeCoords(i, travelers, rand);
        }
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
//    	x = rand.nextInt(rows);
//      y = rand.nextInt(cols);
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
    			if (rand.nextDouble() > 0.999)
    				computeGoalCoords(i, travelers, rand);
    		}
    	}
    }
    
    public void coordsNearHost(Host host1, Host host2) {
    	int x = host1.cellIdX, y = host1.cellIdY;
    	host2.goalCurrentX = host1.currentX;
    	// place host2 to the left of host1 or to the right, 0.5m away
    	host2.goalCurrentY = host1.currentY + 0.5;
    	if (host2.currentY > cells[x][y].minY + width)
    		host2.goalCurrentY = host1.currentY + 0.5;
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
    			}
    		}
    	}
    }
    
    public int updateTarget() {
    	int target = 0;
    
    	for (int i = 0; i < noHosts; i++) {
    		if (hosts[i].movementType != -1) {
    			target++;
    			// System.out.println("movType = " + hosts[i].movementType);
    		}
    	}
 
    	System.out.println("target " + target);
    	return target;
    }
    
    public void computeReturnTime(Host host, int destType, double simTime, Random rand) {
    	if (destType == EDGE_CELL)
    		host.returnTime = (long) (simTime + EDGE_MIN
    				+ (EDGE_MAX - EDGE_MIN) * rand.nextDouble());
    	else
    		host.returnTime = (long) (simTime + FRIENDS_MIN 
    				+ (FRIENDS_MAX - FRIENDS_MIN) * rand.nextDouble());
    	System.out.println("ret time " + host.returnTime);
    }
    
	protected void generateContacts() {
		int x, y;
		Random rand = new Random(seed);
		initHosts();
		initEgdeCellCoords();
		generateInteractionMatrix();
		
		if (showRun)
			setupDisplay();
		
		gridSetup();
		placeGroupsOnGrid();
		
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
        	// according to Zurich festival paper, 20% of the nodes
        	// are on the move at any given time
        	int target = (int) (noHosts * 0.05);
        	System.out.println("TARGET = " + target);
        	target -= updateTarget();
        	System.out.println("TARGET UPDATE = " + target);
        	while (target > 0) {
            	// pick a node
        		int id = rand.nextInt(noHosts);
        		// System.out.println("id " + id + " groupid " + hosts[id].groupId 
        		// 		+ " " + eligibleGroup[hosts[id].groupId]);
        		if (eligibleGroup[hosts[id].groupId]) {
        			eligibleGroup[hosts[id].groupId] = false;
        			hosts[id].movementType = MOVE_NODE;
        			x = hosts[id].cellIdX;
        			y = hosts[id].cellIdY;
        			// pick a destination according to Zipf's law
        			int destType = zipfDistribution(rand.nextDouble());
        			// int destType = 0;
        			System.out.println("dest type " + destType);
        			if (destType == EDGE_CELL) {
        				int edgeCell = rand.nextInt(edgeCellX.length);
        				System.out.println("edge cell " + edgeCell);
        				hosts[id].cellIdX = edgeCellX[edgeCell];
        				hosts[id].cellIdY = edgeCellY[edgeCell];
        			} else if (destType == FRIENDS_CELL) {
        				CellsItem goal = computeCAHost(id);
        				hosts[id].cellIdX = goal.x;
        				hosts[id].cellIdY = goal.y;
        				System.out.println("x = " + goal.x + " y = " + goal.y);
        			}
        			// generate coords in that cell
    				computeGoalCoords(id, hosts, rand);
    				System.out.println("X = " + hosts[id].goalCurrentX + " Y = " + hosts[id].goalCurrentY);
        			// compute the time to return to its community
        			computeReturnTime(hosts[id], destType, simTime, rand);
        			cells[x][y].numberOfHosts--;
        			cells[hosts[id].cellIdX][hosts[id].cellIdY].numberOfHosts++;
        			target--;
        		}
        	}
        	// generate contacts
        }
	}
	
    /**
     * Generates a Zipf distribution.
     * @double value random value for the distribution
     * @return Zipf value for the given index
     */
    private static int zipfDistribution(double value) {
        int zipfExponent = 1;
        int zipfSize = 2; // ordinea frecventei conform zifp's law frequency = 1/rank 0,1,2,3
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
