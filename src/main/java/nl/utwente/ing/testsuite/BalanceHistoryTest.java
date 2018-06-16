package nl.utwente.ing.testsuite;


import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import nl.utwente.ing.model.CandleStick;

public class BalanceHistoryTest {
	private static String sessionID;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		sessionID = getNewSession();
	}
	
	@Test
	public void testResponses() {
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			get("/balance/history?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/balance/history").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/balance/history").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/balance/history");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/balance/history?session_id=" + sessionID);
		
		// Generate category rule object arrays from the responses
		CandleStick[] balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		CandleStick[] balanceHistoryResponseParameter = getResponseParameter.as(CandleStick[].class);
		
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CandleStickListSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CandleStickListSchema.json"));
		
		
		// Check if we get the same number of objects
		assertEquals(balanceHistoryResponseHeader.length, balanceHistoryResponseParameter.length);
		
		// Make sure the default value works
		assertEquals(balanceHistoryResponseHeader.length, 24);
		
		// ---- Interval number ----
		// Interval number less than 1
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/balance/history?intervals=0");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		assertEquals(balanceHistoryResponseHeader.length, 1);
		
		// Interval number equal to 1
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/balance/history?intervals=1");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		assertEquals(balanceHistoryResponseHeader.length, 1);
		
		// Interval number equal to 200
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/balance/history?intervals=200");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		assertEquals(balanceHistoryResponseHeader.length, 200);
		
		// Interval number more than 200
		getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/balance/history?intervals=300");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		assertEquals(balanceHistoryResponseHeader.length, 200);
		
		// ---- Interval size ----
		// Hour
		ChronoUnit interval = ChronoUnit.HOURS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=hour");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
		
		// Day
		
		interval = ChronoUnit.DAYS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=day");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
		
		// Week
		interval = ChronoUnit.WEEKS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=week");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
		
		// Month
		interval = ChronoUnit.MONTHS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=month");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
			
		// Month
		interval = ChronoUnit.MONTHS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=month");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
		
		// Year
		interval = ChronoUnit.YEARS;
		getResponseHeader = given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=year");
		
		balanceHistoryResponseHeader = getResponseHeader.as(CandleStick[].class);
		
		checkIntervalComputation(balanceHistoryResponseHeader, interval);
		
		// Some other wrong interval
		given().
			contentType("application/json").
			header("X-session-ID", sessionID).
		when().
			get("/balance/history?interval=century").then().assertThat().statusCode(405);
	}
	
	
	@Test
	public void testWithTransactions() {
		// Everything test with transaction must have a different session because the data shouldn't interfere
		// or else we can have problems such as not knowing if a transaction that happened an hour ago happened today
		// or yesterday.
		// Assumption: every test happens in one time unit ( i.e. the hour doesn't increment while performing the test )
		
		ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
		ZonedDateTime intervalAgo;
		Response candlestickResponse;
		CandleStick[] candlestickList;
		CandleStick candlestickNow;
		CandleStick candlestickPreviousInterval;
		double open;
		double volume;
		double close;
		double high;
		double low;
		String[] intervals = {"hour","day","week","month","year"};
		
		
		// Every interval of time gets the same test
		for (String interval : intervals) {
			
			sessionID = getNewSession();
			intervalAgo = now.minus(1, ChronoUnit.valueOf((interval + "s").toUpperCase()));
			
			// Check open
			// The balance initially should be 0
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval + "&intervals=1");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[0];
			
			open = 0.0;
			volume = 0.0;
			close = 0.0;
			high = 0.0;
			low = 0.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			JSONObject transaction = new JSONObject().put("date", getDateTime(intervalAgo))
										.put("amount", 100.0)
										.put("externalIBAN", "testIBAN")
										.put("type", "deposit"); // Transaction that happened exactly one interval ago
			// Set the balance to 100 with a deposit of 100 made last time interval
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval + "&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 100.0;
			volume = 0.0;
			close = 100.0;
			high = 100.0;
			low = 100.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 100.0;
			close = 100.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
		
			
			// Set the balance to 50 with a withdrawal of 50 made last interval
	
			transaction.put("type", "withdrawal")
					   .put("amount", 50.0);
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval + "&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 50.0;
			volume = 0.0;
			close = 50.0;
			high = 50.0;
			low = 50.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 150.0;
			close = 50.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
			
			// Set the balance to 120 with a deposit of 70 made this interval
			
			transaction.put("type", "deposit").put("date", getDateTime(now))
			   .put("amount", 70.0); 
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval +"&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 50.0;
			volume = 70.0;
			close = 120.0;
			high = 120.0;
			low = 50.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 150.0;
			close = 50.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
			
			// Make close smaller than high
			// Set the balance to 100 with a withdrawal of 20 made this interval
			
			transaction.put("type", "withdrawal")
			   .put("amount", 20.0); 
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval +"&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 50.0;
			volume = 90.0;
			close = 100.0;
			high = 120.0;
			low = 50.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 150.0;
			close = 50.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
			
			// Make low smaller than open
			// Set the balance to 30 with a withdrawal of 70 made this interval
		
			
			transaction
			   .put("amount", 70.0); 
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval +"&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 50.0;
			volume = 160.0;
			close = 30.0;
			high = 120.0;
			low = 30.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 150.0;
			close = 50.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
			
			// Make close a different value than the rest of the attributes
			// Set the balance to 60 with a deposit of 30 made this interval
		
			
			transaction.put("type", "deposit")
			   .put("amount", 30.0); 
			postTransaction(transaction);
			
			candlestickResponse = given().
					contentType("application/json").
					header("X-session-ID", sessionID).
				when().
					get("/balance/history?interval=" + interval +"&intervals=2");
			candlestickList = candlestickResponse.as(CandleStick[].class);
			candlestickNow = candlestickList[1];
			candlestickPreviousInterval = candlestickList[0];
			
			// Check the candlestick for this interval
			open = 50.0;
			volume = 190.0;
			close = 60.0;
			high = 120.0;
			low = 30.0;
			
			checkCandleStick(candlestickNow, open, volume, close, high, low);
			
			// Check the candlestick for the previous interval
			open = 0.0;
			volume = 150.0;
			close = 50.0;
			high = 100.0;
			low = 0.0;
			
			checkCandleStick(candlestickPreviousInterval, open, volume, close, high, low);
		}
		
	}
	
	@Test
	public void testEfficiency() {
		
		long start;
		long duration;
		double avg = 0;
		int nrTrials = 20;
		for (int i = 0; i < nrTrials; i++) {
			start = System.nanoTime();
			given().
				contentType("application/json").
				header("X-session-ID", sessionID).
			when().
				get("/balance/history?interval=hour&intervals=200");
			duration = System.nanoTime() - start;
			avg += (duration/(Math.pow(10, 6)*nrTrials));
		}
		assertTrue((long)avg < 400);
	}
	
	
	
	private static void checkIntervalComputation(CandleStick[] candlesticks, ChronoUnit interval) {
		// Check that the intervals are computed correctly
		
		// Check if the latest time is correctly computed
		
		ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
		ZonedDateTime nowTruncated;
		
		// The code below rounds down to the beginning of the Hour, Day, Week, Month or Year
		if (interval.equals(ChronoUnit.WEEKS)) {		
			// Rounds down to the nearest day
			nowTruncated = now.truncatedTo(ChronoUnit.DAYS);
			
			// Rounds down to the first day of the week
			nowTruncated = nowTruncated.minus(nowTruncated.getDayOfWeek().getValue() - 1, ChronoUnit.DAYS);
			
		} else if (interval.equals(ChronoUnit.MONTHS)) {
			// Rounds down to the nearest day
			nowTruncated = now.truncatedTo(ChronoUnit.DAYS);
			
			// Rounds down to the first day of the month
			nowTruncated = nowTruncated.withDayOfMonth(1);
			
		} else if (interval.equals(ChronoUnit.YEARS)) {
			// Rounds down to the nearest day
			nowTruncated = now.truncatedTo(ChronoUnit.DAYS);
			
			// Rounds down to the first day of the month
			nowTruncated = nowTruncated.withDayOfMonth(1);
			
			// Rounds down to the first month of the year
			nowTruncated = nowTruncated.withMonth(1);
		}
		else {
			nowTruncated = now.truncatedTo(interval);
		}
		
		assertEquals(nowTruncated.toEpochSecond(), candlesticks[candlesticks.length - 1].getTimestamp());
		
		// Check if Time(n + 1) = Time(n) + interval
		for (int i = 0; i < candlesticks.length - 1; i++) {
			ZonedDateTime first = ZonedDateTime.ofInstant(Instant.ofEpochSecond(candlesticks[i].getTimestamp()), ZoneOffset.UTC);
			ZonedDateTime second = ZonedDateTime.ofInstant(Instant.ofEpochSecond(candlesticks[i+1].getTimestamp()), ZoneOffset.UTC);
			assertEquals(first.plus(1, interval), second);
		}
	}
	
	private static void postTransaction(JSONObject transaction) {
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(transaction.toString()).
		when().
			post("/transactions");
	}
	
	private static String getDateTime(ZonedDateTime inst) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
        .withZone(ZoneOffset.UTC)
        .format(inst);
	}
	
	private static void checkCandleStick(CandleStick c, double open, double volume, double close, double high, double low) {
		assertTrue(c.getOpen() == open);
		assertTrue(c.getVolume() == volume);
		assertTrue(c.getClose() == close);
		assertTrue(c.getHigh() == high);
		assertTrue(c.getLow() == low);
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

}
