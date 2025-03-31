// Dialog nodes
var rootPane, dimPane, headerPane, lastPlayPane;
var movePoint = null; // Point2D

// State variables
var pingers = {};
var result = {};
var launcherUserData;
var launcherServerData;

const JURL = Java.type("java.net.URL");
const JImage = Java.type("javafx.scene.image.Image");
const JInetSocketAddress = Java.type("java.net.InetSocketAddress");
const JPoint = Java.type("javafx.geometry.Point2D");
const JImagePattern = Java.type("javafx.scene.paint.ImagePattern");
const JHBox = Java.type("javafx.scene.layout.HBox");
const JVBox = Java.type("javafx.scene.layout.VBox");
const JRectangle = Java.type("javafx.scene.shape.Rectangle");
const JImageView = Java.type("javafx.scene.image.ImageView");

const dayMonthFormat = java.time.format.DateTimeFormatter.ofPattern("dd.MM");
const moscowZone = java.time.ZoneId.of("Europe/Moscow");

function createImageFromUrl(url) {
    try {
        var conn = new JURL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/537.36");
        return new JImage(conn.getInputStream());
    } catch (err) {
        return new JImage(Launcher.getResourceURL("dialog/main/img/noImage.png"));
    }
}

function createImageFromUrlNull(url) {
    try {
        var conn = new JURL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/537.36");
        return new JImage(conn.getInputStream());
    } catch (err) {
        return new JImage(Launcher.getResourceURL("dialog/main/servers/no-image.png"));
    }
}

function initDialog() {
    // Lookup auth pane and dim
    dimPane = rootPane.lookup("#dim");
    rootPane.lookup("#lastPlayPane").setVisible(false);

    // Init overlays
    debug.initOverlay();
    crash.initOverlay();
    update.initOverlay();

    initDialogHeaderPane();
    initProfilePane();

    // Verify launcher & make request
    verifyLauncher();
}

function initDialogHeaderPane() {
    headerPane = rootPane.lookup("#header");
    headerPane.setOnMousePressed(function (event) movePoint = new JPoint(event.getSceneX(), event.getSceneY()));
    headerPane.setOnMouseDragged(function (event) {
        if (movePoint === null) {
            return;
        }

        // Update stage position
        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });

    headerPane.lookup("#hide").setOnAction(function (event) stage.setIconified(true));
    headerPane.lookup("#close").setOnAction(function (event) stage.hide());
    headerPane.lookup("#settings").setOnAction(goSettings);

    headerPane.lookup("#site").setOnAction(function (event) openURL(config.linkSite));
    headerPane.lookup("#discord").setOnAction(function (event) openURL(config.linkDiscord));
    headerPane.lookup("#vk").setOnAction(function (event) openURL(config.linkVK));
}

function initProfilePane() {
    let userBalance;
    let avatarImageUrl;

    if (launcherUserData) {
        userBalance = launcherUserData.get("money");
        avatarImageUrl = launcherUserData.get("avatar").asString();
    }

    headerPane = rootPane.lookup("#header");
    headerPane.lookup("#name").setText(settings.login);
    headerPane.lookup("#realMoney").setText((userBalance) ? userBalance.getInt("rub", 0) : 0);
    headerPane.lookup("#realMoney").setOnAction(function (event) openURL(config.buyURL));
    headerPane.lookup("#money").setText((userBalance) ? userBalance.getInt("emeralds", 0) : 0);
    headerPane.lookup("#money").setOnAction(function (event) openURL(config.transferURL));

    if (avatarImageUrl) {
        headerPane.lookup("#avatar").setFill(new JImagePattern(createImageFromUrl(avatarImageUrl)));
    } else {
        headerPane.lookup("#avatar").setFill(new JImagePattern(createImageFromUrl("https://skins.mcskill.net/?name=" + settings.login + "&mode=5&fx=48&fy=48")));
    }
}

function initLastPlayPane(profile, profileUUID) {
    if (profile === null || profile.object === null) {
        return;
    }
    lastPlayPane = rootPane.lookup("#lastPlayPane");
    lastPlayPane.setVisible(true);
    lastPlayPane.lookup("#lastPlayTitle").setText("Последний выбор: " + profile.object.getTitle().toUpperCase());
    let play_button = lastPlayPane.lookup("#lastPlay");
    setAction(play_button, null, profile, null, profileUUID);
    return profile;
}

function goPlay(profile) {
    if (profile === null) return; // No profile selected
    processing.resetOverlay();
    overlay.show(processing.overlay, function (event) doUpdate(profile, result.pp, result.accessToken));
}

