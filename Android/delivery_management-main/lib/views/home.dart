import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/views/customers/customers_listview.dart';
import 'package:tiffincrm/views/overview/overview.dart';
import 'package:tiffincrm/views/deliveries/delivery_list_view.dart';
import 'package:tiffincrm/views/revenue_view.dart';
import 'package:flutter/material.dart';

class BarItems {
  IconData icon;
  IconData selectedIcon;
  String title;
  Widget page;
  BarItems(
      {required this.icon,
      required this.title,
      required this.page,
      required this.selectedIcon});
}

List<BarItems> barItems = [
  BarItems(
      icon: Icons.view_comfy_alt_outlined,
      selectedIcon: Icons.view_comfy_alt,
      title: 'Overview',
      page: const Overview()),
  BarItems(
      icon: Icons.delivery_dining_outlined,
      selectedIcon: Icons.delivery_dining,
      title: 'Orders',
      page: const DeliveryList()),
  BarItems(
      icon: Icons.person_outlined,
      selectedIcon: Icons.person,
      title: 'Customers',
      page: const CustomerList()),
  BarItems(
      icon: Icons.account_balance_wallet_outlined,
      selectedIcon: Icons.account_balance_wallet,
      title: 'Finance',
      page: const Revenue()),
];

/// Displays a list of SampleItems.
class HomeView extends StatefulWidget {
  const HomeView({
    super.key,
  });

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  App app = App();
  late List<Widget> _pages;
  int currentIndex = 2;
  bool wantToExit = false;
  late PageController _pageController;

  @override
  void initState() {
    _pages = barItems.map((x) => x.page).toList();

    _pageController = PageController(initialPage: currentIndex);
    super.initState();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;

        if (currentIndex != 2) {
          setState(() {
            currentIndex = 2;
            _pageController.jumpToPage(currentIndex);
          });
        } else {
          if (wantToExit) {
            AppRouter.exit();
          } else {
            setState(() {
              wantToExit = true;
            });
            Utility.showMessage("Press back again to exit");
            Future.delayed(const Duration(seconds: 6), () {
              if (mounted) {
                setState(() {
                  wantToExit = false;
                });
              }
            });
          }
        }
      },
      child: Scaffold(
        body: PageView(
          controller: _pageController,
          //The following parameter is just to prevent
          //the user from swiping to the next page.
          physics: const NeverScrollableScrollPhysics(),
          children: _pages,
        ),
        bottomNavigationBar: BottomNavigationBar(
          type: BottomNavigationBarType.fixed,
          backgroundColor: Colors.white,
          items: List.from(barItems.map((element) {
            return BottomNavigationBarItem(
                icon: Icon(barItems.indexOf(element) == currentIndex
                    ? element.selectedIcon
                    : element.icon),
                label: element.title);
          })),
          currentIndex: currentIndex,
          unselectedItemColor: Colors.grey,
          showUnselectedLabels: true,
          selectedItemColor: ThemeColors.primary,
          onTap: (int index) async {
            setState(() {
              currentIndex = index;
              _pageController.jumpToPage(currentIndex);
            });
            await Logger.logFirebaseEvent(
                "opended_${barItems[index].title.toLowerCase()}_screen", {});
          },
        ),
      ),
    );
  }
}
