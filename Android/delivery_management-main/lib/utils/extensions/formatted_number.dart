extension FormattedNumber on num {
  num format() {
    return num.parse(
        this == roundToDouble() ? toStringAsFixed(0) : toStringAsFixed(3));
  }
}