function initOffline() {
    // Update title
    stage.setTitle(config.title + " [Offline]");

    // Set login field as username field
    loginField.setPromptText("Имя пользователя");
    if (!VerifyHelper.isValidUsername(settings.login)) {
        loginField.setText(""); // Reset if not valid
    }

    // Disable password field
    passwordField.setDisable(true);
    passwordField.setPromptText("Недоступно");
    passwordField.setText("");
}

/* ======== Handler functions ======== */

function goSettings(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) return;

    // Show settings overlay
    overlay.show(settings.overlay, null);
}

/* ======== Processing functions ======== */
function verifyLauncher(e) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function (event) makeLauncherRequest(function (result) {
        if (result.binary !== null) {
            LauncherRequest.update(Launcher.getConfig(), result);
            return;
        }

        // Parse response
        settings.lastSign = result.sign;
        settings.lastProfiles = result.profiles;
        if (settings.offline) {
            initOffline();
        }

        initProfilePane();

        // Update profiles list and hide overlay
        overlay.hide(0, null);
        if (launcherServerData) {
            fetchServers(result.profiles);
        } else {
            getServerListLegacy(result.profiles);
        }
    }));
}

function fetchServers(profiles) {
    const serversBox = rootPane.lookup("#serversVBox");
    const access = hasAccess();
    const lastProfile = getLastClient()

    var i = 0;
    let cells = new JHBox();
    cells.getStyleClass().add("GeneralHbox");

    var mutableProfiles = new java.util.ArrayList(profiles);

    for each (var server in launcherServerData) {
        var address = server.getName();
        var data = server.getValue();

        var profile = getProfileBy(mutableProfiles, address);
        if (!profile) continue;

        if (!access && profile.object.title.toLowerCase().indexOf('test') + 1) continue;

        pingers[profile.object] = new ServerPinger(getSocketAddress(address), data.getString("version", "-"));

        i++;

        clients.initClient(address);

        var allowedProfiles = getProfilesBy(mutableProfiles, address);
        for each (var client in allowedProfiles) {
            if (!access && client.object.title.toLowerCase().indexOf('test') + 1) continue;
			if (client.object.toString() == lastProfile && !lastPlayPane) {
                initLastPlayPane(client, lastProfile);
            }
            mutableProfiles.remove(client);
            clients.createClient(address, client);
        }

        let serverCell = new JHBox();
        serverCell.getStyleClass().add("ServerGeneralHbox");

        let serverIco = new JRectangle();
        serverIco.getStyleClass().add("ServerImage");
        serverIco.setWidth(136);
        serverIco.setHeight(136);

        const serverImage = getServerImage(data, profile);

        if (serverImage) serverIco.setFill(new JImagePattern(serverImage));
        serverCell.getChildren().add(serverIco);

        let serverInfoCell = new JVBox();
        serverInfoCell.getStyleClass().add("ServerGeneralVbox");

        let titleHbox = new JHBox();
        titleHbox.getStyleClass().add("ServerHeaderHbox");

        let tag = data.getString("tag", "none")
        setBadgeByTag(tag, titleHbox);

        let title = new javafx.scene.text.Text();
        title.setText(profile.object.getTitle());
        title.getStyleClass().add("ServerHeaderText");
        titleHbox.getChildren().add(title);

        let infoButton = new javafx.scene.control.Button();
        infoButton.getStyleClass().add("ServerHeaderInfo");
        titleHbox.getChildren().add(infoButton);
        serverInfoCell.getChildren().add(titleHbox);

        let descriptionHbox1 = new JHBox();
        descriptionHbox1.getStyleClass().add("ServerHeaderHbox");

        let imageView1 = new JImageView();
        imageView1.getStyleClass().add("imageViewCube");
        descriptionHbox1.getChildren().add(imageView1);

        let version = new javafx.scene.text.Text();
        version.setText(profile.object.getVersion());
        version.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(version);

        let imageView2 = new JImageView();
        imageView2.getStyleClass().add("imageViewPeople");
        descriptionHbox1.getChildren().add(imageView2);

        let online = new javafx.scene.text.Text();
        pingServer(online, profile, data);
        online.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(online);
        serverInfoCell.getChildren().add(descriptionHbox1);

        let descriptionHbox2 = new JHBox();
        descriptionHbox2.getStyleClass().add("ServerHeaderHbox");

        let imageView3 = new JImageView();
        imageView3.getStyleClass().add("imageViewEarth");
        descriptionHbox2.getChildren().add(imageView3);

        let worldSize = new javafx.scene.text.Text();
        worldSize.setText(data.get("worldSize") != null ? data.get("worldSize") : "Неизвестно");
        worldSize.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(worldSize);

        let imageView4 = new JImageView();
        imageView4.getStyleClass().add("imageViewPvp");
        descriptionHbox2.getChildren().add(imageView4);

        let pvp = new javafx.scene.text.Text();
        pvp.setText(getGameMode(data));
        pvp.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(pvp);
        serverInfoCell.getChildren().add(descriptionHbox2);

        let descriptionHbox3 = new JHBox();
        descriptionHbox3.getStyleClass().add("ServerHeaderHbox");

        let imageView5 = new JImageView();
        imageView5.getStyleClass().add("imageViewFlag");
        descriptionHbox3.getChildren().add(imageView5);

        let lastWipe = new javafx.scene.text.Text();
        lastWipe.setText(getLastWipeDate(data));
        lastWipe.getStyleClass().add("ServerDescriptionText");
        descriptionHbox3.getChildren().add(lastWipe);
        serverInfoCell.getChildren().add(descriptionHbox3);

        let serverPlayHbox = new JHBox();
        serverPlayHbox.getStyleClass().add("ServerPlayHbox");

        let playButton = new javafx.scene.control.Button("Играть");
        playButton.getStyleClass().add("ServerPlayButton");
        if (clients.isMultiRun(address)) {
            openClientOverlay(address, playButton, infoButton, profile, data);
        } else {
            setAction(playButton, infoButton, profile, data, profile.object.toString());
        }
        serverPlayHbox.getChildren().add(playButton);
        serverInfoCell.getChildren().add(serverPlayHbox);

        serverCell.getChildren().add(serverInfoCell);
        cells.getChildren().add(serverCell);

        // Это костыль! Я не знаю зачем это сделали так, но работает это явно плохо...
        if (i >= 2) {
            serversBox.getChildren().add(cells);
            cells = null;
            cells = new JHBox();
            cells.getStyleClass().add("GeneralHbox");
            i = 0;
        }
    }

    for each (var prof in mutableProfiles) {
        if (!access && prof.object.title.toLowerCase().indexOf('test') + 1) continue;

        var serverInfo;
        if (launcherServerData && !launcherServerData.isEmpty()) {
            serverInfo = launcherServerData.get(prof.object.getServerSocketAddress().toString());
        }

        pingers[prof.object] = new ServerPinger(getSocketAddress(address), data.getString("version", "-"));

        i++;

        if (prof.object.toString() == lastProfile && !lastPlayPane) {
            initLastPlayPane(prof, lastProfile);
        }

        let serverCell = new javafx.scene.layout.HBox();
        serverCell.getStyleClass().add("ServerGeneralHbox");

        let serverIco = new javafx.scene.shape.Rectangle();
        serverIco.getStyleClass().add("ServerImage");
        serverIco.setWidth(136);
        serverIco.setHeight(136);

        var serverImage;
        try {
            serverImage = new javafx.scene.image.Image(Launcher.getResourceURL("dialog/main/servers/serversIMG/" + prof.object.getDir() + ".png"));
        } catch (err) {
            try {
                serverImage = createImageFromUrlNull("https://assets.mcskill.net/clients/icons/" + prof.object.getDir() + ".png");
            } catch (err2) {
                serverImage = new javafx.scene.image.Image(Launcher.getResourceURL("dialog/main/servers/no-image.png"));
            }
        }

        if (serverImage) serverIco.setFill(new javafx.scene.paint.ImagePattern(serverImage));
        serverCell.getChildren().add(serverIco);

        let serverInfoCell = new javafx.scene.layout.VBox();
        serverInfoCell.getStyleClass().add("ServerGeneralVbox");

        var titleHbox = new javafx.scene.layout.HBox();
        titleHbox.getStyleClass().add("ServerHeaderHbox");

        let title = new javafx.scene.text.Text();
        title.setText(prof.object.getTitle());
        title.getStyleClass().add("ServerHeaderText");
        titleHbox.getChildren().add(title);

        let infoButton = new javafx.scene.control.Button();
        infoButton.getStyleClass().add("ServerHeaderInfo");
        titleHbox.getChildren().add(infoButton);
        serverInfoCell.getChildren().add(titleHbox);

        let descriptionHbox1 = new javafx.scene.layout.HBox();
        descriptionHbox1.getStyleClass().add("ServerHeaderHbox");

        let imageView1 = new javafx.scene.image.ImageView();
        imageView1.getStyleClass().add("imageViewCube");
        descriptionHbox1.getChildren().add(imageView1);

        let version = new javafx.scene.text.Text();
        version.setText(prof.object.getVersion());
        version.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(version);

        let imageView2 = new javafx.scene.image.ImageView();
        imageView2.getStyleClass().add("imageViewPeople");
        descriptionHbox1.getChildren().add(imageView2);

        let online = new javafx.scene.text.Text();
        pingServer(online, prof, data);
        online.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(online);
        serverInfoCell.getChildren().add(descriptionHbox1);

        let descriptionHbox2 = new javafx.scene.layout.HBox();
        descriptionHbox2.getStyleClass().add("ServerHeaderHbox");

        let imageView3 = new javafx.scene.image.ImageView();
        imageView3.getStyleClass().add("imageViewEarth");
        descriptionHbox2.getChildren().add(imageView3);

        let worldSize = new javafx.scene.text.Text();
        worldSize.setText("Неизвестно");
        worldSize.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(worldSize);

        let imageView4 = new javafx.scene.image.ImageView();
        imageView4.getStyleClass().add("imageViewPvp");
        descriptionHbox2.getChildren().add(imageView4);

        let pvp = new javafx.scene.text.Text();
        pvp.setText("Неизвестно");
        pvp.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(pvp);
        serverInfoCell.getChildren().add(descriptionHbox2);

        let descriptionHbox3 = new javafx.scene.layout.HBox();
        descriptionHbox3.getStyleClass().add("ServerHeaderHbox");

        let imageView5 = new javafx.scene.image.ImageView();
        imageView5.getStyleClass().add("imageViewFlag");
        descriptionHbox3.getChildren().add(imageView5);

        let lastWipe = new javafx.scene.text.Text();
        lastWipe.setText("Неизвестно");
        lastWipe.getStyleClass().add("ServerDescriptionText");
        descriptionHbox3.getChildren().add(lastWipe);
        serverInfoCell.getChildren().add(descriptionHbox3);

        let serverPlayHbox = new javafx.scene.layout.HBox();
        serverPlayHbox.getStyleClass().add("ServerPlayHbox");

        let playButton = new javafx.scene.control.Button("Играть");
        playButton.getStyleClass().add("ServerPlayButton");
        setAction(playButton, infoButton, prof, serverInfo, prof.object.toString());
        serverPlayHbox.getChildren().add(playButton);
        serverInfoCell.getChildren().add(serverPlayHbox);

        serverCell.getChildren().add(serverInfoCell);
        cells.getChildren().add(serverCell);

        if (i >= 2) {
            serversBox.getChildren().add(cells);
            cells = null;
            cells = new javafx.scene.layout.HBox();
            cells.getStyleClass().add("GeneralHbox");
            i = 0;
        }
    }
    serversBox.getChildren().add(cells);
}

