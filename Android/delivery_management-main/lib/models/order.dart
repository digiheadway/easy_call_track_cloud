import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';

class Order {
  int id;
  String time; // Evening, Noon, Morning
  int clientId;
  int quantity = 1;
  num price;
  String items;
  String note;
  bool isActive;
  bool isVegan;
  String type;
  String zone;
  String address;
  String templateName;
  LatLng? location;
  String frequency = "1,1,1,1,1,1,1";
  String status;
  String customerName;
  String labels;
  num customerBalance;
  String customerPhone;
  bool cancelledFromImeal = false;
  int? customerCreditLimit;
  DateTime? updatedOn;
  String excludeReason;

  String customerNote;
  DateTime? startFrom;
  DateTime? expiryDate;
  int customerTiffinCounts = 0;
  String customerTags;
  DateTime createdOn = DateTime.now();
  Map<String, dynamic> statusMap = {};

  String get repeat {
    if (type == "one_time") return "One Time Order";
    if (frequency.contains("0")) return "On Specific Days";
    return "Daily";
  }

  Order(
    this.id,
    this.time,
    this.clientId,
    this.price,
    this.items, {
    this.note = "",
    this.isActive = true,
    this.status = "awaiting",
    this.zone = "",
    this.quantity = 1,
    this.address = "",
    this.customerName = "",
    this.type = "repeat",
    this.customerBalance = 0,
    this.customerTiffinCounts = 0,
    this.cancelledFromImeal = false,
    this.isVegan = false,
    this.customerCreditLimit,
    this.frequency = "1,1,1,1,1,1,1",
    this.customerPhone = "",
    this.customerTags = "",
    this.excludeReason = "",
    this.templateName = "",
    this.labels = "",
    this.customerNote = "",
  });

  static fromMap(dynamic item) {
    //print(item);
    Order order = Order(
      int.parse(item['id']),
      item['time'],
      int.parse(item['client_id']),
      num.parse(item['price']).format(),
      item['items'],
      note: item['note'] ?? "",
      status: ((item as Map).containsKey('custom_status')
              ? item['custom_status']
              : item['status']) ??
          "awaiting",
      zone: (item['zone'] ?? "").toString().capitalize(),
      frequency: item['frequency'] ?? "1,1,1,1,1,1,1",
      address: item['address'] ?? "",
      labels: item['labels'] ?? "",
      type: item['type'] ?? "repeat",
      quantity: int.parse(item['quantity'] ?? "1"),
      templateName: item['template_name'] ?? "",
      excludeReason: item['exclude_reason'] ?? "",
      customerTiffinCounts: int.parse(item['customer_tiffin_counts'] ?? "0"),
      customerName: item['customer_name'] ?? "",
      isVegan: item['is_veg'] == "1" ? true : false,
      customerBalance: num.parse(item['customer_balance'] ?? "0").format(),
      customerNote: item['customer_note'] ?? "",
      customerCreditLimit: int.parse(item['customer_credit_limit'] ?? "0"),
      customerTags: item['customer_tags'] ?? "",
      customerPhone: item['customer_phone'] ?? "",
    );

    if (order.excludeReason.isNotEmpty) {
      order.status = "excluded";
    }

    if (item["cancelled_from_imeal"] != null) {
      order.cancelledFromImeal = item["cancelled_from_imeal"] == "1";
    }

    if (item['is_active'] != null) {
      order.isActive = item['is_active'] == "1" ? true : false;
    }
    if (item['created_on'] != null) {
      order.createdOn = DateTime.parse(item['created_on']);
    }
    if (item['updated_on'] != null) {
      order.updatedOn = DateTime.parse(item['updated_on']);
    }
    if (item['start_from'] != null) {
      order.startFrom = DateTime.parse(item['start_from']);
    }
    if (item['expiry_date'] != null) {
      order.expiryDate = DateTime.parse(item['expiry_date']);
    }
    if (item['customer_location'] != null) {
      List<double> location = List<double>.from(item['customer_location']
          .split(",")
          .map((e) => double.parse(e))
          .toList());
      order.location = LatLng(location[0], location[1]);
    }
    return order;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'time': time,
      'client_id': clientId,
      'price': price,
      'items': items,
      'note': note,
      'status': status,
      'zone': zone,
      'address': address,
      'type': type,
      'customer_name': customerName,
      'is_active': isActive ? 1 : 0,
      'is_veg': isVegan ? 1 : 0,
      'customer_balance': customerBalance,
      'customer_note': customerNote
    };
  }

  @override
  String toString() {
    return toMap().toString();
  }
}
