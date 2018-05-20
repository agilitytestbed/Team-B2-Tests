package nl.utwente.ing.model;

import org.json.JSONObject;

public class CategoryRule {
	private int id;
	private String description;
	private String iBAN;
	private TransactionType type;
	private int category_id;
	private boolean applyOnHistory;
	
	public CategoryRule() {
		
	}
	
	
	
	public CategoryRule(int id, String description, String iBAN, String type, int category_id,
			boolean applyOnHistory) {
		this.id = id;
		this.description = description;
		this.iBAN = iBAN;
		this.type = TransactionType.valueOf(type);
		this.category_id = category_id;
		this.applyOnHistory = applyOnHistory;
	}



	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getiBAN() {
		return iBAN;
	}

	public void setiBAN(String iBAN) {
		this.iBAN = iBAN;
	}

	public TransactionType getType() {
		return type;
	}

	public void setType(TransactionType type) {
		this.type = type;
	}

	public int getCategory_id() {
		return category_id;
	}

	public void setCategory_id(int category_id) {
		this.category_id = category_id;
	}

	public boolean isApplyOnHistory() {
		return applyOnHistory;
	}

	public void setApplyOnHistory(boolean applyOnHistory) {
		this.applyOnHistory = applyOnHistory;
	}
	
	public boolean validCategoryRule() {
		if (description == null || iBAN == null || type == null) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("description", description)
			.put("iBAN", iBAN)
			.put("type", type.toString())
			.put("category_id", category_id)
			.put("applyOnHistory", applyOnHistory);
		return json.toString();
	}
	
	public boolean equals(CategoryRule cr) {
		if (id == cr.getId() && description.equals(cr.getDescription())
			&& iBAN.equals(cr.getiBAN()) && type.equals(cr.getType()) && 
			category_id == cr.getCategory_id() && applyOnHistory == cr.isApplyOnHistory()) {
			
			return true;
		} else {
			return false;
		}
	}
	
	public String toStringData() {
		JSONObject json = new JSONObject();
		json.put("description", description)
			.put("iBAN", iBAN)
			.put("type", type.toString())
			.put("category_id", category_id);
		return json.toString();
	}
}
