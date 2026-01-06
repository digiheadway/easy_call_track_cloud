extension StringExtension on String {
  String capitalize() {
    if (this == "") return "";
    if (length == 1) return toUpperCase();

    return trim().toLowerCase().split(" ").map((e) {
      return "${e[0].toUpperCase()}${e.substring(1)}";
    }).join(" ");
  }
}