function getLastClient() {
    let lastProfile = (settings != null && settings.profile != "none" && settings.profile != undefined && settings.profile != null) ? settings.profile : null;
    if (lastProfile == null) {
        let saveClient = '';
        if (launcherUserData) {
            saveClient = launcherUserData.get("saveClient");
            if (saveClient) {
                saveClient = saveClient.asString();
            }
        }
        if (!saveClient.isEmpty()) {
            const saveClientArray = saveClient.split(':');
            const opt = Launcher.getClientProfileManager().getProfileByIP(saveClientArray[0], parseInt(saveClientArray[1], 10));
            if (opt.isPresent()) {
                lastProfile = opt.get().toString();
            }
        }
    }
    return lastProfile;
}

function setBadgeByTag(tag, titleBox) {
    const serverStatusIMG = new JImageView();
    const statusClass = serverStatusIMG.getStyleClass();
    if (tag === "new")
        statusClass.add("ServerStatusIMGNew");
    else if (tag === "wipe")
        statusClass.add("ServerStatusIMGWipe");
    else if (tag === "beta")
        statusClass.add("ServerStatusIMGBeta");
    else if (tag === "obt")
        statusClass.add("ServerStatusIMGObt");
    else if (tag === "test")
        statusClass.add("ServerStatusIMGTest");
    titleBox.getChildren().add(serverStatusIMG);
}

