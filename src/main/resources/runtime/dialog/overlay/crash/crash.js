const crash = {
    overlay: null, title: null, output: null, forum: null, copy: null, close: null,

    initOverlay: function () {
        crash.overlay = loadFXML("dialog/overlay/crash/crash.fxml");

        // Title
        crash.title = crash.overlay.lookup("#utitle")

        // Crash content
        crash.output = crash.overlay.lookup("#output");
        crash.output.setEditable(false);

        // Forum button
        crash.forum = crash.overlay.lookup("#forum");
        crash.forum.setOnAction(function (event) openURL(config.crashForumURL));

        // Copy button
        crash.copy = crash.overlay.lookup("#copy");
        crash.copy.setOnAction(function () {
            const content = new javafx.scene.input.ClipboardContent();
            content.putString(crash.output.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        // Close button
        crash.close = crash.overlay.lookup("#close");
        crash.close.setText("Закрыть");
        crash.close.getStyleClass().add("close");
        crash.close.setOnAction(function () overlay.hide(0, null));
    },

    showOverlay: function (title, output) {
        crash.title.setText(title);
        crash.output.setText(output);
        stage.toFront();

        if (LogHelper.isDebugEnabled()) {
            overlay.swap(0, crash.overlay, null);
        } else {
            overlay.show(crash.overlay, null);
        }
    }
};