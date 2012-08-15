package realdoc.errortest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ThoroughTest {

	private static final String SUBMIT_ACTION = "submit";
	private static final long MAX_TIME_MILLIS = 100;
	private static final long SLOW_TIME_MILLIS = 3000;

	@Autowired
	@Qualifier("gatewayGoodBad")
	private IntTestGateway gatewayGoodBad;

	@Autowired
	@Qualifier("gatewaySlow")
	private IntTestGateway gatewaySlow;
	
	@Autowired
	@Qualifier("gatewayQuiet")
	private IntTestGateway gatewayQuiet;
	
	@Autowired
	@Qualifier("gatewayBombs")
	private IntTestGateway gatewayBombs;
	
	@Autowired
	@Qualifier("outputQueue")
	private PollableChannel outputQueue;

	@Autowired
	@Qualifier("errorQueue")
	private PollableChannel errorQueue;

	@Test
	public void testGoodBad() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();

		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");

		// Output queues should be empty
		assertNull(outputQueue.receive(0));
		assertNull(errorQueue.receive(0));
		
		stopWatch.start(SUBMIT_ACTION);
		gatewayGoodBad.submit(request);
		stopWatch.stop();

		assertTrue("gatewayGoodBad.submit() method should not be taking so long!",
				stopWatch.getLastTaskTimeMillis() < MAX_TIME_MILLIS);

		// Output queue should NOT have received a message
		assertNull(outputQueue.receive(0));
		
		Message<Map<String, Object>> message = (Message<Map<String, Object>>) errorQueue.receive(0);
		assertNotNull(message);
		Map<String, Object> error = message.getPayload();
		
		assertTrue((Boolean)error.get("GOOD"));
		assertTrue((Boolean)error.get("BAD"));

		// That should be it for the errors
		assertNull(errorQueue.receive(0));

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());

	}

	@Test
	public void testSlow() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();

		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");

		// Output queues should be empty
		assertNull(outputQueue.receive(0));
		assertNull(errorQueue.receive(0));
		
		stopWatch.start(SUBMIT_ACTION);
		gatewaySlow.submit(request);
		stopWatch.stop();

		assertTrue("gatewaySlow.submit() method should take at least 3 seconds.",
				stopWatch.getLastTaskTimeMillis() > SLOW_TIME_MILLIS);

		// Error queue should NOT have received a message
		assertNull(errorQueue.receive(0));
		
		Message<Map<String, Object>> message = (Message<Map<String, Object>>) outputQueue.receive(0);
		assertNotNull(message);
		Map<String, Object> error = message.getPayload();
		
		assertNull(error.get("GOOD"));
		assertNull(error.get("BAD"));
		assertTrue((Boolean)error.get("SLOW"));

		// That should be it for the output messages
		assertNull(outputQueue.receive(0));

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());

	}

	@Test
	public void testQuiet() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();

		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");

		// Output queues should be empty
		assertNull(outputQueue.receive(0));
		assertNull(errorQueue.receive(0));
		
		stopWatch.start(SUBMIT_ACTION);
		gatewayQuiet.submit(request);
		stopWatch.stop();

		assertTrue("gatewayQuiet.submit() method should not be taking so long!",
				stopWatch.getLastTaskTimeMillis() < MAX_TIME_MILLIS);

		// Since the quiet() method has no return value then nothing will show up on the output queue
		assertNull(outputQueue.receive(0));

		// Error queue should NOT have received a message
		assertNull(errorQueue.receive(0));
		
		assertNull(request.get("GOOD"));
		assertNull(request.get("BAD"));
		assertNull(request.get("SLOW"));
		assertTrue((Boolean)request.get("QUIET"));

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());

	}

	@Test
	public void testBombs() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();

		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");

		// Output queues should be empty
		assertNull(outputQueue.receive(0));
		assertNull(errorQueue.receive(0));
		
		boolean exception = false;
		stopWatch.start(SUBMIT_ACTION);
		try {
			gatewayBombs.submit(request);
		} catch (Exception e) {
			exception = true;
		}
		stopWatch.stop();

		assertTrue("gatewayBombs.submit(request) should be throwing an Exception", exception);
		
		assertTrue("gatewayBombs.submit() method should not be taking so long!",
				stopWatch.getLastTaskTimeMillis() < MAX_TIME_MILLIS);

		// Since the quiet() method has no return value then nothing will show up on the output queue
		assertNull(outputQueue.receive(0));

		// Error queue should NOT have received a message
		assertNull(errorQueue.receive(0));
		
		assertNull(request.get("GOOD"));
		assertNull(request.get("SLOW"));
		assertNull(request.get("QUIET"));
		assertTrue((Boolean)request.get("BAD"));

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());

	}

}
