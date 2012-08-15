package realdoc.errortest;

import java.util.Map;

public class ServiceActivator {

	private static final long SLEEP_TIME_MILLIS = 3000;


	public Map<String, Object> good(Map<String, Object> request) {
		System.out.println("Good message: " + request.get("LastName") + "," + request.get("FirstName"));
		request.put("GOOD", Boolean.TRUE);
		return request;
	}
	
	public Map<String, Object> bad(Map<String, Object> request) throws Exception {
		String message = "Bad message: *** " + request.get("LastName") + " ***";
		request.put("BAD", Boolean.TRUE);
		throw new Exception(message);
	}
	
	public Map<String, Object> slow(Map<String, Object> request) throws InterruptedException {
		System.out.println("Slowly handling message...");
		Thread.sleep(SLEEP_TIME_MILLIS);
		System.out.println("...and DONE.");
		request.put("SLOW", Boolean.TRUE);
		return request;
	}
	
	
	public void quiet(Map<String, Object> request) {
		System.out.println("Quietly handling message: " + request.get("LastName") + "," + request.get("FirstName"));
		request.put("QUIET", Boolean.TRUE);
	}
	
}
