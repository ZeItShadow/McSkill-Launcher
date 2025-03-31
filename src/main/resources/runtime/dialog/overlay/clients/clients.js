var clients = {
    overlays: {},

    initClient: function (address) {
        const overlayPane = loadFXML("dialog/overlay/clients/clients.fxml");
        const rootPane = overlayPane.lookup("#root");
        rootPane.lookup("#close").setOnAction(clients.close);
        clients.overlays[address] = overlayPane;
    },

    createClient: function (address, profile) {
        const rootPane = clients.overlays[address];
        const clientBox = loadFXML("dialog/overlay/clients/client.fxml");
        clientBox.lookup("#title").setText(profile.object.title);
        clientBox.lookup("#server").setOnAction(function () {
            overlay.hide(0, function () {
                new java.lang.Thread(function (){
                    goPlay(profile);
                    settings.profile = profile.object.toString();
                    settings.save();
                }).start();
            });
        });
        rootPane.lookup("#clients").getChildren().add(clientBox);
    },

    isMultiRun: function (address) {
        const pane = clients.overlays[address];
        return pane.lookup("#clients").getChildren().size() > 1;
    },

    close: function() {
        overlay.hide(0, null);
    }
};