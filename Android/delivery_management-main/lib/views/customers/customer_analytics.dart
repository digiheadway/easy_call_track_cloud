import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/html2pdf.dart';
import 'package:tiffincrm/utils/phone_call.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/views/customers/profile/profile_view.dart';

import '../../components/dropdown_filter.dart';
import '../../utils/app_router.dart';

class CustomerAnalytics extends StatefulWidget {
  const CustomerAnalytics({super.key});

  @override
  State<CustomerAnalytics> createState() => _CustomerAnalyticsState();
}

class _CustomerAnalyticsState extends State<CustomerAnalytics> {
  int curMonth = DateTime.now().month;
  App app = App();
  int lastMonth = 0;
  bool loading = true;
  String selectedMonth = "Current Month";
  Map<int, Map<int, Map<String, dynamic>>> stats = {};
  Map<String, int> total = {};

  List<String> times = ["Breakfast", "Lunch", "Dinner"];

  Future<void> downloadData() async {
    total = {"Breakfast": 0, "Lunch": 0, "Dinner": 0};
    for (var cx in app.customers) {
      stats[cx.id] = {
        curMonth: {
          "Breakfast": "-",
          "Lunch": "-",
          "Dinner": "-",
          "Breakfast_Bill": "",
          "Lunch_Bill": "",
          "Dinner_Bill": ""
        },
        lastMonth: {
          "Breakfast": "-",
          "Lunch": "-",
          "Dinner": "-",
          "Breakfast_Bill": "",
          "Lunch_Bill": "",
          "Dinner_Bill": ""
        },
      };
    }

    dynamic ordersProcessed =
        await app.getCustomerTxnRecords([curMonth, lastMonth]);
    for (var tnx in ordersProcessed) {
      int clientId = int.parse(tnx['client_id']);
      int month = int.parse(tnx['month']);
      stats[clientId]![month]![tnx['time']] = tnx['total'];
      stats[clientId]![month]![tnx['time'] + "_Bill"] = tnx['total_amount'];

      total[tnx['time']] = (total[tnx['time']] ?? 0) + 1;
    }
    setState(() {
      loading = false;
    });
  }

  @override
  void initState() {
    lastMonth = curMonth == 1 ? 12 : curMonth - 1;
    downloadData();
    super.initState();
  }

  void downloadPDF() async {
    if (stats.isEmpty) {
      Utility.showMessage("No data to download");
      return;
    }
    String html = """
<html lang="en">

<style>
  td {
    white-space: nowrap;
  }
</style>
<body>
    <h1>Customer Analytics (${DateTime.now().toString().split(".")[0]} )</h1>
    <table>
      <tr>
          <th>Name</th>
          ${times.where((time) => total[time]! > 0).map((time) => "<th>$time</th>").join("")}
          <th>Bill</th>
          <th>Balance</th>
          <th>Plans</th>
          <th>Phone</th>
      </tr>
    """;
    for (var customer in app.customers) {
      num bill = 0;
      for (String time in times) {
        dynamic amount = stats[customer.id]![selectedMonth == "Current Month"
            ? curMonth
            : lastMonth]!["${time}_Bill"];
        if (amount != "") {
          bill += num.parse(amount.toString());
        }
      }
      html += """
        <tr>
          <td style='text-align:left;'>${customer.id}. ${customer.name}</td>
          ${times.where((time) => total[time]! > 0).map((time) => "<td>${stats[customer.id]![curMonth]![time]}</td>").join("")}
          <td>${app.currencySymbol}${-1 * bill.format()}</td>
          <td>${app.currencySymbol}${customer.balance.format()}</td>
          <td>${customer.orders}</td>
          <td>${customer.phone}</td>
        </tr>
      """;
    }

    html += "</table> </body> </html>";
    html2pdf(html, "customer_analytics");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("Customer Analytics"),
          actions: [
            IconButton(
                onPressed: () {
                  downloadPDF();
                },
                icon: const Icon(Icons.file_download_outlined)),
            const SizedBox(width: 8),
          ],
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : Padding(
                padding: const EdgeInsets.all(8.0),
                child: SingleChildScrollView(
                  scrollDirection: Axis.vertical,
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.max,
                        children: [
                          Row(
                            mainAxisSize: MainAxisSize.max,
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              dropdownFilter(["Current Month", "Last Month"],
                                  selectedMonth, "Month", (value) {
                                setState(() {
                                  selectedMonth = value!;
                                });
                              }, width: 120),
                              const SizedBox(width: 8),
                            ],
                          ),
                          const SizedBox(height: 12),
                          DataTable(
                              decoration: const BoxDecoration(
                                color: Colors.white,
                              ),
                              showCheckboxColumn: false,
                              columnSpacing: 10,
                              columns: [
                                const DataColumn(label: Text("Name")),
                                ...times.where((time) => total[time]! > 0).map(
                                    (time) => DataColumn(label: Text(time))),
                                const DataColumn(label: Text("Bill")),
                                const DataColumn(label: Text("Balance")),
                                const DataColumn(label: Text("Plans")),
                                const DataColumn(label: Text("Call")),
                              ],
                              rows: app.customers.map((customer) {
                                num bill = 0;
                                for (String time in times) {
                                  dynamic amount = stats[customer.id]![
                                      selectedMonth == "Current Month"
                                          ? curMonth
                                          : lastMonth]!["${time}_Bill"];
                                  if (amount != "") {
                                    bill += num.parse(amount.toString());
                                  }
                                }
                                return DataRow(
                                    onSelectChanged: (value) async {
                                      await AppRouter.navigateTo(
                                          MaterialPageRoute(builder: (context) {
                                        return CustomerProfile(customer.id);
                                      }));
                                    },
                                    cells: [
                                      DataCell(Text(
                                        "${customer.id}. ${customer.name}",
                                        style: const TextStyle(fontSize: 12),
                                      )),
                                      ...times
                                          .where((time) => total[time]! > 0)
                                          .map((time) => DataCell(Text(
                                              stats[customer.id]![
                                                      selectedMonth ==
                                                              "Current Month"
                                                          ? curMonth
                                                          : lastMonth]![time] ??
                                                  "-".toString()))),
                                      DataCell(Text(
                                        "${app.currencySymbol}${-1 * bill.format()}",
                                        style: const TextStyle(fontSize: 12),
                                      )),
                                      DataCell(Text(
                                        "${app.currencySymbol}${customer.balance.toString()}",
                                        style: const TextStyle(fontSize: 12),
                                      )),
                                      DataCell(Text(
                                        customer.orders,
                                        style: const TextStyle(fontSize: 12),
                                      )),
                                      DataCell(
                                        onTap: () {
                                          call(customer.phone);
                                        },
                                        const Icon(Icons.call),
                                      ),
                                    ]);
                              }).toList()),
                        ]),
                  ),
                ),
              ));
  }
}
