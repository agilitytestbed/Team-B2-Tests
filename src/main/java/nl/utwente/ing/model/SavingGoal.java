package nl.utwente.ing.model;

import org.json.JSONObject;

public class SavingGoal {
	private int id;
	private String name;
	private double goal;
	private double savePerMonth;
	private double minBalanceRequired;
	private double balance;
	
	public SavingGoal() {
		
	}

	
	public SavingGoal(int id, String name, double savePerMonth, double minBalanceRequired, double balance) {
		setId(id);
		setName(name);
		setGoal(goal);
		setSavePerMonth(savePerMonth);
		setMinBalanceRequired(minBalanceRequired);
		setBalance(balance);
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public double getGoal() {
		return goal;
	}


	public void setGoal(double goal) {
		this.goal = goal;
	}


	public double getSavePerMonth() {
		return savePerMonth;
	}


	public void setSavePerMonth(double savePerMonth) {
		this.savePerMonth = savePerMonth;
	}


	public double getMinBalanceRequired() {
		return minBalanceRequired;
	}


	public void setMinBalanceRequired(double minBalanceRequired) {
		this.minBalanceRequired = minBalanceRequired;
	}


	public double getBalance() {
		return balance;
	}


	public void setBalance(double balance) {
		this.balance = balance;
	}
	
	public boolean validSavingGoal() {
		if (name == null || goal <= 0 || savePerMonth < 0 || minBalanceRequired < 0 || balance < 0 || balance > goal) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean equalsData(SavingGoal sg) {
		if (sg.getBalance() == balance && sg.getGoal() == goal && sg.getMinBalanceRequired() == minBalanceRequired &&
				sg.getName().equals(name) && sg.getSavePerMonth() == savePerMonth) {
			return true;
		}
		return false;
	}
	
	public String toStringData() {
		JSONObject json = new JSONObject();
		json.put("name", name)
			.put("savePerMonth", savePerMonth)
			.put("minBalanceRequired", minBalanceRequired)
			.put("goal", goal)
			.put("balance", balance);
		return json.toString();
	}
}
