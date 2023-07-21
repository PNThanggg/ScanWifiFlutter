part of '../wifi_scan.dart';

enum CanStartScan {
  notSupported,
  yes,
  noLocationPermissionRequired,
  noLocationPermissionDenied,
  noLocationPermissionUpgradeAccuracy,
  noLocationServiceDisabled,
  failed,
}

CanStartScan _deserializeCanStartScan(int? canCode) {
  switch (canCode) {
    case 0:
      return CanStartScan.notSupported;
    case 1:
      return CanStartScan.yes;
    case 2:
      return CanStartScan.noLocationPermissionRequired;
    case 3:
      return CanStartScan.noLocationPermissionDenied;
    case 4:
      return CanStartScan.noLocationPermissionUpgradeAccuracy;
    case 5:
      return CanStartScan.noLocationServiceDisabled;
  }
  throw UnsupportedError("$canCode cannot be serialized to CanStartScan");
}

enum CanGetScannedResults {
  notSupported,
  yes,
  noLocationPermissionRequired,
  noLocationPermissionDenied,
  noLocationPermissionUpgradeAccuracy,
  noLocationServiceDisabled,
}

CanGetScannedResults _deserializeCanGetScannedResults(int? canCode) {
  switch (canCode) {
    case 0:
      return CanGetScannedResults.notSupported;
    case 1:
      return CanGetScannedResults.yes;
    case 2:
      return CanGetScannedResults.noLocationPermissionRequired;
    case 3:
      return CanGetScannedResults.noLocationPermissionDenied;
    case 4:
      return CanGetScannedResults.noLocationPermissionUpgradeAccuracy;
    case 5:
      return CanGetScannedResults.noLocationServiceDisabled;
  }
  throw UnsupportedError(
      "$canCode cannot be serialized to CanGetScannedNetworks");
}
