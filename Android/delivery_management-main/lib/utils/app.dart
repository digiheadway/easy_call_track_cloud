import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:in_app_review/in_app_review.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:http/http.dart' as http;
import 'package:tiffincrm/models/delivery_man.dart';
import 'package:tiffincrm/models/order.dart';
import 'package:tiffincrm/models/order_template.dart';
import 'package:tiffincrm/models/vendor.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/html2pdf.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/notification_manager.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/countries.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/views/sidebar/app_drawer.dart';

import 'db.dart';

class App {
  static App _app = App._internal();

  void reset() {
    _app = App._internal();
  }

  factory App() {
    return _app;
  }

  EdgeInsets? devicePadding;

  App._internal();

  int vendorId = -1;
  String role = "Admin";
  String authToken = "0";
  Widget? get drawer {
    return role == "Admin" ? const MyAppDrawer() : null;
  }

  void initVendor() {
    role = prefs.getString("role") ?? "Admin";
    authToken = prefs.getString("auth_token") ?? "0";
    deliveryManId = prefs.getInt("delivery_man_id") ?? -1;
    vendorId = prefs.getInt("vendor_id") ?? -1;

    if (kDebugMode) {
      // role = "Admin";
      // role = "DeliveryMan";
      // db.vendorId = 201;
      vendorId = 273; // 149;  // Yogesh
      // deliveryManId = 201;
      authToken = "0miIDfy0XsiQFO6o0xwDH8MzMtRaikt"; // 273
      // authToken = "gdDqAgtBVIUm3IhX5hAQN0TWh9Xc2tEG";

      // vendorId = 1; // Soumya
      // vendorId = 650; // Tahir
      // vendorId = 466; // Tahir
      // app.vendorId = 307; // Tahir
      // vendorId = 1159; // Vital
      // vendorId = 1358;
      // authToken = "0miIDfy0XsiQFO6o0xwDH8MzMtRaiktS";
    }
    // testing
  }

  init() {
    initVendor();
    String name = prefs.getString("vendor_name") ?? "";
    String bizName = prefs.getString("business_name") ?? "Welcome to TiffinCRM";
    vendor = Vendor(-1, name, "", businessName: bizName);
  }

  DeliveryMan? deliveryMan;
  int deliveryManId = -1;
  int dailyUsuageCount = -1;
  String get planTitle {
    return prefs.getString("planTitle") ?? "Free Tier";
  }

  Map<String, dynamic> initialData = {};
  Vendor vendor = Vendor(-1, "New User", "");
  late SharedPreferences prefs;

  // only use in "home_view" & "deliveries_view"
  DateTime _selectedDate = DateTime.now();
  String selectedTime = "";
  Map<String, String> vendorStatus = {};
  List<OrderTemplate> orderTemplates = [];
  List<Customer> customers = [];
  dynamic customersTxnRecord;

  List<String> _orderItems = [];
  Map<String, Function> viewUpdaters = {};

  List<String> zones = [];
  DateTime get selectedDate => _selectedDate;

  // track changes
  bool changeInDrawer = false;
  bool changeInProfile = false;
  bool userInProfile = false;
  bool changeInOrderTemplates = false;

  set selectedDate(DateTime date) {
    _selectedDate = date;
    Logger.logFirebaseEvent("selected_date", {"date": date.toString()});
  }

  bool get canProcessOrdersToday {
    // can process all orders except after tomorrow's
    return selectedDate.isBefore(DateTime.now().add(const Duration(days: 1)));
  }

  String _currencySymbol = "";
  set currencySymbol(String symbol) {
    _currencySymbol = symbol;
  }

  String get currencySymbol {
    if (_currencySymbol == "") {
      String countryCode = prefs.getString("country_code") ?? "IN";
      _currencySymbol =
          (Countries.countries[countryCode] ?? ["India", "+91", "₹"])[2];
    }
    return _currencySymbol;
  }

  String _dialCode = "";
  set dialCode(String code) {
    _dialCode = code;
  }

  String getPhoneForWhatsapp(String phone) {
    if (phone.startsWith("+")) return phone.replaceAll("+", "");
    return "${dialCode.replaceAll("+", "")}$phone";
  }

  String get dialCode {
    if (_dialCode == "") {
      String countryCode = prefs.getString("country_code") ?? "IN";
      _dialCode = (Countries.countries[countryCode] ?? ["India", "+91"])[1];
    }
    return _dialCode;
  }