function getLastWipeDate(serverInfo) {
    if (serverInfo == null) {
        return "Неизвестно";
    }
    const timestamp = serverInfo.getInt("lastWipe", 0);
    if (timestamp <= 0) {
        return "Неизвестно";
    }
    return java.time.Instant.ofEpochSecond(timestamp).atZone(moscowZone).format(dayMonthFormat);
}

function getGameMode(serverInfo) {
    if (serverInfo == null) {
        return "Неизвестно";
    }
    const hasPvp = serverInfo.getBoolean("pvp", false);
    return hasPvp ? "PvP" : "PvE";
}

function getSocketAddress(address) {
    const clientArray = address.split(':');
    const ip = clientArray[0];
    const port = parseInt(clientArray[1], 10);
    return JInetSocketAddress.createUnresolved(ip, port);
}

function getServerImage(serverInfo, profile) {
    try {
        if (serverInfo && serverInfo.get("image")) {
            return  new JImage(Launcher.getResourceURL("dialog/main/servers/serversIMG/" + serverInfo.get("image").asString()));
        } else {
            return new JImage(Launcher.getResourceURL("dialog/main/servers/serversIMG/" + profile.object.getDir() + ".png"));
        }
    } catch (err) {
        try {
            if (serverInfo && serverInfo.get("image")) {
                return createImageFromUrlNull("https://assets.mcskill.net/clients/icons/" + serverInfo.get("image").asString());
            } else {
                return createImageFromUrlNull("https://assets.mcskill.net/clients/icons/" + profile.object.getDir() + ".png");
            }
        } catch (err2) {
            return new JImage(Launcher.getResourceURL("dialog/main/servers/no-image.png"));
        }
    }
}

