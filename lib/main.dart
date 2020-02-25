import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        title: 'Flutter Demo',
        theme: ThemeData(
          primarySwatch: Colors.blue,
        ),
        home: Scaffold(
          appBar: AppBar(
            title: const Text("Activity Transition App"),
          ),
          body: HomePage(),
        ));
  }
}

class HomePage extends StatefulWidget {
  HomePage({Key key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String initialData = "Initial Data";
  var dataList = List<String>();

  var channel = EventChannel('sri');

  @override
  void initState() {
    super.initState();
    setChannel();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: dataList.isEmpty
          ? Text("initial data")
          : ListView.builder(
              itemCount: dataList.length,
              itemBuilder: (context, item) {
                return ListTile(title: Text(dataList[item]));
              }),
    );
  }

  void setChannel() {
    print('Setting up channel');
    channel.receiveBroadcastStream().listen((dynamic event) {
      print('Received event: $event');
      setState(() {
        dataList.add(event);
      });
    }, onError: (dynamic error) {
      print('Received error: ${error.message}');
    });
  }
}
