$(function () {

    let ws = new WebSocket("ws://127.0.0.1:8080/JavaWeb/tp/bc");
    ws.onopen = function () {
        $("#text").text("マッチング成功");
        console.log("マッチング成功");
    }

    ws.onmessage = function (e) {
        console.log(e.data);
        let str = e.data.split(",");
        switch (str[0]) {
            case "system":
                switch (str[1]) {
                    case "start":
                        $("#text").text("バカラ");
                        $(".card").html("");
                        $(".sum").html("");
                        $("#request>div").fadeIn(500);
                        break;
                    case "stop":
                        $("#reqwrap").hide();
                        break;
                    case "banker":
                        $("#text").text("バンカーの勝ち");
                        break;
                    case "player":
                        $("#text").text("プレイヤーの勝ち");
                        break;
                    case "tie":
                        $("#text").text("引き分け");
                }
                break;
            case "banker":
                if (str[1] == "sum") {
                    $("#banker>.sum").text(str[2]);
                } else if (str[1] == "draw") {
                    $("<img>", {
                        class: "trnp",
                        src: "img/" + str[2] + str[3] + ".jpg"
                    }).appendTo("#banker>.card");
                }
                break;
            case "player":
                if (str[1] == "sum") {
                    $("#player>.sum").text(str[2]);
                } else if (str[1] == "draw") {
                    console.log("img/" + str[2] + str[3] + ".jpg");
                    $("<img>", {
                        class: "trnp",
                        src: "img/" + str[2] + str[3] + ".jpg"
                    }).appendTo("#player>.card");
                }
                break;
        }
    }
});
