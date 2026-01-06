import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:tiffincrm/models/model.dart';
import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/values/tables.dart';

List<String> times = ["Breakfast", "Lunch", "Dinner"];

class Customer extends Model {
  Customer(
    int id,
    this.name,
    this.phone,
    this.address,
    this.zone, {
    this.phone2 = "",
    this.note = "",
    this.tags = "",
    this.isVegan = false,
    this.status = "active",
    this.tiffinCounts = 0,
    this.balance = 0,
    this.location,
    this.orders = "",
    this.minOrderPrice = 0,
    this.createdOn,
    this.totalDeliveries = "",
    this.canBeNotified = false,
    this.creditLimit,
    this.allOrderPrice = 0,
    this.authToken = "",
  }) : super(table: Tables.clients, id: id);
  String name;
  String phone;
  String? phone2;
  num balance; // in rupee symbol (Rs. 500)
  String address;
  LatLng? location;
  String zone;
  String note;
  String tags;
  bool isVegan;
  int? creditLimit;
  int tiffinCounts = 0;
  int minPosBalance = double.maxFinite.toInt();
  bool canBeNotified;
  String status;
  String orders;
  String totalDeliveries;
  DateTime? createdOn;
  num allOrderPrice = 0;
  num minOrderPrice = 0;
  String authToken = "";

  static fromMap(json) {
    Customer customer = Customer(
      int.parse(json['id']),
      json['name'],
      json['phone'],
      json['address'] ?? "",
      (json['zone'] ?? "").toString().capitalize(),
      balance: num.parse(json['balance'] ?? "0").format(),
      phone2: json['phone2'],
      note: json['note'] ?? "",
      tags: json['tags'] ?? "",
      canBeNotified: json['can_be_notified'] == "1" ? true : false,
      status: json['status'],
      isVegan: json['is_vegan'] == "1" ? true : false,
      orders: json['orders'] ?? "",
      tiffinCounts: int.parse(json['tiffin_counts'] ?? "0"),
      totalDeliveries: json['total_deliveries'] ?? "",
      allOrderPrice: num.parse(json['all_order_price'] ?? "0").format(),
      minOrderPrice: num.parse(json['min_order_price'] ?? "0").format(),
      createdOn: DateTime.parse(json['created_on']),
      authToken: json['auth_token'] ?? "",
    );

    if (json['credit_limit'] != null) {
      customer.creditLimit = int.parse(json['credit_limit']);
    }
    if (json['min_possible_balance'] != null) {
      customer.minPosBalance = int.parse(json['min_possible_balance']);
    }

    if (customer.zone.isEmpty) {
      customer.zone = "Zone Not Set!";
    }

    if (json['lat_lng'] != null) {
      List<double> location = List<double>.from(
          json['lat_lng'].split(",").map((e) => double.parse(e)).toList());
      customer.location = LatLng(location[0], location[1]);
    }
    customer.balance = customer.balance.format();
    if (customer.orders.isNotEmpty) {
      customer.orders = customer.orders.replaceAll(", ", ",");
      dynamic temp = customer.orders.split(",");
      temp.sort((a, b) => times.indexOf(a).compareTo(times.indexOf(b)));
      customer.orders = temp.join(", ");
    }

    return customer;
  }

  int get minPossibleBalance => -1 * minPosBalance;

  bool get hasLowBalance {
    return (balance - minOrderPrice) < minPossibleBalance;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'phone': phone,
      'address': address,
      'zone': zone,
      'phone2': phone2,
      'note': note,
      'tags': tags,
      'is_vegan': isVegan ? "vegetarian" : "non-vegetarian",
      'status': status,
      'credit_limit': creditLimit,
      'balance': balance,
      'orders': orders,
      'total_deliveries': totalDeliveries,
      'created_on': createdOn.toString(),
    };
  }

  String get displayName => name.isEmpty ? "Customer $id" : name;

  @override
  String toString() {
    return toMap().toString();
  }
}
