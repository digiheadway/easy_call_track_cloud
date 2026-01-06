class Vendor {
  int id;
  String name;
  String phone;
  String? city;
  late String defaultMeal;
  String itemsInPiece = "Chapati";
  late String deliveryManName;
  bool trackTiffinboxes = false;
  String? supportPhone;
  String businessName;
  String? upiId;
  String? fcmToken;
  bool canCustomerLogin = false;
  int? creditLimit;
  String? email;
  int dailyOrderLimit = 25;
  String countryCode = "IN";
  String imealAnnouncement = "";
  int lowBalanceReminderLimit = 0;

  String get displayName => businessName.isEmpty ? name : businessName;

  Vendor(this.id, this.name, this.phone,
      {this.defaultMeal = "",
      this.deliveryManName = "",
      this.city = "",
      this.fcmToken = "",
      this.supportPhone = "",
      this.businessName = "",
      this.trackTiffinboxes = false,
      this.upiId = "",
      this.email = "",
      this.countryCode = "IN",
      this.dailyOrderLimit = 25,
      this.imealAnnouncement = "",
      this.lowBalanceReminderLimit = 0,
      this.canCustomerLogin = false});

  factory Vendor.fromMap(Map<String, dynamic> map) {
    Vendor vendor = Vendor(
      int.parse(map['id']),
      map['name'],
      map['phone'],
      defaultMeal:
          map['default_meal'] ?? "4 Roti, Sabji 1, Sabji 2, Raita, Chawal",
      deliveryManName: map['delivery_man_name'] ?? "",
      city: map['city'],
      supportPhone: map['support_phone'],
      countryCode: map['country_code'],
      trackTiffinboxes: map['track_tiffin_boxes'] == "1" ? true : false,
      businessName: map['business_name'] ?? "",
      fcmToken: map['fcm_token'],
      upiId: map['upi_id'],
      email: map['email'],
      lowBalanceReminderLimit: int.parse(map['low_balance_reminder_limit']),
      imealAnnouncement: map['imeal_announcement'] ?? "",
      dailyOrderLimit: int.parse(map['daily_order_limit']),
      canCustomerLogin: map['can_customer_login'] == "1" ? true : false,
    );
    if (map['credit_limit'] != null) {
      vendor.creditLimit = int.parse(map['credit_limit']);
    }
    return vendor;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'phone': phone,
      'default_meal': defaultMeal,
      'items_in_piece': itemsInPiece,
      'delivery_man_name': deliveryManName,
      'imeal_announcement': imealAnnouncement,
      'low_balance_reminder_limit': lowBalanceReminderLimit.toString(),
      'support_phone': supportPhone,
      'business_name': businessName,
      'upi_id': upiId,
      'credit_limit': creditLimit,
      "country": countryCode,
      'track_tiffin_boxes': trackTiffinboxes ? 1 : 0,
      'can_customer_login': canCustomerLogin ? 1 : 0
    };
  }
}
