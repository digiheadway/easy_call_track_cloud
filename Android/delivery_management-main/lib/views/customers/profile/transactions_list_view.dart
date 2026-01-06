import 'package:intl/intl.dart';
import 'package:tiffincrm/components/bottomsheet_button.dart';
import 'package:tiffincrm/components/dropdown_filter.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/models/transaction.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/html2pdf.dart' deferred as html2pdf;
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';

import '../../../components/onboarding_no_item.dart';
import '../../../utils/db.dart';
import '../../../values/styles.dart';

class CustomerTransactions extends StatefulWidget {
  final Customer customer;
  final Function udpateCustomer;
  const CustomerTransactions(this.customer, this.udpateCustomer, {super.key});

  @override
  State<CustomerTransactions> createState() => _CustomerTransactionsState();
}

class _CustomerTransactionsState extends State<CustomerTransactions> {
  late Customer customer;
  App app = App();
  List<Transaction> transactions = [];
  List<Transaction> filteredTxn = [];
  bool loading = true;
  String month = DateTime.now().format("MMM");

  @override
  void initState() {
    customer = widget.customer;

    updateTransactions();

    super.initState();
  }

  Future<void> updateTransactions() async {
    setState(() {
      loading = true;
    });
    dynamic value = await Database.get(
      Tables.customerTransactionsView,
      where: {"client_id": customer.id},
      silent: true,
    );
    if (mounted) {
      setState(() {
        loading = false;
        transactions = List<Transaction>.from(
            value.map((item) => Transaction.fromMap(item)));
        transactions.sort((a, b) => b.timestamp.compareTo(a.timestamp));
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!loading) {
      filteredTxn = transactions.where((element) {
        return element.timestamp.format("MMM") == month;
      }).toList();
    }
    List<String> months =
        transactions.map((e) => e.timestamp.format("MMM")).toList();
    months.add(month);
    months = months.toSet().toList();

    return Scaffold(
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : transactions.isEmpty
              ? onboardingNoItem(
                  "No Transactions Created Yet!",
                  "Add customer's balance or process customer's\norders to create transactions.",
                  Icons.payment,
                  linkTag: "no_transaction",
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  mainAxisSize: MainAxisSize.max,
                  children: [
                    const SizedBox(height: 5),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Card(
                              child: InkWell(
                                customBorder: Styles.inkwellBorder,
                                onTap: () => downloadReport(),
                                child: const Padding(
                                  padding: EdgeInsets.all(8.0),
                                  child: Row(children: [
                                    Icon(Icons.download_rounded, size: 16),
                                    SizedBox(width: 5),
                                    Text(
                                      "Download Report",
                                      style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w500),
                                    ),
                                  ]),
                                ),
                              ),
                            ),
                            const SizedBox(width: 5),
                            Card(
                              child: InkWell(
                                customBorder: Styles.inkwellBorder,
                                onTap: () => downloadReport(
                                    cxPhone: app.getPhoneForWhatsapp(
                                        widget.customer.phone)),
                                child: Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Row(children: [
                                    Image.asset(
                                      "assets/images/whatsapp.png",
                                      width: 14,
                                    ),
                                    const SizedBox(width: 5),
                                    const Text(
                                      "Send Report",
                                      style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w500),
                                    ),
                                  ]),
                                ),
                              ),
                            ),
                          ],
                        ),
                        SizedBox(
                          height: 40,
                          child:
                              dropdownFilter(months, month, "Month", (value) {
                            setState(() {
                              month = value!;
                            });
                          }, width: 80, padding: 4),
                        ),
                      ],
                    ),
                    const SizedBox(height: 5),
                    filteredTxn.isEmpty
                        ? Expanded(
                            child: onboardingNoItem(
                              "No Transactions in $month!",
                              "Try changing month fitler!",
                              Icons.payment,
                            ),
                          )
                        : Expanded(
                            child: RefreshIndicator(
                              onRefresh: updateTransactions,
                              child: ListView.builder(
                                  padding: const EdgeInsets.only(bottom: 70),
                                  itemCount: filteredTxn.length,
                                  itemBuilder:
                                      (BuildContext context, int index) {
                                    Transaction transaction =
                                        filteredTxn[index];

                                    return Card(
                                      shape: Styles.cardShape(false),
                                      child: ListTile(
                                        onLongPress: transaction.type
                                                .contains("cash")
                                            ? () {
                                                deleteTransaction(transaction);
                                              }
                                            : null,
                                        title: Text(
                                            "${transaction.time.isEmpty ? "" : "${transaction.time} "}${transaction.displayType}${transaction.note.isEmpty ? "" : " (${transaction.note})"}"),
                                        subtitle: Text(
                                            "${transaction.timestamp.format('dd MMM, hh:mm a')}${transaction.orderId == null ? "" : "\nMeal Id: ${transaction.orderId}"}"),
                                        trailing: Text(
                                          "${app.currencySymbol}${transaction.amount}",
                                          style: TextStyle(
                                              color: transaction.amount < 0
                                                  ? Colors.red
                                                  : Colors.green),
                                          textScaler:
                                              const TextScaler.linear(1.1),
                                        ),
                                      ),
                                    );
                                  }),
                            ),
                          ),
                  ],
                ),
      bottomSheet: bottomSheetButton(() => addCash(customer), "+ Add Balance"),
    );
  }

  void downloadReport({String cxPhone = ""}) async {
    String htmlContent = '''
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300..800;1,300..800&display=swap"
      rel="stylesheet"
    />
    <header>
        <h1 style='text-align: center;'>$month Month's Transactions Report</h1>
         <hr/>
         <br/>
        <div class='contact_container flex'>
          <div class='flex-col'>
            <p>From</p>
            <p><b>${app.vendor.displayName}</b></p> 
            <p>Mobile: ${app.vendor.phone}</p>
            ${app.vendor.upiId == null ? "" : "<p><b>UPI ID</b>: ${app.vendor.upiId}</p>"} 
          </div>
          <div class='flex-col'>
            <p>To</p>
            <p><b>${customer.name}</b></p> 
            <p>Mobile: ${customer.phone}</p> 
          </div>
        </div>
      </header>
     ''';

    if (filteredTxn.isEmpty) {
      Utility.showMessage("No transactions found");
      return;
    }

    DateTime oldestTimestamp = filteredTxn.last.timestamp;
    List<num> prevTxns = transactions
        .where((txn) => txn.timestamp.isBefore(oldestTimestamp))
        .map((txn) => txn.amount)
        .toList();

    num total = prevTxns.isEmpty ? 0 : prevTxns.reduce((a, b) => a + b);

    String table = """
   <br><br>
   <table> 
      <tr> 
        <th class='right_border'>Date</th> 
        <th>Type</th> 
        <th>Info</th> 
        <th>Amount</th> 
        <th>Balance</th>
      </tr>
""";

    String lastDate = "";
    Map<String, int> rowSpans = {};
    List<Transaction> txns = List.from(filteredTxn);
    txns.sort((a, b) => a.timestamp.compareTo(b.timestamp));
    Map<String, dynamic> counts = {};

    for (Transaction transaction in txns) {
      if (counts[transaction.displayType] == null) {
        counts[transaction.displayType] = 0;
      }
      counts[transaction.displayType] =
          counts[transaction.displayType]! + transaction.amount;

      total += transaction.amount;

      String date = transaction.timestamp.format("dd MMM");
      if (!rowSpans.containsKey(date)) {
        rowSpans[date] = 1;
      }
      date = date == lastDate ? "" : date;

      table += """
        <tr>
          ${date.isEmpty ? "" : "<td class='$date'>$date</td>"}
          <td>${transaction.time.isEmpty ? "" : "${transaction.time} "}${transaction.displayType}</td>
          <td>${transaction.note.isEmpty ? "-" : transaction.note}</td>
          <td>${app.currencySymbol}${transaction.amount}</td>
          <td>${app.currencySymbol}$total</td>
        </tr>
      """;

      if (date.isEmpty) {
        lastDate = lastDate;
        rowSpans[lastDate] = rowSpans[lastDate]! + 1;
      } else {
        lastDate = date;
      }
    }

    for (var rowSpan in rowSpans.entries) {
      if (rowSpan.value != 1) {
        table = table.replaceAll(
            "class='${rowSpan.key}'", "rowspan='${rowSpan.value}'");
      }
    }

    table += "</table>";

    htmlContent += """
          <br><br>
          <div class='summary_box flex box'>
            <div class='flex-col'>
              <p>Final Balance on ${txns.last.timestamp.format("dd MMM")}:</p>
              <p style="font-size: 30px"><b>${app.currencySymbol}$total</b></p>
            </div>
            <div class='flex-col'>""";
    for (var type in counts.keys) {
      htmlContent +=
          "<p>$type: <b>${app.currencySymbol}${counts[type]}</b></p>";
    }
    htmlContent += "</div></div>";

    htmlContent += table;
    await html2pdf.loadLibrary();
    await html2pdf.html2pdf(htmlContent, customer.name, sharePhone: cxPhone);
  }

  void addCash(Customer customer) async {
    await addCustomerBalance(customer);
    await updateTransactions();
    widget.udpateCustomer();
  }

  Future<bool> deleteTransaction(Transaction transaction) async {
    if (!(await Utility.getConfirmation(
        "Delete Transaction", "Are you sure?"))) {
      return false;
    }
    await Database.delete(Tables.transactions, transaction.id);
    transactions.removeWhere((txn) => txn.id == transaction.id);
    widget.udpateCustomer();
    return true;
  }
}

