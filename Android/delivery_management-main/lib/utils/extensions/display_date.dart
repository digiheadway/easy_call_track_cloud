import 'package:intl/intl.dart';

extension DisplayDate on DateTime {
  String format(String pattern) {
    try {
      return DateFormat(pattern).format(this);
    } catch (e) {
      return toString();
    }
  }
}
