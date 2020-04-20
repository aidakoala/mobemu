package mobemu.parsers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.univocity.parsers.csv.*;
import com.univocity.parsers.common.processor.BeanListProcessor;

import mobemu.node.Context;
import mobemu.trace.*;

public class SonarFestival implements Parser {

	private Trace trace;
	private Map<Integer, Context> context;
	private boolean[][] socialNetwork;
	private Calendar calendar;
	private int devices = 13644;
		
	public SonarFestival() {
		this.trace = new Trace("Sonarfestival");
		this.context = new HashMap<>();
		this.socialNetwork = new boolean[devices][devices];
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
		BeanListProcessor<Contact> rowProcessor = new BeanListProcessor<Contact>(Contact.class);
		
		// parse contacts file
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setInputBufferSize(16 * 1024);
		settings.setProcessor(rowProcessor);
		
		CsvParser parser = new CsvParser(settings);
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(contacts);
			long startTime = System.nanoTime();
			parser.parse(new InputStreamReader(fstream));
			trace.setContacts(rowProcessor.getBeans());
			trace.setSampleTime(MILLIS_PER_SECOND);
			long elapsedTime = System.nanoTime() - startTime;
			System.out.println("read all records " + elapsedTime / 1000000000 + "s " + trace.contacts.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
