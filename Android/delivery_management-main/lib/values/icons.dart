import 'package:flutter/material.dart';

class MyIcons {
  static IconData get(String name) {
    if (name.contains("Tiffin")) return MyIcons.tiffinCounts;
    if (name.startsWith("Balance")) return MyIcons.balance;
    if (name.startsWith("Deliveries in last")) return MyIcons.orders;
    if (name.startsWith("All Upcoming Deliveries")) return Icons.account_circle;
    return _get[name] ?? Icons.label;
  }

  static const Map<String, IconData> _get = {
    "App Notifications": Icons.notifications,
    "delivered": Icons.delivery_dining,
    'wasted': Icons.cancel,
    'disputed': Icons.chat,
    'refund': Icons.money_off,
    "cancelled": Icons.cancel,
    "awaiting": Icons.pending_actions,
    "Business Name": Icons.business,
    "Support Phone": Icons.phone,
    "UPI ID": Icons.qr_code_scanner,
    "Low Balance Reminder Limit": MyIcons.creditLimit,
    "iMeals.in - Check Demo": Icons.web,
    "Share With Customers": Icons.link,
    "Credit Limit": MyIcons.creditLimit,
    "Location": Icons.location_on,
    "Imeal Announcement": Icons.campaign,
    "Country": Icons.location_on,
    "Track Tiffin Boxes": MyIcons.tiffinCounts,
    "Direct Login Link": Icons.link,
    "Contact on Primary Phone": Icons.contact_emergency
  };
  static const IconData zone = Icons.map_outlined;

  static const IconData address = Icons.location_on_outlined;
  static const IconData phone = Icons.phone;
  static const IconData phone2 = Icons.phone_in_talk;

  static const IconData customerNote = Icons.three_p_outlined;
  static const IconData orders = Icons.shopping_cart_outlined;
  static const IconData creditLimit = Icons.payment_outlined;

  static const IconData tiffinCounts = Icons.shopping_bag;

  static const IconData status = Icons.person;

  static IconData notifications = Icons.notifications;

  static IconData balance = Icons.account_balance_wallet_outlined;
}
