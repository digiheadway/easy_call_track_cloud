import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

class AppRouter {
  static Future<dynamic> navigateTo(dynamic route,
      {bool removeUntil = false}) async {
    assert(
      [MaterialPageRoute, String].contains(route.runtimeType),
      "route must be MaterialApp or String",
    );

    if (route.runtimeType == MaterialPageRoute) {
      if (removeUntil) {
        return await navigatorKey.currentState
            ?.pushAndRemoveUntil(route, (route) => false);
      }
      return await navigatorKey.currentState?.push(route);
    }
    if (removeUntil) {
      return await navigatorKey.currentState
          ?.pushNamedAndRemoveUntil(route, (route) => false);
    }
    return await navigatorKey.currentState?.pushNamed(route);
  }

  static Future<void> goBack([dynamic data]) async {
    if (!navigatorKey.currentState!.canPop()) {
      return;
    }
    return navigatorKey.currentState?.pop(data);
  }

  static void exit() {
    if (Navigator.canPop(navigatorKey.currentContext!)) {
      Navigator.of(navigatorKey.currentContext!).pop();
    } else {
      SystemNavigator.pop();
    }
  }
}
