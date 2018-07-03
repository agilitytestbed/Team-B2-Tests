package nl.utwente.ing.testsuite;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import nl.utwente.ing.model.CandleStick;
import nl.utwente.ing.model.SavingGoal;
import nl.utwente.ing.model.Transaction;

public class SavingGoalsTest {
	private static String sessionID;
	private static final int INITIAL_SAVING_GOALS = 10;
	private static String lastSavingGoalID;
	private static final double EPSILON = 0.005;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		sessionID = getNewSession();
		
		JSONObject dummySavingGoal = new JSONObject()
				.put("name", "Dummy Saving Goal")
				.put("goal", 1000.0)
				.put("savePerMonth", 1.0)
				.put("minBalanceRequired", 500.0);
		
		// Add some initial saving goals to work with
		for (int i = 0; i < INITIAL_SAVING_GOALS; i++) {
			postObject(dummySavingGoal, "savingGoals", sessionID);
		}
	}
	
	private static String getNewSession() {
		return given().
		        contentType("application/json").
		when().
		        post("/sessions").
		then().
				contentType(ContentType.JSON).
		extract().
				response().jsonPath().getString("id");
	}
	
	private static void postObject(JSONObject object, String uri, String session) {
		given().
			header("X-session-ID", session).
			header("Content-Type", "application/JSON").
			body(object.toString()).
		when().
			post("/" + uri).
		then().
			assertThat().statusCode(201);
	}
	
	private static double getSavingGoalBalance(String session, int index) {
		JsonPath savingGoalJson = 
				given().
						header("X-session-ID", session).
						header("Content-Type", "application/JSON").
				when().
				        get("/savingGoals").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the saving goal balance
		return Double.valueOf(savingGoalJson.getList("balance").get(index).toString());
	}
	
	/**
	 * Tests a saving goal scenario.
	 * @param initialBalance amount of money that should be present at the start of the scenario
	 * @param minBalance minimum balance required for a saving goal to put money aside
	 * @param savePerMonth amount of money subtracted every month for the saving goal
	 * @param goal the goal that should be achieved by the savingGoal
	 * @param desiredIncrease how much the savingGoal's balance should increase
	 * @param sessionId the id of the session the scenario is operating in
	 * @param index the index of the saving goal in the list that is returned on a GET request
	 */
	public static void testScenario(double initialBalance, double minBalance, double savePerMonth, double goal, double desiredIncrease, String sessionId, int index) {
		JSONObject initialTransaction = new JSONObject()
				.put("date", "2018-02-01T00:00Z")
				.put("amount",initialBalance)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		postObject(initialTransaction, "transactions", sessionId);
		
		JSONObject savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", goal)
				.put("savePerMonth", savePerMonth)
				.put("minBalanceRequired", minBalance);
		
		postObject(savingGoal, "savingGoals", sessionId);
		
		JSONObject secondTransaction = new JSONObject()
				.put("date", "2018-03-01T00:00Z")
				.put("type", "withdrawal")
				.put("amount", initialBalance)
				.put("externalIBAN", "testIBAN");
		
		postObject(secondTransaction, "transactions", sessionId);
		
		assertEquals(getSavingGoalBalance(sessionId, index), desiredIncrease, EPSILON);
	}
	
	private static void checkCandleStick(CandleStick c, double open, double volume, double close, double high, double low) {
		assertEquals(c.getOpen(), open, EPSILON);
		assertEquals(c.getVolume(), volume, EPSILON);
		assertEquals(c.getClose(), close, EPSILON);
		assertEquals(c.getHigh(), high, EPSILON);
		assertEquals(c.getLow(), low, EPSILON);
	}
	
	@Test
	public void testIndividualSavingGoals() {
		// For simplicity each scenario tested here will happen in a separate session
		
		String testSessionId;
		
		/* -------------- Scenario 1 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 500
		 *	Goal = 500
		 *	Desired increase = +500
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 500, 500, 500, testSessionId, 0);

		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 500, 500, 0, testSessionId, 0);
		
		/* -------------- Scenario 2 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 500
		 *	Goal = 400
		 *	Desired increase = +400
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 500, 400, 400, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 500, 500, 0, testSessionId, 0);
		
		/* -------------- Scenario 3 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 500
		 *	Goal = 600
		 *	Desired increase = +500
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 500, 600, 500, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 500, 600, 0, testSessionId, 0);
		
		/* -------------- Scenario 4 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 1500
		 *	Goal = 1000
		 *	Desired increase = +1000
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 1500, 1000, 1000, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 1500, 1000, 0, testSessionId, 0);
		
		/* -------------- Scenario 5 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 1500
		 *	Goal = 1200
		 *	Desired increase = +0
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 1500, 1200, 0, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 1500, 1200, 0, testSessionId, 0);
		
		/* -------------- Scenario 6 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 1500
		 *	Goal = 1500
		 *	Desired increase = +0
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 1500, 1500, 0, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 1500, 1500, 0, testSessionId, 0);
		
		/* -------------- Scenario 7 --------------
		 *  ( Part A )
		 *  Initial Balance = 1000
		 *	MinBalance = 1000
		 *	SavePerMonth = 1500
		 *	Goal = 1600
		 *	Desired increase = +0
		 *	
		 *	( Part B )
		 *	if MinBalance = 1500 and the rest is the same
		 *	Desired increase = +0
		*/
		testSessionId = getNewSession();
		// ( Part A )
		testScenario(1000, 1000, 1500, 1600, 0, testSessionId, 0);
		
		testSessionId = getNewSession();
		// ( Part B )
		testScenario(1000, 1500, 1500, 1600, 0, testSessionId, 0);
		
	}
	
	@Test
	public void testMultipleSavingGoals() {
		String testSessionId;
		JSONObject savingGoal;
		
		/* -------------- Scenario 1 --------------
		 *  Description:
		 *  One will be posted first and will make
		 *  the other not execute because of MinBalance constraint.
		 * 
		 *  Initial Balance = 1000
		 *  
		 *  ( SavingGoal 1 ) 
		 *	MinBalance = 1000
		 *	SavePerMonth = 400
		 *	Goal = 400
		 *	
		 *	( SavingGoal 2 ) 
		 *	MinBalance = 1000
		 *	SavePerMonth = 600
		 *	Goal = 600
		 *
		 *  ( Part A )
		 *  Description:
		 *  SavingGoal 1 is posted first
		 *  SG 1 : Desired increase = +400
		 *  SG 2 : Desired increase = +0
		 *  
		 *  ( Part B )
		 *  Description:
		 *  SavingGoal 2 is posted first
		 *  SG 1 : Desired increase = +600
		 *  SG 2 : Desired increase = +0
		*/
		
		// ( Part A )
		testSessionId = getNewSession();
		
		savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 400)
				.put("savePerMonth", 400)
				.put("minBalanceRequired", 1000);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		// Check that the second savingGoal has the desired increase
		testScenario(1000, 1000, 600, 600, 0, testSessionId, 1);
		
		// Check that the first savingGoal has the desired increase
		assertEquals(getSavingGoalBalance(testSessionId, 0), 400, EPSILON);
		
		// ( Part B )
		testSessionId = getNewSession();
		
		savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 600)
				.put("savePerMonth", 600)
				.put("minBalanceRequired", 1000);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		// Check that the second savingGoal has the desired increase
		testScenario(1000, 1000, 400, 400, 0, testSessionId, 1);
		
		// Check that the first savingGoal has the desired increase
		assertEquals(getSavingGoalBalance(testSessionId, 0), 600, EPSILON);
		
		/* -------------- Scenario 2 --------------
		 *  Description:
		 *  In the first part one will be posted first and will make
		 *  the other not execute because the balance will be insufficient.
		 *  In the second part the second saving goal will execute.
		 * 
		 *  Initial Balance = 1000
		 *  
		 *  ( SavingGoal 1 ) 
		 *	MinBalance = 0
		 *	SavePerMonth = 400
		 *	Goal = 400
		 *	
		 *	( SavingGoal 2 ) 
		 *	MinBalance = 0
		 *	SavePerMonth = 600
		 *	Goal = 600
		 *
		 *  ( Part A )
		 *  Description:
		 *  SavingGoal 1 is posted first
		 *  SG 1 : Desired increase = +400
		 *  SG 2 : Desired increase = +600
		 *  
		 *  ( Part B )
		 *  Description:
		 *  SavingGoal 2 is posted first
		 *  SG 1 : Desired increase = +600
		 *  SG 2 : Desired increase = +0
		*/
		
		// ( Part A )
		testSessionId = getNewSession();
		
		savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 400)
				.put("savePerMonth", 400)
				.put("minBalanceRequired", 0);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		// Check that the second savingGoal has the desired increase
		testScenario(1000, 0, 600, 600, 600, testSessionId, 1);
		
		// Check that the first savingGoal has the desired increase
		assertEquals(getSavingGoalBalance(testSessionId, 0), 400, EPSILON);
		
		// ( Part B )
		testSessionId = getNewSession();
		
		savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 600)
				.put("savePerMonth", 600)
				.put("minBalanceRequired", 1000);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		// Check that the second savingGoal has the desired increase
		testScenario(1000, 1000, 400, 400, 0, testSessionId, 1);
		
		// Check that the first savingGoal has the desired increase
		assertEquals(getSavingGoalBalance(testSessionId, 0), 600, EPSILON);
		
	}
	
	@Test
	public void testMultipleMonths() {
		
		String testSessionId = getNewSession();
		
		JSONObject initialTransaction = new JSONObject()
				.put("date", "2018-02-01T00:00Z")
				.put("amount",1200.0)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		postObject(initialTransaction, "transactions", testSessionId);
		
		JSONObject savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 1000)
				.put("savePerMonth", 400)
				.put("minBalanceRequired", 400);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		JSONObject secondTransaction = new JSONObject()
				.put("date", "2018-03-01T00:00Z")
				.put("type", "deposit")
				.put("amount", 1)
				.put("externalIBAN", "testIBAN");
		
		postObject(secondTransaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 400, EPSILON);
		
		JSONObject thirdTransaction = new JSONObject()
				.put("date", "2018-04-01T00:00Z")
				.put("type", "deposit")
				.put("amount", 1)
				.put("externalIBAN", "testIBAN");
		
		postObject(thirdTransaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 800, EPSILON);
		
		JSONObject fourthTransaction = new JSONObject()
				.put("date", "2018-05-01T00:00Z")
				.put("type", "deposit")
				.put("amount", 1)
				.put("externalIBAN", "testIBAN");
		
		postObject(fourthTransaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 1000, EPSILON);
	}
	
	@Test
	public void testSystemClock() {
		String testSessionId = getNewSession();
		
		JSONObject transaction = new JSONObject()
				.put("date", "2018-05-01T00:00Z")
				.put("type", "deposit")
				.put("amount", 1000)
				.put("externalIBAN", "testIBAN");
		
		postObject(transaction, "transactions", testSessionId);
		
		JSONObject savingGoal = new JSONObject()
				.put("name", "Saving Goal")
				.put("goal", 1000)
				.put("savePerMonth", 200)
				.put("minBalanceRequired", 0);
		
		postObject(savingGoal, "savingGoals", testSessionId);
		
		transaction.put("date", "2018-06-01T00:00Z");
		postObject(transaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 200, EPSILON);
		
		transaction.put("date", "2018-05-01T00:00Z");
		postObject(transaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 200, EPSILON);
		
		transaction.put("date", "2018-07-01T00:00Z");
		postObject(transaction, "transactions", testSessionId);
		
		assertEquals(getSavingGoalBalance(testSessionId, 0), 400, EPSILON);
		
	}
	
	/**
	 * Test if the user sees internal transactions,
	 * which he obviously shouldn't be able to do.
	 */
	@Test
	public void testInternalTransactions() {
		String testSessionId = getNewSession();
		int nrOfNormalTransactions = 3;
		JSONObject normalTransaction = new JSONObject()
				.put("date", "2018-02-01T00:00Z")
				.put("amount",1)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		for (int i = 0; i < nrOfNormalTransactions; i++) {
			postObject(normalTransaction, "transactions", testSessionId);
		}
		testScenario(1000, 1000, 400, 400, 400, testSessionId, 0);
		
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/transactions");
		// Check that the response code is correct
		getResponseHeader.then().assertThat().statusCode(200);
		
		// Generate transaction object array from the response
		Transaction[] transactionlResponseHeader = getResponseHeader.as(Transaction[].class);
		assertEquals(transactionlResponseHeader.length, nrOfNormalTransactions + 2);
		
	}
	
	/**
	 * Test if the internal transactions count 
	 * toward the balance history values.
	 */
	@Test
	public void testBalanceHistoryTransactions() {
		String testSessionId = getNewSession();
		
		testScenario(1000, 1000, 400, 400, 400, testSessionId, 0);
		
		Response candlestickResponse = given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
			when().
				get("/balance/history?interval=year&intervals=1");
		CandleStick[] candlestickList = candlestickResponse.as(CandleStick[].class);
		CandleStick candlestickNow = candlestickList[0];
		
		double open = 0.0;
		double volume = 2400.0;
		double close = -400.0;
		double high = 1000.0;
		double low = -400.0;
		
		checkCandleStick(candlestickNow, open, volume, close, high, low);
	}
	
	@Test
	public void testGetRequest() {
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			get("/savingGoals?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/savingGoals").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/savingGoals").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/savingGoals");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/savingGoals?session_id=" + sessionID);
		
		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(200);
		getResponseParameter.then().assertThat().statusCode(200);
		
		// Generate saving goal object arrays from the responses
		SavingGoal[] savingGoalResponseHeader = getResponseHeader.as(SavingGoal[].class);
		SavingGoal[] savingGoalResponseParameter = getResponseParameter.as(SavingGoal[].class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("SavingGoalListSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("SavingGoalListSchema.json"));
		
		
		// Check if we get the same number of objects
		assertEquals(savingGoalResponseHeader.length, savingGoalResponseParameter.length);
		
		// Check that they give the same responses
		for (int i = 0; i < savingGoalResponseHeader.length; i++) {
			// Check if the data is the same
			assertTrue(savingGoalResponseHeader[i].equalsData(savingGoalResponseParameter[i]));
			// Check if the ids are the same
			assertEquals(savingGoalResponseHeader[i].getId(), savingGoalResponseParameter[i].getId());
		}
	}
	
	@Test
	public void testPostRequest() {
		JSONObject postSavingGoal = new JSONObject()
				.put("name", "Post Saving Goal")
				.put("savePerMonth", 1.0)
				.put("minBalanceRequired", 900.0)
				.put("goal", 1500.0)
				.put("balance", 0);
		
		
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header, no body
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);
		
		// Valid session id as parameter, no body
		given().
			header("Content-Type", "application/JSON").
		when().
			post("/savingGoals?session_id=" + sessionID).
		then().
			assertThat().statusCode(405);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        		body(postSavingGoal.toString()).
        	when().
        		post("/savingGoals");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals?session_id=" + sessionID);
		
		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(201);
		getResponseParameter.then().assertThat().statusCode(201);
		
		// Generate saving goal object arrays from the responses
		SavingGoal savingGoalResponseHeader = getResponseHeader.as(SavingGoal.class);
		SavingGoal savingGoalResponseParameter = getResponseParameter.as(SavingGoal.class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("SavingGoalSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("SavingGoalSchema.json"));
		
		// Check if the object we put in the body is the one that was posted
		assertEquals(postSavingGoal.toString(), savingGoalResponseHeader.toStringData());
		
		// Check that they give the same responses
		// Check if the data is the same
		assertTrue(savingGoalResponseHeader.equalsData(savingGoalResponseParameter));
		// Check if the ids are consecutive
		assertEquals(savingGoalResponseHeader.getId() + 1, savingGoalResponseParameter.getId());
		
		// Invalid input
		// name is null
		postSavingGoal.remove("name");
		
		given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        		body(postSavingGoal.toString()).
        	when().
        		post("/savingGoals").
        	then().
        		assertThat().statusCode(405);
		
		postSavingGoal.put("name", "Post Saving Goal");
		
		// goal is negative 
		postSavingGoal.put("goal", -5);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);

		
		postSavingGoal.put("goal", 900);
		
		// savePerMonth is negative
		postSavingGoal.put("savePerMonth", -10);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);
		
		// savePerMonth is zero
		postSavingGoal.put("savePerMonth", 0);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);
		
		postSavingGoal.put("savePerMonth", 50);
		
		// minBalanceRequired is negative
		postSavingGoal.put("minBalanceRequired", -30);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);
		
		postSavingGoal.put("minBalanceRequired", 100);
		
		// Edge cases
		// goal is zero
		postSavingGoal.put("goal", 0);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals").
		then().
			assertThat().statusCode(405);
		
		// Post request body contains nonzero goal
		postSavingGoal.put("goal", 500);
		
		Response response = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postSavingGoal.toString()).
		when().
			post("/savingGoals");
		
		// The request should be successful
		response.then().assertThat().statusCode(201);
		
		SavingGoal savingGoalResponse = response.as(SavingGoal.class);
		
		// Check that the balance is 0 by default
		assertTrue(savingGoalResponse.getBalance() == 0);
		
		
		// minBalanceRequired is null
		postSavingGoal.remove("minBalanceRequired");
		
		response = given().
				contentType("application/json").
				header("X-session-ID", sessionID).
				body(postSavingGoal.toString()).
			when().
				post("/savingGoals");
			
		// The request should be successful
		response.then().assertThat().statusCode(201);
		
		savingGoalResponse = response.as(SavingGoal.class);
		
		// Check that the minBalanceRequired is 0 by default
		assertTrue(savingGoalResponse.getMinBalanceRequired() == 0);
		
		// minBalanceRequired is 0
		postSavingGoal.put("minBalanceRequired", 0);
		
		response = given().
				contentType("application/json").
				header("X-session-ID", sessionID).
				body(postSavingGoal.toString()).
			when().
				post("/savingGoals");
			
		// The request should be successful
		response.then().assertThat().statusCode(201);
		
		savingGoalResponse = response.as(SavingGoal.class);
		
		// Check that the minBalanceRequired is 0
		assertTrue(savingGoalResponse.getMinBalanceRequired() == 0);
		
	}
	
	@Test
	public void testDeleteSavingGoal() {
		// Get a valid saving goal ID to work with
		JsonPath savingGoalJson = 
				given().
						header("X-session-ID", sessionID).
						header("Content-Type", "application/JSON").
				when().
				        get("/savingGoals").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the last Saving Goal id
		int listSize = savingGoalJson.getList("id").size();
		lastSavingGoalID = savingGoalJson.getList("id").get(listSize - 1).toString();
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			delete("/savingGoals/" + lastSavingGoalID + "?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			delete("/savingGoals/" + lastSavingGoalID).
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			delete("/savingGoals/" + lastSavingGoalID).
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		delete("/savingGoals/" + lastSavingGoalID).
        	then().
        		assertThat().statusCode(204);
		
		// Check that the saving goal was deleted
		// try another delete
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			delete("/savingGoals/" + lastSavingGoalID).
		then().
			assertThat().statusCode(404);
		
		// Add another object which should take the old object's place
		JSONObject deleteSavingGoal = new JSONObject()
				.put("name", "Delete Saving Goal")
				.put("savePerMonth", 1.0)
				.put("minBalanceRequired", 900.0)
				.put("goal", 1500.0)
				.put("balance", 0);
		
		
		Response response = given().
				contentType("application/json").
				header("X-session-ID", sessionID).
				body(deleteSavingGoal.toString()).
			when().
				post("/savingGoals");
		
		SavingGoal savingGoalResponse = response.as(SavingGoal.class);
		
		assertEquals(savingGoalResponse.getId(), Integer.parseInt(lastSavingGoalID));
		
		// Valid session id as a parameter
		given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		delete("/savingGoals/" + lastSavingGoalID + "?session_id=" + sessionID).
        	then().
        		assertThat().statusCode(204);
		
		// Check that the saving goal was deleted
		// try another delete
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			delete("/savingGoals/" + lastSavingGoalID + "/session_id=" + sessionID).
		then().
			assertThat().statusCode(404);
		
		// Add the object back
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(deleteSavingGoal.toString()).
		when().
			post("/savingGoals");
	}
	
	
}
