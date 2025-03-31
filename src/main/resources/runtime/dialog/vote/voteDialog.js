function setVoteAction(vote_button, url) {
    vote_button.setOnAction(function () {
        new java.lang.Thread(function () {
            var desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(url);
            }
        }).start();
    });
}

function initVoteDialog(stage, data) {
    stage.setTitle("Голосуй - Получай бонусы!");
    for each (var icon in config.icons) {
        var iconURL = Launcher.getResourceURL(icon).toString();
        stage.getIcons().add(new javafx.scene.image.Image(iconURL));
    }
    stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
    stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
    stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
    stage.setResizable(false);
    stage.sizeToScene();
    stage.centerOnScreen();
    stage.setOnCloseRequest(function (event) {
        event.consume();
    });
    var scene = stage.getScene();
    var root = scene.lookup("#root");

    //Move
    var header = root.lookup("#header");
    var xOffset, yOffset;
    header.setOnMousePressed(function (event) {
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
    });
    header.setOnMouseDragged(function (event) {
        stage.setX(event.getScreenX() + xOffset);
        stage.setY(event.getScreenY() + yOffset);
    });


    //Case
    var caseImage = createImageFromUrl(Launcher.getResourceURL("dialog/vote/img/case-default.png"));
    var caseImageView = scene.lookup("#case_image");
    caseImageView.setImage(caseImage);
    caseImageView.setCache(true);
    caseImageView.setOnMouseEntered(function () {
        var glow = new javafx.scene.effect.Glow();
        glow.setLevel(1);
        caseImageView.setEffect(glow);
    });
    caseImageView.setOnMouseExited(function () {
        caseImageView.setEffect(null);
    });
    caseImageView.setOnMouseClicked(function () {
        new java.lang.Thread(function () {
            var desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(new java.net.URI("https://mcskill.net/?page=vote"));
            }
        }).start();
    });


    //Tops
    var tops_root = scene.lookup("#tops_root");
    var tops = [];
    data.get("tops").forEach(function (v) {
        v = v.getValue();
        tops.push({
            name: v.getString("name", ""),
            reward: v.getInt("reward", 0),
            url: v.getString("url", ""),
            //img: v.getString("img", "")
            img: Launcher.getResourceURL("dialog/vote/img/emerald.gif").toString()
        });
    });

    tops.sort(function (a, b) {
        if (a.reward < b.reward) {
            return 1;
        }
        if (a.reward > b.reward) {
            return -1;
        }
        return 0;
    });
    var i;
    for (i in tops) {
        var top = tops[i];
        var top_root = loadFXML("dialog/vote/top.fxml");
        top_root.lookup("#top_name").setText(top.name);
        top_root.lookup("#top_image").setImage(new javafx.scene.image.Image(top.img));
        var vote_button = top_root.lookup("#top_vote_button");
        var url = new java.net.URI(top.url);
        setVoteAction(vote_button, url)
        var vote_reward = top_root.lookup("#vote_reward");
        if (top.reward > 0) {
            vote_reward.setText("+" + top.reward);
        } else {
            vote_reward.setVisible(false);
        }
        tops_root.getChildren().add(top_root);
    }


    //Close button
    var close = scene.lookup("#close");
    var timer = new java.util.Timer();
    var countdown = 0;
    timer.scheduleAtFixedRate(function () {
        if (countdown > 0) {
            javafx.application.Platform.runLater(function () {
            });
            countdown--;
        } else {
            timer.cancel();
            javafx.application.Platform.runLater(function () {
                close.setDisable(false);
                close.setOnAction(function () {
                    stage.close();
                })
                stage.setOnCloseRequest(function (event) {
                });
            });
        }
    }, 1000, 1000);
}
