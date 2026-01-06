import 'package:flutter/material.dart';
import 'package:table_sticky_headers/table_sticky_headers.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/models/order.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/tables.dart';

class DeliveryCancelView extends StatefulWidget {
  final Customer customer;
  const DeliveryCancelView(this.customer, {super.key});

  @override
  State<DeliveryCancelView> createState() => _DeliveryCancelViewState();
}

class _DeliveryCancelViewState extends State<DeliveryCancelView> {
  List<String> selected = [];
  Map<int, Order> orders = {};
  App app = App();
  Map<String, int> timeCounts = {"Breakfast": 0, "Lunch": 0, "Dinner": 0};
  bool loading = true;
  String selectedStatus = "";
  List<String> times = ["Breakfast", "Lunch", "Dinner"];
  List<DateTime> dates = List.generate(30, (i) => i)
      .map((e) => DateTime.now().add(Duration(days: e)))
      .toList();

  @override
  void initState() {
    getOrders().then((_) async {
      await getStatus();
    });

    super.initState();
  }

  Future<void> getOrders() async {
    List<dynamic> data = await Database.get(Tables.realOrders,
        fields:
            "id, time, price,client_id, items, frequency, is_active, expiry_date, start_from",
        where: {"client_id": widget.customer.id, "deleted": "0"},
        silent: true);

    data.sort(
        (a, b) => times.indexOf(a["time"]).compareTo(times.indexOf(b["time"])));
    for (var orderJson in data) {
      Order order = Order.fromMap(orderJson);
      timeCounts[order.time] = timeCounts[order.time]! + 1;
      orders[order.id] = order;
    }
  }

  Future<void> getStatus() async {
    orders.forEach((key, value) {
      value.statusMap = {};
    });

    if (orders.isNotEmpty) {
      dynamic data = await Database.get(Tables.deliveries,
          fields: "id, order_id, status, date",
          where: "order_id IN (${orders.keys.join(",")}) AND date >= CURDATE()",
          silent: true);
      for (var delivery in data) {
        String date = DateTime.parse(delivery['date']).format("dd MMM");
        orders[int.parse(delivery['order_id'])]!.statusMap[date] = {
          "delivery_id": delivery['id'],
          "status": delivery['status'],
        };
      }
    }

    if (mounted) {
      setState(() {
        selected = [];
        loading = false;
        selectedStatus = "";
      });
    }
  }

  dynamic getOrderAndDate(String cellId) {
    Order order = orders.values.toList()[int.parse(cellId.split("_")[0])];
    DateTime date = dates[int.parse(cellId.split("_")[1])];
    return {"order": order, "date": date};
  }