function getProfileBy(profiles, address) {
    for each(var profile in profiles) {
        var socketAddress = profile.object.getServerSocketAddress();
        if (socketAddress && socketAddress.toString() == address) {
            return profile;
        }
    }
    return null;
}

function getProfilesBy(profiles, address) {
    var list = [];
    for each (var profile in profiles) {
        var socketAddress = profile.object.getServerSocketAddress();
        if (socketAddress && socketAddress.toString() == address) {
            list.push(profile);
        }
    }
    return list;
}

function hasAccess() {
    let accessData = launcherUserData.get("accestInfo");
    if (accessData) {
        accessData = accessData.get("access");
        if (accessData) {
            accessData = accessData.asString();
        }
    }
    if (!accessData) return false;
    return accessData === "tech";
}

function selectClient(address) {
    processing.resetOverlay();
    overlay.show(clients.overlays[address], null);
}

function openClientOverlay(address, play_button, discription, profiles, serverInfo) {
    play_button.setOnAction(function () {
        selectClient(address);
    });
    if (discription !== null) {
        discription.setOnAction(function () {
            new java.lang.Thread(function () {
                app.getHostServices().showDocument((serverInfo != null) ? serverInfo.get("about").asString() : "https://mcskill.net/?page=servers");
            }).start();
        });
    }
}

function setAction(play_button, discription, profile, serverInfo, profileName) {
    play_button.setOnAction(function () {
        new java.lang.Thread(function (){
            goPlay(profile);
            settings.profile = profileName;
            settings.save();
        }).start();
    });
    if (discription !== null) {
        discription.setOnAction(function () {
            new java.lang.Thread(function () {
                app.getHostServices().showDocument((serverInfo != null) ? serverInfo.get("about").asString() : "https://mcskill.net/?page=servers");
            }).start();
        });
    }
}

