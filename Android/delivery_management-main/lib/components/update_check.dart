import 'package:in_app_update/in_app_update.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';

Future<bool> checkForUpdate() async {
  try {
    AppUpdateInfo info = await InAppUpdate.checkForUpdate();
    if (info.updateAvailability != UpdateAvailability.updateAvailable) {
      return true;
    }

    return await Utility.showCustomDialog<bool>(
            "Update Available",
            "There is an update available. Please update to continue using this app.",
            {
              "Update": () async {
                try {
                  await InAppUpdate.performImmediateUpdate();
                  return true;
                } catch (e) {
                  Utility.showMessage(e.toString());
                  return false;
                }
              },
            }) ??
        false;
  } catch (e, s) {
    String msg = e.toString();
    if (!msg.contains("not owned") && !msg.contains("PlatformException")) {
      Utility.showMessage(e.toString());
    }
    Logger.logError(e, s, reason: "Update check");
    return true;
  }
}
