package model;

public class Card{
	private String type;
	private int num;

	public Card(String type, int num) {
		super();
		this.type = type;
		this.num = num;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

}