  Future<void> notifyVendorCreate(Map data) async {
    await http.post(Uri.parse("${Database.api}/tools/webhook_new.php"),
        body: jsonEncode(data));
  }

  Future<List<Customer>> getCustomers(
      {dynamic where = const {}, String fields = "*"}) async {
    dynamic clients = await Database.get(Tables.customersView,
        fields: fields, where: where, silent: true);
    return List<Customer>.from(clients.map((x) => Customer.fromMap(x)));
  }

  Future<void> getInitialData({bool forceUpdate = true}) async {
    if (initialData.isEmpty || forceUpdate) {
      initialData = await Database.request({
        "action": "getInitialData1",
        "data": {
          "vendor_id": vendorId,
          "date": selectedDate.toString().split(" ")[0],
          "role": role,
          "delivery_man_id": deliveryManId,
          "auth_token": authToken
        }
      }, silent: true);
    }

    setInitialData();
  }

  Future<List<String>> getOrderItems() async {
    if (_orderItems.isNotEmpty) {
      return _orderItems;
    }

    List<dynamic> orderItemsData = await Database.get(Tables.orders,
        where:
            "`client_id` IN (SELECT `id` FROM clients WHERE `vendor_id` = $vendorId)",
        fields: "DISTINCT items AS order_items");

    _orderItems = [];
    for (var orderItem in orderItemsData) {
      _orderItems
          .addAll(orderItem["order_items"].replaceAll("  ", " ,").split(","));
    }
    _orderItems = _orderItems.toSet().toList();

    return _orderItems;
  }

  Future<void> updateVendor(Map<String, String> data,
      {bool silent = false}) async {
    await Database.update(Tables.vendors, vendorId, data, silent: silent);
  }

  void setInitialData() {
    if (role != "Admin") {
      deliveryMan = DeliveryMan.fromJson(initialData['deliveryMan']);
    }
    zones = List<String>.from(initialData["zones"])
        .where((element) => element != "")
        .map((element) => element.capitalize())
        .toSet()
        .toList();
  }

  Future<void> setVendor({update = false}) async {
    if (vendor.id != -1 && !update) {
      return;
    }

    List<dynamic> result = await Database.get(Tables.vendors,
        where: {'id': vendorId, "auth_token": authToken}, silent: true);
    if (result.isEmpty) {
      await logout(silent: true);
      return;
    }
    vendor = Vendor.fromMap(result[0]);

    if (vendor.fcmToken == null) {
      NotificationManager.getInstance().saveFCMToken(null);
    }

    await prefs.setString("country_code", vendor.countryCode);
    await prefs.setString("vendor_name", vendor.name);
    if (vendor.businessName.isNotEmpty) {
      await prefs.setString("business_name", vendor.businessName);
    }
    _dialCode = "";
    _currencySymbol = "";
  }

  dynamic getStats(List<Order> orders) {
    Map<String, Map<String, int>> stat = {
      'Breakfast': {},
      'Lunch': {},
      'Dinner': {},
      'All': {}
    };
    Map<String, List<int>> customersCount = {
      'Breakfast': [],
      'Lunch': [],
      'Dinner': [],
      'All': []
    };

    for (Order order in orders) {
      // stat for all status
      for (String time in [order.time, "All"]) {
        stat[time]![order.status] = stat[time]![order.status] == null
            ? 1
            : stat[time]![order.status]! + 1;
        stat[time]!["total_orders"] = stat[time]!["total_orders"] == null
            ? 1
            : stat[time]!["total_orders"]! + 1;
        customersCount[time]!.add(order.clientId);
      }
    }

    for (String time in customersCount.keys) {
      stat[time]!["total_customers"] = customersCount[time]!.toSet().length;
    }

    return stat;
  }

  setVendorStatus(List<Order> orders, Map<String, dynamic> venStatus) async {
    Map<String, Map<String, int>> stat = getStats(orders);

    vendorStatus = {
      'Breakfast': venStatus['Breakfast'].toString(),
      'Lunch': venStatus['Lunch'].toString(),
      'Dinner': venStatus['Dinner'].toString(),
    };

    if (selectedTime.isEmpty) {
      for (var t in ["Breakfast", "Lunch", "Dinner"]) {
        if (['awaiting', 'processing'].contains(vendorStatus[t]) &&
            stat[t]!.containsKey(vendorStatus[t])) {
          selectedTime = t;
          break;
        }
      }
    }
  }

