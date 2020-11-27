var connect, cutting;
$(function () {
    let act = "";
    let notification;
    $("#cnt").draggable();
    let ws = null;
    let mynum;
    let onOpen = function () {
        $("#system").text("マッチング中...しばらくお待ちください");
        // ブラウザが通知をサポートしているか確認する
        if (!("Notification" in window)) {
            alert("このブラウザはシステム通知をサポートしていません");
        }

        // すでに通知の許可を得ているか確認する
        else if (Notification.permission === "granted") {
            // 許可を得ている場合は、通知を作成する
            notification = new Notification("ブラックジャック開始");
        }

        // 許可を得ていない場合は、ユーザに許可を求めなければならない
        else if (Notification.permission !== 'denied') {
            Notification.requestPermission(function (permission) {
                // ユーザが許可した場合は、通知を作成する
                if (permission === "granted") {
                    notification = new Notification("ブラックジャック開始");
                }
            });
        }
    };
    let onMessage = function (e) {
        console.log(e.data);
        var str = (e.data);
        str = str.split(",");

        switch (str[0]) {
            case "my":
                switch (str[1]) {
                    case "num":
                        notification = new Notification("あなたはプレイヤー" + str[2]);
                        mynum = str[2];
                        break;
                    case "draw":
                        $("<img>", {
                            class: "trnp",
                            src: "../img/" + str[2] + str[3] + ".jpg"
                        }).appendTo("#my>.card").hide().fadeIn(1000).css("transform", "rotateY(360deg)");
                        break;
                    case "sum":
                        $("#my>.sum").text(str[2]);
                        break;
                    case "turn":
                        $(".play").css("background", "#0f03");
                        $("#my").css("background", "#f003");
                        $("#system").text("あなたのターンです");
                        notification = new Notification("あなたのターンです");
                        $("#draw").click(function () {
                            console.log("ドロー");
                            ws.send("draw");
                            $("#draw").off();
                            $("#end").off();
                            $("#surrender").off();
                            $("#double").off();
                        });
                        $("#end").click(function () {
                            console.log("エンド");
                            ws.send("end");
                            $("#draw").off();
                            $("#end").off();
                            $("#surrender").off();
                            $("#double").off();
                        });
                        $("#surrender").click(function () {
                            console.log("サレンダー");
                            ws.send("surrender");
                        });
                        $("#double").click(function () {
                            console.log("ダブル");
                            ws.send("double");
                        });
                        break;
                    case "timeout":
                        notification = new Notification("タイムアウトです");
                    case "end":
                        $("#system").text("他のプレイヤーの番です");
                        ws.send("end");
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        break;
                    case "burst":
                        $("#my").addClass("burst");
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        break;
                    case "double":
                        console.log("ダブル実行");
                        $("<img>", {
                            class: "trnp double",
                            src: "../img/" + str[2] + str[3] + ".jpg"
                        }).appendTo("#my>.card").hide().fadeIn(1000).css("transform", "rotateY(360deg)");
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        break;
                    case "win":
                        $("#system").text("勝ちです");
                        break;
                    case "tie":
                        $("#system").text("引き分けです");
                        break;
                    case "lose":
                        $("#system").text("負けです");
                        break;
                    case "blackjack":
                        $("#my").addClass("blackjack");
                        ws.send("end");
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        break;
                    case "surrender":
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        break;
                }
                break;
            case "0":
                play(str);
                break;
            case "1":
                play(str);
                break;
            case "2":
                play(str);
                break;
            case "3":
                play(str);
                break;
            case "4":
                play(str);
                break;
            case "host":
                play(str);
                break;
            case "system":
                switch (str[1]) {
                    case "start":
                        ws.send("draw");
                        $("#draw").off();
                        $("#end").off();
                        $("#surrender").off();
                        $("#double").off();
                        $("#my").attr("class", "play");
                        $("#main").children().each(function (i, e) {
                            $(e).attr("class", "play")
                        });
                        $("#main").children().remove();
                        for (let i = 0; i < 5; i++) {
                            console.log("実行！");
                            if (mynum == i)
                                continue;
                            $("<div>", {
                                id: i,
                                class: "play"
                            }).appendTo("#main");
                            $("<div>", {
                                class: "num",
                                text: i
                            }).appendTo("#" + i);
                            $("<div>", {
                                class: "sum"
                            }).appendTo("#" + i);
                            $("<div>", {
                                class: "card"
                            }).appendTo("#" + i);
                        }
                        $("#system").text("他のプレイヤーの番です");
                        clear();

                        break;
                    case "error":
                        $("#system").text("エラーが発生しました")
                        break;
                    case "dolphin":
                        ws.send("dolphin");
                        break;
                }
                break;
        }
    };

    let onClose = function () {
        $("#system").text("ホストとの接続が切れました");
        clear();
    }


    connect = function () {
        console.log("コネクト！");
        ws = new WebSocket('ws://127.0.0.1:8080/JavaWeb/tp/bl');
        ws.onopen = onOpen;
        ws.onmessage = onMessage;
        ws.onclose = onClose;

        $("#connectbtn").text("通信切断");
        $("#connectbtn").off();
        $("#connectbtn").click(cutting);
    };

    $("#connectbtn").click(connect);

    cutting = function () {
        console.log("カッティング！");
        ws.close();
        $("#connectbtn").text("通信開始");
        $("#connectbtn").off();
        $("#connectbtn").click(connect);
    }

    function clear() {
        $(".card").html("");
        $(".sum").html("");
    }

    //他のプレイヤーの処理 str配列
    function play(str) {
        switch (str[1]) {
            case "draw":
                let imgT = $("<img>", {
                    class: "trnp",
                    src: "../img/" + str[2] + (str[3] == undefined ? "" : str[3]) + ".jpg"
                });
                if (str[3] == undefined) {
                    imgT.attr("id", "back");
                }
                $("#" + str[0] + ">.card").append(imgT).hide().fadeIn(1000).css("transform", "rotateY(360deg)");
                break;
            case "sum":
                $("#" + str[0] + ">.sum").text(str[2]);
                break;
            case "open":
                $("#back").remove();
                $(".play").css("background", "#0f03");
                $("#system").text("ホストのターン");
                $("<img>", {
                    class: "trnp",
                    src: "../img/" + str[2] + (str[3] == undefined ? "" : str[3]) + ".jpg"
                }).appendTo("#" + str[0] + ">.card").hide().fadeIn(1000).css("transform", "rotateY(360deg)");
                break;
            case "turn":
                $(".play").css("background", "#0f03");
                $("#" + str[0]).css("background", "#f003");
                break;
            case "double":
                $("#" + str[0] + ">.card").append("<img>", {
                    class: "trnp double",
                    src: "../img/" + str[2] + str[3] + ".jpg"
                }).hide().fadeIn(1000).css("transform", "rotateX(90deg)");
                break;
            case "burst":
                $("#" + str[0]).addClass("burst");
                break;
            case "surrender":
                $("#" + str[0]).addClass("surrender");
                break;
            case "blackjack":
                $("#" + str[0]).addClass("blackjack");
                break;
        }
    }
    //ws.send()で送信できる
});
