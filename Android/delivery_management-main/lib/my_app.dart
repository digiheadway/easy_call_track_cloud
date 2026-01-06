import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tiffincrm/components/update_check.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/notification_manager.dart';
import 'package:tiffincrm/utils/utility.dart';

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  App app = App();

  @override
  void initState() {
    Utility.tryCatch(() async {
      NotificationManager.getInstance().init();
    }, silent: true);

    check();

    super.initState();
  }

  void check() async {
    app.prefs = await SharedPreferences.getInstance();
    if (app.prefs.getString("auth_token") == null) {
      // if not logged in
      AppRouter.navigateTo("/login", removeUntil: true);
      return;
    }

    if (kReleaseMode) {
      checkUpdate();
    }
    moveToHome();
  }

  void moveToHome() async {
    app.init();

    // SAVING FCM TOKEN
    if (kReleaseMode && (app.prefs.getBool("save_fcm_token") ?? true)) {
      NotificationManager.getInstance().saveFCMToken(null);
    }

    Logger.setUserId();
    AppRouter.navigateTo(getView(), removeUntil: true);
  }

  String getView() {
    if (app.prefs.getBool("show_intro_video") ?? false) {
      return "/intro_video";
    }

    if (app.prefs.getBool("intro_quiz_pending") ?? false) {
      return "/intro_quiz";
    }

    return app.role == "Admin" ? "/home" : "/deliveries";
  }

  @override
  Widget build(BuildContext context) {
    app.devicePadding ??= MediaQuery.of(context).padding;

    return const Scaffold(
      body: Center(child: CircularProgressIndicator()),
    );
  }

  Future<void> checkUpdate() async {
    checkForUpdate().then((updated) {
      if (!updated) {
        checkUpdate();
      }
    });
  }
}
