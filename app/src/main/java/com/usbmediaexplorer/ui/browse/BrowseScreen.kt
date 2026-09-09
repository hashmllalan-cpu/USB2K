@@
     fun openWith(item: DocItem) {
-        val external: android.net.Uri =
-            container.docRepository.externalUri(item.node)
-                ?: run {
-                    toast(context.getString(R.string.msg_no_app_to_open))
-                    return
-                }
-        val opened = Intents.openWith(context, item.node, external)
+        // externalUri may return null or be of an unclear type; normalise safely to Uri.
+        val raw = container.docRepository.externalUri(item.node)
+        val externalUri = when (raw) {
+            is android.net.Uri -> raw
+            is String -> android.net.Uri.parse(raw)
+            null -> {
+                toast(context.getString(R.string.msg_no_app_to_open))
+                return
+            }
+            else -> android.net.Uri.parse(raw.toString())
+        }
+        val opened = Intents.openWith(context, item.node, externalUri)
         if (!opened) toast(context.getString(R.string.msg_no_app_to_open))
     }
@@
     private fun shareNodes(
         context: android.content.Context,
         viewModel: BrowseViewModel,
         nodes: List<DocNode>,
         toast: (String) -> Unit,
     ) {
-    val uris = viewModel.shareUris(nodes)
-    if (uris.isEmpty() || !Intents.share(context, uris, nodes.firstOrNull()?.mimeType)) {
-        toast(context.getString(R.string.msg_shared_failed))
-    }
+    val uris = viewModel.shareUris(nodes)
+    if (uris.isEmpty() || !Intents.share(context, uris, nodes.firstOrNull()?.mimeType)) {
+        toast(context.getString(R.string.msg_shared_failed))
+    }
     }
*** End Patch