enum Tables {
  vendorExpenses("vendor_expenses"),
  deliveryMen("delivery_men"),
  vendors("vendors"),
  transactions("transactions"),
  transactionsView("transactions_view"),
  customerTransactionsView("customer_transactions_view"),
  deliveries("deliveries"),
  orders("orders"),
  realOrders("real_orders"),
  orderTemplates("order_templates"),
  clients("clients"),
  settings("settings"),
  appLogs("app_logs"),
  customersView("customers_view"),
  ;

  final String name;
  const Tables(this.name);
}
