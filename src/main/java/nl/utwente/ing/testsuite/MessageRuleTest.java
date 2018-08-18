package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import nl.utwente.ing.model.Message;
import nl.utwente.ing.model.MessageRule;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageRuleTest {
	private static String testSessionId;
	private static Instant now;
	private static String nowString;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		now = Instant.now();
		nowString = getDateString(now);
		
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
	public void testPostRequest(){
		testSessionId = getNewSession();

		JSONObject category = new JSONObject().put("name", "Message rule test");
		int categoryId = postObject(category, "categories", testSessionId).as(nl.utwente.ing.model.Category.class).getId();
		JSONObject messageRule = new JSONObject().put("type", "info")
				.put("value", 200.0)
				.put("category_id", categoryId);

		// Mismatching session IDs
		given().
				header("Content-Type", "application/JSON").
				header("X-session-ID", Integer.parseInt(testSessionId)-1).
				body(messageRule.toString()).
				when().
				post("/messageRules?session_id=" + testSessionId).
				then().
				assertThat().statusCode(401);

		// No header
		given().
				header("Content-Type", "application/JSON").
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(401);

		// Invalid header
		given().
				header("X-session-ID", -1).
				header("Content-Type", "application/JSON").
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(401);

		// Valid session id in header, no body
		given().
				header("X-session-ID", testSessionId).
				header("Content-Type", "application/JSON").
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(405);

		// Valid session id as parameter, no body
		given().
				header("Content-Type", "application/JSON").
				when().
				post("/messageRules?session_id=" + testSessionId).
				then().
				assertThat().statusCode(405);

		// Valid session id in header
		Response getResponseHeader = given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
				body(messageRule.toString()).
				when().
				post("/messageRules");

		// Valid session id as parameter
		Response getResponseParameter = given().
				header("Content-Type", "application/JSON").
				body(messageRule.toString()).
				when().
				post("/messageRules?session_id=" + testSessionId);

		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(201);
		getResponseParameter.then().assertThat().statusCode(201);

		// Generate saving goal object arrays from the responses
		MessageRule messageRuleResponseHeader = getResponseHeader.as(MessageRule.class);
		MessageRule messageRuleResponseParameter = getResponseParameter.as(MessageRule.class);


		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
				body(JsonSchemaValidator.matchesJsonSchemaInClasspath("MessageRuleSchema.json"));
		getResponseParameter.then().assertThat().
				body(JsonSchemaValidator.matchesJsonSchemaInClasspath("MessageRuleSchema.json"));

		// Check if the object we put in the body is the one that was posted
		assertEquals(messageRule.toString(), messageRuleResponseHeader.toStringData());

		// Check that they give the same responses
		// Check if the data is the same
		assertTrue(messageRuleResponseHeader.equalsData(messageRuleResponseParameter));
		// Check if the ids are consecutive
		assertEquals(messageRuleResponseHeader.getId() + 1, messageRuleResponseParameter.getId());

		// Invalid input
		// type is null
		messageRule.remove("type");

		given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(405);

		// type is invalid
		messageRule.put("type", "Invalid_type");

		given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(405);

		messageRule.put("type", "info");

		// value is negative
		messageRule.put("value", -1.0);

		given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(405);

		// value is zero
		messageRule.put("value", 0.0);

		given().
				contentType("application/json").
				header("X-session-ID", testSessionId).
				body(messageRule.toString()).
				when().
				post("/messageRules").
				then().
				assertThat().statusCode(201);

		messageRule.put("value", 200.0);


	}
	
	@Test
	public void testWithTransactions() {
		/*
		Below we test a few scenarios:
			- The threshold is not reached
			- The threshold of 200 is reached by a transaction 29 days after the first transaction
			- The thresholds of 200 and 300 are reached by a transaction 30 days after the first transaction
			- The threshold of 200 is reached by a transaction 61 days after the first transaction
			- The threshold is not reached by a transaction 60 days after the first transaction because the transaction
			is not in the future.

		For simplicity, there is a category rule to automatically add the category to the transactions.

		The following behaviours have to be seen:
			- If there is a message, it must mean the threshold is reached and/or passed
			- Spending is counted starting 30 days before the last transaction
			- Only transactions posted 'in the future' are considered
			- Message rules create messages with the type and threshold given by the user
			- Every time the threshold is passed, a new message is added.
		 */
		List<Message> messages = new ArrayList<>();
		testSessionId = getNewSession();
		
		JSONObject initialTransaction = new JSONObject().
				put("date", nowString).
				put("amount",1000.0).
				put("externalIBAN", "NL12ABNA0457688830").
				put("description", "Initial transaction to set the balance to 1000").
				put("type", "deposit");
		postObject(initialTransaction, "transactions", testSessionId);
		
		JSONObject category = new JSONObject().put("name", "Message rule test");
		int categoryId = postObject(category, "categories", testSessionId).as(nl.utwente.ing.model.Category.class).getId();
		
		JSONObject categoryRule = new JSONObject()
				.put("description", "Message rule transaction")
				.put("iBAN", "testIBAN")
				.put("type", "withdrawal")
				.put("category_id", categoryId);
		postObject(categoryRule, "categoryRules", testSessionId);
		
		JSONObject messageRule = new JSONObject().put("type", "info")
												.put("value", 200.0)
												.put("category_id", categoryId);
		postObject(messageRule, "messageRules", testSessionId);

		// Add a second message rule with a different type and value
		messageRule
				.put("type", "warning")
				.put("value", 300.0);
		postObject(messageRule, "messageRules", testSessionId);


		// There should be no new messages
		checkGetRequest(messages, testSessionId);
		
		JSONObject transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",200.0)
				.put("externalIBAN", "testIBAN")
				.put("description", "This transaction should not produce a message because it doesn't have a category")
				.put("type", "withdrawal");
		postObject(transaction, "transactions", testSessionId);

		transaction
				.put("amount", 100.0)
				.put("description", "Message rule transaction");
		postObject(transaction, "transactions", testSessionId);

		// There should be no new messages because the threshold is not reached
		checkGetRequest(messages, testSessionId);

		transaction
				.put("date", getDateString(now.plus(29, ChronoUnit.DAYS)));
		postObject(transaction, "transactions", testSessionId);

		String msg = "Spending exceeded threshold of 200.0 on category with id " + categoryId + ".";
		Message m = new Message(0, msg, now.plus(29, ChronoUnit.DAYS).getEpochSecond(), false, "info");
		messages.add(m);

		// There should a new message that the threshold has been crossed
		checkGetRequest(messages, testSessionId);

		transaction
				.put("date", getDateString(now.plus(30, ChronoUnit.DAYS)));
		postObject(transaction, "transactions", testSessionId);
		m = new Message(0, msg, now.plus(30, ChronoUnit.DAYS).getEpochSecond(), false, "info");
		messages.add(m);

		msg = "Spending exceeded threshold of 300.0 on category with id " + categoryId + ".";
		m = new Message(0, msg, now.plus(30, ChronoUnit.DAYS).getEpochSecond(), false, "warning");
		messages.add(m);

		// There should a new message that the threshold has been crossed
		checkGetRequest(messages, testSessionId);

		// This transaction is 31 days after the last transaction so no new message should be added
		transaction
				.put("date", getDateString(now.plus(61, ChronoUnit.DAYS)));
		postObject(transaction, "transactions", testSessionId);

		// There should no new messages that the threshold has been crossed
		checkGetRequest(messages, testSessionId);

		transaction
				.put("type", "deposit");
		// There should no new messages that the threshold has been crossed
		checkGetRequest(messages, testSessionId);

		// This transaction is not 'in the future' so the system should not add any messages
		transaction
				.put("type", "withdrawal")
				.put("date", getDateString(now.plus(60, ChronoUnit.DAYS)));
		postObject(transaction, "transactions", testSessionId);

		// There should no new messages that the threshold has been crossed
		checkGetRequest(messages, testSessionId);
	}
	
	
}
