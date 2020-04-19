package mobemu.parsers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import mobemu.node.Context;
import mobemu.trace.*;

public class SonarFestival implements Parser {

	private Trace trace;
	private Map<Integer, Context> context;
	private boolean[][] socialNetwork;
	private Calendar calendar;
	private int devices = 13645;
	
	public SonarFestival() {
		this.trace = new Trace("Sonarfestival");
		this.context = new HashMap<>();
		this.socialNetwork = null;
		this.calendar = Calendar.getInstance();
		calendar.set(2015, 06, 18, 12, 0, 0); // start of the trace 06/18/2015 12:00
		
		String prefix = "traces" + File.separator + "sonor-festival-data" + File.separator;
		parseSonarFestival(prefix + "contacts-parsed-sonar-data.csv");
	}
	
	@Override
	public Trace getTraceData() {
		// TODO Auto-generated method stub
		return trace;
	}

	@Override
	public Map<Integer, Context> getContextData() {
			return context;
	}

	@Override
	public boolean[][] getSocialNetwork() {
		return socialNetwork;
	}

	@Override
	public int getNodesNumber() {
		return devices;
	}

	@Override
	public int getStaticNodesNumber() {
		return 0;
	}
	
	private void parseSonarFestival(String contacts) {
		// determine the end and the start of the trace
		long end = Long.MIN_VALUE;
		long start = Long.MAX_VALUE;
		
		// parse contacts file
		try {
			String line;
			FileInputStream fstream = new FileInputStream(contacts);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				int i = 0;
				while ((line = br.readLine()) != null) {
					i++;

					String[] tokens;
					String delimiter = ",";

					tokens = line.split(delimiter);

					int observerID = Integer.parseInt(tokens[0]);
					int observedID = Integer.parseInt(tokens[1]);

					// the time is already in unix time
					long contactStart = Long.parseLong(tokens[2]) * MILLIS_PER_SECOND;
					long contactEnd = Long.parseLong(tokens[3]) * MILLIS_PER_SECOND;
					contactStart -= contactStart % MILLIS_PER_SECOND;
					contactEnd -= contactEnd % MILLIS_PER_SECOND;

					// compute trace finish time.
					if (contactEnd > end) {
						end = contactEnd;
					}

					// compute trace start time.
					if (contactStart < start) {
						start = contactStart;
					}

					System.out.println(i + " " + observerID + " " + observedID + " " + contactStart + " " + contactEnd);
					trace.addContact(new Contact(observerID, observedID, contactStart, contactEnd));
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("SonarFestival Parser exception: " + e.getMessage());
		}
		
		trace.setStartTime(start);
		trace.setEndTime(end);
		trace.setSampleTime(MILLIS_PER_SECOND);
	}

}
