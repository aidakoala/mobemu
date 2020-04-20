package mobemu;

import java.util.List;

import mobemu.parsers.SonarFestival;
import mobemu.trace.Contact;
import static mobemu.trace.Parser.MILLIS_PER_SECOND;

public class SonarFestivalTask implements Runnable {
	SonarFestival sonarFestivalData;
	int taskId;
	int noThreads;
	TraceTime traceTime;
	
	public SonarFestivalTask(SonarFestival sonarFestivalData, TraceTime traceTime, int taskId, int noThreads) {
		this.sonarFestivalData = sonarFestivalData;
		this.taskId = taskId;
		this.noThreads = noThreads;
		this.traceTime = traceTime;
	}

	@Override
	public void run() {
		List<Contact> contacts = sonarFestivalData.getTraceData().getContacts();
		int size = contacts.size();
		int startIndex = taskId * size / noThreads;
		int endIndex = (taskId + 1) * size / noThreads;
		long contactStart, contactEnd;
		
		System.out.println("Hello thread " + taskId + " size = " + size + " startIndex = " + startIndex + " endIndex " + endIndex);
		for (int i = startIndex; i < endIndex; i++) {
			Contact contact = contacts.get(i);
			// the time is already in unix time
			// System.out.println("contactStart = " + contact.getStart() + " contactEnd = " + contact.getEnd());
			contactStart = contact.getStart() * MILLIS_PER_SECOND;
			contactEnd = contact.getEnd() * MILLIS_PER_SECOND;
			// System.out.println("contactStart = " + contactStart+ " contactEnd = " + contactEnd);
			contactStart -= contactStart % MILLIS_PER_SECOND;
			contactEnd -= contactEnd % MILLIS_PER_SECOND;
			// System.out.println("contactStart = " + contactStart + " contactEnd = " + contactEnd);
			contact.setStart(contactStart);
			contact.setEnd(contactEnd);
			
			// compute trace start time this thread
			if (traceTime.start > contactStart) {
				traceTime.start = contactStart;
			}
			
			// compute trace end time per this thread
			if (traceTime.end < contactEnd) {
				traceTime.end = contactEnd;
			}
		}
		System.out.println("Thread " + taskId + " traceTime start = " + traceTime.start + " traceTime end " + traceTime.end);
	}	

}
