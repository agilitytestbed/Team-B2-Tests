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
import nl.utwente.ing.model.CategoryRule;
import nl.utwente.ing.model.Transaction;

public class CategoryRuleTest {
	private static String sessionID;
	private static String validCategoryID;
	private static String validCategoryRuleID;
	private static final int INITIAL_CATEGORIES = 5;
	private static final int INITIAL_CATEGORY_RULES = 5;
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
		sessionID = 
		given().
		        contentType("application/json").
		when().
		        post("/sessions").
		then().
				contentType(ContentType.JSON).
		extract().
				response().jsonPath().getString("id");
		
		
		// Add some categories to test with
		JSONObject category = new JSONObject()
											.put("name", "test");
		for (int i = 0; i < INITIAL_CATEGORIES; i++) {
			given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
				body(category.toString()).
			when().
				post("/categories");
		}
		// Get a valid categoryID to work with
		JsonPath categoryJson = 
				given().
						header("X-session-ID", sessionID).
						header("Content-Type", "application/JSON").
				when().
				        get("/categories").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the first categoryID
		validCategoryID = categoryJson.getString("id[0]");
		
		// Add some category rules
				JSONObject categoryRule = new JSONObject()
						.put("description", "")
						.put("iBAN", "testIBAN")
						.put("type", "deposit")
						.put("category_id", validCategoryID);
				
				for (int i = 0; i < INITIAL_CATEGORY_RULES; i++) {
					given().
						header("X-session-ID", sessionID).
						header("Content-Type", "application/JSON").
						body(categoryRule.toString()).
					when().
						post("/categoryRules");
				}
		
