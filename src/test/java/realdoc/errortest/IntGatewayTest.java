package realdoc.errortest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class IntGatewayTest {

	private static final String SUBMIT_ACTION = "submit";
	
	@Autowired
	@Qualifier("testClean")
	private IntTestGateway gateway;
	
	
	@Test
	public void test() {
		Map<String, Object> request = new HashMap<String, Object>();
		StopWatch stopWatch = new StopWatch();
		
		request.put("FirstName", "Robert");
		request.put("LastName", "Hudson");
		
		stopWatch.start(SUBMIT_ACTION);
		gateway.submit(request);
		stopWatch.stop();

		System.out.println("DONE");
		System.out.println(stopWatch.prettyPrint());
		
	}

}
