package mobemu.parsers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

public class FestivalMobility extends mobemu.utils.FestivalMobility implements Parser {
	public Trace trace;
	private long end = Long.MIN_VALUE;
	private long start = Long.MAX_VALUE;
	private boolean[][] socialNetwork;
	private List<Contact> contactsInProgress;
	
	public FestivalMobility(long simulationTime, float minHostSpeed, float maxHostSpeed, float bluetoothRadius,
			float wifiDirectRadius, double connectionTreshold, float gridHeight, int rows, float gridWidth, int columns, 
			float travelSpeed, int groupSize, double rewiringProb, double remainingProb, boolean showRun, long sleepTime,
			int seed) {

		this.trace = new Trace("FestivalMobility");
		this.contactsInProgress = new ArrayList<>();
		this.noHosts = computeNoHostFestival(gridHeight, gridWidth, rows, columns);
		
		/* setup simulation parameters */
		// 5% of the hosts number will correspond to traveler hosts
		this.noOfTravelers = (int) (0.03 * noHosts);
		this.socialNetwork = new boolean[noHosts][noHosts];
		this.groupSize = groupSize;
		this.noOfGroups = this.noHosts / groupSize;
		this.height = gridHeight;
		this.width = gridWidth;
		this.rows = rows;
		this.cols = columns;
		this.heightCell = height / rows; // x
		this.widthCell = width / columns; // y
		/*
		 *  due to a phenomenon called "The effect of dense crowds" the connectivity range for 
		 *  Bluetooth and WiFi Direct will be set lower than in optimal conditions when there is a LOS
		 */
		this.bluetoothRadius = 5.0f;
		this.wifiDirectRadius = 30.0f;
		
		this.totalSimulationTime = simulationTime;
		
		this.minHostSpeed = minHostSpeed;
		this.maxHostSpeed = maxHostSpeed;
		this.travelerSpeed = travelSpeed;
		
		this.showRun = showRun;
		this.sleepTime = sleepTime;
		
		this.seed = seed;
	
		runSimulation();
		
		addContactsInProgress(simulationTime);
		
		trace.setStartTime(start == Long.MAX_VALUE ? 0 : start);
		trace.setEndTime(end == Long.MIN_VALUE ? simulationTime * MILLIS_PER_SECOND : end);
		trace.setSampleTime(MILLIS_PER_SECOND);
	}
	
	/* 
	 * sizeX * sizeY have to be a multiple of the groupSize so all the people 
	 * will fit the in cells when placing the communities in the grid
	 */
	public int computeNoHostFestival(float gridHeight, float gridWidth, int rows, int columns) {
		int hosts = 0;
		
		int sizeX = (int) (gridWidth / columns);
		int sizeY = (int) (gridHeight / rows);
		
		int p1 = (int) (rows / 6);
		int p2 = (int) ((rows - p1) * 2 / 3);
		int p3 = (int) (rows - p1 - p2);
		System.out.println("p1 = " + p1 + " p2 = " + p2 + " p3 = " + p3);
		hosts = (int) ((p1 * maxDensity + p2 * midDensity + p3 * minDensity) * sizeX * sizeY * columns);

		return hosts;
	}
	
	@Override
	protected void startContact(int nodeA, int nodeB, double tick) {
		tick *= MILLIS_PER_SECOND;

		contactsInProgress.add(new Contact(nodeA, nodeB, (long) tick, (long) tick));

		if ((long) tick < start) {
			start = (long) tick;
		}
	}
	
	@Override
	protected void endContact(int nodeA, int nodeB, double tick) {
		tick *= MILLIS_PER_SECOND;
		Contact contact = null;

		for (Contact currentContact : contactsInProgress) {
			if (currentContact.getObserver() == nodeA && currentContact.getObserved() == nodeB) {
				contact = currentContact;
				break;
			}
		}

		if (contact == null) {
			return;
		}

		contactsInProgress.remove(contact);
		contact.setEnd((long) tick);

		if ((long) tick > end) {
			end = (long) tick;
		}

		trace.addContact(contact);
	}
	
	/*
	 * when the simulation ends, add the contacts which were 
	 * in progress to the contact list of the trace
	 */
	private void addContactsInProgress(long simulationTime) {
		for (Contact contact : contactsInProgress) {
			long endTime = simulationTime * MILLIS_PER_SECOND;

			if (endTime > end) {
				end = endTime;
			}

			contact.setEnd(endTime);
			trace.addContact(contact);
		}
	}
	
	@Override
	public Trace getTraceData() {
		trace.sort();
		return trace;
	}
	@Override
	public boolean[][] getSocialNetwork() {
		return socialNetwork;
	}
	@Override
	public int getNodesNumber() {
		return noHosts;
	}
	@Override
	public int getStaticNodesNumber() {
		return 0;
	}
	
	@Override
	public Map<Integer, Context> getContextData() {
		return null;
	}

	@Override
	protected void generateInteractionMatrix() {
		// parse contacts file
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setInputBufferSize(1024);
		List<String[]> allRows;
		int[] friendsNo = new int[0];
		int vecSize = 0;
		
		CsvParser parser = new CsvParser(settings);
		FileInputStream fstream;
		try {
			fstream = new FileInputStream("src/mobemu/parsers/festival-friends.csv");
			allRows = parser.parseAll(new InputStreamReader(fstream));
			// create a vector of number of friends extracted from the google form
			vecSize = allRows.size();
			friendsNo = new int[vecSize];
			for (int i = 1; i < vecSize; i++) {
				friendsNo[i] = Integer.parseInt(allRows.get(i)[2]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// create an interaction matrix using the social info from the google form
		// establish groups of 5 people, then generate a random social degree of
		// interaction with the rest of the nodes
		this.interMat = new float[noHosts][noHosts];
		for (int i = 0; i < noHosts; i++) {
			Arrays.fill(interMat[i], 0.0f);
		}
		
		// a seed is needed for test purposes
		Random random = new Random(seed);
		int groupIndex = 0;
		for (int i = 0; i < noHosts; i++) {
			// if the number is a multiple of 5, the next 4 nodes will be its friends
			if (i % 5 == 0) {
				for (int j = 0; j < groupSize; j++) {
					interMat[i][i + j] = 1.0f;
					interMat[i + 1][i + j] = 1.0f;
					interMat[i + 2][i + j] = 1.0f;
					interMat[i + 3][i + j] = 1.0f;
					interMat[i + 4][i + j] = 1.0f;
				}
				// create the groups of friends
				groups[groupIndex][0] = i;
				hosts[i].groupId = groupIndex;
				groups[groupIndex][1] = i + 1;
				hosts[i + 1].groupId = groupIndex;
				groups[groupIndex][2] = i + 2;
				hosts[i + 2].groupId = groupIndex;
				groups[groupIndex][3] = i + 3;
				hosts[i + 3].groupId = groupIndex;
				groups[groupIndex][4] = i + 4;
				hosts[i + 4].groupId = groupIndex;
				groupIndex++;
			}
			
			// chose a number of social peers beside its group
			int noFriends = friendsNo[i % vecSize];

			while (noFriends > 0) {
				// pick a random host so there will not be isolated communities
				int hostId = random.nextInt(noHosts);
				if (interMat[i][hostId] != 0.0f)
					continue;
				// generate a random social degree of interaction
				interMat[i][hostId] = random.nextFloat();
				// although it is not necessarily true, the social
				// relationships will be assumed to be symmetric
				interMat[hostId][i] = interMat[i][hostId];
				noFriends--;
			}
		}
	}

}
