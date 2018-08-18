package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import nl.utwente.ing.model.Message;
import nl.utwente.ing.model.PaymentRequest;
import nl.utwente.ing.model.SavingGoal;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTest {
	private static String testSessionId;
	private static Instant now;
	private static String nowString;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		now = Instant.now();
		nowString = getDateString(now);
		
	}
	
	@Test
	public void testGetRequest() {
		testSessionId = getNewSession();
		
		JSONObject transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",100.0)
				.put("externalIBAN", "InitialTestIban")
				.put("description", "")
				.put("type", "withdrawal");
		postObject(transaction, "transactions", testSessionId);
		
		
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(testSessionId)-1).
		when().
			get("/messages?session_id=" + testSessionId).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/messages").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/messages").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/messages");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/messages?session_id=" + testSessionId);
		
		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(200);
		getResponseParameter.then().assertThat().statusCode(200);
		
		// Generate payment request object arrays from the responses
		Message[] messageResponseHeader = getResponseHeader.as(Message[].class);
		Message[] messageResponseParameter = getResponseParameter.as(Message[].class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("MessageListSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("MessageListSchema.json"));
		
		
		// Check if we get the same number of objects
		assertEquals(messageResponseHeader.length, messageResponseParameter.length);
		
		// Check that they give the same responses
		for (int i = 0; i < messageResponseHeader.length; i++) {
			// Check if the data is the same
			assertTrue(messageResponseHeader[i].equalsData(messageResponseParameter[i]));
			// Check if the ids are the same
			assertEquals(messageResponseHeader[i].getId(), messageResponseParameter[i].getId());
		}
		
	}

	
	public static String getDateString(Instant i) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
                .withZone(ZoneOffset.UTC)
                .format(i);
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
	
	private static Response postObject(JSONObject object, String uri, String session) {
		Response response = given().
			header("X-session-ID", session).
			header("Content-Type", "application/JSON").
			body(object.toString()).
		when().
			post("/" + uri);
		response.then().
			assertThat().statusCode(201);
		return response;
	}
	
	private static void checkGetRequest(List<Message> messages, String sessionId) {
		int nrMessages = messages.size();
		
		
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionId).
        	when().
        		get("/messages");
		
		Message[] requestMessages = getResponseHeader.as(Message[].class);
		
		// Check that the size is correct
		assertEquals(requestMessages.length, nrMessages);
		
		// Check that the data is correct
		for (int i = 0; i < nrMessages; i++) {
			assertTrue(requestMessages[i].equalsData(messages.get(i)));
		}
	}
	
	@Test
	public void testReadMessage() {
		testSessionId = getNewSession();
		
		JSONObject transaction = new JSONObject()
				.put("date", nowString)
				.put("amount", 50.0)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "withdrawal");
		
		// Add a transaction that makes the balance negative
		postObject(transaction, "transactions", testSessionId);
		
		
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/messages");
		Message[] requestMessages = getResponseHeader.as(Message[].class);
		
		// Make sure we have one message
		assertEquals(requestMessages.length, 1);
		Message m = requestMessages[0];
		
		// Set the message as read
		given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		put("/messages/" + m.getId());
		
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/messages");
		requestMessages = getResponseHeader.as(Message[].class);
		
		// Make sure we have no more unread message
		assertEquals(requestMessages.length, 0);
	}
	

	@Test
	public void testNegativeBalance() {
		
		testSessionId = getNewSession();
		List<Message> messages = new ArrayList<>();
		
		JSONObject transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",50.0)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		
		// Add a transaction that increases the balance by 50
		postObject(transaction, "transactions", testSessionId);
		
		checkGetRequest(messages, testSessionId);
		
		transaction
				.put("amount", 100.0)
				.put("type", "withdrawal");
		// Add a transaction that drops the balance below zero
		postObject(transaction, "transactions", testSessionId);
		
		String msg = "Balance dropped below zero!";
		Message m = new Message(0, msg, now.getEpochSecond(), false, "warning");
		messages.add(m);
		
		checkGetRequest(messages, testSessionId);
		
		transaction
				.put("amount", 50.0)
				.put("type", "deposit");
		// Add a transaction that puts the balance back to zero
		postObject(transaction, "transactions", testSessionId);
		
		checkGetRequest(messages, testSessionId);
	}
	
	@Test
	public void testHighestBalance() {
		testSessionId = getNewSession();
		List<Message> messages = new ArrayList<>();
		
		ZonedDateTime transactionTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
		
		JSONObject transaction = new JSONObject()
				.put("date", getDateString(transactionTime.toInstant()))
				.put("amount",50.0)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		
		postObject(transaction, "transactions", testSessionId);
		// No messages should be added because there isn't 3 months of data
		checkGetRequest(messages, testSessionId);
		
		transactionTime = transactionTime.plus(1, ChronoUnit.MONTHS);
		transaction
				.put("amount", 20.0)
				.put("type", "withdrawal")
				.put("date", getDateString(transactionTime.toInstant()));
		
		postObject(transaction, "transactions", testSessionId);
		
		// No messages should be added because there isn't 3 months of data
		checkGetRequest(messages, testSessionId);
		
		transactionTime = transactionTime.plus(1, ChronoUnit.MONTHS);
		
		transaction
				.put("date", getDateString(transactionTime.toInstant()));
		
		postObject(transaction, "transactions", testSessionId);
		
		
		transactionTime = transactionTime.plus(1, ChronoUnit.MONTHS);
		
		transaction
				.put("type", "deposit")
				.put("date", getDateString(transactionTime.toInstant()));
		
		postObject(transaction, "transactions", testSessionId);

		checkGetRequest(messages, testSessionId);
		
		transactionTime = transactionTime.plus(1, ChronoUnit.MONTHS);
		transaction
				.put("amount", 70.0)
				.put("date", getDateString(transactionTime.toInstant()));
		// The balance should now be 100, so a new message must be added
		postObject(transaction, "transactions", testSessionId);
		String msg = "Your balance reached a new high of 100.0!";
		Message m = new Message(0, msg, transactionTime.toEpochSecond(), false, "info");
		messages.add(m);
		checkGetRequest(messages, testSessionId);
		
	}
	
	@Test
	public void testPaymentRequestFill() {
		testSessionId = getNewSession();
		List<Message> messages = new ArrayList<>();
		
		JSONObject paymentRequest = new JSONObject()
				.put("description", "This payment request should be filled after posting the right transaction.")
				.put("due_date", getDateString(now.plus(1, ChronoUnit.HOURS)))
			    .put("amount", 12345.67)
			    .put("number_of_requests", 2);
		
		int paymentRequestId = postObject(paymentRequest, "paymentRequests", testSessionId).as(PaymentRequest.class).getId();
		
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/messages");
		Message[] requestMessages = getResponseHeader.as(Message[].class);
		
		// Make sure we have no messages
		assertEquals(requestMessages.length, 0);
		
		JSONObject transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",12345.67)
				.put("externalIBAN", "InitialTestIban")
				.put("description", "")
				.put("type", "deposit");
		
		postObject(transaction, "transactions", testSessionId);
		
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", testSessionId).
        	when().
        		get("/messages");
		requestMessages = getResponseHeader.as(Message[].class);
		
		// Make sure we have no messages
		assertEquals(requestMessages.length, 0);
		
		postObject(transaction, "transactions", testSessionId);
		String msg = "Payment request with id " + paymentRequestId + " filled!";
		Message m = new Message(0, msg, now.getEpochSecond(), false, "info");
		messages.add(m);
		checkGetRequest(messages, testSessionId);
		
	}
	
	@Test
	public void testSavingGoalReached() {
		testSessionId = getNewSession();
		
		List<Message> messages = new ArrayList<>();
		
		JSONObject savingGoal = new JSONObject()
				.put("name", "Testing Saving Goal complete message")
				.put("goal", 1000.0)
				.put("savePerMonth", 500.0)
				.put("minBalanceRequired", 0.0);
		
		int sgId = postObject(savingGoal, "savingGoals", testSessionId).as(SavingGoal.class).getId();
		
		checkGetRequest(messages, testSessionId);
		
		JSONObject initialTransaction = new JSONObject()
				.put("date", "2018-02-01T00:00Z")
				.put("amount", 1002)
				.put("externalIBAN", "TestIban")
				.put("description", "")
				.put("type", "deposit");
		
		postObject(initialTransaction, "transactions", testSessionId);
		
		checkGetRequest(messages, testSessionId);
		
		JSONObject secondTransaction = new JSONObject()
				.put("date", "2018-03-01T00:00Z")
				.put("type", "withdrawal")
				.put("amount", 1)
				.put("externalIBAN", "testIBAN");
		
		postObject(secondTransaction, "transactions", testSessionId);
		
		checkGetRequest(messages, testSessionId);
		
		JSONObject thirdTransaction = new JSONObject()
				.put("date", "2018-04-01T00:00Z")
				.put("type", "withdrawal")
				.put("amount", 1)
				.put("externalIBAN", "testIBAN");
		
		postObject(thirdTransaction, "transactions", testSessionId);
		
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		long completeTime = LocalDateTime.from(formatter.parse("2018-04-01T00:00Z")).toEpochSecond(ZoneOffset.UTC);
		String msg = "Saving goal with id " + sgId + " has been filled!";
		
		Message m = new Message(0, msg, completeTime, false, "info");
		messages.add(m);
		
		checkGetRequest(messages, testSessionId);
	}
	
	@Test
	public void testExpiredPaymentRequest() {
		List<Message> messages = new ArrayList<>();
		testSessionId = getNewSession();
		
		JSONObject paymentRequest = new JSONObject()
				.put("description", "This payment request should be filled after posting the right transaction.")
				.put("due_date", getDateString(now.plus(1, ChronoUnit.HOURS)))
			    .put("amount", 12345.67)
			    .put("number_of_requests", 2);
		int paymentRequestId = postObject(paymentRequest, "paymentRequests", testSessionId).as(PaymentRequest.class).getId();
		
		checkGetRequest(messages, testSessionId);
		
		JSONObject transaction = new JSONObject()
				.put("date", getDateString(now.plus(2, ChronoUnit.HOURS)))
				.put("amount",12345.67)
				.put("externalIBAN", "InitialTestIban")
				.put("description", "")
				.put("type", "deposit");
		postObject(transaction, "transactions", testSessionId);
		String msg = "Payment request with id " + paymentRequestId + " has not been filled on time!";
		Message m = new Message(0, msg, now.plus(2, ChronoUnit.HOURS).getEpochSecond(), false, "warning");
		messages.add(m);
		
		checkGetRequest(messages, testSessionId);
	}
	
}
