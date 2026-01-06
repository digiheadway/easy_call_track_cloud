import 'dart:async';
import 'package:geolocator/geolocator.dart';

import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:google_places_flutter/google_places_flutter.dart';
import 'package:google_places_flutter/model/prediction.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/get_location.dart';
import 'package:tiffincrm/models/customer.dart';

import '../../../utils/utility.dart';

class SetLocationView extends StatefulWidget {
  final Customer customer;
  const SetLocationView(this.customer, {super.key});

  @override
  State<SetLocationView> createState() => SetLocationViewState();
}

class SetLocationViewState extends State<SetLocationView> {
  late Customer customer;
  bool loaded = false;
  late Completer<GoogleMapController> _controller =
      Completer<GoogleMapController>();

  final controller = TextEditingController();
  FocusNode focusNode = FocusNode();
  Marker? marker;

  late CameraPosition _kGooglePlex;

  @override
  initState() {
    customer = widget.customer;
    if (customer.location != null) {
      updateMarker(customer.location as LatLng);
      _kGooglePlex = CameraPosition(
        target: customer.location as LatLng,
        zoom: 14.4746,
      );

      setState(() {
        loaded = true;
      });
    } else {
      determinePosition().then((Position pos) {
        _kGooglePlex = CameraPosition(
          target: LatLng(pos.latitude, pos.longitude),
          zoom: 14.4746,
        );
        setState(() {
          loaded = true;
        });
      }).catchError((e) async {
        if (!mounted) return;
        await Utility.showMessage(e.toString());
        AppRouter.goBack();
      });
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    InputBorder border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: Colors.white, width: 0.0),
    );
    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        actions: [
          IconButton(
              onPressed: () async {
                if (marker == null) {
                  Utility.showMessage("Please select a location first");
                  return;
                }
                bool result = await Utility.getConfirmation(
                  "Save Location",
                  "Are you sure?",
                );
                if (!result) {
                  return;
                }
                AppRouter.goBack(marker?.position);
              },
              icon: const Icon(Icons.save))
        ],
        titleSpacing: 0,
        title: GooglePlaceAutoCompleteTextField(
          containerVerticalPadding: 0,
          containerHorizontalPadding: 0,
          boxDecoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(
                color: Colors.grey,
              ),
              borderRadius: BorderRadius.circular(8)),
          textEditingController: controller,
          googleAPIKey: "AIzaSyBBFiMHn6_7kI_mzH_8qVzOS52Xdlvp2aY",
          itemBuilder: (context, index, Prediction prediction) {
            return Container(
              padding: const EdgeInsets.all(10),
              child: Row(
                children: [
                  const Icon(Icons.location_on),
                  const SizedBox(
                    width: 7,
                  ),
                  Expanded(child: Text(prediction.description ?? ""))
                ],
              ),
            );
          },
          inputDecoration: InputDecoration(
            fillColor: Colors.white,
            enabledBorder: border,
            focusedBorder: border,
            border: border,
            hintText: "Enter Location",
          ),
          debounceTime: 800, // default 600 ms,
          isLatLngRequired:
              true, // if you required coordinates from place detail
          getPlaceDetailWithLatLng: (Prediction prediction) async {
            // this method will return latlng with place detail
            LatLng latLng = LatLng(double.parse(prediction.lat.toString()),
                double.parse(prediction.lng.toString()));
            _goToTheLake(CameraPosition(
                bearing: 192.8334901395799,
                tilt: 59.440717697143555,
                zoom: 19.151926040649414,
                target: latLng));
            focusNode.requestFocus();
            updateMarker(latLng);
          }, // this callback is called when isLatLngRequired is true
          itemClick: (Prediction prediction) async {
            controller.text = prediction.description.toString();

            controller.selection = TextSelection.fromPosition(
                TextPosition(offset: prediction.description.toString().length));
          },
          // if you want to add seperator between list items
          seperatedBuilder: const Divider(),
          // want to show close icon
          isCrossBtnShown: true,
          // optional container padding
          focusNode: focusNode,
        ),
      ),
      body: !loaded
          ? const Center(child: CircularProgressIndicator())
          : GoogleMap(
              markers: {marker ?? const Marker(markerId: MarkerId("1"))},
              myLocationEnabled: true,
              mapType: MapType.normal,
              initialCameraPosition: _kGooglePlex,
              onMapCreated: (GoogleMapController controller) {
                _controller.complete(controller);
              },
              onTap: (latLng) {
                updateMarker(latLng);
              },
            ),
    );
  }

  updateMarker(LatLng latLng) {
    setState(() {
      marker = Marker(
        markerId: const MarkerId("1"),
        position: latLng,
        draggable: true,
        onDragStart: updateMarker,
      );
    });
  }

  @override
  void dispose() {
    _controller = Completer();
    focusNode.dispose();
    super.dispose();
  }

  Future<void> _goToTheLake(CameraPosition position) async {
    final GoogleMapController controller = await _controller.future;
    await controller.moveCamera(CameraUpdate.newCameraPosition(position));
  }
}
