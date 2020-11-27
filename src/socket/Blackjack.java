package socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import model.Card;
import model.Toranpu;

@ServerEndpoint("/tp/bl")
public class Blackjack {

	//セッション保持 部屋番号割り当て
	private static HashMap<Session, Integer> sesMap = new HashMap<>();
	//部屋の情報
	private static ArrayList<Task> taskList = new ArrayList<>();

	//スレッドプール
	ExecutorService exec = Executors.newSingleThreadExecutor();

	@OnOpen
	//セッションが開始されたとき
	public void onOpen(Session pSession) throws IOException {
		pSession.getBasicRemote().sendText("system,dolphin");
	}

	@OnMessage
	//メッセージを受信したとき voidをStringにするとメッセージを返せる
	public void onMessage(final String pText, Session mSession) throws IOException {
		if ("dolphin".equals(pText)) {
			//httpセッション情報 のuseridをとりだしてSessionとStringで保存
			//useridがあるか確認して、存在していたらerrorを返す。そしてreturn

			System.out.println("connect! ID:" + mSession.getId());
			if (taskList.size() == 5 && !taskList.get(4).possible()) {
				mSession.getBasicRemote().sendText("system,error");
				return;
			}
			if (taskList.size() == 0) {
				taskList.add(new Task());
			}
			//部屋番号割り当て
			for (int i = 0; i < taskList.size(); i++) {
				if (taskList.get(i).possible()) {
					System.out.println("ID:" + mSession.getId() + "_部屋番号" + i);
					sesMap.put(mSession, i);

					taskList.get(i).joinPlayer(mSession);

					if (taskList.get(i).runState() && taskList.get(i).onePlayer()) {
						System.out.println("スタートさせます！");
						exec.submit(taskList.get(i));
					}
				} else {
					taskList.add(new Task());
				}
			}
		} else {
			taskList.get(sesMap.get(mSession)).msg(mSession, pText);
		}
	}

	@OnError
	//エラーが発生したとき
	public void onError(Throwable pError, Session eSession) {
		System.out.println("ID:" + eSession.getId() + "でエラー発生");
		taskList.get(sesMap.get(eSession)).exit(eSession);
//		try {
//			eSession.getBasicRemote().sendText("system,error");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	@OnClose
	//セッションが閉じられたとき
	public void onClose(Session pSession) {
		try {
			System.out.println("ID:" + pSession.getId() + "のセッションを終了！");
			taskList.get(sesMap.get(pSession)).exit(pSession);
			System.out.println("remove完了");
			sesMap.remove(pSession);
		} catch (Exception e) {
			System.out.println("-----RemoveError-----");
		}
	}
}

//-----------------------------------------------------------------------
//ブラックジャックの部屋を管理するクラス
class Task extends Thread {
	//セッション情報 順番0~4
	private HashMap<Session, Integer> playerList = new HashMap<>();
	private boolean st = true;
	private boolean[] playerwait = new boolean[5];
	//ゲームに参加しているプレイヤー
	private Session[] playnow;
	int playnow_index = 0;
	//プレイヤーの処理情報を持つ
	private String[] processing = new String[5];

	boolean pwait = true;
	private ArrayList<Card> h, p1, p2, p3, p4, p5;

	private Toranpu trnp = new Toranpu();
	private HashMap<Session, Integer> plList = null;
	private Timer playerTimer = new Timer();
	private TimerTask waitTask = null;
	private Timer startTimer = new Timer();

	public void run() {

		try {
			//誰かプレイヤーがいたら実行
			if (playerList.size() > 0) {
				//力技
				if (st) {
					System.out.println("スタート！！---------------------------------------");
					startEvent();
				}
			}
		} catch (IOException e) {
			System.out.println("メッセージ送信時エラー");
			e.printStackTrace();
		}

	}

