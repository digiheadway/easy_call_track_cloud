import 'dart:convert';
import 'dart:io';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart' as scheduler;
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/values/my_colors.dart';

class NotificationManager {
  static late NotificationManager _instance;
  late FirebaseMessaging _firebaseMessaging;
  App app = App();
  FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool _initialized = false;

  NotificationManager._internal();

  static NotificationManager getInstance() {
    _instance = NotificationManager._internal();
    _instance._firebaseMessaging = FirebaseMessaging.instance;
    return _instance;
  }

  Future<bool> init() async {
    if (_initialized) {
      return false;
    }

    FirebaseMessaging.onMessageOpenedApp.listen(_handleMessage);
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

    FirebaseMessaging.instance.onTokenRefresh.listen((fcmToken) async {
      await saveFCMToken(fcmToken);
    }).onError((err) {});

    // _firebaseMessaging.getInitialMessage().then(_handleMessage);

    _initialized = false;

    if (Platform.isAndroid) {
      _initialized = true;
      await flutterLocalNotificationsPlugin.initialize(
          const InitializationSettings(
            android: AndroidInitializationSettings('@drawable/notification'),
          ),
          onDidReceiveNotificationResponse: onSelectNotification);
    }

    // ask for notification permission
    await requestPermission();

    return _initialized;
  }

  Future<void> saveFCMToken(String? token) async {
    token ??= await getToken();
    if (token == null) {
      return;
    }

    if (app.role == "Admin") {
      await app.updateVendor({"fcm_token": token}, silent: true);
      await app.prefs.setBool("save_fcm_token", false);
    }
  }

  Future<bool> requestPermission() async {
    NotificationSettings settings = await _firebaseMessaging.requestPermission(
      alert: true,
      announcement: false,
      badge: true,
      carPlay: false,
      criticalAlert: false,
      provisional: false,
      sound: true,
    );
    await _firebaseMessaging.setForegroundNotificationPresentationOptions(
      alert: true,
      badge: true,
      sound: true,
    );
    return settings.authorizationStatus == AuthorizationStatus.authorized;
  }

  Future<void> _handleMessage(RemoteMessage message) async {
    debugPrint('handlePushNotification: ${message.data.toString()}');
  }

  void _handleForegroundMessage(RemoteMessage message) async {
    RemoteNotification? notification = message.notification;
    AndroidNotification? android = message.notification?.android;

    BigPictureStyleInformation? bigPictureStyleInformation;

    if (notification != null && android != null) {
      if (android.imageUrl != null) {
        final http.Response response =
            await http.get(Uri.parse(android.imageUrl!));
        bigPictureStyleInformation = BigPictureStyleInformation(
            ByteArrayAndroidBitmap.fromBase64String(
                base64Encode(response.bodyBytes)));
      }
      flutterLocalNotificationsPlugin.show(
          notification.hashCode,
          notification.title,
          notification.body,
          NotificationDetails(
            android: AndroidNotificationDetails('tiffin_crm', 'tiffin_crm',
                importance: Importance.max,
                priority: Priority.high,
                channelDescription: 'Default channel',
                icon: 'notification',
                enableVibration: true,
                playSound: true,
                colorized: true,
                color: scheduler.SchedulerBinding.instance.platformDispatcher
                            .platformBrightness ==
                        Brightness.dark
                    ? Colors.white
                    : ThemeColors.primary,
                sound: const RawResourceAndroidNotificationSound('bell'),
                // largeIcon: const DrawableResourceAndroidBitmap('notification'),
                styleInformation: bigPictureStyleInformation),
          ),
          payload: jsonEncode(message.data));
    }
  }

  void onSelectNotification(NotificationResponse? response) {
    String? payload = response?.payload;
    debugPrint('onSelectNotification: $payload');
    var data = jsonDecode(payload!);
    debugPrint('onSelectNotification: $data');
    if (data['url'] != null) {}
  }

  Future<String?> getToken() {
    return _firebaseMessaging.getToken();
  }
}

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // If you're going to use other Firebase services in the background, such as Firestore,
  // make sure you call `initializeApp` before using other Firebase services.
  await Firebase.initializeApp();
}