function getServerListLegacy(profiles) {
    var serversBox = rootPane.lookup("#serversVBox");

    var accessData;
    if (launcherUserData) {
        accessData = launcherUserData.get("accestInfo");
        if (accessData) {
            accessData = accessData.get("access");
            if (accessData) {
                accessData = accessData.asString();
            }
        }
    }

    var access = false;
    if (accessData) {
        if (accessData == "tech")
            access = true;
        else
            access = false;
    }
    var lastProfile = (settings != null && settings.profile != "none" && settings.profile != undefined && settings.profile != null) ? settings.profile : null;
    if (lastProfile == null) {
        var saveClient = '';
        if (launcherUserData) {
            saveClient = launcherUserData.get("saveClient");
            if (saveClient) {
                saveClient = saveClient.asString();
            }
        }
        if (!saveClient.isEmpty()) {
            var saveClientArray = saveClient.split(':');
            var ip = saveClientArray[0];
            var port = parseInt(saveClientArray[1], 10);
            var opt = Launcher.getClientProfileManager().getProfileByIP(ip, port);
            if (opt.isPresent()) {
                lastProfile = opt.get().toString();
            }
        }
    }

    //LogHelper.info("Current last profile: " + lastProfile);
    var i = 0;
    var leight = profiles.size();
    var cell = new javafx.scene.layout.HBox();
    cell.getStyleClass().add("GeneralHbox");

    for each(var profile in profiles) {
        if (profile.object.title.toLowerCase().indexOf('test') + 1) {
            if (!access) {
                continue;
            }
        }

        var serverInfo;
        if (launcherServerData && !launcherServerData.isEmpty()) {
            serverInfo = launcherServerData.get(profile.object.getServerSocketAddress().toString());
        }

        pingers[profile.object] = new ServerPinger(profile.object.getServerSocketAddress(), profile.object.getVersion());

        i++;

        if (profile.object.toString() == lastProfile) {
            profile = initLastPlayPane(profile, lastProfile);
        }

        let serverCell = new javafx.scene.layout.HBox();
        serverCell.getStyleClass().add("ServerGeneralHbox");

        let serverIco = new javafx.scene.shape.Rectangle();
        serverIco.getStyleClass().add("ServerImage");
        serverIco.setWidth(136);
        serverIco.setHeight(136);

        var serverImage;
        try {
            if (serverInfo && serverInfo.get("image")) {
                serverImage = new javafx.scene.image.Image(Launcher.getResourceURL("dialog/main/servers/serversIMG/" + serverInfo.get("image").asString()));
            } else {
                serverImage = new javafx.scene.image.Image(Launcher.getResourceURL("dialog/main/servers/serversIMG/" + profile.object.getDir() + ".png"));
            }
        } catch (err) {
            try {
                if (serverInfo && serverInfo.get("image")) {
                    LogHelper.debug("LoadServerImageFromURL: " + serverInfo.get("image").asString())
                    serverImage = createImageFromUrlNull("https://assets.mcskill.net/clients/icons/" + serverInfo.get("image").asString());
                } else {
                    LogHelper.debug("LoadServerImageFromURL: " + profile.object.getDir() + ".png")
                    serverImage = createImageFromUrlNull("https://assets.mcskill.net/clients/icons/" + profile.object.getDir() + ".png");
                }
            } catch (err2) {
                serverImage = new javafx.scene.image.Image(Launcher.getResourceURL("dialog/main/servers/no-image.png"));
            }
        }

        if (serverImage) serverIco.setFill(new javafx.scene.paint.ImagePattern(serverImage));
        serverCell.getChildren().add(serverIco);

        let serverInfoCell = new javafx.scene.layout.VBox();
        serverInfoCell.getStyleClass().add("ServerGeneralVbox");

        var titleHbox = new javafx.scene.layout.HBox();
        titleHbox.getStyleClass().add("ServerHeaderHbox");

        if ((serverInfo != null) ? (serverInfo.get("tag") != null && serverInfo.get("tag").asString() != "none") : false) {
            var serverStatusIMG = new javafx.scene.image.ImageView();
            if (serverInfo.get("tag").asString() == "new")
                serverStatusIMG.getStyleClass().add("ServerStatusIMGNew");
            else if (serverInfo.get("tag").asString() == "wipe")
                serverStatusIMG.getStyleClass().add("ServerStatusIMGWipe");
            else if (serverInfo.get("tag").asString() == "beta")
                serverStatusIMG.getStyleClass().add("ServerStatusIMGBeta");
            else if (serverInfo.get("tag").asString() == "obt")
                serverStatusIMG.getStyleClass().add("ServerStatusIMGObt");
            else if (serverInfo.get("tag").asString() == "test")
                serverStatusIMG.getStyleClass().add("ServerStatusIMGTest");
            titleHbox.getChildren().add(serverStatusIMG);
        }

        let title = new javafx.scene.text.Text();
        title.setText(profile.object.getTitle());
        title.getStyleClass().add("ServerHeaderText");
        titleHbox.getChildren().add(title);

        let infoButton = new javafx.scene.control.Button();
        infoButton.getStyleClass().add("ServerHeaderInfo");
        titleHbox.getChildren().add(infoButton);
        serverInfoCell.getChildren().add(titleHbox);

        let descriptionHbox1 = new javafx.scene.layout.HBox();
        descriptionHbox1.getStyleClass().add("ServerHeaderHbox");

        let imageView1 = new javafx.scene.image.ImageView();
        imageView1.getStyleClass().add("imageViewCube");
        descriptionHbox1.getChildren().add(imageView1);

        let version = new javafx.scene.text.Text();
        version.setText(profile.object.getVersion());
        version.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(version);

        let imageView2 = new javafx.scene.image.ImageView();
        imageView2.getStyleClass().add("imageViewPeople");
        descriptionHbox1.getChildren().add(imageView2);

        let online = new javafx.scene.text.Text();
        pingServer(online, profile, serverInfo);
        online.getStyleClass().add("ServerDescriptionText");
        descriptionHbox1.getChildren().add(online);
        serverInfoCell.getChildren().add(descriptionHbox1);

        let descriptionHbox2 = new javafx.scene.layout.HBox();
        descriptionHbox2.getStyleClass().add("ServerHeaderHbox");

        let imageView3 = new javafx.scene.image.ImageView();
        imageView3.getStyleClass().add("imageViewEarth");
        descriptionHbox2.getChildren().add(imageView3);

        let worldSize = new javafx.scene.text.Text();
        worldSize.setText((serverInfo != null) ? (serverInfo.get("worldSize") != null && serverInfo.get("worldSize").asString()) : "Неизвестно");
        worldSize.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(worldSize);

        let imageView4 = new javafx.scene.image.ImageView();
        imageView4.getStyleClass().add("imageViewPvp");
        descriptionHbox2.getChildren().add(imageView4);

        let pvp = new javafx.scene.text.Text();
        pvp.setText((serverInfo != null) ? (serverInfo.get("pvp") != null && serverInfo.get("pvp").asString()) : "Неизвестно");
        pvp.getStyleClass().add("ServerDescriptionText");
        descriptionHbox2.getChildren().add(pvp);
        serverInfoCell.getChildren().add(descriptionHbox2);

        let descriptionHbox3 = new javafx.scene.layout.HBox();
        descriptionHbox3.getStyleClass().add("ServerHeaderHbox");

        let imageView5 = new javafx.scene.image.ImageView();
        imageView5.getStyleClass().add("imageViewFlag");
        descriptionHbox3.getChildren().add(imageView5);

        let lastWipe = new javafx.scene.text.Text();
        lastWipe.setText((serverInfo != null) ? (serverInfo.get("lastWipe") != null && serverInfo.get("lastWipe").asString()) : "Неизвестно");
        lastWipe.getStyleClass().add("ServerDescriptionText");
        descriptionHbox3.getChildren().add(lastWipe);
        serverInfoCell.getChildren().add(descriptionHbox3);

        let serverPlayHbox = new javafx.scene.layout.HBox();
        serverPlayHbox.getStyleClass().add("ServerPlayHbox");

        let playButton = new javafx.scene.control.Button("Играть");
        playButton.getStyleClass().add("ServerPlayButton");
        setAction(playButton, infoButton, profile, serverInfo, profile.object.toString());
        serverPlayHbox.getChildren().add(playButton);
        serverInfoCell.getChildren().add(serverPlayHbox);

        serverCell.getChildren().add(serverInfoCell);
        cell.getChildren().add(serverCell);

        if (i >= 2) {
            serversBox.getChildren().add(cell);
            cell = null;
            cell = new javafx.scene.layout.HBox();
            cell.getStyleClass().add("GeneralHbox");
            i = 0;
        }
    }
    if (cell != null)
        serversBox.getChildren().add(cell);
}

