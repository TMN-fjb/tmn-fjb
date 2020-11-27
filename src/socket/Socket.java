package socket;

import java.io.IOException;
import java.util.HashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/Socket")
public class Socket {

	//セッション保持
	private static HashMap<Session,Task> sesMap = new HashMap<>();

	@OnOpen
	//セッションが開始されたとき
	public void onOpen(Session pSession) {
		System.out.println("connect! ID:" + pSession.getId());
		sesMap.put(pSession,new Task(pSession));
	}

	@OnMessage
	//メッセージを受信したとき voidをStringにするとメッセージを返せる
	public void onMessage(final String pText,Session mSession) {
		System.out.println("ID:"+mSession.getId()+"の「"+pText+"」をブロードキャスト");
		broadcast(mSession.getId()+":"+pText);
	}

	@OnError
	//エラーが発生したとき
	public void onError(Throwable pError) {
		System.out.println("error");
	}

	@OnClose
	//セッションが閉じられたとき
	public void onClose(Session pSession) {
		try {
			System.out.println("ID:"+pSession.getId()+"のセッションを終了！");
			sesMap.remove(pSession);
		} catch (Exception e) {
			System.out.println("-----RemoveError-----");
		}
	}

	//ブロードキャスト
	private static void broadcast(String mess) {
		for(Session ses:sesMap.keySet()) {
			try {
				System.out.println("送信！！");
				ses.getBasicRemote().sendText(mess);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("送信完了");
	}

	class Task extends Thread {

		public Task(Session ses) {
			run(ses);
		}

		public void run(Session ses) {
//			for (int i = 0; i < 5; i++) {
//				try {
//					sleep(3000);
//					ses.getAsyncRemote().sendText("サーバから送信！！");
//					System.out.println("ID:"+ses.getId()+"へ送信後3秒間待ちます");
//				} catch (InterruptedException e) {
//					System.out.println("-----InterruptedException-----");
//					e.printStackTrace();
//				}
//			}
			System.out.println("ID:"+ses.getId()+" 送信終了！！！");
		}
	}
}
