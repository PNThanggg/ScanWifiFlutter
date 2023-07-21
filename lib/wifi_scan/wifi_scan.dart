import 'dart:async';

import 'package:flutter/services.dart';

part 'src/accesspoint.dart';
part 'src/can.dart';

class WiFiScan {
  WiFiScan._();

  static final instance = WiFiScan._();

  final _channel = const MethodChannel('wifi_scan');
  final _scannedResultsAvailableChannel = const EventChannel('wifi_scan/onScannedResultsAvailable');
  Stream<List<WiFiAccessPoint>>? _onScannedResultsAvailable;

  Future<CanStartScan> canStartScan({bool askPermissions = true}) async {
    final canCode = await _channel.invokeMethod<int>("canStartScan", {
      "askPermissions": askPermissions,
    });
    return _deserializeCanStartScan(canCode);
  }

  Future<bool> startScan() async {
    final isSuccess = await _channel.invokeMethod<bool>("startScan");
    return isSuccess!;
  }

  Future<CanGetScannedResults> canGetScannedResults({bool askPermissions = true}) async {
    final canCode = await _channel.invokeMethod<int>("canGetScannedResults", {
      "askPermissions": askPermissions,
    });
    return _deserializeCanGetScannedResults(canCode);
  }

  Future<List<WiFiAccessPoint>> getScannedResults() async {
    final scannedResults = await _channel.invokeListMethod<Map>("getScannedResults");
    return scannedResults!.map((map) => WiFiAccessPoint._fromMap(map)).toList(growable: false);
  }

  Stream<List<WiFiAccessPoint>> get onScannedResultsAvailable =>
      _onScannedResultsAvailable ??= _scannedResultsAvailableChannel.receiveBroadcastStream().map((event) {
        if (event is Error) throw event;
        if (event is List) {
          return event.map((map) => WiFiAccessPoint._fromMap(map)).toList(growable: false);
        }
        return const <WiFiAccessPoint>[];
      });
}
