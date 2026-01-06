import 'dart:async';

import 'package:core_utils/core_utils.dart';
import 'package:intl/intl.dart';
import 'package:table_sticky_headers/table_sticky_headers.dart';
import 'package:tiffincrm/components/bottomsheet_button.dart';
import 'package:tiffincrm/components/dropdown_filter.dart';
import 'package:tiffincrm/models/expense.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/components/refresh_strip.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/styles.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';

import '../components/learn_more_button.dart';
import '../components/onboarding_no_item.dart';

class Revenue extends StatefulWidget {
  const Revenue({super.key});

  @override
  State<Revenue> createState() => _RevenueState();
}

class _RevenueState extends State<Revenue>
    with AutomaticKeepAliveClientMixin<Revenue>, TickerProviderStateMixin {
  App app = App();
  double cellWidth = 105;
  bool loading = true;
  List<String> expenseTypes = ["Raw Materials", "Salary"];
  bool _keepAlive = true;
  @override
  bool get wantKeepAlive => _keepAlive;
  Map<String, Map<String, Map<String, num>>> stat = {};
  List<String> dates = [];
  Map<String, num> total = {};
  List<Expense> expenses = [];
  num advances = 0;
  num credits = 0;
  List<String> months = [];
  String selectedMonth = "";
  TabController? tabController;

  ValueNotifier<DateTime> dataDownloadedAt =
      ValueNotifier<DateTime>(DateTime.now());

  @override
  void initState() {
    tabController = TabController(length: 2, vsync: this);
    tabController?.addListener(() {
      setState(() {});
    });
    openLearnMore("finance", firstTime: true);
    app.viewUpdaters["revenue_view"] = updateData;

    Utility.tryCatch(
      updateData,
      onException: (_) {
        _keepAlive = false;
        updateKeepAlive();
      },
    );

    super.initState();
  }

  Future<void> updateData() async {
    stat = {};
    total = {
      'cash_added': 0,
      'order_delivered': 0,
      'order_refund': 0,
      'expense': 0,
    };

    setState(() {
      loading = true;
    });

    dynamic result = await Database.request({
      "action": "getFinanceData",
      "data": {"vendor_id": app.vendorId, "month": selectedMonth},
    }, silent: true);
    months = List.from(result['months'].map((e) => e["month"].toString()));
    if (selectedMonth.isEmpty) {
      selectedMonth = result["selectedMonth"];
    }
    if (!months.contains(selectedMonth)) {
      months.add(selectedMonth);
    }
    months = months.reversed.toList();
    expenseTypes = result['expense_types'].toString().split(",");
    expenses = List.from(result['expenses'].map((e) => Expense.fromMap(e)));
    expenses.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    advances = num.tryParse(result['balance_data']['advances'].toString()) ?? 0;
    credits = num.tryParse(result['balance_data']['credits'].toString()) ?? 0;

    for (var item in result['transactions']) {
      if (stat[item['date']] == null) {
        stat[item['date']] = {
          'cash_added': {'entries': 0, 'amount': 0},
          'order_delivered': {'entries': 0, 'amount': 0},
          'order_refund': {'entries': 0, 'amount': 0},
          'initial_balance': {'entries': 0, 'amount': 0},
          'expenses': {'entries': 0, 'amount': 0}
        };
      }
      if (stat[item['date']]![item['type']] == null) {
        stat[item['date']]![item['type']] = {'entries': 0, 'amount': 0};
      }
      stat[item['date']]![item['type']]!['entries'] =
          num.tryParse(item['count']) ?? 0;
      stat[item['date']]![item['type']]!['amount'] =
          num.tryParse(item['amount']) ?? 0;

      total[item['type']] =
          (total[item['type']] ?? 0) + (num.tryParse(item['amount']) ?? 0);
    }

    for (Expense expense in expenses) {
      if (stat[expense.date] == null) {
        stat[expense.date] = {
          'cash_added': {'entries': 0, 'amount': 0},
          'order_delivered': {'entries': 0, 'amount': 0},
          'order_refund': {'entries': 0, 'amount': 0},
          'initial_balance': {'entries': 0, 'amount': 0},
          'expenses': {'entries': 0, 'amount': 0}
        };
      }
      stat[expense.date]!['expenses']!['entries'] =
          (stat[expense.date]!['expenses']!['entries'] ?? 0) + 1;
      stat[expense.date]!['expenses']!['amount'] =
          (stat[expense.date]!['expenses']!['amount'] ?? 0) + expense.amount;

      total['expense'] = (total['expense'] ?? 0) + expense.amount;
    }
    total['order_delivered'] = (total['order_delivered'] ?? 0) * -1;
    dates = stat.keys.toList();
    dates.sort((a, b) => DateTime.parse(b).compareTo(DateTime.parse(a)));

    if (!mounted) return;

    setState(() {
      loading = false;
    });
    dataDownloadedAt = ValueNotifier<DateTime>(DateTime.now());
  }

  Widget dashboard() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            [
              "Revenue",
              "${app.currencySymbol}${(total["order_delivered"]! - total["order_refund"]!).format()}",
              "Revenue from All Orders Processed - Sum of Amount Refunded",
            ],
            [
              "Expenses",
              "${app.currencySymbol}${(total["expense"]!).format()}",
              "Expenses Added Manually To Tiffin CRM"
            ],
            [
              "Profit",
              "${app.currencySymbol}${(total["order_delivered"]! - total["order_refund"]! - total["expense"]!).format()}",
              "Expenses Subtracted from Revenue",
            ],
            [
              "Deposits",
              "${app.currencySymbol}${(total["cash_added"]!).format()}",
              "Total Amount Received from Customers"
            ],
            [
              "Advances",
              "${app.currencySymbol}${advances.format()}",
              "Unused Customer Deposits"
            ],
            [
              "Credit Given",
              "${app.currencySymbol}${credits.format()}",
              "Total Amount to be Received from Customers"
            ],
            [
              "In Hand",
              "${app.currencySymbol}${(total['cash_added']! - total['expense']!).format()}",
              "Total Cash Added - Total Expenses"
            ]
          ]
              .map((item) => Padding(
                    padding: const EdgeInsets.all(2.0),
                    child: Tooltip(
                      message: item.length > 2 ? item[2] : "",
                      child: Card(
                        child: Padding(
                          padding: const EdgeInsets.all(10.0),
                          child: Column(
                            children: [
                              Text(
                                item[1].toString(),
                                style: const TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                              Text(
                                item[0].toString(),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ))
              .toList(),
        ),
      ),
    );
  }

  Widget table() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 80),
      child: SizedBox(
        height: dates.length * 48 + 100,
        child: StickyHeadersTable(
            columnsLength: 4,
            scrollControllers: ScrollControllers(
                verticalTitleController:
                    ScrollController(keepScrollOffset: false)),
            cellDimensions: CellDimensions.variableColumnWidth(
                columnWidths: [
                  cellWidth,
                  cellWidth - 20,
                  cellWidth - 20,
                  cellWidth
                ],
                contentCellHeight: 40,
                stickyLegendHeight: 40,
                stickyLegendWidth: 65),
            rowsLength: dates.length,
            columnsTitleBuilder: (int i) => Text([
                  "Processed",
                  "Refund",
                  "Expenses",
                  "Deposits",
                ][i]),
            rowsTitleBuilder: (int i) =>
                Text(DateFormat.MMMd().format(DateTime.parse(dates[i]))),
            contentCellBuilder: (int i, int j) {
              dynamic entry = stat[dates[j]];
              String item = [
                "order_delivered",
                "order_refund",
                "expenses",
                "cash_added",
              ][i];
              int totalEntries = entry![item]!["entries"];
              num totalAmount = (entry![item]!["amount"] as num).format();
              return Card(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(0),
                    side: const BorderSide(width: 0.1, color: Colors.grey)),
                surfaceTintColor: Colors.red,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: SizedBox(
                    width: cellWidth,
                    child: Text(
                      "$totalEntries${totalEntries > 0 ? " (${app.currencySymbol}${(i == 0 ? -1 : 1) * totalAmount})" : ""}",
                      style: const TextStyle(
                        fontSize: 12,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              );
            }),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return Scaffold(
        drawer: app.drawer,
        bottomSheet: tabController!.index == 1
            ? Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: bottomSheetButton(() {
                  handleExpenseAddEdit(null);
                }, "Add Expense",
                    bottomMargin: 10,
                    insetPadding: MediaQuery.of(context).viewInsets),
              )
            : null,
        onDrawerChanged: (isOpened) {
          if (isOpened) {
            app.changeInDrawer = false;
          } else if (app.changeInDrawer) {
            setState(() {});
          }
        },
        appBar: AppBar(
          titleSpacing: 0,
          actions: [
            Padding(
              padding: const EdgeInsets.only(right: 8, top: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  dropdownFilter(months, selectedMonth, "Months", (value) {
                    setState(() {
                      selectedMonth = value!;
                    });
                    updateData();
                  }, padding: 3, width: 100),
                ],
              ),
            ),
          ],
          title: Row(
            children: [
              const Text('Finance'),
              const SizedBox(width: 10),
              learnMoreIcon("finance"),
            ],
          ),
          bottom: TabBar(
            indicatorSize: TabBarIndicatorSize.tab,
            controller: tabController,
            tabs: [
              "Revenue",
              "Expenses (${app.currencySymbol}${(total["expense"]!).format()})"
            ].map((e) => Tab(text: e)).toList(),
          ),
        ),
        body: TabBarView(controller: tabController, children: [
          loading
              ? const Center(child: CircularProgressIndicator())
              : dates.isEmpty
                  ? months.isEmpty
                      ? onboardingNoItem(
                          "No Financial Data Created Yet!",
                          "Process Any order, Create Customer's Transaction,\nor Add Expenses to see finance data!",
                          Icons.receipt,
                          linkTag: "finance",
                        )
                      : onboardingNoItem(
                          "No Finances Available For $selectedMonth!",
                          "Try Selecting a Different Month!",
                          Icons.receipt,
                        )
                  : RefreshIndicator(
                      onRefresh: updateData,
                      child: SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            ValueListenableBuilder(
                                valueListenable: dataDownloadedAt,
                                builder: (context, value, child) =>
                                    RefreshStrip(
                                      update: updateData,
                                      dataDownloadedAt: value,
                                    )),
                            dashboard(),
                            lineChart(),
                            table()
                          ],
                        ),
                      ),
                    ),
          expensesList()
        ]));
  }

  Widget lineChart() {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: SizedBox(
        height: 160,
        child: LineChart(
          LineChartData(
              lineTouchData: LineTouchData(
                  touchTooltipData: LineTouchTooltipData(
                      getTooltipItems: (touchedSpots) {
                        return touchedSpots.map((LineBarSpot spot) {
                          return LineTooltipItem(
                              "${app.currencySymbol}${spot.y.format()}\n${DateFormat.MMMd().format(DateTime.parse(dates.reversed.toList()[spot.x.toInt()]))}",
                              const TextStyle(color: ThemeColors.primary));
                        }).toList();
                      },
                      getTooltipColor: (_) => Colors.white,
                      tooltipBorder: const BorderSide(color: Colors.black12))),
              titlesData: FlTitlesData(
                show: true,
                topTitles: const AxisTitles(
                  sideTitles: SideTitles(showTitles: false),
                ),
                rightTitles: const AxisTitles(
                  sideTitles: SideTitles(showTitles: false),
                ),
                bottomTitles: AxisTitles(
                  axisNameWidget: Text(
                      "${DateFormat.MMMd().format(DateTime.parse(dates.last))} - ${DateFormat.MMMd().format(DateTime.parse(dates.first))}"),
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 30,
                    interval: 2,
                    getTitlesWidget: (value, meta) {
                      return SideTitleWidget(
                        axisSide: meta.axisSide,
                        child: Text(DateFormat.d().format(DateTime.parse(
                            dates.reversed.toList()[value.toInt()]))),
                      );
                    },
                  ),
                ),
                leftTitles: const AxisTitles(
                  axisNameWidget: Text("Orders Processed"),
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 50,
                  ),
                ),
              ),
              lineBarsData: [
                "order_delivered",
                // "order_refund",
                // "expenses",
                // "cash_added",
              ]
                  .map((item) => LineChartBarData(
                        color: ThemeColors.primary,
                        isCurved: true,
                        barWidth: 3,
                        belowBarData: BarAreaData(
                            show: true,
                            color: ThemeColors.primary.withAlpha(100)),
                        isStrokeCapRound: true,
                        spots: [
                          for (int i = dates.length - 1; i >= 0; i--)
                            FlSpot(
                                i.toDouble(),
                                (-1 *
                                    (stat[dates.reversed.toList()[i]]![item]![
                                            "amount"] as num)
                                        .format()
                                        .toDouble())),
                        ],
                      ))
                  .toList()),
        ),
      ),
    );
  }

  String sanitize(String text) {
    return text
        .replaceAll("_", " ")
        .replaceAll("delivered", "processed")
        .capitalize();
  }

  Future<void> handleExpenseAddEdit(Expense? expense) async {
    dynamic formData =
        await Navigator.push(context, MaterialPageRoute(builder: (context) {
      return FormView("${expense == null ? "Add" : "Edit"} Expense", [
        FormInput("Category", "select",
            prefixIcon: const Icon(Icons.category),
            value: expense != null ? expense.type : "",
            options: expenseTypes),
        FormInput("Amount", "number",
            value: expense != null ? expense.amount.toString() : "",
            prefixIcon: const Icon(Icons.receipt_long)),
        FormInput("Date", "date",
            showInAppBar: true,
            value: DateFormat.yMMMd()
                .format(expense != null ? expense.timestamp : DateTime.now()),
            firstDate: DateTime.now().subtract(const Duration(days: 60)),
            prefixIcon: const Icon(Icons.calendar_today)),
        FormInput(
          "Note",
          "text",
          isRequired: false,
          value: expense != null ? expense.note : "",
        )
      ]);
    }));

    if (formData == null) {
      return;
    }

    Map<String, String> data = {
      "type": formData["Category"],
      "amount": formData["Amount"],
      "timestamp": DateFormat.yMMMd().parse(formData["Date"]).toIso8601String(),
      "note": formData["Note"]
    };
    if (expense == null) {
      data["vendor_id"] = app.vendorId.toString();
      await Database.add(Tables.vendorExpenses, data);
    } else {
      await Database.update(Tables.vendorExpenses, expense.id, data);
    }
    Utility.showMessage(
        "Expense ${expense == null ? "Added" : "Updated"} Successfully");
    await updateData();
  }

  Widget expensesList() {
    return expenses.isEmpty
        ? months.isEmpty
            ? Expanded(
                child: onboardingNoItem(
                  "No Expenses Added Yet!",
                  "Manage Your Business Expenses Here!",
                  Icons.receipt,
                  linkTag: "finance_expenses",
                ),
              )
            : Expanded(
                child: onboardingNoItem(
                "No Expenses Available for $selectedMonth!",
                "Try Changing the Month!",
                Icons.receipt,
              ))
        : SingleChildScrollView(
            child: SizedBox(
              height: MediaQuery.of(context).size.height - 255,
              child: ListView.builder(
                padding: EdgeInsets.only(
                    top: 20,
                    left: 10,
                    right: 10,
                    bottom: MediaQuery.of(context).padding.bottom + 60),
                shrinkWrap: true,
                itemCount: expenses.length,
                itemBuilder: (context, index) {
                  Expense expense = expenses[index];
                  return Card(
                    child: Column(
                      children: [
                        ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 0),
                          dense: true,
                          title: Text(
                              "${expense.type} (${app.currencySymbol}${(expense.amount).format()})"),
                          subtitle: Text(
                              "${DateFormat.yMMMd().format(expenses[index].timestamp)}${expenses[index].note}"),
                          trailing: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              GestureDetector(
                                  child: const Icon(Icons.edit, size: 18),
                                  onTap: () async {
                                    await handleExpenseAddEdit(expense);
                                    setState(() {});
                                  }),
                              const SizedBox(width: 10),
                              GestureDetector(
                                child: const Icon(Icons.delete, size: 18),
                                onTap: () async {
                                  bool confirmed =
                                      await Utility.getConfirmation(
                                    "Delete Expense",
                                    "Are you sure you want to delete this expense?",
                                  );
                                  if (!confirmed) {
                                    return;
                                  }
                                  await Database.delete(
                                      Tables.vendorExpenses, expense.id);
                                  await updateData();
                                  await Utility.showMessage(
                                      "Expense Deleted Successfully");

                                  if (context.mounted) {
                                    setState(() {});
                                  }
                                },
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          );
  }
}