Future<int> addCustomerBalance(Customer customer) async {
  List<FormInput> balanceForm = [
    FormInput("Type", "select",
        options: ["Add", "Deduct"],
        value: "Add",
        canAddOption: false,
        prefixIcon: const Icon(Icons.payment)),
    FormInput("Amount", "number",
        minValue: 1,
        value: "",
        prefixIcon: const Icon(Icons.receipt_long),
        autoFocus: true),
    FormInput("Date", "date",
        value: DateFormat.yMMMd().format(DateTime.now()),
        showInAppBar: true,
        firstDate: DateTime.now().subtract(const Duration(days: 60)),
        prefixIcon: const Icon(
          Icons.calendar_today,
        )),
    FormInput("Note", "text",
        isRequired: false, helperText: "Shown to Customers and You!"),
  ];
  dynamic formData =
      await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
    return FormView("Add Balance", balanceForm);
  }));
  if (formData == null) {
    return 0;
  }
  App app = App();
  int amount = int.parse(formData["Amount"]);
  amount *= formData["Type"] == "Add" ? 1 : -1;
  Map<String, String> data = {
    "type": formData["Type"] == "Add" ? "cash_added" : "cash_deducted",
    "amount": amount.toString(),
    "client_id": customer.id.toString(),
    "timestamp":
        app.getDateTime(DateFormat.yMMMd().parse(formData["Date"]).toString()),
    "note": formData["Note"],
  };
  await Database.add(Tables.transactions, data);

  Utility.showMessage("Cash ${formData['Type']}ed Successfully");

  // HANDLE Notification
  app.showNotification([
    customer.id
  ], "${app.currencySymbol}${formData["Amount"]} has beed ${formData['Type']}ed!");

  return amount;
}
