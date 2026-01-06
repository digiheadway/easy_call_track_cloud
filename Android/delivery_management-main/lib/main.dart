import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:tiffincrm/firebase_options.dart';
import 'package:tiffincrm/my_app.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';
import 'package:tiffincrm/views/customers/customer_analytics.dart';
import 'package:tiffincrm/views/deliveries/delivery_list_view.dart';
import 'package:tiffincrm/views/home.dart';
import 'package:tiffincrm/views/initial/intro_quiz.dart';
import 'package:tiffincrm/views/initial/intro_video.dart';
import 'package:tiffincrm/views/initial/login_view.dart';
import 'package:tiffincrm/views/sidebar/imeals_screen.dart';
import 'package:tiffincrm/views/sidebar/learn_more.dart';
import 'package:tiffincrm/views/sidebar/order_template_listview.dart';

import 'utils/app_router.dart';
import 'utils/utility.dart';
import 'views/customers/contacts_list_view.dart';
import 'views/sidebar/deliverymen_listview.dart';
import 'views/sidebar/settings.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
  ]);

//Setting SysemUIOverlay
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    systemStatusBarContrastEnforced: false,
    systemNavigationBarContrastEnforced: true,
    systemNavigationBarColor: Colors.white,
    systemNavigationBarDividerColor: Colors.white,
  ));