		// Get a valid categoryRule id to work with
		JsonPath categoryRuleJson = 
				given().
						header("X-session-ID", sessionID).
						header("Content-Type", "application/JSON").
				when().
				        get("/categoryRules").
				then().
						contentType(ContentType.JSON).
				extract().
						response().jsonPath();
		// Get the first categoryID
		validCategoryRuleID = categoryRuleJson.getString("id[0]");

		
	}
	
	@Test
	public void testGetCategoryRules() {
		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			get("/transactions?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules").
		then().
			assertThat().statusCode(401);
		
		// Valid session id in header
		Response getResponseHeader = given().
        		contentType("application/json").
        		header("X-session-ID", sessionID).
        	when().
        		get("/categoryRules");
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules?session_id=" + sessionID);
		
		// Check if both requests have been successful
		getResponseHeader.
		then().
			assertThat().statusCode(200);
		
		getResponseParameter.
		then().
			assertThat().statusCode(200);
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleListSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleListSchema.json"));
		
		// Generate category rule object arrays from the responses
		CategoryRule[] categoryRuleResponseHeader = getResponseHeader.as(CategoryRule[].class);
		CategoryRule[] categoryRuleResponseParameter = getResponseParameter.as(CategoryRule[].class);
		
		// Check if we get the same number of objects
		assertEquals(categoryRuleResponseHeader.length, categoryRuleResponseParameter.length);
		
		// Check if the two lists contain the same objects and the objects are valid
		for (int i = 0; i < categoryRuleResponseHeader.length; i++) {
			assertTrue(categoryRuleResponseHeader[i].equals(categoryRuleResponseParameter[i]));
			assertTrue(categoryRuleResponseHeader[i].validCategoryRule());
			assertTrue(categoryRuleResponseParameter[i].validCategoryRule());
		}
	}
	
	@Test
	public void testPostCategoryRule() {
		JSONObject categoryRule = new JSONObject().put("description", "University of Twente")
				.put("iBAN", "NL39RABO0300065264")
				.put("type", "deposit")
				.put("category_id", 0)
				.put("applyOnHistory", true);
		
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
			body(categoryRule.toString()).
		when().
			post("/categoryRules?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(401);
		
		// Valid session id as header
		Response postResponseHeader = given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules");
		
		// Valid session id as parameter
		Response postResponseParameter = given().
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules?session_id=" + sessionID);
		
		// Check if both requests have been successful
		postResponseHeader.
		then().
			assertThat().statusCode(201);
		
		postResponseParameter.
		then().
			assertThat().statusCode(201);
		
		// Generate category rule objects from the responses
		CategoryRule categoryRuleResponseHeader = postResponseHeader.as(CategoryRule.class);
		CategoryRule categoryRuleResponseParameter = postResponseParameter.as(CategoryRule.class);
		
		// Make sure the results follows the correct model specification
		postResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		postResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		
		// Make sure the two objects are valid
		assertTrue(categoryRuleResponseHeader.validCategoryRule());
		assertTrue(categoryRuleResponseParameter.validCategoryRule());
		
		// Make sure the two objects have the same body
		assertEquals(categoryRuleResponseHeader.toString(), categoryRuleResponseParameter.toString());
		
		// Make sure the id is properly updated
		assertTrue(categoryRuleResponseParameter.getId() == categoryRuleResponseHeader.getId() + 1);
		
		// Invalid input
		// null iBAN
		categoryRule.
					remove("iBAN");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(405);
		
		// null description
		categoryRule.
					put("iBAN", "NL39RABO0300065264").
					remove("description");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(405);
		
		// null type
		categoryRule.
			put("description", "University of Twente").
			remove("type");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(405);

	
		
		// wrong type
		categoryRule.
					put("type", "invalid_type");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule.toString()).
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(405);
		
		// no body
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			post("/categoryRules").
		then().
			assertThat().statusCode(405);
		
	}
	
	@Test
	public void testGetCategoryRule() {
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
		when().
			get("/categoryRules/" + validCategoryRuleID + "?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(401);
		
		// Valid session id as header
		Response getResponseHeader = given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules/" + validCategoryRuleID);
		
		// Valid session id as parameter
		Response getResponseParameter = given().
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules/" + validCategoryRuleID + "?session_id=" + sessionID);
		

		// Check if both requests have been successful
		getResponseHeader.
		then().
			assertThat().statusCode(200);
		
		getResponseParameter.
		then().
			assertThat().statusCode(200);
		
		// Generate category rule objects from the responses
		CategoryRule categoryRuleResponseHeader = getResponseHeader.as(CategoryRule.class);
		CategoryRule categoryRuleResponseParameter = getResponseParameter.as(CategoryRule.class);
		
		// Make sure the results follows the correct model specification
		getResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		getResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		
		// Make sure the two objects are valid
		assertTrue(categoryRuleResponseHeader.validCategoryRule());
		assertTrue(categoryRuleResponseParameter.validCategoryRule());
		
		// Make sure the two objects have the same body and id
		assertEquals(categoryRuleResponseHeader.toString(), categoryRuleResponseParameter.toString());
		assertEquals(categoryRuleResponseHeader.getId(), categoryRuleResponseParameter.getId());
		
		// ---- Non-existent ID ----
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			get("/categoryRules/" + -1).
		then().
			assertThat().statusCode(404);
	}
	
	@Test
	public void testPutCategoryRule() {
		JSONObject categoryRuleHeader = new JSONObject().put("description", "University of Twente")
				.put("iBAN", "NL39RABO0300065264")
				.put("type", "deposit")
				.put("category_id", 0);
		JSONObject categoryRuleParameter = new JSONObject().put("description", "University of Twente")
				.put("iBAN", "NL51ABNA0872781392")
				.put("type", "withdrawal")
				.put("category_id", 1);
		
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", Integer.parseInt(sessionID)-1).
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID + "?session_id=" + sessionID).
		then().
			assertThat().statusCode(401);
		
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(401);
		
		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(401);
		
		// Valid session id as header
		Response putResponseHeader = given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID);
		
		// Valid session id as parameter
		Response putResponseParameter = given().
			header("Content-Type", "application/JSON").
			body(categoryRuleParameter.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID + "?session_id=" + sessionID);
		
		// Check if both requests have been successful
		putResponseHeader.
		then().
			assertThat().statusCode(200);
		
		putResponseParameter.
		then().
			assertThat().statusCode(200);
		
		// Generate category rule objects from the responses
		CategoryRule categoryRuleResponseHeader = putResponseHeader.as(CategoryRule.class);
		CategoryRule categoryRuleResponseParameter = putResponseParameter.as(CategoryRule.class);
		
		// Make sure the results follows the correct model specification
		putResponseHeader.then().assertThat().
        		body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		putResponseParameter.then().assertThat().
			body(JsonSchemaValidator.matchesJsonSchemaInClasspath("CategoryRuleSchema.json"));
		
		// Make sure the two objects are valid
		assertTrue(categoryRuleResponseHeader.validCategoryRule());
		assertTrue(categoryRuleResponseParameter.validCategoryRule());
		
		// Check that the PUT was performed successfully
		assertEquals(categoryRuleResponseHeader.toStringData(), categoryRuleHeader.toString());
		assertEquals(categoryRuleResponseParameter.toStringData(), categoryRuleParameter.toString());
		
		// ---- Non-existent ID ----
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + -1).
		then().
			assertThat().statusCode(404);
		
		// Invalid input
		// null iBAN
		categoryRuleHeader.
					remove("iBAN");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(405);
		
		// null description
		categoryRuleHeader.
					put("iBAN", "NL39RABO0300065264").
					remove("description");
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(405);
		
		// null type
		categoryRuleHeader.
			put("description", "University of Twente").
			remove("type");
		
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(405);

	
		
		// wrong type
		categoryRuleHeader.
					put("type", "invalid_type");
		
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRuleHeader.toString()).
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(405);
		
		// no body
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
		when().
			put("/categoryRules/" + validCategoryRuleID).
		then().
			assertThat().statusCode(405);
		
	}
	
	@Test
	public void testDeleteCategoryRule() {
		JsonPath categoryRuleJsonPath = 
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
				when().
			        get("/categoryRules").
				then().
					contentType(ContentType.JSON).
				extract().
					response().jsonPath();
				// Get the last transaction id
				int listSize = categoryRuleJsonPath.getList("id").size();
				String lastCategoryRuleID = categoryRuleJsonPath.getList("id").get(listSize - 1).toString();
				
				CategoryRule categoryRule = given().
						header("X-session-ID", sessionID).
						header("Content-Type", "application/JSON").
					when().
				        get("/categoryRules/" + lastCategoryRuleID).as(CategoryRule.class);
				
				// ---- Headers ----
				// Mismatching session IDs
				given().
					header("Content-Type", "application/JSON").
					header("X-session-ID", Integer.parseInt(sessionID)-1).
				when().
					delete("/categoryRules/" + lastCategoryRuleID + "?session_id=" + sessionID).
				then().
					assertThat().statusCode(401);
				// No header
				given().
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID).
				then().
					assertThat().statusCode(401);
				
				// Invalid header
				given().
					header("X-session-ID", -1).
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID).
				then().
					assertThat().statusCode(401);
				
				// Valid header
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID).
				then().
					assertThat().statusCode(204);
				
				// Check that category rule was indeed deleted
				// Try another delete
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID).
				then().
					assertThat().statusCode(404);
				// Try a get request
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
				when().
					get("/categoryRules/" + lastCategoryRuleID).
				then().
					assertThat().statusCode(404);

				
				// Valid parameter session id
				
				// Post a new category rule ( should have the same id as the one we just deleted )
				JSONObject newCategoryRule = new JSONObject().put("description", "University of Twente")
						.put("iBAN", "NL39RABO0300065264")
						.put("type", "deposit")
						.put("category_id", 0)
						.put("applyOnHistory", false);
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(newCategoryRule.toString()).
				when().
					post("/categoryRules").
				then().
					assertThat().statusCode(201);
				
				// Perform the delete and check the response code
				given().
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID + "?session_id=" + sessionID).
				then().
					assertThat().statusCode(204);
				
				// Check that transaction was indeed deleted
				// Try another delete
				given().
					header("Content-Type", "application/JSON").
				when().
					delete("/categoryRules/" + lastCategoryRuleID  + "?session_id=" + sessionID).
				then().
					assertThat().statusCode(404);
				// Try a get request
				given().
					header("Content-Type", "application/JSON").
				when().
					get("/categoryRules/" + lastCategoryRuleID  + "?session_id=" + sessionID).
				then().
					assertThat().statusCode(404);
				
				// Add the object back
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(categoryRule.toString()).
				when().
					post("/categoryRules");
	}
	
	@Test
	public void testAutoCategorization() {
		
		JSONObject categoryRule1 = new JSONObject().put("description", "Testing Auto Categorization")
				.put("iBAN", "NL12ABNA0457688830")
				.put("type", "withdrawal")
				.put("category_id", validCategoryID)
				.put("applyOnHistory", false);
		
		// Post first category rule
		Response firstCategoryRuleResponse = given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule1.toString()).
		when().
			post("/categoryRules");
		
		int firstCategoryRuleId = firstCategoryRuleResponse.as(CategoryRule.class).getId();
		
		
		JSONObject t1 = new JSONObject().
				put("date", "2018-04-08T21:15:55.450Z").
				put("amount",201.03).
				put("externalIBAN", "NL12ABNA0457688830").
				put("description", "Testing Auto Categorization").
				put("type", "withdrawal");
		Response response =
			given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
				body(t1.toString()).
			when().
				post("/transactions");
		
		
		Transaction transactionResponse = response.as(Transaction.class);
		// Check if this transaction got the correct category ID assigned to it
		assertEquals((Integer)transactionResponse.CategoryID() , Integer.valueOf(validCategoryID));
		
		int secondCategoryID = Integer.valueOf(validCategoryID) + 1;
		
		JSONObject categoryRule2 = new JSONObject().put("description", "Testing Auto Categorization")
				.put("iBAN", "NL12ABNA0457688830")
				.put("type", "withdrawal")
				.put("category_id", secondCategoryID)
				.put("applyOnHistory", false);
		
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule2.toString()).
		when().
			post("/categoryRules");

		
		response =
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(t1.toString()).
				when().
					post("/transactions");
			
			
		transactionResponse = response.as(Transaction.class);
		// Check if this transaction got the correct category ID assigned to it
		assertEquals((Integer)transactionResponse.CategoryID(), Integer.valueOf(validCategoryID));

		// Remove first category rule
		given().
			header("Content-Type", "application/JSON").
		when().
			delete("/categoryRules/" + firstCategoryRuleId  + "?session_id=" + sessionID);
		
		// Check that the second category rule is applied
		response =
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(t1.toString()).
				when().
					post("/transactions");
		transactionResponse = response.as(Transaction.class);
		assertEquals((Integer)transactionResponse.CategoryID(), Integer.valueOf(secondCategoryID));
		
		// Add the first rule back
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule1.toString()).
		when().
			post("/categoryRules");
		
		int thridCategoryID = Integer.valueOf(validCategoryID) + 1;
		
		JSONObject categoryRule3 = new JSONObject().put("description", "Testing Auto Categorization But this description is longer so it won't be applied because there is one that is older and matches before this one :)")
				.put("iBAN", "NL12ABNA0457688830")
				.put("type", "withdrawal")
				.put("category_id", thridCategoryID)
				.put("applyOnHistory", false);
		
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule3.toString()).
		when().
			post("/categoryRules");
		
		response =
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(t1.toString()).
				when().
					post("/transactions");
		transactionResponse = response.as(Transaction.class);
		assertEquals((Integer)transactionResponse.CategoryID(), Integer.valueOf(validCategoryID));
	}
	
	@Test
	public void testApplyOnHistory() {
		JSONObject t1 = new JSONObject().
				put("date", "2018-04-08T21:15:55.450Z").
				put("amount",201.03).
				put("externalIBAN", "NL12ABNA0457688830").
				put("description", "Some random description designed to make this transaction uniquely selectable").
				put("type", "withdrawal");
		JSONObject t2 = new JSONObject().
				put("date", "2018-04-08T21:15:55.450Z").
				put("amount",201.03).
				put("externalIBAN", "NL12ABNA0457688830").
				put("description", "Another description").
				put("type", "withdrawal");
		int transactionResponse1 =
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(t1.toString()).
				when().
					post("/transactions").as(Transaction.class).getId();
		int transactionResponse2 =
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
					body(t2.toString()).
				when().
					post("/transactions").as(Transaction.class).getId();
		// Add some categories to test with
		JSONObject category = new JSONObject()
											.put("name", "test");
		int newCategory1 = given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
				body(category.toString()).
			when().
				post("/categories").as(nl.utwente.ing.model.Category.class).getId();
		
		int newCategory2 = given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
				body(category.toString()).
			when().
				post("/categories").as(nl.utwente.ing.model.Category.class).getId();
		
		int newCategory3 = given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
				body(category.toString()).
			when().
				post("/categories").as(nl.utwente.ing.model.Category.class).getId();
		
		// Targeting specific transaction
		JSONObject categoryRule1 = new JSONObject()
				.put("description", "Some random description designed to make this transaction uniquely selectable")
				.put("iBAN", "NL12ABNA0457688830")
				.put("type", "withdrawal")
				.put("category_id", newCategory1)
				.put("applyOnHistory", true);
		
		// Targeting specific transaction
		JSONObject categoryRule2 = new JSONObject()
				.put("description", "Another description")
				.put("iBAN", "NL12ABNA0457688830")
				.put("type", "withdrawal")
				.put("category_id", newCategory2)
				.put("applyOnHistory", true);
		
		// Targeting all transactions
		JSONObject categoryRule3 = new JSONObject()
				.put("description", "")
				.put("iBAN", "")
				.put("type", "withdrawal")
				.put("category_id", newCategory3)
				.put("applyOnHistory", true);
		// Targeting all transactions but sets the categoryID to a non existent category
		JSONObject categoryRule4 = new JSONObject()
				.put("description", "")
				.put("iBAN", "")
				.put("type", "withdrawal")
				.put("category_id", newCategory3 + 1)
				.put("applyOnHistory", true);
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule1.toString()).
		when().
			post("/categoryRules");
		
		Response response =  
			given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
			when().
				get("/transactions/" + transactionResponse1);
		
		assertEquals(response.as(Transaction.class).CategoryID(), newCategory1);
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule2.toString()).
		when().
			post("/categoryRules");
		
		response =  
				given().
					header("X-session-ID", sessionID).
					header("Content-Type", "application/JSON").
				when().
					get("/transactions/" + transactionResponse2);
		assertEquals(response.as(Transaction.class).CategoryID(), newCategory2);
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule3.toString()).
		when().
			post("/categoryRules");
		
		Transaction[] allTransactionsArray = given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
			when().
				get("/transactions").as(Transaction[].class);
		for (Transaction t : allTransactionsArray) {
			if (t.getType().toString().equals("withdrawal")) {
				assertEquals(t.CategoryID(), newCategory3);
			}
		}
		
		// Post a new categoryRule
		given().
			header("X-session-ID", sessionID).
			header("Content-Type", "application/JSON").
			body(categoryRule4.toString()).
		when().
			post("/categoryRules");
		
		allTransactionsArray = given().
				header("X-session-ID", sessionID).
				header("Content-Type", "application/JSON").
			when().
				get("/transactions").as(Transaction[].class);
		for (Transaction t : allTransactionsArray) {
			if (t.getType().toString().equals("withdrawal")) {
				System.out.println(t);
				assertEquals(t.CategoryID(), newCategory3);
			}
		}

	}
}
