import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class EncyclopediaScreen extends StatefulWidget {
  const EncyclopediaScreen({super.key});

  @override
  State<EncyclopediaScreen> createState() => _EncyclopediaScreenState();
}

class _EncyclopediaScreenState extends State<EncyclopediaScreen> {
  static const platform = MethodChannel('com.lovelive.dreamycolor/encyclopedia');
  
  List<dynamic> characters = [];
  List<dynamic> voiceActors = [];
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final result = await platform.invokeMethod('getEncyclopediaData').timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw PlatformException(
            code: 'TIMEOUT',
            message: 'Request timed out',
          );
        },
      );
      setState(() {
        characters = result['characters'] ?? [];
        voiceActors = result['voiceActors'] ?? [];
        isLoading = false;
      });
    } on PlatformException catch (e) {
      debugPrint("Failed to get data: '${e.message}'.");
      setState(() {
        isLoading = false;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load data: ${e.message}')),
        );
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Encyclopedia'),
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : DefaultTabController(
              length: 2,
              child: Column(
                children: [
                  const TabBar(
                    tabs: [
                      Tab(text: 'Characters'),
                      Tab(text: 'Voice Actors'),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        _buildCharacterList(),
                        _buildVoiceActorList(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildCharacterList() {
    return ListView.builder(
      itemCount: characters.length,
      itemBuilder: (context, index) {
        final character = characters[index];
        return ListTile(
          leading: CircleAvatar(
            child: character['imageRes'] != null
                ? Image.asset(character['imageRes'])
                : const Icon(Icons.person),
          ),
          title: Text(character['name'] ?? ''),
          subtitle: Text(character['japaneseName'] ?? ''),
          onTap: () {},
        );
      },
    );
  }

  Widget _buildVoiceActorList() {
    return ListView.builder(
      itemCount: voiceActors.length,
      itemBuilder: (context, index) {
        final voiceActor = voiceActors[index];
        return ListTile(
          leading: CircleAvatar(
            child: voiceActor['imageRes'] != null
                ? Image.asset(voiceActor['imageRes'])
                : const Icon(Icons.mic),
          ),
          title: Text(voiceActor['name'] ?? ''),
          subtitle: Text(voiceActor['agency'] ?? ''),
          onTap: () {},
        );
      },
    );
  }
}