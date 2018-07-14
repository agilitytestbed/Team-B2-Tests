package nl.utwente.ing.testsuite;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import nl.utwente.ing.model.PaymentRequest;
import nl.utwente.ing.model.Transaction;

public class PaymentRequestTest {
	private static String sessionID;
	private static final int INITIAL_PAYMENT_REQUESTS = 10;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		sessionID = getNewSession();
		
		JSONObject dummyPaymentRequest = new JSONObject()
				.put("description","Payback for lunch")
			    .put("due_date", "2018-07-07T14:41:44.511Z")
			    .put("amount", 213.04)
			    .put("number_of_requests", 1);
		
		// Add some initial saving goals to work with
		for (int i = 0; i < INITIAL_PAYMENT_REQUESTS; i++) {
			postObject(dummyPaymentRequest, "paymentRequests", sessionID);
		}
	}
	
	private static boolean getFilledStatus(String session, int index) {
		JsonPath paymentRequestJson = 
				given().
						header("X-session-ID", session).
						header("Content-Type", "application/JSON").
				when().
				        get("/paymentRequests").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the payment request filled status
		return Boolean.valueOf(paymentRequestJson.getList("filled").get(index).toString());
	}
	
	private static int getNrTransactions(String session, int index) {
		JsonPath paymentRequestJson = 
				given().
						header("X-session-ID", session).
						header("Content-Type", "application/JSON").
				when().
				        get("/paymentRequests").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the payment request number of transactions
		return paymentRequestJson.getList("transactions[" + index + "]").size();
	}
	
	private static boolean checkTransactionList(String session, int index, List<Transaction> transactions) {
		PaymentRequest[] requests = given().
			header("X-session-ID", session).
			header("Content-Type", "application/JSON").
		when().
	        get("/paymentRequests").as(PaymentRequest[].class);
		
		List<Transaction> requestTransactions = requests[index].getTransactions();

		for (int i = 0; i < requestTransactions.size(); i++) {
			if (!transactions.get(i).equals(requestTransactions.get(i))) {
				return false;
			}
		}
		return true;
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
	
	

	@Test
	public void testGetRequest() {
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			get("/paymentRequests?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/paymentRequests").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/paymentRequests").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/paymentRequests");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/paymentRequests?session_id=" + sessionID);
		
		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(200);
		getResponseParameter.then().assertThat().statusCode(200);
		
		// Generate payment request object arrays from the responses
		PaymentRequest[] paymentRequestResponseHeader = getResponseHeader.as(PaymentRequest[].class);
		PaymentRequest[] paymentRequestResponseParameter = getResponseParameter.as(PaymentRequest[].class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("PaymentRequestListSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("PaymentRequestListSchema.json"));
		
		
		// Check if we get the same number of objects
		assertEquals(paymentRequestResponseHeader.length, paymentRequestResponseParameter.length);
		
		// Check that they give the same responses
		for (int i = 0; i < paymentRequestResponseHeader.length; i++) {
			// Check if the data is the same
			assertTrue(paymentRequestResponseHeader[i].equalsData(paymentRequestResponseParameter[i]));
			// Check if the ids are the same
			assertEquals(paymentRequestResponseHeader[i].getId(), paymentRequestResponseParameter[i].getId());
		}
	}
	
	@Test
	public void testPostRequest() {
		JSONObject postPaymentRequest =  new JSONObject()
				.put("description","Post payment request")
			    .put("due_date", "2018-07-07T14:41:44.511Z")
			    .put("amount", 150.04)
			    .put("number_of_requests", 2);
		
		
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header, no body
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
		// Valid session id as parameter, no body
		given().
			header("Content-Type", "application/JSON").
		when().
			post("/paymentRequests?session_id=" + sessionID).
		then().
			assertThat().statusCode(405);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        		body(postPaymentRequest.toString()).
        	when().
        		post("/paymentRequests");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests?session_id=" + sessionID);
		
		// Check that the response codes are correct
		getResponseHeader.then().assertThat().statusCode(201);
		getResponseParameter.then().assertThat().statusCode(201);
		
		// Generate saving goal object arrays from the responses
		PaymentRequest paymentRequestResponseHeader = getResponseHeader.as(PaymentRequest.class);
		PaymentRequest paymentRequestResponseParameter = getResponseParameter.as(PaymentRequest.class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("PaymentRequestSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("PaymentRequestSchema.json"));
		
		// Check if the object we put in the body is the one that was posted
		assertEquals(postPaymentRequest.toString(), paymentRequestResponseHeader.toStringData());
		
		// Check that they give the same responses
		// Check if the data is the same
		assertTrue(paymentRequestResponseHeader.equalsData(paymentRequestResponseParameter));
		// Check if the ids are consecutive
		assertEquals(paymentRequestResponseHeader.getId() + 1, paymentRequestResponseParameter.getId());
		
		// Invalid input
		// description is null
		postPaymentRequest.remove("description");
		
		given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        		body(postPaymentRequest.toString()).
        	when().
        		post("/paymentRequests").
        	then().
        		assertThat().statusCode(405);
		
		postPaymentRequest.put("description", "Post Payment Request");
		
		// amount is 0
		postPaymentRequest.put("amount", 0.0);
		
		given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        		body(postPaymentRequest.toString()).
        	when().
        		post("/paymentRequests").
        	then().
        		assertThat().statusCode(405);
		
		// amount is negative
		postPaymentRequest.put("amount", -15.0);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
		postPaymentRequest.put("amount", 200.5);
		
		// due date is null
		postPaymentRequest.remove("due_date");
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
		// due date has incorrect format
		postPaymentRequest.put("due_date", "2018-07-07 21:48:17");
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
		postPaymentRequest.put("due_date", "2018-07-07T21:48:17.458Z");
		
		// number of requests is zero
		postPaymentRequest.put("number_of_requests", 0);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
		// number of requests is negative
		postPaymentRequest.put("number_of_requests", -5);
		
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
			body(postPaymentRequest.toString()).
		when().
			post("/paymentRequests").
		then().
			assertThat().statusCode(405);
		
	}
	
	
	
	@Test
	public void testWithTransactions() {
		String testSessionId = getNewSession();
		JSONObject paymentRequest;
		JSONObject transaction;
		Instant now = Instant.now();
		Instant oneHourLater = now.plus(1, ChronoUnit.HOURS);
		String oneHourLaterString = getDateString(oneHourLater);
		String nowString = getDateString(now);
		
		/*
		 * Fill one payment request out of one payment requests
		 */
		paymentRequest = new JSONObject()
				.put("description", "This payment request should be filled after posting the right transaction.")
				.put("due_date", oneHourLaterString)
			    .put("amount", 12345.67)
			    .put("number_of_requests", 1);
		postObject(paymentRequest, "paymentRequests", testSessionId);
		
		assertFalse(getFilledStatus(testSessionId, 0));
		
		// Transaction is not a deposit
		transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",12345.67)
				.put("externalIBAN", "TestIban")
				.put("description", "Should not work")
				.put("type", "withdrawal");
		
		postObject(transaction, "transactions", testSessionId);
		
		assertFalse(getFilledStatus(testSessionId, 0));
		transaction.put("type", "deposit");
		
		// transaction has the wrong amount
		transaction.put("amount", 76543.21);
		postObject(transaction, "transactions", testSessionId);
		assertFalse(getFilledStatus(testSessionId, 0));
		transaction.put("amount", 12345.67);
		
		// transaction is after the due date
		
		transaction.put("date", getDateString(now.plus(2, ChronoUnit.HOURS)));
		postObject(transaction, "transactions", testSessionId);
		assertFalse(getFilledStatus(testSessionId, 0));
		transaction.put("date", nowString);
		
		// transaction is correct
		
		transaction.put("description", "Should work");
		
		// Get the transaction object that was just posted
		Transaction transactionObject = postObject(transaction, "transactions", testSessionId).as(Transaction.class);
		// Put it into a list to compare to what the payment request displays
		List<Transaction> transactionList = new ArrayList<Transaction>();
		transactionList.add(transactionObject);
		
		assertTrue(getFilledStatus(testSessionId, 0));
		assertTrue(checkTransactionList(testSessionId, 0, transactionList));
		
		/*
		 * Fill in one payment request out of 2 payment requests
		 */
		
		testSessionId = getNewSession();
		
		paymentRequest = new JSONObject()
				.put("description", "This payment request should be filled after posting the right transaction.")
				.put("due_date", oneHourLaterString)
			    .put("amount", 12345.67)
			    .put("number_of_requests", 1);
		postObject(paymentRequest, "paymentRequests", testSessionId);
		postObject(paymentRequest, "paymentRequests", testSessionId);
		
		assertFalse(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		// Transaction is not a deposit
		transaction = new JSONObject()
				.put("date", nowString)
				.put("amount",12345.67)
				.put("externalIBAN", "TestIban")
				.put("description", "Should not work")
				.put("type", "withdrawal");
		
		postObject(transaction, "transactions", testSessionId);
		
		assertFalse(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		transaction.put("type", "deposit");
		
		// transaction has the wrong amount
		transaction.put("amount", 76543.21);
		postObject(transaction, "transactions", testSessionId);
		
		assertFalse(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		transaction.put("amount", 12345.67);
		
		// transaction is after the due date
		
		transaction.put("date", getDateString(now.plus(2, ChronoUnit.HOURS)));
		postObject(transaction, "transactions", testSessionId);
		assertFalse(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		transaction.put("date", nowString);
		
		// transaction is correct
		transaction.put("description", "Should work");
		
		
		// Get the transaction object that was just posted
		transactionObject = postObject(transaction, "transactions", testSessionId).as(Transaction.class);
		// Put it into a list to compare to what the payment request displays
		transactionList = new ArrayList<Transaction>();
		transactionList.add(transactionObject);
		
		// Check that the first payment request was fulfilled but not the second
		assertTrue(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		// Check that the first payment request has the correct transaction
		assertTrue(checkTransactionList(testSessionId, 0, transactionList));
		
		postObject(transaction, "transactions", testSessionId);
		
		// Check that both payment requests are now fulfilled
		assertTrue(getFilledStatus(testSessionId, 0));
		assertTrue(getFilledStatus(testSessionId, 1));
		
		// Check that both payment requests have the correct transactions
		assertTrue(checkTransactionList(testSessionId, 0, transactionList));
		assertTrue(checkTransactionList(testSessionId, 1, transactionList));
		
		/*
		 * Payment request with multiple transactions
		 */
		testSessionId = getNewSession();
		paymentRequest.put("number_of_requests", 2);
		postObject(paymentRequest, "paymentRequests", testSessionId);
		paymentRequest.put("number_of_requests", 1);
		postObject(paymentRequest, "paymentRequests", testSessionId);
		transactionList = new ArrayList<Transaction>();
		
		// Get the transaction object that was just posted
		transactionObject = postObject(transaction, "transactions", testSessionId).as(Transaction.class);
		// Put it into a list to compare to what the payment request displays
		transactionList.add(transactionObject);
		
		// The transaction should only affect the first payment request but not fill it
		assertFalse(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		// Check that the posted transaction is recorded by the payment request
		assertEquals(getNrTransactions(testSessionId, 0), 1);
		assertEquals(getNrTransactions(testSessionId, 1), 0);
		
		// Check that the first payment request has the correct transaction
		assertTrue(checkTransactionList(testSessionId, 0, transactionList));
		
		// Get the transaction object that was just posted
		transactionObject = postObject(transaction, "transactions", testSessionId).as(Transaction.class);
		// Put it into a list to compare to what the payment request displays
		transactionList.add(transactionObject);
		
		// The transaction should fill the first payment request but not the second one
		assertTrue(getFilledStatus(testSessionId, 0));
		assertFalse(getFilledStatus(testSessionId, 1));
		
		// The first payment request should have two transactions and the second one none
		assertEquals(getNrTransactions(testSessionId, 0), 2);
		assertEquals(getNrTransactions(testSessionId, 1), 0);
		
		// Check that the first payment request has the correct transactions
		assertTrue(checkTransactionList(testSessionId, 0, transactionList));
		
		postObject(transaction, "transactions", testSessionId);
		
		// Both payment requests should be filled
		assertTrue(getFilledStatus(testSessionId, 0));
		assertTrue(getFilledStatus(testSessionId, 1));
		
		// Both payment requests should have the necessary number of transactions
		assertEquals(getNrTransactions(testSessionId, 0), 2);
		assertEquals(getNrTransactions(testSessionId, 1), 1);
	}
	
	
	
}
