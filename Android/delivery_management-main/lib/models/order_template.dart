import 'package:tiffincrm/utils/extensions/formatted_number.dart';

class OrderTemplate {
  int id;
  num price;
  String time;
  String name;
  String items;
  bool isVegan;
  String type;
  int ordersLinked = 0;
  String frequency = "1,1,1,1,1,1,1";

  OrderTemplate(
    this.id,
    this.name,
    this.price,
    this.items, {
    this.time = "Breakfast",
    this.type = "repeat",
    this.isVegan = false,
    this.ordersLinked = 0,
    this.frequency = "1,1,1,1,1,1,1",
  });

  String get repeat {
    if (type == "one_time") return "One Time Order";
    if (frequency.contains("0")) return "On Specific Days";
    return "Daily";
  }

  static fromMap(dynamic item) {
    OrderTemplate template = OrderTemplate(
      int.parse(item['id']),
      item['template_name'],
      num.parse(item['price']).format(),
      item['items'],
      time: item['time'],
      frequency: item['frequency'],
      type: item['type'],
      ordersLinked: int.parse(item['orders_linked'] ?? 0),
      isVegan: item['is_veg'] == "1" ? true : false,
    );

    return template;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'time': time,
      'price': price,
      'items': items,
      'type': type,
      'is_veg': isVegan ? 1 : 0,
    };
  }
}