//Setting SystmeUIMode
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, ]);
  // await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  await Logger.setUpCrashReporting();

  runApp(
    MaterialApp(
      debugShowCheckedModeBanner: false,
      scaffoldMessengerKey: snackbarKey,
      navigatorKey: navigatorKey,
      theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            secondary: Colors.white,
            surface: Colors.white,
            seedColor: ThemeColors.primary,
          ),
          useMaterial3: true,
          dropdownMenuTheme: const DropdownMenuThemeData(
              textStyle: TextStyle(color: Colors.white),
              inputDecorationTheme: InputDecorationTheme(
                  labelStyle: TextStyle(
                color: Colors.white,
              ))),
          appBarTheme: const AppBarTheme(
              iconTheme: IconThemeData(color: Colors.white),
              elevation: 5,
              shadowColor: Colors.grey,
              titleTextStyle: TextStyle(
                  fontSize: 18,
                  color: Colors.white,
                  fontWeight: FontWeight.bold),
              backgroundColor: ThemeColors.primary),
          tabBarTheme: const TabBarTheme(
            indicatorColor: ThemeColors.primary,
            labelColor: Colors.white,
            labelStyle: TextStyle(fontWeight: FontWeight.bold),
            unselectedLabelStyle:
                TextStyle(color: Colors.white, fontWeight: FontWeight.w400),
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
              style:
                  TextButton.styleFrom(foregroundColor: ThemeColors.primary)),
          datePickerTheme: DatePickerThemeData(
              backgroundColor: Colors.white,
              surfaceTintColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(Numbers.borderRadius),
              )),
          scaffoldBackgroundColor: const Color.fromARGB(255, 240, 239, 248),
          textTheme: GoogleFonts.interTextTheme()
              .copyWith(bodyMedium: const TextStyle(color: Color(0xFF323232))),
          // textTheme:
          //     const TextTheme(bodyMedium: TextStyle(color: Color(0xFF323232))),
          inputDecorationTheme: InputDecorationTheme(
            contentPadding: const EdgeInsets.only(left: 10),
            filled: true,
            fillColor: Colors.white,
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(Numbers.borderRadius),
              borderSide: const BorderSide(color: ThemeColors.primary),
            ),
            outlineBorder: const BorderSide(color: ThemeColors.primary),
            disabledBorder: OutlineInputBorder(
                borderSide: const BorderSide(color: ThemeColors.borderColor),
                borderRadius: BorderRadius.circular(Numbers.borderRadius)),
            enabledBorder: OutlineInputBorder(
                borderSide: const BorderSide(color: ThemeColors.borderColor),
                borderRadius: BorderRadius.circular(Numbers.borderRadius)),
          ),
          textButtonTheme: TextButtonThemeData(
            style: TextButton.styleFrom(
              foregroundColor: ThemeColors.primary,
              textStyle: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
          snackBarTheme: const SnackBarThemeData(showCloseIcon: true),
          dialogTheme: const DialogTheme(
            elevation: 2,
            surfaceTintColor: Colors.white,
            backgroundColor: Colors.white,
            shape: RoundedRectangleBorder(
                borderRadius:
                    BorderRadius.all(Radius.circular(Numbers.borderRadius))),
          ),
          floatingActionButtonTheme: FloatingActionButtonThemeData(
              shape: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(
                    Numbers.borderRadius,
                  ),
                  borderSide: const BorderSide(color: Colors.transparent)),
              foregroundColor: Colors.white,
              backgroundColor: ThemeColors.primary,
              extendedTextStyle: const TextStyle(fontWeight: FontWeight.bold)),
          bottomSheetTheme: const BottomSheetThemeData(
              backgroundColor: Colors.transparent, elevation: 0),
          listTileTheme: ListTileThemeData(
              shape: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(
                    Numbers.borderRadius,
                  ),
                  borderSide: const BorderSide(color: Colors.transparent)),
              contentPadding: const EdgeInsets.only(left: 12, right: 12),
              horizontalTitleGap: 8,
              leadingAndTrailingTextStyle: const TextStyle(
                  fontWeight: FontWeight.w500, color: ThemeColors.normalBlack),
              titleTextStyle: const TextStyle(
                  fontWeight: FontWeight.w500, color: ThemeColors.normalBlack)),
          bottomNavigationBarTheme: const BottomNavigationBarThemeData(
              elevation: 10,
              selectedIconTheme: IconThemeData(size: 26),
              selectedLabelStyle: TextStyle(fontWeight: FontWeight.bold)),
          cardTheme: CardTheme(
              elevation: 0.6,
              margin: const EdgeInsets.only(bottom: 9),
              color: Colors.white,
              surfaceTintColor: Colors.white,
              shape: OutlineInputBorder(
                borderRadius: BorderRadius.circular(Numbers.borderRadius),
                borderSide: const BorderSide(color: ThemeColors.borderColor),
              )),
          popupMenuTheme: const PopupMenuThemeData(
              labelTextStyle: WidgetStatePropertyAll(TextStyle(
                  fontWeight: FontWeight.w500, color: Colors.black87)),
              color: Colors.white,
              surfaceTintColor: Colors.white)),
      supportedLocales: const [
        Locale('en', ''), // English, no country code
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      navigatorObservers: [
        FirebaseAnalyticsObserver(analytics: FirebaseAnalytics.instance)
      ],
      onGenerateRoute: (settings) {
        Map<String, WidgetBuilder> routes = {
          "/": (context) => const MyApp(),
          "/login": (context) => const LoginView(),
          "/intro_quiz": (context) => const IntroQuiz(),
          "/intro_video": (context) => const YoutubeVideoScreen(),
          "/home": (context) => const HomeView(),
          "/deliveries": (context) => const DeliveryList(),
          "/imeals": (context) => const ImealsScreen(),
          "/order_templates": (context) => const OrderTemplates(),
          "/templates": (context) => const OrderTemplates(),
          "/contacts": (context) => const ContactsListView(),
          "/deliverymen": (context) => const DeliveryMenList(),
          "/learn": (context) => const LearnMore(),
          "/settings": (context) => const SettingsView(),
          "/customer_analytics": (context) => const CustomerAnalytics(),
        };

        Uri uri = Uri.parse(settings.name!);
        // String routeName = "${uri.host}${uri.path}";
        Logger.logScreenView(uri.path);
        // Logger.logInDB("screen_view: ${settings.name}");
        // if (!routeName.startsWith("/")) {
        //   routeName = "/$routeName";
        // }
        // if (!routes.containsKey(routeName)) {
        //   return null;
        // }
        return MaterialPageRoute(builder: (routes[uri.path]!));
      },
    ),
  );
}