function doUpdate(profile, pp, accessToken) {
    var digest = profile.object.isUpdateFastCheck();

    // Update JVM dir
    update.resetOverlay("Обновление файлов JVM");

    var jvmDirName = profile.object.supportsJVMWithARM() ? JVMHelper.JAVA_DIR : JVMHelper.JAVA_DIR_NO_ARM;
    var jvmCustomDir = profile.object.block.getEntryValue("jvmVersion", StringConfigEntryClass) + jvmDirName;
    overlay.swap(0, update.overlay, function (event) {
        var jvmDir = settings.updatesDir.resolve(jvmCustomDir);
        makeUpdateRequest(jvmCustomDir, jvmDir, null, digest, function (jvmHDir) {
            settings.lastHDirs.put(jvmDirName, jvmHDir);

            // Update asset dir
            update.resetOverlay("Обновление файлов ресурсов");
            var assetDirName = profile.object.block.getEntryValue("assetDir", StringConfigEntryClass);
            var assetDir = settings.updatesDir.resolve(assetDirName);
            var assetMatcher = profile.object.getAssetUpdateMatcher();
            makeUpdateRequest(assetDirName, assetDir, assetMatcher, digest, function (assetHDir) {
                settings.lastHDirs.put(assetDirName, assetHDir);

                // Update client dir
                update.resetOverlay("Обновление файлов клиента");
                var clientDirName = profile.object.block.getEntryValue("dir", StringConfigEntryClass);
                var clientDir = settings.updatesDir.resolve(clientDirName);
                var clientMatcher = profile.object.getClientUpdateMatcher();
                makeUpdateRequest(clientDirName, clientDir, clientMatcher, digest, function (clientHDir) {
                    settings.lastHDirs.put(clientDirName, clientHDir);
                    doLaunchClient(jvmDir, jvmHDir, assetDir, assetHDir, clientDir, clientHDir, profile, pp, accessToken);
                });
            });
        });
    });
}

