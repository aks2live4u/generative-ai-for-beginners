import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../state/providers.dart';

class AddCollectionSheet extends ConsumerStatefulWidget {
  const AddCollectionSheet({super.key});

  @override
  ConsumerState<AddCollectionSheet> createState() => _AddCollectionSheetState();
}

class _AddCollectionSheetState extends ConsumerState<AddCollectionSheet> {
  final _nameController = TextEditingController();
  final _descController = TextEditingController();

  Future<void> _save() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) return;
    await ref.read(collectionRepositoryProvider).create(
          name,
          description: _descController.text.trim().isEmpty ? null : _descController.text.trim(),
        );
    ref.invalidate(collectionsListProvider);
    if (mounted) Navigator.of(context).pop();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        bottom: 20 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('New Collection', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextField(
            controller: _nameController,
            autofocus: true,
            decoration: const InputDecoration(hintText: 'Collection name'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _descController,
            decoration: const InputDecoration(hintText: 'Description (optional)'),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(onPressed: _save, child: const Text('Create')),
          ),
        ],
      ),
    );
  }
}
