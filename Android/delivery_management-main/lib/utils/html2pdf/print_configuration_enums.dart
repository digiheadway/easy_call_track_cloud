enum PrintSize {
  a0,
  a1,
  a2,
  a3,
  a4,
  a5,
  a6,
  a7,
  a8,
  a9,
  a10,
}

extension PrintSizeExt on PrintSize {
  /// Returns the printing pixel dimensions for `72 PPI`
  List<int> get getDimensionsInPixels {
    switch (this) {
      case PrintSize.a0:
        return [2384, 3370];
      case PrintSize.a1:
        return [1684, 2384];
      case PrintSize.a2:
        return [1191, 1684];
      case PrintSize.a3:
        return [842, 1191];
      case PrintSize.a4:
        return [595, 842];
      case PrintSize.a5:
        return [420, 595];
      case PrintSize.a6:
        return [298, 420];
      case PrintSize.a7:
        return [210, 298];
      case PrintSize.a8:
        return [147, 210];
      case PrintSize.a9:
        return [105, 147];
      case PrintSize.a10:
        return [74, 105];
    }
  }

  /// Returns Key for android implementation
  String get printSizeKey {
    switch (this) {
      case PrintSize.a0:
        return "A0";
      case PrintSize.a1:
        return "A1";
      case PrintSize.a2:
        return "A2";
      case PrintSize.a3:
        return "A3";
      case PrintSize.a4:
        return "A4";
      case PrintSize.a5:
        return "A5";
      case PrintSize.a6:
        return "A6";
      case PrintSize.a7:
        return "A7";
      case PrintSize.a8:
        return "A8";
      case PrintSize.a9:
        return "A9";
      case PrintSize.a10:
        return "A10";
    }
  }
}

enum PrintOrientation {
  portrait,
  landscape,
}

extension PrintOrientationExt on PrintOrientation {
  /// Returns the index for getting width of print frame from array of
  int get getWidthDimensionIndex {
    switch (this) {
      case PrintOrientation.landscape:
        return 1;
      case PrintOrientation.portrait:
        return 0;
    }
  }

  /// Returns the index for getting height of print frame from array of
  int get getHeightDimensionIndex {
    switch (this) {
      case PrintOrientation.landscape:
        return 0;
      case PrintOrientation.portrait:
        return 1;
    }
  }

  /// Returns Key for android implementation
  String get orientationKey {
    switch (this) {
      case PrintOrientation.landscape:
        return "LANDSCAPE";
      case PrintOrientation.portrait:
        return "PORTRAIT";
    }
  }
}
