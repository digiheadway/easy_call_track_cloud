import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';

class Transaction {
  int id;
  int clientId;
  num amount;
  String type; // cash_added, order_delivered;
  DateTime timestamp;
  String note;
  String time;
  String? orderId;

  String get displayType {
    return type
        .replaceAll("_", " ")
        .replaceAll("delivered", "Processed")
        .replaceAll("added", "Added")
        .replaceAll("deducted", "Deducted")
        .capitalize();
  }

  Transaction(
    this.id,
    this.clientId,
    this.amount,
    this.timestamp,
    this.type, {
    this.note = "",
    this.time = "",
    this.orderId,
  });

  static fromMap(item) {
    return Transaction(
      int.parse(item['id']),
      int.parse(item['client_id']),
      num.parse(item['amount']).format(),
      DateTime.parse(item['timestamp']),
      item['type'],
      note: item['note'] ?? "",
      time: item['time'] ?? "",
      orderId: item['order_id'],
    );
  }
}
