import 'package:tiffincrm/utils/extensions/formatted_number.dart';

class Expense {
  int id;
  String type;
  num amount;
  DateTime timestamp;
  String date;
  String note = "";

  Expense(
    this.id,
    this.type,
    this.amount,
    this.timestamp,
    this.date, {
    this.note = "",
  });

  static fromMap(item) {
    return Expense(
      int.parse(item['id']),
      item['type'],
      num.parse(item['amount']).format(),
      DateTime.parse(item['timestamp']),
      item['date'],
      note: item['note'] ?? "",
    );
  }
}