function doLaunchClient(jvmDir, jvmHDir, assetDir, assetHDir, clientDir, clientHDir, profile, pp, accessToken) {
    processing.resetOverlay();
    overlay.swap(0, processing.overlay, function () launchClient(
        jvmDir,
        jvmHDir,
        assetHDir,
        clientHDir,
        profile,
        new ClientLauncherParams(settings.lastSign, assetDir, clientDir, pp, accessToken, settings.autoEnter, settings.fullScreen, settings.ram, 0, 0),
        function (process) debug.launch(process)
        ));
}

/* ======== Server handler functions ======== */
function pingServer(online, profile, serverInfo) {
    if (serverInfo) {
        if (serverInfo.getInt("online", 0) == 1) {
            let pn = serverInfo.getInt("players_now", 0);
            let pm = serverInfo.getInt("players_max", 0);
            if (pn + pm != 0) {
                //online.setText(java.lang.String.format("%d/%d", pn, pm));
                online.setText(java.lang.String.format("%d игроков", pn));
            }
        } else {
            if (serverInfo.getInt("onmaintenance", 0) == 1) {
                online.setText("Недоступен");
            } else {
                online.setText("Тех.Работы");
            }
        }
    } else {
        var task = newTask(function () pingers[profile.object].ping());
        task.setOnSucceeded(function (event) {
            var result = task.getValue();
            online.setText(java.lang.String.format("%d/%d", result.onlinePlayers, result.maxPlayers));
        });
        task.setOnFailed(function (event) online.setText("Недоступен"));
        startTask(task);
    }
}

/* ======== Overlay helper functions ======== */
function fade(region, delay, from, to, onFinished) {
    var transition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(100), region);
    if (onFinished !== null) {
        transition.setOnFinished(onFinished);
    }

    // Launch transition
    transition.setDelay(javafx.util.Duration.millis(delay));
    transition.setFromValue(from);
    transition.setToValue(to);
    transition.play();
}

var overlay = {
    current: null,

    show: function (newOverlay, onFinished) {
        // Freeze root pane
        //news.setDisable(true);
        //authPane.setDisable(true);
        overlay.current = newOverlay;

        // Show dim pane
        dimPane.setVisible(true);
        dimPane.toFront();

        // Fade dim pane
        fade(dimPane, 0.0, 0.0, 1.0, function (event) {
            dimPane.requestFocus();
            dimPane.getChildren().add(newOverlay);

            // Fix overlay position
            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            //newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            var listener = new javafx.beans.value.ChangeListener({
                changed: function(observable, oldValue, newValue) {
                    newOverlay.setLayoutY((dimPane.getPrefHeight() - newValue) / 2.0);
                }
            });
            newOverlay.heightProperty().addListener(listener);

            // Fade in
            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        });
    },

    hide: function (delay, onFinished) {
        fade(overlay.current, delay, 1.0, 0.0, function (event) {
            dimPane.getChildren().remove(overlay.current);
            fade(dimPane, 0.0, 1.0, 0.0, function (event) {
                dimPane.setVisible(false);

                // Unfreeze root pane
                //news.setDisable(false);
                //authPane.setDisable(false);
                rootPane.requestFocus();

                // Reset overlay state
                overlay.current = null;
                if (onFinished !== null) {
                    onFinished();
                }
            });
        });
    },

    swap: function (delay, newOverlay, onFinished) {
        dimPane.toFront();
        fade(overlay.current, delay, 1.0, 0.0, function (event) {
            dimPane.requestFocus();

            // Hide old overlay
            if (overlay.current !== newOverlay) {
                var child = dimPane.getChildren();
                child.set(child.indexOf(overlay.current), newOverlay);
            }

            // Fix overlay position
            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            // Show new overlay
            overlay.current = newOverlay;
            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        });
    }
};

/* ======== Overlay scripts ======== */
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/crash/crash.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/clients/clients.js"))
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/debug/debug.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/processing/processing.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/settings/settings.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/update/update.js"));