  String generateRandomToken({int length = 32}) {
    final random = Random.secure();
    const chars =
        'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    final token =
        List.generate(length, (_) => chars[random.nextInt(chars.length)]);
    return token.join();
  }

  Future<void> showNotification(List<int> clientIds, String content) async {
    String title = "Imeals.in";
    if (vendor.displayName != "New User") {
      title = vendor.displayName;
    }

    Database.request({
      "action": "showWebNotification",
      "data": {
        "client_ids": clientIds.join(","),
        "title": title,
        "content": content,
        "openUrl": "/"
      }
    }, silent: true);
  }

  Future<void> fetchCustomers({bool update = false}) async {
    if (customers.isEmpty || update) {
      customers = await getCustomers(where: {"vendor_id": vendorId});
    }
  }

  Future<void> logout({bool silent = false}) async {
    if (!silent &&
        !(await Utility.getConfirmation("Logout", "Are you sure?"))) {
      return;
    }

    await prefs.clear();
    reset();

    AppRouter.navigateTo("/", removeUntil: true);
  }

  Future<void> fetchOrderTemplates({bool update = false}) async {
    if (orderTemplates.isEmpty || update) {
      dynamic value = await Database.get(Tables.orderTemplates,
          fields:
              "*, (SELECT COUNT(id) FROM orders WHERE orders.template_id = order_templates.id AND orders.is_active = 1) AS orders_linked",
          where: {"vendor_id": vendorId},
          silent: true);
      orderTemplates = List<OrderTemplate>.from(
          value.map((item) => OrderTemplate.fromMap(item)));
    }
  }

  Future<dynamic> getCustomerTxnRecords(List<int> months) async {
    customersTxnRecord = await Database.get(Tables.transactions,
        fields:
            "SUM(amount) as total_amount, COUNT(*) as total, client_id, MONTH(timestamp) as month, (SELECT time FROM orders WHERE orders.id IN (SELECT order_id FROM deliveries WHERE ref_id = deliveries.id)) as time",
        where:
            "client_id IN (SELECT id FROM clients WHERE clients.vendor_id = ${vendor.id}) AND type = 'order_delivered' AND MONTH(timestamp) IN (${months.join(",")}) GROUP BY client_id, MONTH(timestamp), time;",
        silent: true);

    return customersTxnRecord;
  }

  Future<void> downloadCustomers() async {
    if (customers.isEmpty) {
      Utility.showMessage("No customers addded yet");
      return;
    }

    String html = """
      <h1>Customers Backup (${DateTime.now().toString().split(".")[0]} )</h1>
        <table>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Phone</th>
          <th>Meal Plans</th>
          <th >Balance</th>
        </tr>
 """;
    for (var customer in customers) {
      html += """
        <tr>
          <td>${customer.id}</td>
          <td>${customer.name}</td>
          <td>${customer.phone}</td>
          <td>${customer.orders}</td>
          <td>$currencySymbol${customer.balance}</td>
        </tr>
      """;
    }
    await html2pdf(html, "Backup");
  }

  void refreshOtherViews() async {
    bool force = true;
    if (viewUpdaters.containsKey("delivery_list_view")) {
      await viewUpdaters["delivery_list_view"]!(forceUpdate: force);
      force = false;
    }
    if (viewUpdaters.containsKey("overview")) {
      viewUpdaters["overview"]!(forceUpdate: force);
    }
    if (viewUpdaters.containsKey("revenue_view")) {
      viewUpdaters["revenue_view"]!();
    }
  }

  void restart() {
    reset();
    AppRouter.navigateTo("/", removeUntil: true);
  }

  String getDateTime(String date) {
    if (date.contains(" ")) {
      date = date.split(" ")[0];
    }
    String time = DateTime.now().toString().split(" ")[1];
    return DateTime.parse("$date $time").toString();
  }

  void notifyDBRequest(Map<String, dynamic> body) {
    if (userInProfile &&
        ["insert", "insertMany", "update", "delete"].contains(body["action"]) &&
        ["transactions", "orders", "clients", "deliveries"]
            .contains(body["table"])) {
      changeInProfile = true;
    }

    if (!changeInOrderTemplates &&
        body["action"] == "update" &&
        body["table"] == "order_templates") {
      changeInOrderTemplates = true;
    }
  }

  void askForReview() async {
    if (vendorId > 650) return;

    Utility.tryCatch(() async {
      final InAppReview inAppReview = InAppReview.instance;
      if (await inAppReview.isAvailable()) {
        await inAppReview.requestReview();
      }
    }, silent: true);
  }
}
