import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
  // 定义 MethodChannel
  static const platform = MethodChannel('com.example.dreamy_color/channel');
  String _message = "等待来自 Native 的消息";

  // 初始化方法
  @override
  void initState() {
    super.initState();
    // 设置接收来自 Native 的消息处理器
    platform.setMethodCallHandler(_handleMethodCall);
  }

  // 处理来自 Native 的消息
  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'messageFromNative':
        setState(() {
          _message = call.arguments.toString();
        });
        return "Flutter 已收到消息";
      default:
        throw PlatformException(
          code: 'NotImplemented',
          message: '没有实现 ${call.method} 方法',
        );
    }
  }

  // 发送消息到 Native
  Future<void> _sendMessageToNative() async {
    try {
      final String result = await platform.invokeMethod('sendMessage', {
        'message': 'Hello from Flutter! Counter: $_counter'
      });
      setState(() {
        _message = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _message = "调用 Native 方法失败: ${e.message}";
      });
    }
  }

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
    // 计数器更新后发送消息到 Native
    _sendMessageToNative();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text('You have pushed the button this many times:'),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 20),
            Text(
              '来自 Native 的消息: $_message',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            ElevatedButton(
              onPressed: _sendMessageToNative,
              child: const Text('发送消息到 Native'),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
    );
  }
}
