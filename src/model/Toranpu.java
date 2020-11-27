package model;

import java.util.Random;

public class Toranpu {
	Card card[] = new Card[53];
	int index;

	public Toranpu() {
		initialization();
		index = 0;
	}

	//初期化処理
	public void initialization() {
		index = 0;
		for (int i = 0; i < 13; i++) {
			card[i] = new Card("spade", i + 1);
		}
		for (int i = 13; i < 26; i++) {
			card[i] = new Card("clover", i + 1 - 13);
		}
		for (int i = 26; i < 39; i++) {
			card[i] = new Card("heart", i + 1 - 26);
		}
		for (int i = 39; i < 52; i++) {
			card[i] = new Card("diamond", i + 1 - 39);
		}
		card[52] = new Card("joker", 0);
	}

	//シャッフル
	public void shuffle() {
		Card temp;
		Random r = new Random(System.currentTimeMillis());
		int rand;
		for (int i = index; i < 53; i++) {
			rand = r.nextInt(53 - index) + index;
			temp = card[i];
			card[i] = card[rand];
			card[rand] = temp;
		}
	}

	//カードを一枚引く
	public Card draw(boolean notjoker) {
		Card c;
		if (notjoker) {
			if ("joker".equals(card[index].getType())) {
				c = card[++index];
				index++;
			} else {
				c = card[index++];
			}
		} else {
			c = card[index++];
		}
		return c;
	}
}