	//スタートのメソッド
	private void startEvent() throws IOException {
		System.out.println("マッチ開始!!!!!!!!!!");
		st = false;

		h = new ArrayList<>();
		p1 = new ArrayList<>();
		p2 = new ArrayList<>();
		p3 = new ArrayList<>();
		p4 = new ArrayList<>();
		p5 = new ArrayList<>();
		playnow_index = 0;
		//初期化してシャッフル
		trnp.initialization();
		trnp.shuffle();
		//ゲーム開始前に参加しているプレイヤーを取得
		plList = playerList;
		playnow = new Session[plList.size()];
		int count = 0;
		for (Session ses : plList.keySet()) {
			playnow[count++] = ses;
		}

		//ランダムに並び替え
		Random r = new Random(System.currentTimeMillis());
		int rand;
		Session temp;
		for (int i = 0; i < playnow.length; i++) {
			rand = r.nextInt(playnow.length);
			temp = playnow[i];
			playnow[i] = playnow[rand];
			playnow[rand] = temp;
		}
		for (Session ses : playnow) {
			if (sesCheck(ses))
				ses.getBasicRemote().sendText("system,start");
		}

		//ホストのドローと全員に通知
		Card c = trnp.draw(true);
		h.add(c);
		for (Session s : playnow) {
			if (sesCheck(s)) {
				s.getBasicRemote()
						.sendText("host,draw," + c.getType() + "," + Integer.toString(c.getNum()));

				s.getBasicRemote().sendText("host,sum," + sum(h));
			}
		}
		c = trnp.draw(true);
		h.add(c);
		for (Session s : playnow) {
			if (sesCheck(s)) {
				s.getBasicRemote().sendText("host,draw,back");
			}
		}

		int num = 0;
		ArrayList<Card> ac = null;
		//全員に２枚ずつ配る
		for (Session ses : playnow) {
			for (int i = 0; i < 2; i++) {
				draw(ses, trnp.draw(true), plList, "draw");
				try {
					sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			num = plList.get(ses);
			switch (num) {
			case 0:
				ac = p1;
				break;
			case 1:
				ac = p2;
				break;
			case 2:
				ac = p3;
				break;
			case 3:
				ac = p4;
				break;
			case 4:
				ac = p5;
				break;
			}
			if (sum(ac) == 21) {
				for (Session s : playnow) {
					if (s == ses) {
						ses.getBasicRemote().sendText("my,blackjack");
						continue;
					}
					if (sesCheck(s))
						s.getBasicRemote().sendText(playerList.get(ses) + ",blackjack");
				}
			}
		}
		st = false;
		playerwait[playerList.get(playnow[playnow_index])] = true;
		timerSet(playerList.get(playnow[playnow_index]));

	}

	//全員終了したら呼ぶ ホストのカードをオープンし、ホストのカードを引く
	private void endEvent(){
		try {
			System.out.println("end実行！！！");
			//ホストの裏返されていたカードを表に
			for (Session ses : playnow) {
				if (sesCheck(ses)) {
					ses.getBasicRemote().sendText("host,open," + h.get(1).getType() + "," + h.get(1).getNum());
					ses.getBasicRemote().sendText("host,sum," + sum(h));
				}
			}

			sleep(2000);

			//ホストのカードを引いて通知する処理
			System.out.println("ホストのカード合計：" + sum(h));
			Card c;
			while (sum(h) <= 16) {
				c = trnp.draw(true);
				h.add(c);
				for (Session s : playnow) {
					if (sesCheck(s)) {
						s.getBasicRemote().sendText("host,draw," + c.getType() + "," + c.getNum());
						s.getBasicRemote().sendText("host,sum," + sum(h));
					}
				}
				System.out.println("ホストの合計:" + sum(h));
				sleep(2000);
			}
			int p = 0;
			for (Session s : playnow) {
				if (sesCheck(s)) {
					s.getBasicRemote().sendText("host,end");
				}
				if (sesCheck(s))
					p = playerList.get(s);
				ArrayList<Card> ca;
				switch (p) {
				case 0:
					ca = p1;
					break;
				case 1:
					ca = p2;
					break;
				case 2:
					ca = p3;
					break;
				case 3:
					ca = p4;
					break;
				case 4:
					ca = p5;
					break;
				default:
					ca = null;
					break;
				}
				if (sum(ca) <= 21) {
					if (sum(h) > 21) {
						if (sesCheck(s))
							s.getBasicRemote().sendText("my,win");
					} else if ((21 - sum(ca)) < (21 - sum(h))) {
						if (sesCheck(s))
							s.getBasicRemote().sendText("my,win");
					} else if ((21 - sum(ca)) == (21 - sum(h))) {
						if ((21 - sum(ca)) == 0 && (21 - sum(h)) == 0) {
							if (h.size() == 2 && ca.size() > 2) {
								if (sesCheck(s))
									s.getBasicRemote().sendText("my,lose");
							} else if (h.size() > 2 && ca.size() == 2) {
								if (sesCheck(s))
									s.getBasicRemote().sendText("my,win");
							} else {
								if (sesCheck(s))
									s.getBasicRemote().sendText("my,tie");
							}
						} else {
							if (sesCheck(s))
								s.getBasicRemote().sendText("my,tie");
						}
					} else {
						if (sesCheck(s))
							s.getBasicRemote().sendText("my,lose");
					}
				} else {
					if (sesCheck(s))
						s.getBasicRemote().sendText("my,lose");
				}
			}

			startTimer.cancel();
			startTimer = new Timer(true);
			startTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (!noPlayer()) {
						System.out.println("再スタート");
						try {
							startEvent();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("システム終了");
						st = true;
					}
				}

			}, 5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//プレイヤータイマーをセットするメソッド
	private void timerSet(int num) {
		System.out.println("プレイヤータイマーセット");
		System.out.println("プレイヤー" + num + "の動作受付中");
		try {
			if (sesCheck(playnow[playnow_index])) {
				playnow[playnow_index].getBasicRemote().sendText("my,turn");
			}
			for (Session ses : playnow) {
				if (ses == playnow[playnow_index]) {
					continue;
				}
				if (sesCheck(ses)) {
					ses.getBasicRemote().sendText(playerList.get(playnow[playnow_index]) + ",turn");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//30秒後に待つのをやめる
		waitTask = new TimerTask() {
			@Override
			public void run() {
				try {
					if (sesCheck(playnow[playnow_index]))
						playnow[playnow_index].getBasicRemote().sendText("my,timeout");
					System.out.println("プレイヤー" + num + "の動作受付終了");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				playerwait[num] = false;
				pwait = true;
				if (playnow.length > playnow_index + 1) {
					System.out.println("手番を次のプレイヤーに移します");
					System.out.println(
							playerList.get(playnow[playnow_index]) + "→" + playerList.get(playnow[playnow_index + 1]));
					try {
						if (sesCheck(playnow[playnow_index])) {
							playnow[playnow_index].getBasicRemote().sendText("my,timeout");
						}
						if (playnow.length > playnow_index + 1 && playerList.get(playnow[playnow_index + 1]) != null) {
							playerwait[playerList.get(playnow[++playnow_index])] = true;

							timerSet(playerList.get(playnow[playnow_index]));
						} else {
							for (int i = playnow_index; i < playnow.length; i++) {
								if (playerList.get(playnow[i]) != null) {
									timerSet(playerList.get(playnow[i]));
									playnow_index = i;
									break;
								}
							}
							System.out.println("全員終了");
							processing[num] = null;
							endEvent();
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("全員終了");
					processing[num] = null;
					endEvent();
				}
			}
		};

		playerTimer.cancel();
		playerTimer = new Timer(true);
		playerTimer.schedule(waitTask, 30000);
	}

	//だいたいの処理
	private void tpmain(int num, Toranpu trnp, Session ses,
			HashMap<Session, Integer> players) throws IOException {

		if (processing[num] != null) {
			int sum = 0;
			//メッセージがあったらメッセージごとに処理を変える
			switch (processing[num]) {
			case "draw"://ドロー
				System.out.println("プレイヤー" + num + ":draw");
				switch (num) {
				case 0:
					if (sum(p1) < 21)
						draw(ses, trnp.draw(true), players, processing[num]);
					sum = sum(p1);
					break;
				case 1:
					if (sum(p2) < 21)
						draw(ses, trnp.draw(true), players, processing[num]);
					sum = sum(p2);
					break;
				case 2:
					if (sum(p3) < 21)
						draw(ses, trnp.draw(true), players, processing[num]);
					sum = sum(p3);
					break;
				case 3:
					if (sum(p4) < 21)
						draw(ses, trnp.draw(true), players, processing[num]);
					sum = sum(p4);
					break;
				case 4:
					if (sum(p5) < 21)
						draw(ses, trnp.draw(true), players, processing[num]);
					sum = sum(p5);
					break;
				}
				System.out.println("プレイヤータイマーセット");
				timerSet(num);
				if (sum == 21) {
					ses.getBasicRemote().sendText("my,end");
				} else if (sum > 21) {
					ses.getBasicRemote().sendText("my,burst");
					for (Session s : playnow) {
						if (plList.get(s) == num) {
							continue;
						}
						s.getBasicRemote().sendText(num + ",burst");
					}
					processing[num] = "end";
					tpmain(num, trnp, playnow[playnow_index], plList);
				} else {
					processing[num] = null;
				}
				break;
			case "end"://エンド
				System.out.println("プレイヤー" + num + ":end");
				playerwait[num] = false;
				if (playnow.length > playnow_index + 1) {
					System.out.println("手番を次のプレイヤーに移します");
					System.out.println(
							playerList.get(playnow[playnow_index]) + "→" + playerList.get(playnow[playnow_index + 1]));
					try {
						if (sesCheck(playnow[playnow_index])) {
							playnow[playnow_index].getBasicRemote().sendText("my,timeout");
						}
						if (sesCheck(playnow[playnow_index])) {
							playerwait[playerList.get(playnow[++playnow_index])] = true;
						}
						timerSet(playerList.get(playnow[playnow_index]));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					playerTimer.cancel();
					System.out.println("全員終了");
					processing[num] = null;
					endEvent();
				}
				break;
			case "surrender":
				System.out.println("プレイヤー" + num + ":surrender");
				ArrayList<Card> ac = null;
				switch (playerList.get(ses)) {
				case 0:
					ac = p1;
					break;
				case 1:
					ac = p2;
					break;
				case 2:
					ac = p3;
					break;
				case 3:
					ac = p4;
					break;
				case 4:
					ac = p5;
					break;

				}
				if (ac.size() > 2) {
					System.out.println("3枚以上のカードを所持していたのでダブルをキャンセル");
					break;
				}
				for (Session s : playnow) {
					if (playerList.get(s) == num) {
						s.getBasicRemote().sendText("my,surrender");
						continue;
					}
					if (sesCheck(s))
						s.getBasicRemote().sendText(num + ",surrender");
				}
				switch (num) {
				case 0:
					p1.add(new Card("none", 21));
					p1.add(new Card("none", 21));
					break;
				case 1:
					p2.add(new Card("none", 21));
					p2.add(new Card("none", 21));
					break;
				case 2:
					p3.add(new Card("none", 21));
					p3.add(new Card("none", 21));
					break;
				case 3:
					p4.add(new Card("none", 21));
					p4.add(new Card("none", 21));
					break;
				case 4:
					p5.add(new Card("none", 21));
					p5.add(new Card("none", 21));
					break;
				}
				playerwait[num] = false;
				if (playnow.length > playnow_index + 1) {
					System.out.println("手番を次のプレイヤーに移します");
					System.out.println(
							playerList.get(playnow[playnow_index]) + "→" + playerList.get(playnow[playnow_index + 1]));
					try {
						if (sesCheck(playnow[playnow_index])) {
							playnow[playnow_index].getBasicRemote().sendText("my,timeout");
						}
						if (sesCheck(playnow[playnow_index])) {
							playerwait[playerList.get(playnow[++playnow_index])] = true;
						}
						timerSet(playerList.get(playnow[playnow_index]));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					playerTimer.cancel();
					System.out.println("全員終了");
					processing[num] = null;
					endEvent();
				}
				break;
			case "double":
				System.out.println("プレイヤー" + num + ":double");

				ArrayList<Card> ac1 = null;
				switch (playerList.get(ses)) {
				case 0:
					ac1 = p1;
					break;
				case 1:
					ac1 = p2;
					break;
				case 2:
					ac1 = p3;
					break;
				case 3:
					ac1 = p4;
					break;
				case 4:
					ac1 = p5;
					break;

				}
				if (ac1.size() > 2) {
					System.out.println("3枚以上のカードを所持していたのでダブルをキャンセル");
					break;
				}
				System.out.println("ダブルの処理を実行！！！！");
				playerwait[num] = false;

				if (sum(ac1) < 21)
					draw(ses, trnp.draw(true), players, processing[num]);

				if (sum(ac1) == 21) {
					ses.getBasicRemote().sendText("my,end");
				} else if (sum(ac1) > 21) {
					ses.getBasicRemote().sendText("my,burst");
					for (Session s : playnow) {
						if (plList.get(s) == num) {
							continue;
						}
						s.getBasicRemote().sendText(num + ",burst");
					}
				}

				if (playnow.length > playnow_index + 1) {
					System.out.println("手番を次のプレイヤーに移します");
					System.out.println(
							playerList.get(playnow[playnow_index]) + "→" + playerList.get(playnow[playnow_index + 1]));
					try {
						if (sesCheck(playnow[playnow_index])) {
							playnow[playnow_index].getBasicRemote().sendText("my,timeout");
						}
						if (sesCheck(playnow[playnow_index])) {
							playerwait[playerList.get(playnow[++playnow_index])] = true;
						}
						timerSet(playerList.get(playnow[playnow_index]));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					playerTimer.cancel();
					System.out.println("全員終了");
					processing[num] = null;
					endEvent();
				}
				break;
			}
		}
	}

	//カードの合計を計算
	private int sum(ArrayList<Card> c) {
		boolean ace = false;
		int sum = 0;

		for (Card num : c) {
			if (num.getNum() == 1) {
				ace = true;
			}
			sum += num.getNum() > 10 ? 10 : num.getNum();
		}
		if (ace) {
			sum = sum <= 11 ? sum + 10 : sum;
		}

		return sum;
	}

	//カードを引いてその人とほかのプレイヤーに通知する
	public void draw(Session ses, Card c, HashMap<Session, Integer> players, String move) {
		int sum = 0;
		System.out.println("プレイヤー" + playerList.get(ses) + "_" + c.getType() + ":" + c.getNum());
		try {
			if (sesCheck(ses)) {
				switch (players.get(ses)) {
				case 0:
					p1.add(c);
					sum = sum(p1);
					ses.getBasicRemote().sendText("my,sum," + sum);
					break;
				case 1:
					p2.add(c);
					sum = sum(p2);
					ses.getBasicRemote().sendText("my,sum," + sum);
					break;
				case 2:
					p3.add(c);
					sum = sum(p3);
					ses.getBasicRemote().sendText("my,sum," + sum);
					break;
				case 3:
					p4.add(c);
					sum = sum(p4);
					ses.getBasicRemote().sendText("my,sum," + sum);
					break;
				case 4:
					p5.add(c);
					sum = sum(p5);
					ses.getBasicRemote().sendText("my,sum," + sum);
					break;
				}
				if (sum > 0) {
					ses.getBasicRemote().sendText("my," + move + "," + c.getType() + "," + c.getNum());
				}
			}
			String num = Integer.toString(playerList.get(ses));
			for (Session s : players.keySet()) {
				if (ses.getId() == s.getId()) {
					continue;
				}
				if (sesCheck(s)) {
					s.getBasicRemote().sendText(num + "," + move + "," + c.getType() + "," + c.getNum());
					s.getBasicRemote().sendText(num + ",sum," + sum);
				}
			}
		} catch (IOException e) {
			System.out.println("ドロー通知エラー");
			e.printStackTrace();
		}
	}

	//プレイヤーの処理を受けるメソッド
	public void msg(Session ses, String str) {
		if (playerwait[playerList.get(ses)]
				&& (str.equals("draw") || str.equals("end") || str.equals("double") || str.equals("surrender"))) {
			processing[playerList.get(ses)] = str;
			try {
				tpmain(playerList.get(ses), trnp, playnow[playnow_index], plList);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//参加させるメソッド
	public void joinPlayer(Session ses) {
		try {
			int num = 0;
			label: for (int i = 0; i < 5; i++) {
				num = i;
				for (int p : playerList.values()) {
					if (i == p) {
						continue label;
					}
				}
				break;
			}
			System.out.println("ID:" + ses.getId() + "はプレイヤー" + num);
			playerList.put(ses, num);
			if (sesCheck(ses)) {
				ses.getBasicRemote().sendText("my,num," + Integer.toString(num));
			}
		} catch (IOException e) {
			System.out.println("joinメッセージ送信エラー");
			e.printStackTrace();
		}
	}

	//参加可能かを返す 5人まで
	public boolean possible() {
		return playerList.size() < 5 ? true : false;
	}

	//退室するメソッド
	public void exit(Session ses) {
		System.out.println("exit実行！！");
		playerList.remove(ses);
	}

	//一人しかいない時にtrueを返す
	public boolean onePlayer() {
		System.out.println("size:" + playerList.size());
		return (playerList.size() == 1);
	}

	//誰もいないときにtrueを返す
	public boolean noPlayer() {
		return (playerList.size() == 0);
	}

	//セッションが存在するかチェック
	private boolean sesCheck(Session ses) {
		for (Session s : playerList.keySet()) {
			if (s == ses)
				return true;
		}
		return false;
	}

	public boolean runState() {
		return st;
	}
}
