import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';

class Database {
  static String api = "https://backend123.tiffincrm.com";

  static Future<dynamic> request(Map<String, dynamic> body,
      {bool silent = false}) async {
    String url = '${Database.api}/api2.php?compress=1&stop_numeric_check=1';
    App app = App();
    if (app.vendorId != -1) {
      url += '&vendor_id=${app.vendorId}';
    }

    app.notifyDBRequest(body);
    Logger.logDBRequest(body);
    // silent = true;

    try {
      // Fetch Response
      http.Response response;
      if (silent) {
        response = await http.post(Uri.parse(url), body: jsonEncode(body));
      } else {
        Utility.showLoaderDialog();
        response = await http.post(Uri.parse(url), body: jsonEncode(body));
        Utility.closeCurrentDialog();
      }
      // Handle Response
      if (response.statusCode != 200) {
        throw Exception(response.body);
      }

      var inflated = GZipCodec().decode(response.bodyBytes);
      var responseBody = utf8.decode(inflated);
      if (kDebugMode) {
        print("${responseBody.length / 1024} KB");
        print(DateTime.now());
      }
      Map<String, dynamic> data = jsonDecode(responseBody);
      if (data["res"] != 'success') {
        throw Exception(data["data"]);
      }

      return data["data"];
    } catch (e) {
      // Handle Exception
      if (body["table"] == Tables.appLogs.name) {
        return;
      }
      if (e.toString().contains("Failed host lookup")) {
        await Utility.getConfirmation(
            "No Internet", "Please check your internet connection",
            showCancel: false, okText: "Restart!");
        app.restart();
      } else if (e.toString().contains("Vendor not found") ||
          e.toString().contains("Delivery Man not found")) {
        Utility.showMessage("Some issue found, please re-login!");
        await app.logout();
      } else {
        Utility.showMessage(e.toString());
        rethrow;
      }
      return;
    }
  }

  static Future<void> addMany(
      Tables table, List<Map<String, dynamic>> insertData) async {
    await Database.request({
      'table': table.name,
      'data': insertData,
      'action': 'insertMany',
    });
  }

  static Future<dynamic> fcmNotify(String token) async {
    return await Database.request({
      "action": "showFCMNotification",
      "data": {"fcm_token": token, "title": "title", "content": "content"}
    });
  }

  static Future<void> delete(Tables table, int id) async {
    return await Database.request({
      'table': table.name,
      'where': {'id': id},
      'action': 'delete',
    });
  }

  static Future<void> deleteMany(Tables table, List<int> ids) async {
    return await Database.request({
      'table': table.name,
      'where': "id IN (${ids.join(",")})",
      'action': 'delete',
      'limit': ids.length
    });
  }

  static Future<List<dynamic>> get(Tables table,
      {dynamic where = const {},
      String fields = "*",
      bool silent = false}) async {
    return await Database.request({
      'table': table.name,
      'where': where,
      'cols': fields,
      'action': 'select'
    }, silent: silent);
  }

  static Future<void> update(
      Tables table, int id, Map<String, String> updateData,
      {bool silent = false}) async {
    return await Database.request({
      'table': table.name,
      'data': updateData,
      'action': 'update',
      'where': {'id': id},
    }, silent: silent);
  }

  static Future<int> add(Tables table, Map<String, dynamic> insertData,
      {bool silent = false}) async {
    String id = await Database.request({
      'table': table.name,
      'data': insertData,
      'action': 'insert',
    }, silent: silent);
    return int.parse(id);
  }

  static Future<void> updateMany(
      String table, dynamic where, Map<String, String> updateData,
      {bool silent = false}) async {
    return await Database.request({
      'table': table,
      'data': updateData,
      'action': 'update',
      'where': where
    }, silent: silent);
  }

  static Future<void> checkOTP(String phone, String otp) async {
    if (otp.length != 4) {
      throw Exception("Invalid OTP");
    }
    await Database.request({
      'action': 'checkOTP',
      'data': {'phone_number': phone, 'otp': otp}
    }, silent: true);
  }

  static Future<void> sendOTP(String phone) async {
    if (phone.length != 10) {
      throw Exception("Invalid phone number");
    }

    await Database.request({
      'action': 'sendOTP',
      'data': {'phone_number': phone}
    }, silent: true);
  }
}