  Widget getBottomSheet() {
    String action = selectedStatus == "cancelled"
        ? "Undo Cancellation"
        : "Cancel Selected Deliveries";
    return Card(
      margin: const EdgeInsets.all(0),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 10, 10, 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text("Selected: ${selected.length}"),
            TextButton(
                onPressed: () async {
                  bool result = await Utility.getConfirmation(
                    action,
                    "Are you sure you want to proceed?",
                  );
                  if (!result) {
                    return;
                  }

                  if (selectedStatus == "cancelled") {
                    List<int> deliveryIds = selected.map((cellId) {
                      dynamic data = getOrderAndDate(cellId);
                      DateTime date = data["date"];
                      Order order = data["order"];
                      return int.parse(order.statusMap[date.format("dd MMM")]
                          ["delivery_id"]);
                    }).toList();
                    await Database.deleteMany(Tables.deliveries, deliveryIds);
                  } else {
                    await Database.addMany(
                        Tables.deliveries,
                        selected.map((cellId) {
                          dynamic data = getOrderAndDate(cellId);
                          DateTime date = data["date"];
                          Order order = data["order"];

                          Map<String, dynamic> entry = {
                            "order_id": order.id,
                            "hash":
                                "${order.id}_${date.toString().split(" ")[0]}",
                            "status": "cancelled",
                            "timestamp": date.toString().split(".")[0]
                          };
                          return entry;
                        }).toList());

                    /// Handle Notification
                    /// if (["delivered", "cancelled"].contains(status)) {
                    app.showNotification([widget.customer.id],
                        "Requested deliveries are cancelled!");

                    Utility.showMessage("Deliveries Cancelled Successfully");
                  }

                  await getStatus();
                },
                child: Text(action))
          ],
        ),
      ),
    );
  }

  Widget buildCell(int i, int j) {
    String id = "${i}_$j";
    Order order = orders.values.toList()[i];
    String status = "pending";
    String date = dates[j].format("dd MMM");
    if (order.statusMap.containsKey(date)) {
      status = order.statusMap[date]['status'];
    }

    bool available = true;
    if (order.frequency.split(",")[dates[j].weekday - 1] != "1" ||
        (order.startFrom != null && dates[j].isBefore(order.startFrom!)) ||
        (order.expiryDate != null && dates[j].isAfter(order.expiryDate!))) {
      available = false;
      status = "";
    }
    IconData icon = !order.isActive
        ? Icons.pause
        : status == "cancelled"
            ? Icons.close
            : status == "pending"
                ? Icons.delivery_dining
                : Icons.check;
    bool isSelected = selected.contains(id);

    Color bgColor = isSelected
        ? ThemeColors.primary
        : status == "cancelled" && selectedStatus != "pending" && order.isActive
            ? Colors.red.shade50
            : Colors.white;

    return GestureDetector(
      onTap: () {
        if (!["pending", "cancelled"].contains(status) ||
            !status.contains(selectedStatus) ||
            !order.isActive) {
          return;
        }

        if (selected.contains(id)) {
          selected.remove(id);
        } else {
          selected.add(id);
        }
        if (selected.isEmpty && selectedStatus.isNotEmpty) {
          selectedStatus = "";
        }
        if (selected.length == 1 && selectedStatus.isEmpty) {
          selectedStatus = status;
        }
        setState(() {});
      },
      child: AnimatedOpacity(
        opacity: status.contains(selectedStatus) && order.isActive ? 1 : 0.1,
        duration: const Duration(milliseconds: 300),
        child: Container(
          decoration: BoxDecoration(
            color: bgColor,
            border: Border.all(color: Colors.grey, width: isSelected ? 1 : 0),
          ),
          height: double.infinity,
          child: !available
              ? Container()
              : Center(
                  child: Icon(
                    icon,
                    color: isSelected ? Colors.white : Colors.black,
                  ),
                ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return loading
        ? const Center(child: CircularProgressIndicator())
        : Scaffold(
            bottomSheet: selected.isNotEmpty ? getBottomSheet() : null,
            body: Padding(
              padding:
                  EdgeInsets.only(left: 10, bottom: selected.isEmpty ? 3 : 68),
              child: StickyHeadersTable(
                columnsLength: orders.length,
                rowsLength: 30,
                cellDimensions:
                    const CellDimensions.uniform(width: 75, height: 45),
                rowsTitleBuilder: (i) => Text(
                  dates[i].format("dd MMM (EEE)"),
                  style: const TextStyle(
                      fontSize: 10, fontWeight: FontWeight.bold),
                ),
                columnsTitleBuilder: (i) {
                  Order order = orders.values.toList()[i];
                  String idText =
                      timeCounts[order.time]! > 1 ? "\n(id: ${order.id})" : "";

                  return Text(
                    "${order.time}$idText",
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                        fontSize: 10, fontWeight: FontWeight.bold),
                  );
                },
                contentCellBuilder: buildCell,
                legendCell: const Text(
                  "Meal Plan/Date",
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ),
          );
  }

  Wrap getCalender(BuildContext context) {
    return Wrap(
      children: dates.map((date) {
        return Padding(
          padding: const EdgeInsets.all(3.0),
          child: SizedBox(
            height: MediaQuery.of(context).size.height / 10,
            width: MediaQuery.of(context).size.width / 5.5,
            child: Card(
              margin: const EdgeInsets.only(bottom: 3),
              child: Stack(children: [
                Positioned(
                  bottom: 2,
                  right: 5,
                  child: Text(
                    date.format("dd"),
                    style: const TextStyle(fontSize: 12),
                  ),
                ),
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 5, vertical: 5),
                  child: SingleChildScrollView(
                    child: Wrap(
                      // crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ...orders.values.map((order) {
                          String idText = timeCounts[order.time]! > 1
                              ? " (${order.id})"
                              : "";
                          String date2 = date.format("dd MMM");
                          String status = "pending";
                          if (order.statusMap.containsKey(date2)) {
                            status = order.statusMap[date2]["status"];
                          }
                          return SizedBox(
                            // width: double.infinity,
                            child: Card(
                              elevation: 0,
                              borderOnForeground: false,
                              margin: const EdgeInsets.only(bottom: 2),
                              color: {
                                "cancelled":
                                    const Color.fromARGB(255, 223, 137, 131),
                                "delivered":
                                    const Color.fromARGB(255, 121, 193, 123),
                                "pending":
                                    const Color.fromARGB(255, 250, 214, 159)
                              }[status],
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 6, vertical: 2),
                                child: Text(
                                  "${order.time}$idText",
                                  maxLines: 1,
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                      fontSize: 8,
                                      fontWeight: FontWeight.w500,
                                      color: Colors.black),
                                ),
                              ),
                            ),
                          );
                        })
                      ],
                    ),
                  ),
                ),
              ]),
            ),
          ),
        );
      }).toList(),
    );
  }
}
