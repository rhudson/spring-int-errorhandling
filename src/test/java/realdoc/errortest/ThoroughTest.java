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
	@Qualifier("gatewayErrorChannelInHeader")
	private IntTestGateway gatewayErrorChannelInHeader;
	
	@Autowired
	@Qualifier("gatewayErrorChannelInHeaderQueued")
	private IntTestGateway gatewayErrorChannelInHeaderQueued;
	
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
				stopWatch.getLastTaskTimeMillis() >= SLOW_TIME_MILLIS);

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

	/**
	 * This method shows that Exceptions will be thrown back to the calling
	 * Gateway method when using direct channels even if the errorChannel
	 * header is set.
	 * 
	 * From:
	 * http://static.springsource.org/spring-integration/reference/htmlsingle/#namespace-errorhandler
	 * 
	 * "The most important thing to understand here is that the messaging-based
	 *  error handling will only apply to Exceptions that are thrown by a Spring
	 *  Integration task that is executing within a TaskExecutor. This does not
	 *  apply to Exceptions thrown by a handler that is operating within the
	 *  same thread as the sender (e.g. through a DirectChannel as described above)."
	 */
	@Test
	public void testErrorChannelInHeader() {
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
			gatewayErrorChannelInHeader.submit(request);
		} catch (Exception e) {
			exception = true;
		}
		stopWatch.stop();

		assertTrue("gatewayErrorChannelInHeader.submit(request) should be throwing an Exception", exception);
		
		assertTrue("gatewayErrorChannelInHeader.submit() method should not be taking so long!",
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

	/**
	 * In this case the messages submitted to the gateway will be put in a queue
	 * to be picked up and processed by another thread.  Since a separate thread
	 * will be handling the message, an Exception will never be thrown to the 
	 * calling gateway.  The Exception in this case will go to the errorChannel
	 * specified in the message header.
	 */
	@Test
	public void testErrorChannelInHeaderQueued() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();

		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");

		// Output queues should be empty
		assertNull(outputQueue.receive(0));
		assertNull(errorQueue.receive(0));
		
		stopWatch.start(SUBMIT_ACTION);
		gatewayErrorChannelInHeaderQueued.submit(request);
		stopWatch.stop();

		assertTrue("gatewayErrorChannelInHeaderQueued.submit() method should not be taking so long!",
				stopWatch.getLastTaskTimeMillis() < MAX_TIME_MILLIS);

		// Output queue should NOT have received a message
		assertNull(outputQueue.receive(0));
		
		Message<Map<String, Object>> message = (Message<Map<String, Object>>) errorQueue.receive(1000);
		assertNotNull(message);
		Map<String, Object> error = message.getPayload();
		
		assertNull(error.get("GOOD"));
		assertNull(error.get("SLOW"));
		assertNull(error.get("QUIET"));
		assertTrue((Boolean)error.get("BAD"));

		// That should be it for the errors
		assertNull(errorQueue.receive(0));

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());

	}

}
