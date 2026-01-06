import 'dart:developer';
import 'dart:isolate';
import 'dart:ui';

import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/db.dart';

import '../values/tables.dart';

class Logger {
  static logFirebaseEvent(String name, Map<String, Object> parameters) async {
    App app = App();
    parameters["vendor_id"] = app.vendorId.toString();
    await FirebaseAnalytics.instance.logEvent(
      name: name,
      parameters: parameters,
    );
  }

  static setUserId() async {
    App app = App();
    // SAVE USER ID IN FIREBASE
    try {
      String id = "vendor_${app.vendorId}";
      FirebaseAnalytics.instance.setUserId(id: id);
      FirebaseAnalytics.instance.setUserProperty(name: "id", value: id);
      FirebaseCrashlytics.instance.setUserIdentifier(id);
    } catch (_) {}
  }

  static logScreenView(String screenName) {
    try {
      FirebaseAnalytics.instance.logScreenView(screenName: screenName);
    } catch (_) {}
  }

  static Future<void> logError(
    dynamic exception,
    StackTrace? stack, {
    dynamic reason = "",
  }) async {
    try {
      return FirebaseCrashlytics.instance
          .recordError(exception, stack, reason: reason);
    } catch (_) {}
  }

  static logInDB(String message) {
    Database.add(Tables.appLogs, {"message": message});
  }

  static Future<void> setUpCrashReporting() async {
    try {
      FlutterError.onError =
          FirebaseCrashlytics.instance.recordFlutterFatalError;

      PlatformDispatcher.instance.onError = (error, stack) {
        FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);
        return true;
      };

      Isolate.current.addErrorListener(RawReceivePort((pair) async {
        final List<dynamic> errorAndStacktrace = pair;
        await FirebaseCrashlytics.instance.recordError(
          errorAndStacktrace.first,
          errorAndStacktrace.last.runtimeType == StackTrace
              ? errorAndStacktrace.last
              : null,
          fatal: true,
        );
      }).sendPort);
    } catch (_) {}
  }

  static void logDBRequest(Map<String, dynamic> body) {
    String logMessage = body["action"];
    if (body.containsKey("table")) {
      logMessage += "_${body["table"]}";
    }
    Logger.logFirebaseEvent(logMessage, {});
  }

  static void logTime(String message) {
    log(message, time: DateTime.now());
  }
}
