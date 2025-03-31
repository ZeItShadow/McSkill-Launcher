// ====== LAUNCHER CONFIG ====== //
const config = {
	dir: "McSkill",
	dirwin: "AppData\\Roaming\\McSkill",
	title: "McSkill Launcher",
	icons: ["favicon.png"],

	// Auth config
	linkURL: new java.net.URL("https://mcskill.net/register"),
	linkResetPasswd: new java.net.URL("https://mcskill.net/changepassword"),
	linkSite: new java.net.URL("https://mcskill.net/"),
	linkDiscord: new java.net.URL("https://discord.su/mcskill"),
	linkVK: new java.net.URL("https://vk.com/mcskill"),
	buyURL: new java.net.URL("https://mcskill.net/pay"),
	transferURL: new java.net.URL("https://mcskill.net/emeralds"),
	crashForumURL: new java.net.URL("https://u.mcskill.net/dissupport"),

	// Settings defaults
	settingsMagic: 0xC0DE5, // Ancient magic, don't touch
	autoEnterDefault: false, // Should autoEnter be enabled by default?
	fullScreenDefault: false, // Should fullScreen be enabled by default?
	ramDefault: 4096, // Default RAM amount (0 for auto)

	// Custom JRE config
	jvmMustdie32Dir: "jre-8u202-win32", jvmMustdie64Dir: "jre-8u202-win64",
	jvmLinux32Dir: "jre-8u202-linux32", jvmLinux64Dir: "jre-8u202-linux64",
	jvmMacOSXDir: "jre-8u202-macosx", jvmUnknownDir: "jre-8u202-unknown"
};

// ====== DON'T TOUCH! ====== //
let dir;
switch (JVMHelper.OS_TYPE) {
	case JVMHelperOS.MUSTDIE: dir = IOHelper.HOME_DIR.resolve(config.dirwin); break;
	default: dir = IOHelper.HOME_DIR.resolve(config.dir); break;
}

if (!IOHelper.isDir(dir)) {
	java.nio.file.Files.createDirectory(dir);
}
const defaultUpdatesDir = dir.resolve("updates");
if (!IOHelper.isDir(defaultUpdatesDir)) {
	java.nio.file.Files.createDirectory(defaultUpdatesDir);
}
