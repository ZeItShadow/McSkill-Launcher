const debug = {
    overlay: null, output: null, action: null, process: null,

    initOverlay: function () {
        debug.overlay = loadFXML("dialog/overlay/debug/debug.fxml");

        debug.output = debug.overlay.lookup("#output");
        debug.output.setEditable(false);

        debug.copy = debug.overlay.lookup("#copy");
        debug.copy.setOnAction(function () {
            const content = new javafx.scene.input.ClipboardContent();
            content.putString(debug.output.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        debug.action = debug.overlay.lookup("#action");
        debug.action.setOnAction(function (event) {
            var process = debug.process;
            if (process !== null && process.isAlive()) {
                process.destroyForcibly();
                debug.updateActionButton(true);
                return;
            }

            overlay.hide(0, null);
        });
    },

    resetOverlay: function () {
        debug.output.clear();
        debug.action.setText("");
        debug.action.getStyleClass().remove("kill");
        debug.action.getStyleClass().add("close");
    },

    append: function (text) {
        debug.output.appendText(text);
    },

    updateActionButton: function (forceClose) {
        const process = debug.process;
        const alive = !forceClose && process !== null && process.isAlive();

        const text = alive ? "Завершить" : "Закрыть";
        const addClass = alive ? "kill" : "close";
        const removeClass = alive ? "close" : "kill";

        debug.action.setText(text);
        debug.action.getStyleClass().remove(removeClass);
        debug.action.getStyleClass().add(addClass);
    },

    launch: function (process) {
        if (!LogHelper.isDebugEnabled()) {
            javafx.application.Platform.exit();
            return;
        }

        debug.resetOverlay();
        overlay.swap(0, debug.overlay, function () debugProcess(process));
    }
};

function debugProcess(process) {
    debug.process = process;
    debug.updateActionButton(false);

    const task = newTask(function () {
        const buffer = IOHelper.newCharBuffer();
        const reader = IOHelper.newReader(process.getInputStream(), java.nio.charset.Charset.defaultCharset());

        for (let length = reader.read(buffer); length >= 0; length = reader.read(buffer)) {
            const line = new java.lang.String(buffer, 0, length)

            javafx.application.Platform.runLater(function () debug.append(line));
        }

        return process.waitFor();
    });

    task.setOnFailed(function () {
        debug.updateActionButton(true);
        debug.append(java.lang.System.lineSeparator() + task.getException());
    });
    task.setOnSucceeded(function () {
        debug.updateActionButton(false);
        debug.append(java.lang.System.lineSeparator() + "Exit code " + task.getValue());
    });

    startTask(task);
}
