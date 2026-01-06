import 'dart:io';

import 'package:open_filex/open_filex.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:whatsapp_share/whatsapp_share.dart';

import '../values/strings.dart';
import 'html2pdf/html_to_pdf.dart';
import 'html2pdf/pdf_print_configuration.dart';
import 'html2pdf/print_configuration_enums.dart';

Future<void> html2pdf(String html, String targetFileName,
    {String sharePhone = ""}) async {
  Utility.showMessage("Generating PDF...");
  Utility.tryCatch(() async {
    PermissionStatus _ = await Permission.storage.request();
    // if (status != PermissionStatus.granted) {
    //   print("Permission denied");
    //   return;
    // }

    Directory dir = Directory("/storage/emulated/0/Download/TiffinCRM");
    if (targetFileName == "Backup") {
      dir = Directory("/storage/emulated/0/Download/TiffinCRM/Backup");
    }
    if (!dir.existsSync()) {
      await dir.create(recursive: true);
    }

    targetFileName = targetFileName.replaceAll("/", "-");
    targetFileName += " ${DateTime.now().format('MMM yyyy-MM-dd_HH-mm a')}";

    File pdf = await FlutterHtmlToPdf.convertFromHtmlContent(
      htmlContent: Strings.tableStyling + html,
      printPdfConfiguration: PrintPdfConfiguration(
        targetDirectory: dir.path,
        targetName: targetFileName,
        printSize: PrintSize.a3,
        printOrientation: PrintOrientation.portrait,
      ),
    );

    if (sharePhone.isEmpty) {
      OpenResult result = await OpenFilex.open(pdf.path);
      if (result.type != ResultType.done) {
        Utility.showMessage(result.message);
      }
    } else {
      try {
        bool? wbInstalled =
            await WhatsappShare.isInstalled(package: Package.businessWhatsapp);
        await WhatsappShare.shareFile(
            phone: sharePhone,
            filePath: [pdf.path],
            package: wbInstalled == true
                ? Package.businessWhatsapp
                : Package.whatsapp);
      } catch (e) {
        Utility.showMessage("Whatsapp not found!");
      }
    }
  });
}
