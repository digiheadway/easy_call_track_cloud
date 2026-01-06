import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';

class Model {
  Tables table;
  int id;
  Model({required this.table, required this.id});

  Future<void> update(Map<String, String> data, {String? successMessage}) async {
    await Database.update(table, id, data);
    if (successMessage != null) {
      Utility.showMessage(successMessage);
    }
  }
}
