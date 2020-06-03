package mobemu.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

public class FMM implements Parser {

	private Trace trace;
	private Map<Integer, Context> context;
	private boolean[][] socialNetwork;
	public int[][] commonFriends;
	private long end = Long.MIN_VALUE;
	private long start = Long.MAX_VALUE;
	private boolean staticNodes;
	private int devices;
	
	public FMM() {
		this.trace = new Trace("FMM");
		
		String prefix = "traces" + File.separator + "fmm-festival" + File.separator;
		parseFMMTraceInfo(prefix + "trace-info.txt");
		parseFMMContacts(prefix + "contacts.csv");
		parseFMMChatPairs(prefix + "chat-pairs.csv");
		this.socialNetwork = new boolean[devices][devices];
		this.commonFriends = new int[devices][devices];
		parseFMMSocialNetwork(prefix + "social-network.dat");
		computeCommonFriends();
		
		this.context = new HashMap<>();
		for (int i = 0; i < devices; i++) {
			this.context.put(i, new Context(i));
		}
	}
	
	@Override
	public Trace getTraceData() {
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
	
    public void computeCommonFriends() {
    	for (int i = 0; i < devices; i++) {
    		for (int j = 0; j < devices; j++) {
    			for (int k = 0; k < devices; k++) {
    				if (k != i && k != j) {
    					if (socialNetwork[i][k] && socialNetwork[j][k]) {
    						commonFriends[i][j]++;
    						commonFriends[j][i]++;
    					}
    				}
    			}
    		}
    	}
    }
    
	@Override
	public int[] getCommonFriends(int id) {
		return commonFriends[id];
	}

	public void parseFMMTraceInfo(String fileName) {
		try {		
			BufferedReader rdr = new BufferedReader(new FileReader(fileName));

			this.devices = Integer.parseInt(rdr.readLine());
			System.out.println("CHECK nodes = " + this.devices);
			this.start = Long.parseLong(rdr.readLine());
			trace.setStartTime(this.start);
			System.out.println("CHECK tarce start = " + this.start);
			this.end = Long.parseLong(rdr.readLine());
			trace.setEndTime(this.end);
			trace.setSampleTime(MILLIS_PER_SECOND);
			System.out.println("CHECK trace end = " + this.end);

			rdr.close();
		} catch (IOException | NumberFormatException e) {
			System.err.println("FMM Trace info Parser exception: " + e.getMessage());
		}
	}
	
	public void parseFMMContacts(String fileName) {
		BeanListProcessor<Contact> rowProcessor = new BeanListProcessor<Contact>(Contact.class);
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setInputBufferSize(16 * 1024);
		settings.setProcessor(rowProcessor);
		settings.setHeaderExtractionEnabled(true);

		CsvParser parser = new CsvParser(settings);
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(fileName);
			parser.parse(new InputStreamReader(fstream));
			trace.setContacts(rowProcessor.getBeans());
			trace.setSampleTime(MILLIS_PER_SECOND);
		} catch (FileNotFoundException e) {
			System.err.println("FMM Parser exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("CHECK contacts = " + trace.getContactAt(0).getObserver());
	}
	
	public void parseFMMChatPairs(String fileName) {
		BeanListProcessor<ChatPair> rowProcessor = new BeanListProcessor<ChatPair>(ChatPair.class);
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setInputBufferSize(16 * 1024);
		settings.setProcessor(rowProcessor);
		settings.setHeaderExtractionEnabled(true);
		
		CsvParser parser = new CsvParser(settings);
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(fileName);
			parser.parse(new InputStreamReader(fstream));
			trace.setChatPairs(rowProcessor.getBeans());
			trace.setSampleTime(MILLIS_PER_SECOND);
		} catch (FileNotFoundException e) {
			System.err.println("FMM Parser exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("CHECK chat pair  = " + trace.getChatPairs().get(0).nodeAway);
	}
	
	public void parseFMMSocialNetwork(String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			DataInputStream inStream = new DataInputStream(new BufferedInputStream(fis));
			for (int i = 0; i < devices; i++) {
				for (int j = 0; j < devices; j++) {
					socialNetwork[i][j] = inStream.readBoolean();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		System.out.println("CHECK socialNetwork = " + socialNetwork[0][0]);
	}

}
