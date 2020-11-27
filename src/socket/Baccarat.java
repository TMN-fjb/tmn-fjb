package socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import model.Card;
import model.Toranpu;

@ServerEndpoint("/tp/bc")
public class Baccarat {
	ExecutorService exec = Executors.newSingleThreadExecutor();
	private static BcTask task = new BcTask();

	@OnOpen
	public void onOpen(Session ses) {
		task.join(ses);
		System.out.println("task.size:" + task.size());
		if (task.size() == 1 && task.state()) {
			exec.submit(task);
		}
	}

	@OnMessage
	public void onMessage(final String txt, Session ses) {

	}

	@OnError
	public void onError(Throwable pError, Session ses) {
		task.exit(ses);
	}

	@OnClose
	public void onClose(Session ses) {
		task.exit(ses);
	}
}

class BcTask extends Thread {
	private Toranpu trnp = new Toranpu();
	private ArrayList<Session> plset = new ArrayList<>();
	private HashMap<Session, Integer> request = new HashMap<>();
	private boolean st = true, permission = false;
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	//プレイヤーを参加させる
	public void join(Session ses) {
		plset.add(ses);
	}

	//プレイヤーを退室
	public void exit(Session ses) {
		plset.remove(ses);
	}

	//スタートしていいか
	public boolean state() {
		return st;
	}

	@Override
	public void run() {
		st = false;
		System.out.println("バカラ開始----------------------------------------");
		startE();
	}

	//プレイヤーかバンカーか引き分けに賭ける
	public void request(String p, int k) {

	}

	//ゲームを始める
	private void startE() {
		broadcast(plset, "system,start");
		request = new HashMap<>();
		trnp.initialization();
		trnp.shuffle();

		//掛け金とどれにかけるかを設定する時間を設ける
		permission = true;
		service.schedule(() -> {
			try {
				mainE(plset);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, 5, TimeUnit.SECONDS);

	}

	//ゲームのメイン処理
	private void mainE(ArrayList<Session> nowset) throws InterruptedException {

		permission = false;

		broadcast(nowset, "system,stop");

		int banker = 0, player = 0;
		broadcast(nowset, "system,draw");
		sleep(1000);

		player = hit(nowset, trnp, "player", player);

		sleep(1000);

		banker = hit(nowset, trnp, "banker", banker);

		sleep(1000);

		player = hit(nowset, trnp, "player", player);

		sleep(1000);

		banker = hit(nowset, trnp, "banker", banker);

		sleep(2000);

		System.out.println("player:" + player + " banker:" + banker);
		if (player < 8 && banker < 8) {

			//プレイヤーは5以下の時にヒット
			if (player <= 5) {
				player = hit(nowset, trnp, "player", player);
			}

			sleep(1000);

			if (banker <= 2) {
				banker = hit(nowset, trnp, "banker", banker);
			} else {
				switch (banker) {
				case 3:
					if (player <= 7 || player == 9) {
						banker = hit(nowset, trnp, "banker", banker);
					}
					break;
				case 4:
					if (player >= 2 && player <= 7) {
						banker = hit(nowset, trnp, "banker", banker);
					}
					break;
				case 5:
					if (player >= 4 && player <= 7) {
						banker = hit(nowset, trnp, "banker", banker);
					}
					break;
				case 6:
					if (player == 6 || player == 7) {
						banker = hit(nowset, trnp, "banker", banker);
					}
					break;
				}
			}
		}

		sleep(2000);

		if (banker > player) {
			broadcast(nowset, "system,banker");
		} else if (banker < player) {
			broadcast(nowset, "system,player");
		} else {
			broadcast(nowset, "system,tie");
		}

		if (plset.size() > 0) {
			System.out.println("ループします");
			service.schedule(() -> {
				startE();
			}, 3, TimeUnit.SECONDS);
		} else {
			st = true;
			System.out.println("バカラ終了----------------------------------------");
		}
	}

	private int hit(ArrayList<Session> n, Toranpu t, String p, int temp) {
		/**
		 * ヒットして手札に加えたときの値を返す
		 * @param t トランプクラスのインスタンス
		 * @param p "player" or "banker"
		 * @param temp int 型の変数
		 *
		 */
		Card c;
		c = trnp.draw(true);
		temp = (temp + (c.getNum() >= 10 ? 0 : c.getNum())) % 10;
		broadcast(n, p + ",draw," + c.getType() + "," + c.getNum(), p + ",sum," + temp);
		return temp;
	}

	private void broadcast(ArrayList<Session> n, String str) {
		try {
			for (Session s : n) {
				if (sesCheck(s))
					s.getBasicRemote().sendText(str);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void broadcast(ArrayList<Session> n, String str1, String str2) {
		try {
			for (Session s : n) {
				if (sesCheck(s)) {
					s.getBasicRemote().sendText(str1);
					s.getBasicRemote().sendText(str2);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean sesCheck(Session s) {
		for (Session ses : plset) {
			if (s == ses)
				return true;
		}
		return false;
	}

	public void msg(Session ses, Integer num) {
		if (permission) {
			request.put(ses, num);
		}
	}

	public int size() {
		return plset.size();
	}
}
