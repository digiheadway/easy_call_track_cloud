import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../models/customer.dart';

class DistanceBetweenMarkers extends StatefulWidget {
  final List<Customer> customers;
  const DistanceBetweenMarkers(this.customers, {super.key});

  @override
  State<DistanceBetweenMarkers> createState() => _DistanceBetweenMarkersState();
}

class _DistanceBetweenMarkersState extends State<DistanceBetweenMarkers> {
  late Marker marker1;
  late Marker marker2;
  late Polyline polyline;

  @override
  void initState() {
    super.initState();
    marker1 = const Marker(
      markerId: MarkerId('Marker 1'),
      position: LatLng(37.7749, -122.4194),
    );
    marker2 = const Marker(
      markerId: MarkerId('Marker 2'),
      position: LatLng(37.8024, -122.4056),
    );
    polyline = Polyline(
      polylineId: const PolylineId('Polyline'),
      points: widget.customers
          .where((e) => e.location != null)
          .map((e) => e.location!)
          .toList(),
      color: Colors.red,
      geodesic: true,
      width: 2,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: GoogleMap(
        initialCameraPosition: CameraPosition(
          target: polyline.points.first,
          zoom: 12,
        ),
        // markers: {marker1, marker2},
        polylines: {polyline},
      ),
    );
  }
}
