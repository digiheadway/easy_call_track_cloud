import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:url_launcher/url_launcher.dart';

class IntroQuiz extends StatefulWidget {
  const IntroQuiz({super.key});

  @override
  State<IntroQuiz> createState() => _IntroQuizState();
}

class _IntroQuizState extends State<IntroQuiz> {
  List<String> questions = [
    "What are you looking for?",
    "How many customers do you have?",
    "Why do you want to try out?"
  ];
  List<String> desc = [
    "To confirm you are at the right place!",
    "To understand your requirements better!",
    "Provide a reason for your interest in trying it out."
  ];
  List<List<String>> options = [
    [
      "Tiffin Service to order food",
      "New Customers for My Business",
      "Just want to try out the app",
      "App to Manage My Business",
    ],
    [
      "Business Not started yet!",
      "1-10 customers",
      "10-30 customers",
      "30-60 customers",
      "60-100 customers",
      "100+ customers",
    ],
    [
      "⁠Going to Start Tiffin Business",
      "⁠For My Existing Tiffin Business",
      "⁠Trying out for curiosity only",
      "⁠Downloaded by Mistake",
      "⁠Other"
    ]
  ];
  List<String> selected = ["", ""];
  int level = 0;
  bool showMsg = false;
  App app = App();

  @override
  Widget build(BuildContext context) {
    int queLevel = level;
    if (level == 1 && selected[0].contains("try")) {
      queLevel = 2; // just changing question, not level
    }
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;

        if (level == 0 && !showMsg) {
          AppRouter.exit();
        }

        handleBack();
      },
      child: Scaffold(
        appBar: AppBar(
          leading: showMsg || level > 0
              ? IconButton(
                  icon: const Icon(Icons.arrow_back),
                  onPressed: handleBack,
                )
              : null,
          title: const Text("Signing up!"),
          actions: [
            IconButton(
              icon: const Icon(Icons.support_agent),
              onPressed: contactSupport,
            )
          ],
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
              crossAxisAlignment: showMsg
                  ? CrossAxisAlignment.center
                  : CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children:
                  showMsg && level == 0 ? errorMessage() : queLayout(queLevel)),
        ),
      ),
    );
  }

  void handleBack() {
    if (level > 0) {
      level--;
    }
    setState(() {
      showMsg = false;
    });
  }

  void contactSupport() async {
    await launchUrl(Uri.parse(Strings.whatsappLink
        .replaceFirst("__text__", "Hi, I want help in onboarding!")
        .replaceFirst("__phone__", Strings.supportPhone)));
  }

  List<Widget> queLayout(int queLevel) {
    return [
      const SizedBox(height: 20),
      Text(questions[queLevel],
          style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: Color(0xFF4B4B4B))),
      const SizedBox(height: 4),
      Text(desc[queLevel], style: const TextStyle()),
      const SizedBox(height: 30),
      Column(
          children:
              List.generate(options[queLevel].length, (int i) => i).map((e) {
        return GestureDetector(
          onTap: () => handleOptionSelect(e, queLevel),
          child: Card(
              shape: OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.grey.shade300),
                  borderRadius: BorderRadius.circular(10)),
              child: ListTile(
                leading: Text(options[queLevel][e],
                    style: const TextStyle(
                        fontSize: 14, fontWeight: FontWeight.w500)),
                trailing: const Icon(Icons.chevron_right),
              )),
        );
      }).toList()),
      const SizedBox(height: 20),
      Center(
        child: Text("Choose any option to continue!",
            style: TextStyle(color: Colors.grey.shade500)),
      ),
    ];
  }

  List<Widget> errorMessage() {
    return [
      const SizedBox(height: 60),
      Center(
        child: SizedBox(
          child: Column(
            children: [
              const Icon(
                Icons.sentiment_dissatisfied,
                size: 50,
              ),
              const SizedBox(height: 10),
              Text(
                "Sorry, it's not for ${selected[0].contains("food") ? "ordering food" : "finding customers"}!",
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 25),
              ),
              const SizedBox(height: 10),
              const Text(
                "It is a CRM app to manage daily operations of a tiffin business like orders and finance management, etc.",
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14),
              ),
            ],
          ),
        ),
      )
    ];
  }

  void handleOptionSelect(int e, int queLevel) async {
    setState(() {
      selected[level] = options[level][e];
    });
    showMsg = false;
    if (level == 0) {
      app.updateVendor({"app_download_purpose": options[level][e]},
          silent: true);
      // handle level based on selected option
      if (e >= 2) {
        level = 1;
      } else {
        showMsg = true;
      }
    } else if (level == 1) {
      app.updateVendor({"business_size": options[queLevel][e]}, silent: true);
      await done();
      return;
    }
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> done() async {
    String? phone = app.prefs.getString("user_phone");
    String countryCode = app.prefs.getString("country_code") ?? "IN";

    Utility.tryCatch(() async {
      app.notifyVendorCreate({
        "vendor_id": app.vendorId.toString(),
        "phone": phone,
        "answers": selected,
        "country_code": countryCode
      });
    });

    await app.prefs.setBool("intro_quiz_pending", false);
    if (mounted) {
      AppRouter.navigateTo("/", removeUntil: true);
    }
  }
}
