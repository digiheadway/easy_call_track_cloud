import 'package:tiffincrm/utils/extensions/string_ext.dart';

class DeliveryMan {
  int id;
  String name;
  String phone;
  List<String> zones;
  String email;

  DeliveryMan(
    this.id,
    this.name,
    this.phone,
    this.zones, {
    this.email = "",
  });

  factory DeliveryMan.fromJson(Map<String, dynamic> json) {
    return DeliveryMan(
      int.parse(json['id']),
      json['name'],
      json['phone'],
      (json['zones'] ?? "")
          .toString()
          .split(",")
          .where((e) => e != "")
          .map((e) => e.trim().capitalize())
          .toList(),
      email: json['email'] ?? "",
    );
  }
}
