package mobemu.utils;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
    float[][] CA; // cell attractivity
    
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
    		hosts[i].speed = maxHostSpeed;
    	}
    	numberOfMembers = new int[groupSize];
    	
    	travelers = new Host[noOfTravelers];
    	for (int i = 0; i < noOfTravelers; i++) {
    		travelers[i] = new Host(BLUETOOTH);
    		// maybe generate a random speed
    		travelers[i].speed = travelerSpeed;
    	}
    	
    	groups = new int[noOfGroups][groupSize];

		isConnected = new boolean[noHosts][noHosts];
        // setup of the links
        for (int i = 0; i < noHosts; i++) {
            for (int j = 0; j < noHosts; j++) {
                isConnected[i][j] = false;
                // lastValues[i][j] = 0.0;
            }
        }
    }
    
    public void gridSetup() {
    	cells = new CellsItem[rows][cols];
    	
    	for (int i = 0; i < rows; i++) {
    		for (int j = 0; j < cols; j++) {
    			cells[i][j] = new CellsItem();
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
    
    public void computeCoords(int hostId, boolean firstTime, Host[] hostVec, Random rand) {
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
    		
//       double unif2 = rand.nextDouble();
//       hostVec[hostId].currentX = unif2 * hostVec[hostId].currentX + (1 - unif2) * hostVec[hostId].goalCurrentX;
//       hostVec[hostId].currentY = unif2 * hostVec[hostId].currentY + (1 - unif2) * hostVec[hostId].goalCurrentY;

       hostVec[hostId].previousGoalX = hostVec[hostId].currentX;
       hostVec[hostId].previousGoalY = hostVec[hostId].currentY;
    }
    
    public void computeCoordsHost(int hostId, double minX, double minY,
    		float heightCell, float widthCell, Random rand) {
    	double r;

    	do {
    		hosts[hostId].currentX = minX + rand.nextDouble() * heightCell;
    		hosts[hostId].currentY = minY + rand.nextDouble() * widthCell;
    		
    		hosts[hostId].goalCurrentX = minX + rand.nextDouble() * heightCell;
    		hosts[hostId].goalCurrentY = minY + rand.nextDouble() * widthCell;
    		
    		r = Math.pow(Math.pow(hosts[hostId].goalCurrentX - hosts[hostId].currentX, 2)
                    + Math.pow(hosts[hostId].goalCurrentY - hosts[hostId].currentY, 2), 1 / 2) / Math.sqrt(2);
    	} while (rand.nextDouble() >= r);
    	
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
//        			if (rand.nextFloat() > 0.5) {
	        			groupsCell /= 2;
	            		width = widthCell / groupsCell;            		
	            		for (int k = 0; k < cells[i][j].groupIds.size(); k++) {
	            			groupId = cells[i][j].groupIds.get(k);
	        				if (done == 2) {
	        					minY = minY + width;
	        					done = 0;
	        				}
	            			for (int l = 0; l < groupSize; l++) {
	            				computeCoordsHost(groups[groupId][l], minX, minY, heightCell, width, rand);
	            			}
	        				done++;
	            		}
//        			} else {
//        				height = heightCell / 2;
//        				limit = cells[i][j].groupIds.size() / 2;
//        				for (int k = 0; k < cells[i][j].groupIds.size(); k++) {
//	            			groupId = cells[i][j].groupIds.get(k);
//	        				if (done == limit) {
//	        					minX = minX + height;
//	        					done = 0;
//	        				}
//	            			for (int l = 0; l < groupSize; l++) {
//	            				computeCoordsHost(groups[groupId][l], minX, minY, height, widthCell, rand);
//	            			}
//	        				done++;
//	            		}
//        			}
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
        	System.out.println("group id " + i);
        	 do {
                 cellIdX = rand.nextInt(rows);
                 cellIdY = rand.nextInt(cols);
//
//                 System.out.println("x = " + cellIdX + " y = " + cellIdY + 
//                 		" hostCellMem = " +  hostsCells[cellIdX][cellIdY]);
             } while (hostsCells[cellIdX][cellIdY] - groupSize < 0);

        	 cells[cellIdX][cellIdY].groupIds.add(i);
        	 hostsCells[cellIdX][cellIdY] -= groupSize;
        	 
//        	 for (int l = 0; l < rows; l++) {
//        		 for (int c = 0; c < cols; c++) {
//        			 System.out.print(hostsCells[l][c] + " ");
//        		 }
//        		 System.out.println();
//        	 }
        	 
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
             computeCoords(i, firstTime, travelers, rand);
        }
    }
    
	protected void generateContacts() {
		initHosts();
		generateInteractionMatrix();
		
		if (showRun) {
			setupDisplay();
		}
		
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
        	
        	
        	
        }
		
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
		int frameWidth = 700;
		int frameHeight = 700;
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
