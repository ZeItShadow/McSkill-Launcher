let app, stage, scene;

// Engine scripts (API should be imported through static link)
launcher.loadScript(LauncherClass.static.getResourceURL("engine/api.js"));
launcher.loadScript(Launcher.getResourceURL("config.js"));

// Dialog scripts
launcher.loadScript(Launcher.getResourceURL("dialog/main/main.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/auth/auth.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/vote/voteDialog.js"));

function makeApiRequest(url) {
    try {
        const yc = new java.net.URL(url).openConnection();
        const i = new java.io.BufferedReader(new java.io.InputStreamReader(yc.getInputStream()));
        let inputLine;
        let buffer = "";
        while ((inputLine = i.readLine()) != null)
            buffer += inputLine;
        i.close();
        return com.eclipsesource.json.Json.parse(buffer)
    } catch (err) {
    }
}

// Override application class
// noinspection JSUnusedGlobalSymbols
const LauncherApp = Java.extend(JSApplication, {
    init: function () {
        app = JSApplication.getInstance();
        cliParams.init(app.getParameters());
        settings.load();
    },
    start: function (primaryStage) {
        stage = primaryStage;
        stage.setResizable(false);
        stage.setTitle(config.title);
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        // Set icons
        for each (var icon in config.icons) {
            var iconURL = Launcher.getResourceURL(icon).toString();
            stage.getIcons().add(new javafx.scene.image.Image(iconURL));
        }

        // Load dialog FXML
        rootPane = loadFXML("dialog/auth/auth.fxml");
        initAuthPane();

        // Set scene
        scene = new javafx.scene.Scene(rootPane);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);

        // Center and show stage
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    },
    stop: function () {
        settings.save();
    }
});

// Helper functions
function loadFXML(name) {
    const loader = new javafx.fxml.FXMLLoader(Launcher.getResourceURL(name));
    loader.setCharset(IOHelper.UNICODE_CHARSET);
    return loader.load();
}

// Start function - there all begins
function start(args) {
    // Set font rendering properties
    LogHelper.debug("Setting FX properties 2");
    java.lang.System.setProperty("prism.lcdtext", "false");

    // Start laucher JavaFX stage
    LogHelper.debug("Launching JavaFX application");
    javafx.application.Application.launch(LauncherApp.class, args);
}
