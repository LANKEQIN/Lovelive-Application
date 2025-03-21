import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  // 定义 MethodChannel 用于与原生代码通信
  static const platform = MethodChannel('com.example.dreamy_color/channel');
  
  // 状态变量
  bool _showThemeDialog = false;
  bool _showTextSizeDialog = false;
  bool _showColorThemeDialog = false;
  bool _showDisclaimer = false;
  int _remainingTime = 7;
  bool _showDarkRealmSnackbar = false;
  
  // 设置选项
  String _themeMode = 'FOLLOW_SYSTEM'; // FOLLOW_SYSTEM, LIGHT, DARK
  String _textSize = 'FOLLOW_SYSTEM'; // FOLLOW_SYSTEM, SMALL, MEDIUM, LARGE
  String _colorTheme = 'MATERIAL_YOU'; // MATERIAL_YOU, PURPLE, ORANGE, etc.
  bool _showPinyin = false;
  String _versionInfo = '';
  String _messageFromNative = '';

  @override
  void initState() {
    super.initState();
    // 设置接收来自 Native 的消息处理器
    platform.setMethodCallHandler(_handleMethodCall);
    // 获取当前设置
    _getSettings();
    // 获取版本信息
    _getVersionInfo();
  }

  // 处理来自 Native 的消息
  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'messageFromNative':
        setState(() {
          _messageFromNative = call.arguments.toString();
        });
        return "Flutter 已收到消息";
      case 'updateSettings':
        // 处理设置更新
        final Map<String, dynamic> settings = call.arguments;
        setState(() {
          _themeMode = settings['themeMode'] ?? _themeMode;
          _textSize = settings['textSize'] ?? _textSize;
          _colorTheme = settings['colorTheme'] ?? _colorTheme;
          _showPinyin = settings['showPinyin'] ?? _showPinyin;
        });
        return "设置已更新";
      default:
        throw PlatformException(
          code: 'NotImplemented',
          message: '没有实现 ${call.method} 方法',
        );
    }
  }

  // 获取当前设置
  Future<void> _getSettings() async {
    try {
      final Map<String, dynamic> settings = await platform.invokeMethod('getSettings');
      setState(() {
        _themeMode = settings['themeMode'] ?? _themeMode;
        _textSize = settings['textSize'] ?? _textSize;
        _colorTheme = settings['colorTheme'] ?? _colorTheme;
        _showPinyin = settings['showPinyin'] ?? _showPinyin;
      });
    } on PlatformException catch (e) {
      debugPrint("获取设置失败: ${e.message}");
    }
  }

  // 获取版本信息
  Future<void> _getVersionInfo() async {
    try {
      final String version = await platform.invokeMethod('getVersionInfo');
      setState(() {
        _versionInfo = version;
      });
    } on PlatformException catch (e) {
      debugPrint("获取版本信息失败: ${e.message}");
    }
  }

  // 更新主题模式
  Future<void> _updateThemeMode(String mode) async {
    try {
      await platform.invokeMethod('setThemeMode', {'themeMode': mode});
      setState(() {
        _themeMode = mode;
        _showThemeDialog = false;
      });
    } on PlatformException catch (e) {
      debugPrint("设置主题模式失败: ${e.message}");
    }
  }

  // 更新文字大小
  Future<void> _updateTextSize(String size) async {
    try {
      await platform.invokeMethod('setTextSize', {'textSize': size});
      setState(() {
        _textSize = size;
        _showTextSizeDialog = false;
      });
    } on PlatformException catch (e) {
      debugPrint("设置文字大小失败: ${e.message}");
    }
  }

  // 更新主题颜色
  Future<void> _updateColorTheme(String theme) async {
    try {
      await platform.invokeMethod('setColorTheme', {'colorTheme': theme});
      setState(() {
        _colorTheme = theme;
        _showColorThemeDialog = false;
      });
    } on PlatformException catch (e) {
      debugPrint("设置主题颜色失败: ${e.message}");
    }
  }

  // 更新拼音显示设置
  Future<void> _updateShowPinyin(bool show) async {
    try {
      await platform.invokeMethod('setShowPinyin', {'showPinyin': show});
      setState(() {
        _showPinyin = show;
      });
    } on PlatformException catch (e) {
      debugPrint("设置拼音显示失败: ${e.message}");
    }
  }

  // 激活免责声明
  Future<void> _activateDisclaimer() async {
    try {
      await platform.invokeMethod('setShowCoefficient', {'showCoefficient': true});
      setState(() {
        _showDisclaimer = false;
        _showDarkRealmSnackbar = true;
      });
      // 显示Snackbar
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('您已进入黑暗领域')),
      );
    } on PlatformException catch (e) {
      debugPrint("激活免责声明失败: ${e.message}");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('个人设置'),
      ),
      body: Stack(
        children: [
          // 主内容区
          SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const SizedBox(height: 40),
                
                // 版本信息
                _buildVersionEntry(),
                
                const SizedBox(height: 18),
                
                // 文字大小设置
                _buildTextSizeCard(),
                
                const SizedBox(height: 18),
                
                // 主题模式设置
                _buildThemeModeCard(),
                
                const SizedBox(height: 18),
                
                // 主题颜色设置
                _buildColorThemeCard(),
                
                const SizedBox(height: 18),
                
                // 拼音显示设置
                _buildPinyinCard(),
                
                const SizedBox(height: 18),
                
                // 显示来自Native的消息
                if (_messageFromNative.isNotEmpty)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('来自Native的消息:'),
                          Text(_messageFromNative),
                        ],
                      ),
                    ),
                  ),
                
                const SizedBox(height: 40),
              ],
            ),
          ),
          
          // 对话框
          if (_showThemeDialog) _buildThemeDialog(),
          if (_showTextSizeDialog) _buildTextSizeDialog(),
          if (_showColorThemeDialog) _buildColorThemeDialog(),
          if (_showDisclaimer) _buildDisclaimerDialog(),
        ],
      ),
    );
  }

  // 版本信息卡片
  Widget _buildVersionEntry() {
    return GestureDetector(
      onTap: () {
        // 点击版本号的逻辑
        setState(() {
          _showDisclaimer = true;
        });
      },
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                _versionInfo.isEmpty ? '版本信息加载中...' : _versionInfo,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 文字大小设置卡片
  Widget _buildTextSizeCard() {
    return Card(
      child: InkWell(
        onTap: () {
          setState(() {
            _showTextSizeDialog = true;
          });
        },
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '文字大小',
                style: TextStyle(
                  color: Theme.of(context).primaryColor,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Row(
                children: [
                  Text(
                    _getTextSizeText(),
                    style: TextStyle(
                      color: Theme.of(context).hintColor,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.arrow_forward_ios,
                    size: 14,
                    color: Theme.of(context).hintColor,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 主题模式设置卡片
  Widget _buildThemeModeCard() {
    return Card(
      child: InkWell(
        onTap: () {
          setState(() {
            _showThemeDialog = true;
          });
        },
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '主题模式',
                style: TextStyle(
                  color: Theme.of(context).primaryColor,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Row(
                children: [
                  Text(
                    _getThemeModeText(),
                    style: TextStyle(
                      color: Theme.of(context).hintColor,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.arrow_forward_ios,
                    size: 14,
                    color: Theme.of(context).hintColor,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 主题颜色设置卡片
  Widget _buildColorThemeCard() {
    return Card(
      child: InkWell(
        onTap: () {
          setState(() {
            _showColorThemeDialog = true;
          });
        },
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '主题颜色',
                style: TextStyle(
                  color: Theme.of(context).primaryColor,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Row(
                children: [
                  Text(
                    _getColorThemeText(),
                    style: TextStyle(
                      color: Theme.of(context).hintColor,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.arrow_forward_ios,
                    size: 14,
                    color: Theme.of(context).hintColor,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 拼音显示设置卡片
  Widget _buildPinyinCard() {
    return Card(
      child: InkWell(
        onTap: () {
          _updateShowPinyin(!_showPinyin);
        },
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '百科卡片显示拼音及罗马音',
                style: TextStyle(
                  color: Theme.of(context).primaryColor,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Switch(
                value: _showPinyin,
                onChanged: (value) {
                  _updateShowPinyin(value);
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  // 主题模式对话框
  Widget _buildThemeDialog() {
    return AlertDialog(
      title: const Text('选择主题模式'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildThemeOption('FOLLOW_SYSTEM', '跟随系统'),
          _buildThemeOption('LIGHT', '浅色'),
          _buildThemeOption('DARK', '深色'),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            setState(() {
              _showThemeDialog = false;
            });
          },
          child: const Text('关闭'),
        )
      ],
    );
  }

  // 文字大小对话框
  Widget _buildTextSizeDialog() {
    return AlertDialog(
      title: const Text('选择文字大小'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildTextSizeOption('FOLLOW_SYSTEM', '跟随系统'),
          _buildTextSizeOption('SMALL', '小号'),
          _buildTextSizeOption('MEDIUM', '中号'),
          _buildTextSizeOption('LARGE', '大号'),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            setState(() {
              _showTextSizeDialog = false;
            });
          },
          child: const Text('关闭'),
        )
      ],
    );
  }

  // 主题颜色对话框
  Widget _buildColorThemeDialog() {
    // 自定义顺序列表
    final customOrder = [
      'MATERIAL_YOU',
      'PURPLE',
      'ORANGE',
      'DEEP_BLUE',
      'LIGHT_BLUE',
      'ROSE',
      'YELLOW',
      'PINK',
      'GREEN',
      'WHITE',
    ];

    return AlertDialog(
      title: const Text('选择主题颜色'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: customOrder.map((theme) => _buildColorThemeOption(theme)).toList(),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () {
            setState(() {
              _showColorThemeDialog = false;
            });
          },
          child: const Text('关闭'),
        )
      ],
    );
  }

  // 免责声明对话框
  Widget _buildDisclaimerDialog() {
    // 倒计时效果
    Future.delayed(const Duration(seconds: 1), () {
      if (_remainingTime > 0 && _showDisclaimer) {
        setState(() {
          _remainingTime--;
        });
      }
    });

    return AlertDialog(
      title: const Text('免责声明'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text(
            '警告：您即将进入黑暗领域，这里的功能可能会导致不可预知的后果。',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          const Text(
            '继续操作表示您已了解并接受所有风险。',
            style: TextStyle(fontSize: 12),
          ),
          const SizedBox(height: 16),
          Text(
            '确认按钮将在 $_remainingTime 秒后启用',
            style: const TextStyle(fontSize: 12, color: Colors.red),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            setState(() {
              _showDisclaimer = false;
              _remainingTime = 7; // 重置倒计时
            });
          },
          child: const Text('取消'),
        ),
        TextButton(
          onPressed: _remainingTime <= 0 ? _activateDisclaimer : null,
          child: const Text('确认'),
        ),
      ],
    );
  }

  // 主题选项构建
  Widget _buildThemeOption(String mode, String label) {
    return RadioListTile<String>(
      title: Text(label),
      value: mode,
      groupValue: _themeMode,
      onChanged: (value) {
        if (value != null) {
          _updateThemeMode(value);
        }
      },
    );
  }

  // 文字大小选项构建
  Widget _buildTextSizeOption(String size, String label) {
    return RadioListTile<String>(
      title: Text(label),
      value: size,
      groupValue: _textSize,
      onChanged: (value) {
        if (value != null) {
          _updateTextSize(value);
        }
      },
    );
  }

  // 主题颜色选项构建
  Widget _buildColorThemeOption(String theme) {
    return RadioListTile<String>(
      title: Text(_getColorThemeText(theme)),
      value: theme,
      groupValue: _colorTheme,
      onChanged: (value) {
        if (value != null) {
          _updateColorTheme(value);
        }
      },
    );
  }

  // 获取主题模式文本
  String _getThemeModeText() {
    switch (_themeMode) {
      case 'FOLLOW_SYSTEM':
        return '跟随系统';
      case 'LIGHT':
        return '浅色';
      case 'DARK':
        return '深色';
      default:
        return '跟随系统';
    }
  }

  // 获取文字大小文本
  String _getTextSizeText() {
    switch (_textSize) {
      case 'FOLLOW_SYSTEM':
        return '跟随系统';
      case 'SMALL':
        return '小号';
      case 'MEDIUM':
        return '中号';
      case 'LARGE':
        return '大号';
      default:
        return '跟随系统';
    }
  }

  // 获取主题颜色文本
  String _getColorThemeText([String? theme]) {
    final currentTheme = theme ?? _colorTheme;
    switch (currentTheme) {
      case 'MATERIAL_YOU':
        return 'Material You';
      case 'PURPLE':
        return '奇迹紫';
      case 'ORANGE':
        return '奇迹橙';
      case 'DEEP_BLUE':
        return '深蓝';
      case 'LIGHT_BLUE':
        return '浅蓝';
      case 'ROSE':
        return '丹红';
      case 'YELLOW':
        return '亮黄';
      case 'PINK':
        return '轻粉';
      case 'GREEN':
        return '天绿';
      case 'WHITE':
        return '霜灰';
      default:
        return 'Material You';
    }
  }
}