package mobemu.parsers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.univocity.parsers.common.processor.BeanWriterProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

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
	private Map<Integer, Context> context;
	
	public FestivalMobility(long simulationTime, float minHostSpeed, float maxHostSpeed, float bluetoothRadius,
			float wifiDirectRadius, double connectionTreshold, float gridHeight, int rows, float gridWidth, int columns, 
			float travelSpeed, int groupSize, double rewiringProb, double remainingProb, boolean showRun, long sleepTime,
			int seed, String fileName) {
		String chatPairsFile = "traces/fmm-festival/chat-pairs.csv";
		String socialNetworkFile = "traces/fmm-festival/social-network.dat";
		String traceInfoFile = "traces/fmm-festival/trace-info.txt";

		this.trace = new Trace("FestivalMobility");
		this.contactsInProgress = new ArrayList<>();
		this.noHosts = computeNoHostFestival(gridHeight, gridWidth, rows, columns);
		
		this.context = new HashMap<>();
		for (int i = 0; i < noHosts; i++) {
			this.context.put(i, new Context(i));
		}
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
	
		// create a file where to write the contacts for later use
		FileOutputStream fos = null;
		DataOutputStream outStream = null;
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setSkipEmptyLines(true);
		settings.setRowWriterProcessor(new BeanWriterProcessor<Contact>(Contact.class));
		CsvWriter csvWriter = null;
		try {
			 fos = new FileOutputStream(fileName);
			 outStream = new DataOutputStream(new BufferedOutputStream(fos));
			 csvWriter = new CsvWriter(outStream, settings);
			 csvWriter.writeHeaders("id1", "id2", "tstart","tend");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		writeTraceInfo(traceInfoFile, simulationTime);
		// writeSocialNetwork(socialNetworkFile);
		
		runSimulation(csvWriter);

		// writeChatPairs(chatPairsFile);

		addContactsInProgress(simulationTime, csvWriter);
		
		trace.setStartTime(start == Long.MAX_VALUE ? 0 : start);
		trace.setEndTime(end == Long.MIN_VALUE ? simulationTime * MILLIS_PER_SECOND : end);
		trace.setSampleTime(MILLIS_PER_SECOND);
		
		csvWriter.flush();
		csvWriter.close();
		try {
			outStream.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeTraceInfo(String fileName, long traceEnd) {
		try {
			 FileOutputStream fos = new FileOutputStream(fileName);
			 DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
			 // write node number
			 outStream.writeInt(this.noHosts);
			 outStream.writeChar('\n');
			 // write trace start
			 outStream.writeLong(this.start);
			 outStream.writeChar('\n');
			 // write trace end
			 outStream.writeLong(traceEnd);
			 outStream.close();
			 fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeSocialNetwork(String fileName) {
		try {
			 FileOutputStream fos = new FileOutputStream(fileName);
			 DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
			 for (int i = 0; i < socialNetwork.length; i++) {
				 for (int j = 0; j < socialNetwork.length; j++) {
					 outStream.writeBoolean(socialNetwork[i][j]);
					 if (j != socialNetwork.length - 1)
						 outStream.writeChar(',');
				 }
				 outStream.writeChar('\n');
			 }
			 outStream.flush();
			 outStream.close();
			 fos.close();
			 System.out.println("WRITE SOCIAL NETWORK DONE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	
	public void writeChatPairs(String fileName) {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setSkipEmptyLines(true);
		settings.setRowWriterProcessor(new BeanWriterProcessor<ChatPair>(ChatPair.class));
		try {
			 FileOutputStream fos = new FileOutputStream(fileName);
			 DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
			 CsvWriter csvWriter = new CsvWriter(outStream, settings);
			 csvWriter.writeHeaders("id1", "id2", "tstart","tend");
				
		    for (Integer key : chatPairs.keySet()) {
		         LinkedList<ChatPair> groupPairs = chatPairs.get(key);
		         csvWriter.processRecords(groupPairs);
		    }
		    csvWriter.flush();
		    csvWriter.close();
			outStream.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	protected void endContact(int nodeA, int nodeB, double tick, CsvWriter csvWriter) {
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

		csvWriter.processRecord(contact);
		trace.addContact(contact);
	}
	
	/*
	 * when the simulation ends, add the contacts which were 
	 * in progress to the contact list of the trace
	 */
	private void addContactsInProgress(long simulationTime, CsvWriter csvWriter) {
		for (Contact contact : contactsInProgress) {
			long endTime = simulationTime * MILLIS_PER_SECOND;

			if (endTime > end) {
				end = endTime;
			}

			contact.setEnd(endTime);
			csvWriter.processRecord(contact);
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
		// maybe also consider the traveler nodes
		return noHosts;
	}
	@Override
	public int getStaticNodesNumber() {
		return 0;
	}
	
	@Override
	public Map<Integer, Context> getContextData() {
		return context;
	}
	
	public int[][] getGroups() {
		return groups;
	}
	
	public int getGroupSize() {
		return groupSize;
	}
	
	public HashMap<Integer, LinkedList<ChatPair>> getChatPairs() {
		return chatPairs;
	}

	public Host[] getHosts() {
		return hosts;
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
				// also complete social network information
				socialNetwork[i][hostId] = true;
				socialNetwork[hostId][i] = true;
				noFriends--;
			}
		}
		
		writeSocialNetwork("traces/fmm-festival/social-network.dat");
	}

}
