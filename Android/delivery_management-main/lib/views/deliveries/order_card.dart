import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/navigate.dart';
import 'package:tiffincrm/utils/phone_call.dart';

import '../../components/card_chip.dart';
import '../../components/property_with_icon.dart';
import '../../components/veg_icon.dart';
import '../../models/order.dart';
import '../../utils/app.dart';
import '../../utils/utility.dart';
import '../../values/icons.dart';
import '../../values/numbers.dart';
import '../../values/styles.dart';

class OrderCard extends StatefulWidget {
  Order order;
  bool isSelected;
  String curStatus;
  final Function onSelect;
  final List<Widget> menuItems;
  OrderCard(
      {required this.order,
      required this.curStatus,
      required this.isSelected,
      required this.onSelect,
      required this.menuItems,
      super.key});

  @override
  State<OrderCard> createState() => _OrderCardState();
}

class _OrderCardState extends State<OrderCard> {
  double swipeProgress = 0;
  App app = App();

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      key: ValueKey("card_${widget.order.id}"),
      onTap: () {
        if (!["refund", "excluded"].contains(widget.curStatus)) {
          widget.onSelect();
        }
      },
      child: Card(
          shape: Styles.cardShape(widget.isSelected),
          child: ClipRRect(
            clipBehavior: Clip.hardEdge,
            child: Dismissible(
              onUpdate: (details) {
                setState(() {
                  swipeProgress = details.progress;
                });
              },
              key: ValueKey(widget.order.id),
              direction: DismissDirection.horizontal,
              confirmDismiss: (direction) =>
                  handleCardDismiss(direction, widget.order),
              background: Container(
                decoration: BoxDecoration(
                    color:
                        Color.fromRGBO(255, 152, 0, 0.3 + 0.7 * swipeProgress),
                    borderRadius: BorderRadius.circular(Numbers.borderRadius)),
                child: const Align(
                  alignment: Alignment.centerLeft,
                  child: Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Icon(
                      Icons.navigation_rounded,
                      color: Colors.white,
                      size: 30,
                    ),
                  ),
                ),
              ),
              secondaryBackground: Container(
                decoration: BoxDecoration(
                    color:
                        Color.fromRGBO(276, 175, 80, 0.3 + 0.7 * swipeProgress),
                    borderRadius: BorderRadius.circular(Numbers.borderRadius)),
                child: const Align(
                  alignment: Alignment.centerRight,
                  child: Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Icon(
                      Icons.call,
                      color: Colors.white,
                      size: 30,
                    ),
                  ),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.only(
                    left: 15, right: 5, top: 10, bottom: 10),
                child: IntrinsicHeight(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Flexible(
                                    child: Text(
                                      widget.order.customerName,
                                      textScaler: const TextScaler.linear(1.1),
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  if (widget.order.isVegan &&
                                      (app.prefs.getBool(
                                              "show_delivery_veg_symbol") ??
                                          true))
                                    Row(
                                      children: [
                                        const SizedBox(width: 10),
                                        vegIcon,
                                      ],
                                    ),
                                ],
                              ),
                              const SizedBox(height: 5),
                              if (widget.curStatus == "excluded")
                                getPropertyWithIcon(
                                    widget.order.excludeReason
                                        .replaceAll("_", " ")
                                        .capitalize(),
                                    Icons.block,
                                    fontWeight: FontWeight.bold),
                              getPropertyWithIcon(
                                  widget.order.zone, MyIcons.zone),
                              if (app.prefs.getBool("show_delivery_balance") ??
                                  true)
                                getPropertyWithIcon(
                                    widget.order.customerBalance.toString(),
                                    MyIcons.balance),
                              if (app.prefs.getBool("show_delivery_items") ??
                                  true)
                                getPropertyWithIcon(
                                    "${widget.order.items} (${app.currencySymbol}${widget.order.price})",
                                    Icons.shopping_cart_outlined),
                              if (app.prefs.getBool("show_delivery_address") ??
                                  true)
                                getPropertyWithIcon(
                                    widget.order.address, MyIcons.address),
                              if (app.prefs
                                      .getBool("show_delivery_order_note") ??
                                  true)
                                getPropertyWithIcon(
                                    widget.order.note, Icons.notes_outlined),
                              if (app.prefs
                                      .getBool("show_delivery_customer_note") ??
                                  true)
                                getPropertyWithIcon(widget.order.customerNote,
                                    MyIcons.customerNote),
                              if ((app.prefs.getBool(
                                          "show_delivery_tiffin_box_to_collect") ??
                                      true) &&
                                  app.vendor.trackTiffinboxes)
                                getPropertyWithIcon(
                                    widget.order.customerTiffinCounts
                                        .toString(),
                                    MyIcons.tiffinCounts),
                              if (app.prefs.getBool(
                                      "show_delivery_customer_labels") ??
                                  true)
                                cardChips(widget.order.customerTags.split(",")),
                              if (widget.curStatus == "cancelled" &&
                                  widget.order.cancelledFromImeal)
                                Padding(
                                  padding: const EdgeInsets.only(top: 3),
                                  child: Text(
                                      "Cancelled From Imeals | ${DateFormat("hh:mm a").format(widget.order.updatedOn!)}",
                                      style: const TextStyle(
                                        color: Colors.black38,
                                      )),
                                ),
                            ]),
                      ),
                      Container(
                        color: Colors.white,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 8),
                          child: Column(
                              mainAxisSize: MainAxisSize.max,
                              crossAxisAlignment: CrossAxisAlignment.end,
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: widget.menuItems),
                        ),
                      )
                    ],
                  ),
                ),
              ),
            ),
          )),
    );
  }

  Future<bool?> handleCardDismiss(
      DismissDirection direction, Order item) async {
    bool performAction = true;
    String message = direction == DismissDirection.endToStart
        ? "Calling to ${item.customerName}"
        : "Navigating";
    if (message.contains("Navigating") && item.location == null) {
      Utility.showMessage("Location not set!");
      return false;
    }

    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(
          showCloseIcon: false,
          content: Text("$message in 2 seconds..."),
          duration: const Duration(seconds: 2),
          action: SnackBarAction(
            label: "Cancel",
            onPressed: () {
              performAction = false;
              ScaffoldMessenger.of(context).hideCurrentSnackBar();
            },
          ),
        ))
        .closed
        .then((value) async {
      if (!performAction) return;
      Utility.showMessage("$message...");
      if (direction == DismissDirection.endToStart) {
        await call(item.customerPhone);
      } else {
        try {
          await navigate(item.location!);
        } catch (e) {
          Utility.showMessage(e.toString());
        }
      }
    });
    return false;
  }
}
