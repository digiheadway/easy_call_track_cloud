import 'package:country_picker/country_picker.dart';
import 'package:flutter/services.dart';

import 'package:google_sign_in/google_sign_in.dart';
import 'package:otp_autofill/otp_autofill.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/values/numbers.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../values/styles.dart';

class LoginView extends StatefulWidget {
  const LoginView({super.key});

  @override
  State<LoginView> createState() => _LoginViewState();
}

class _LoginViewState extends State<LoginView> {
  App app = App();
  String otp = "";
  String dialCode = "91";
  bool slideSeen = false;
  bool otpSend = false;
  bool loading = false;
  String googleTester = "9876543210";
  String countryCode = "IN";
  bool shownPhonePickHint = false;
  late OTPTextEditController otpController =
      OTPTextEditController(codeLength: 4);
  TextEditingController phoneController = TextEditingController();

  OTPInteractor otpInteractor = OTPInteractor();

  @override
  void dispose() {
    phoneController.dispose();
    otpController.stopListen();
    otpController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    otpController = OTPTextEditController(
        codeLength: 4,
        otpInteractor: otpInteractor,
        onCodeReceive: (code) {
          // auto sign in after otp read
          handleBtnClick();
        });

    super.initState();
  }

  void contactSupport() async {
    Utility.tryCatch(
      () async {
        await launchUrl(Uri.parse(Strings.whatsappLink
            .replaceFirst("__text__", "Hi, I want help in login!")
            .replaceFirst("__phone__", Strings.supportPhone)));
      },
      silent: true,
      onException: (e) async {
        String url =
            "sms:+${Strings.supportPhone}?body=Hi, I want help in login!";
        await launchUrl(Uri.parse(url));
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xfff8f8f8),
      appBar: !slideSeen
          ? AppBar(
              backgroundColor: Colors.transparent,
              surfaceTintColor: Colors.transparent,
              shadowColor: Colors.transparent,
              toolbarHeight: 0,
              systemOverlayStyle: const SystemUiOverlayStyle(
                // Status bar brightness (optional)
                statusBarIconBrightness:
                    Brightness.dark, // For Android (dark icons)
                statusBarBrightness: Brightness.light, // For iOS (dark icons)
              ),
            )
          : AppBar(
              title: const Text("Login/Signup"),
              actions: [
                IconButton(
                  icon: const Icon(Icons.support_agent),
                  onPressed: contactSupport,
                )
              ],
            ),
      body: !slideSeen
          ? slide()
          : SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const SizedBox(height: 30),
                    if (otpSend)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text("OTP sent to ${phoneController.text}"),
                          const SizedBox(width: 5),
                          GestureDetector(
                            onTap: () {
                              setState(() {
                                otpSend = false;
                              });
                            },
                            child: const Icon(Icons.edit, size: 16),
                          ),
                        ],
                      ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: otpSend ? otpController : phoneController,
                      onTap: () async {
                        if (shownPhonePickHint || countryCode != "IN") {
                          return;
                        }
                        shownPhonePickHint = true;
                        setState(() {});

                        Utility.tryCatch(() async {
                          String? value = await otpInteractor.hint;
                          if (value != null) {
                            setState(() {
                              // Replace all non-digit characters
                              phoneController.text = value
                                  .replaceAll("+$dialCode", "")
                                  .replaceAll(RegExp(r'\D'), "");
                              phoneController.text = phoneController.text
                                  .substring(phoneController.text.length - 10);
                            });
                          }
                        });
                      },
                      keyboardType: TextInputType.phone,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      maxLength: otpSend ? 4 : (countryCode == "IN" ? 10 : 20),
                      decoration: InputDecoration(
                        counterText: "",
                        border: const OutlineInputBorder(
                          borderSide: BorderSide(color: ThemeColors.primary),
                        ),
                        floatingLabelBehavior: FloatingLabelBehavior.always,
                        prefixIcon: otpSend == false
                            ? GestureDetector(
                                onTap: () {
                                  Utility.openCountryPicker(
                                    (Country cntry) {
                                      setState(() {
                                        countryCode = cntry.toJson()['iso2_cc'];
                                        dialCode = cntry.phoneCode;
                                      });
                                    },
                                  );
                                },
                                child: Padding(
                                  padding: const EdgeInsets.all(12.0),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                        "+$dialCode ",
                                        style: const TextStyle(fontSize: 16),
                                      ),
                                      const Icon(Icons.arrow_drop_down)
                                    ],
                                  ),
                                ),
                              )
                            : null,
                        labelText: otpSend ? 'OTP' : 'Phone Number',
                      ),
                    ),
                    const SizedBox(height: 10),
                    loading
                        ? const Center(child: CircularProgressIndicator())
                        : ElevatedButton(
                            style: Styles.mainButton,
                            onPressed: handleBtnClick,
                            child: Text(
                              countryCode != "IN"
                                  ? "Sign in with Google!"
                                  : otpSend
                                      ? "Continue"
                                      : "Get OTP",
                            ),
                          )
                  ],
                ),
              ),
            ),
    );
  }

  Future<void> saveAndMove(
      {required String vendorId,
      required String role,
      required String authToken,
      required String deliveryManId}) async {
    await app.prefs
        .setInt('delivery_man_id', int.tryParse(deliveryManId) ?? -1);
    await app.prefs.setInt('vendor_id', int.tryParse(vendorId) ?? -1);
    await app.prefs.setString('auth_token', authToken);
    await app.prefs.setString('role', role);
    await app.prefs.setString("country_code", countryCode);
    await app.prefs.setString("user_phone", phoneController.text);
    await AppRouter.navigateTo("/", removeUntil: true);
  }

  Future<void> letUserIn(String key, String value, String name) async {
    try {
      dynamic result = await Database.request({
        "action": "letUserIn",
        "data": {
          "key": key,
          "value": value,
          "name": name,
          "countryCode": countryCode,
          "appInstallReferrer": "", // Removed discontinued getReferrer()
          "phone": phoneController.text
        },
      }, silent: true);

      if (result["new_vendor_created"]) {
        await app.prefs.setBool("intro_quiz_pending", true);
        await app.prefs.setBool("show_intro_video", true);
      }

      await saveAndMove(
          vendorId: result["vendor_id"],
          role: result["role"],
          authToken: result["auth_token"],
          deliveryManId: result["delivery_man_id"]);
    } catch (e) {
      setState(() {
        loading = false;
      });
    }
  }

  void handleBtnClick() async {
    if (loading) return;

    if (phoneController.text.length < 4) {
      Utility.showMessage("Please enter valid input");
      return;
    }

    if (phoneController.text == googleTester) {
      await letUserIn("phone", phoneController.text, "New User");
      return;
    }

    setState(() {
      loading = true;
    });
    try {
      if (countryCode == "IN") {
        if (otpSend == false) {
          await Database.sendOTP(phoneController.text);
          Utility.showMessage("OTP sent!");

          setState(() {
            otpSend = true;
            loading = false;
          });
          otpController.startListenUserConsent(
            (code) {
              final exp = RegExp(r'(\d{4})');
              return exp.stringMatch(code ?? '') ?? '';
            },
          );
          return;
        }

        // OTP verification
        await Database.checkOTP(phoneController.text, otpController.text);
        await letUserIn("phone", phoneController.text, "New User");
        return;
      }

      // Google Sign In
      GoogleSignIn googleSignIn = GoogleSignIn(scopes: ['email']);
      GoogleSignInAccount? account = await googleSignIn.signIn();
      if (account != null) {
        await letUserIn(
            "email", account.email, account.displayName ?? "New User");
      }
    } catch (e) {
      Utility.showMessage(e.toString().replaceAll("Exception: ", ""));
    }
    if (mounted) {
      setState(() {
        loading = false;
      });
    }
  }

  Widget slide() {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Center(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: [
              Image.asset("assets/images/intro.png", width: 300),
              const SizedBox(height: 10),
              const Text(
                "Manage Your Business Easily",
                style: Styles.textHeading,
              ),
              const SizedBox(height: 4),
              const Text(
                "#1 CRM built for Tiffin service businesses to\neasily and quickly manage their workflow.",
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xff767676)),
              ),
              const SizedBox(height: 30),
              SizedBox(
                width: double.infinity,
                child: Card(
                  color: const Color(0xffedf2ff),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(Numbers.borderRadius),
                      side: const BorderSide(color: Color(0xff2859d2))),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(22.0, 10.0, 14.0, 10.0),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        [
                          "Manage Daily Deliveries",
                          Icons.manage_history_outlined
                        ],
                        [
                          "Manage Customers's Data",
                          Icons.manage_accounts_outlined
                        ],
                        [
                          "Web App for Your Customers",
                          Icons.phone_android_outlined
                        ],
                        ["App for Delivery Man", Icons.route],
                        ["Everything for Tiffin Business", Icons.checklist]
                      ]
                          .map((e) => Padding(
                                padding: const EdgeInsets.only(bottom: 5),
                                child: getPropertyWithIcon(
                                    e[0] as String, e[1] as IconData,
                                    fontSize: 16,
                                    fontWeight: FontWeight.w500,
                                    textColor: const Color(0xff343434)),
                              ))
                          .toList(),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 30),
              SizedBox(
                width: 400,
                child: ElevatedButton.icon(
                  iconAlignment: IconAlignment.end,
                  icon: const Icon(Icons.arrow_forward),
                  style: Styles.mainButton,
                  onPressed: () {
                    setState(() {
                      slideSeen = true;
                    });
                  },
                  label: const Text("Login/Signup Now"),
                ),
              ),
              const SizedBox(height: 5),
              TextButton(
                onPressed: contactSupport,
                child: Text("Ask Questions!", style: Styles.buttonText),
              )
            ],
          ),
        ),
      ),
    );
  }
}
